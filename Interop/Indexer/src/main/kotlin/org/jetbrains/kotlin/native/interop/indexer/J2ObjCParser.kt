package org.jetbrains.kotlin.native.interop.indexer

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type.getArgumentTypes
import org.jetbrains.org.objectweb.asm.Type.getReturnType
import java.util.jar.JarFile

/**
 * Visits a Java class and builds an ObjCClass
 */
class J2ObjCParser: ClassVisitor(Opcodes.ASM7) {

  var className = ""
  var access = 0;
  var interfaceNames = mutableListOf<String>()
  var superName = ""
  val methodDescriptors = mutableListOf<MethodDescriptor>()
  val parameterNames = mutableListOf<List<String>>()
  var isNestedClass = false

  override fun visit(version: Int,
                     access: Int,
                     name: String,
                     signature: String?,
                     superName: String,
                     interfaces: Array<out String>) {
    className = name
    this.access = access
    interfaceNames.addAll(interfaces)
    this.superName = superName
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(access: Int,
                           name: String,
                           descriptor: String,
                           signature: String?,
                           exceptions: Array<out String>?): MethodVisitor? {

    val methodBuilder = MethodBuilder(parameterNames)
    methodDescriptors.add(MethodDescriptor(name, descriptor, access))
    return methodBuilder
  }

  override fun visitNestHost(nestHost: String?) {
    isNestedClass = (nestHost != null)
    super.visitNestHost(nestHost)
  }

  /**
   * Generates an ObjCClass out of data collected while visiting
   *
   * @return An ObjCClass that matches a Java class
   */
  fun buildClass(): ObjCClass {
    val methods = (methodDescriptors zip parameterNames).map { buildClassMethod(it.first, it.second)}
    val generatedClass = ObjCClassImpl(
      name = if (isNestedClass) className.split('/').last().replace('$', '_') else className.split('/').last(),
      isForwardDeclaration = false,
      binaryName = buildJ2objcClassName(className,'/').replace('$', '_'),
      location = Location(HeaderId("")) // Leaving headerId empty for now.
    )
    generatedClass.methods.addAll(methods)
    generatedClass.protocols.addAll(interfaceNames.map{ObjCProtocolImpl(
      name = buildJ2objcClassName(it,'/'),
      isForwardDeclaration = true,
      location = Location(HeaderId(""))
    )})
    if (superName == "java/lang/Object") {
      generatedClass.baseClass = ObjCClassImpl(
        name = "NSObject",
        binaryName = null,
        isForwardDeclaration = false,
        location = Location(headerId = HeaderId("usr/include/objc/NSObject.h"))
      )
    } else {
      generatedClass.baseClass = ObjCClassImpl(
        name = superName.split('/').last(),
        binaryName = buildJ2objcClassName(superName, '/'),
        isForwardDeclaration = false,
        location = Location(headerId = HeaderId(""))
      )
    }
    generatedClass.properties.addAll(parseForProperties(methods, generatedClass))
    return generatedClass
  }

  /**
   * Creates an ObjCProtocol out of parsed Java interface
   *
   * @return Generated ObjCProtocol
   */
  fun buildInterface(): ObjCProtocol {
    val methods = (methodDescriptors zip parameterNames).map { buildClassMethod(it.first, it.second)}

    val generatedProtocol = ObjCProtocolImpl(
      name = buildJ2objcClassName(className,'/'),
      isForwardDeclaration = false,
      location = Location(HeaderId(""))
    )
    generatedProtocol.methods.addAll(methods)
    return generatedProtocol
  }

  /**
   * Creates a ObjCMethod out of method data and parameter names
   *
   * @param methodDescriptor A methodDescriptor containing the name, descriptor string, and access level of method
   * @param paramNames List of parameter names of this method taken from MethodBuilder
   * @return An ObjCMethod built from the descriptor
   */
  private fun buildClassMethod(methodDescriptor: MethodDescriptor, paramNames: List<String>): ObjCMethod {
    val methodParameters = parseMethodParameters(methodDescriptor.descriptor, paramNames)
    if (methodDescriptor.isConstructor) {
      return ObjCMethod(
        selector = buildJ2objcMethodName("init", methodDescriptor.descriptor),
        encoding = "[]",
        parameters = methodParameters,
        returnType = ObjCInstanceType(nullability = ObjCPointer.Nullability.Unspecified),
        isVariadic = false,
        isClass = false,
        nsConsumesSelf = true,
        nsReturnsRetained = true,
        isOptional = false,
        isInit = true,
        isExplicitlyDesignatedInitializer = false)
    } else {
      val selector = buildJ2objcMethodName(methodDescriptor.name, methodDescriptor.descriptor)
      val methodReturnType = parseMethodReturnType(methodDescriptor.descriptor)
      return ObjCMethod(
        selector = selector,
        encoding = "[]", //TODO: Implement encoding properly
        parameters = methodParameters,
        returnType = methodReturnType,
        isVariadic = false,
        isClass = methodDescriptor.access == Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, // TODO: Currently only handles Public instance and Public static methods, true when static, false when instance.
        nsConsumesSelf = false,
        nsReturnsRetained = false,
        isOptional = false,
        isInit = false,
        isExplicitlyDesignatedInitializer = false,
        nameOverride = selector.split("With").first())
    }
  }

  private fun buildJ2objcMethodName(methodName: String, methodDesc: String): String {
    val outputMethodName = StringBuilder(methodName)
    val typeNames = getArgumentTypes(methodDesc).map{
      if(it.className == "java.lang.String") "NSString" else
        if (it.className == "java.lang.Number") "NSNumber"  else
          buildJ2objcClassName(it.className,'.').capitalize()}

    if (typeNames.size > 0) {
      outputMethodName.append("With" + typeNames.get(0) + ":")
    }
    typeNames.drop(1).forEach{outputMethodName.append("with" + it + ":")}
    return outputMethodName.toString()
  }

  private fun buildJ2objcClassName(className: String, delimiter: Char): String {
    return className.split(delimiter).reduce{ acc, string -> acc.capitalize() + string.capitalize()}
  }

  /**
   * Parses the methods in the class to find getters/setters and generates properties accordingly
   *
   * TODO: This is a very simple and fragile check, the source java class must have the getters/setters/is methods defined
   * TODO: and parses these to figure out what properties to create.
   *
   * @param methods List of ObjCMethods in the class
   * @param containingClass ObjCClass that is being parsed for properties
   * @return List of ObjCProperty based off of the getters/setters in class
   */
  private fun parseForProperties(methods: List<ObjCMethod>, containingClass: ObjCClass): List<ObjCProperty> {
    val getters =
      methods.filter {
        ((it.selector.startsWith("get")) || ((it.selector.startsWith("is")) && it.getReturnType(
          containingClass) == ObjCBoolType)) && it.parameters.isEmpty() && it.getReturnType(containingClass) != VoidType
      }

    return getters.map{ getter ->
      val varName = getter.selector.substring(3)
      val setter = methods.find { setter ->
        setter.selector.startsWith("set${varName}")
        && setter.parameters.size == 1
        && setter.getReturnType(containingClass) == VoidType
        && (getter.getReturnType(containingClass) == setter.parameters.get(0).type)
        && getter.isClass == setter.isClass // Checks if getter is static, then setter must be static
      }
      ObjCProperty(
        getter = getter,
        name = varName.decapitalize(),
        setter = setter
      )
    }
  }

  /**
   * Parses an ASM method's parameters and returns a list of Kotlin parameters
   *
   * @param methodDesc A methodDescriptor containing the name, descriptor string, and access level of method
   * @param paramNames List of parameter names of methods from [methodDesc]
   * @return A list of Kotlin parameters with associated types/names
   */

  private fun parseMethodParameters(methodDesc: String, paramNames: List<String>): List<Parameter> {
    val parameterTypes = getArgumentTypes(methodDesc)
    return parameterTypes.mapIndexed { i, paramType ->
      Parameter(name = paramNames.get(i), type = parseType(paramType), nsConsumed = false)
    }
  }

  /**
   * Parses an ASM method's return type and returns a Kotlin type
   *
   * @param method ASM method descriptor
   * @return Type corresponding to method's return type
   */
  private fun parseMethodReturnType(methodDesc: String): Type {
    return parseType(getReturnType(methodDesc))
  }

  /**
   * Helper function to parse ASM types and return a Kotlin type
   *
   * @param type ASM type
   * @return Kotlin type
   */
  private fun parseType(type: org.jetbrains.org.objectweb.asm.Type): Type {
    return when (type.className) {
      "boolean" -> ObjCBoolType
      "byte" -> IntegerType(size = 1, spelling = "byte", isSigned = true)
      "char" -> CharType
      "double" -> FloatingType(size = 8, spelling = "double")
      "float" -> FloatingType(size = 4, spelling = "float")
      "int" -> IntegerType(size = 4, spelling = "int", isSigned = true)
      "long" -> IntegerType(size = 8, spelling = "long", isSigned = true)
      "short" -> IntegerType(size = 2, spelling = "short", isSigned = true)
      "void" -> VoidType
      "java.lang.String" -> ObjCObjectPointer(
        ObjCClassImpl(
          name = "NSString",
          isForwardDeclaration = false,
          binaryName = null,
          location = Location(headerId = HeaderId("System/Library/Frameworks/Foundation.framework/Verisons/C/Headers/NSString.h"))
        ),
        ObjCPointer.Nullability.valueOf("Unspecified"),
        listOf()
      )
      "java.lang.Number" -> ObjCObjectPointer(
        ObjCClassImpl(
          name = "NSNumber",
          isForwardDeclaration = false,
          binaryName = null,
          location = Location(headerId = HeaderId("System/Library/Frameworks/Foundation.framework/Versions/C/Headers/NSValue.h"))
        ),
        ObjCPointer.Nullability.valueOf("Unspecified"),
        listOf()
      )
      else -> if (interfaceNames.contains(type.internalName))
        ObjCIdType(nullability = ObjCPointer.Nullability.valueOf("Unspecified"),
                   protocols = listOf<ObjCProtocol>(
                     ObjCProtocolImpl(isForwardDeclaration = false,
                                      location = Location(headerId = HeaderId("")),
                                      name = buildJ2objcClassName(type.className,'.')
                     )))
      else ObjCObjectPointer(
        ObjCClassImpl(
          name = type.className.split('.').last(),
          isForwardDeclaration = false,
          binaryName = null,
          location = Location(headerId = HeaderId(""))
        ),
        ObjCPointer.Nullability.valueOf("Unspecified"),
        listOf()
      )
    }
  }

}

data class MethodDescriptor(val name: String, val descriptor: String, val access: Int) {
  val isConstructor: Boolean = (name == "<init>")
}

/**
 * Visits methods from J2ObjCParser and collects parameter names
 *
 * @param paramNames List of parameter name strings to be added to
 */
private class MethodBuilder(val paramNames: MutableCollection<List<String>>): MethodVisitor(Opcodes.ASM7) {
  val params = mutableListOf<String>()

  override fun visitParameter(name: String, access: Int) {
    params.add(name)
  }

  override fun visitEnd() {
    paramNames.add(params)
    super.visitEnd()
  }
}

/**
 * Helper function to load a jar file and create and populate a J2ObjC NativeIndex and return an IndexerResult with it
 *
 * @param jarFiles List of Java .jar file locations to be loaded
 * @return A IndexerResult with a J2ObjCNativeIndex that is populated from jarFiles
 */
fun buildJ2ObjcNativeIndex(jarFiles: List<String>): IndexerResult {
  val jarFile = JarFile(jarFiles[0])

  val j2objcClasses = mutableListOf<ObjCClass>()
  var j2objcProtocols = mutableListOf<ObjCProtocol>()

  jarFile.use {it.entries().iterator().asSequence().filter{it.name.endsWith(".class")}.forEach{
    val parser = J2ObjCParser()
    ClassReader(jarFile.getInputStream(it).readBytes()).accept(parser, 0)
    if (parser.access and Opcodes.ACC_INTERFACE != 0)
      j2objcProtocols.add(parser.buildInterface())
    else
      j2objcClasses.add(parser.buildClass())
  }}

  return IndexerResult(J2ObjCNativeIndex(j2objcClasses,j2objcProtocols), CompilationWithPCH(emptyList<String>(), Language.J2ObjC))
}
