package org.jetbrains.kotlin.native.interop.indexer

import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.Type.getArgumentTypes
import org.jetbrains.org.objectweb.asm.Type.getReturnType

/**
 *  Parses an ASM ClassNode and builds an ObjCClass
 *
 *  @param classNode An ASM classNode
 */
class J2ObjCParser(val classNode: ClassNode) {

  /**
   * Builds and returns an ObjCClass implementation with associated methods
   *
   * @return A generated ObjCClass implementation
   */
  fun buildClass(): ObjCClassImpl {
    val generatedClass = ObjCClassImpl(
      name = classNode.name,
      isForwardDeclaration = false,
      binaryName = null,
      location = Location(HeaderId("")) // Leaving headerId empty for now
    )
    generatedClass.methods.addAll(buildClassMethods())
    generatedClass.baseClass = ObjCClassImpl(
      name = "NSObject",
      binaryName = null,
      isForwardDeclaration = false,
      location = Location(headerId = HeaderId("usr/include/objc/NSObject.h"))
    )
    return generatedClass
  }

  /**
   * Parses ASM methods and builds a list of ObjCMethods
   *
   * @return A list of ObjCMethods
   */
   private fun buildClassMethods(): List<ObjCMethod> {
    val methods = mutableListOf<ObjCMethod>()
    for (method in classNode.methods) {
      var selector = method.name
      if (!method.parameters.isNullOrEmpty() && method.parameters.size > 1) {
        for (i in 1 until method.parameters.size) {
          selector += ":" + method.parameters.get(i).name
        }
        selector += ":"
      }
      val generatedMethod = ObjCMethod(
        selector = if (!method.parameters.isNullOrEmpty() && method.parameters.size == 1) method.name + ":" else if (!method.parameters.isNullOrEmpty() && method.parameters.size > 1) selector  else method.name,
        encoding = "[]", //TODO: Implement encoding properly
        parameters = parseMethodParameters(method),
        returnType = parseMethodReturnType(method),
        isVariadic = false,
        isClass = true,
        nsConsumesSelf = false,
        nsReturnsRetained = false,
        isOptional = false,
        isInit = false,
        isExplicitlyDesignatedInitializer = false
      )
      methods.add(generatedMethod)
      parseMethodParameters(method)
    }
    return methods
  }

  /**
   * Parses an ASM method's parameters and returns a list of Kotlin parameters
   *
   * @param method ASM MethodNode
   * @return A list of Kotlin parameters with associated types/names
   */

  private fun parseMethodParameters(method: MethodNode): List<Parameter> {
    val methodParameters = mutableListOf<Parameter>()
    val parameterTypes = getArgumentTypes(method.desc)

    for (i in 0 until parameterTypes.size) {
      when (parameterTypes.get(i).className) {
        // Java byte type not implemented currently
        "boolean" -> methodParameters.add(Parameter(name = method.parameters.get(i).name, type = ObjCBoolType, nsConsumed = false))
        "char" ->  methodParameters.add(Parameter(name = method.parameters.get(i).name, type = CharType, nsConsumed = false))
        "double" ->  methodParameters.add(Parameter(name = method.parameters.get(i).name, type = FloatingType(size = 8, spelling = "double"), nsConsumed = false))
        "float" ->  methodParameters.add(Parameter(name = method.parameters.get(i).name, type = FloatingType(size = 4, spelling = "float"), nsConsumed = false))
        "int" -> methodParameters.add(Parameter(name = method.parameters.get(i).name, type = IntegerType(size = 4, spelling = "int", isSigned = true), nsConsumed = false))
        "long" ->  methodParameters.add(Parameter(name = method.parameters.get(i).name, type = IntegerType(size = 8, spelling = "long", isSigned = true), nsConsumed = false))
        "short" ->  methodParameters.add(Parameter(name = method.parameters.get(i).name, type = IntegerType(size = 2, spelling = "short", isSigned = true), nsConsumed = false))
        else -> {
          throw NotImplementedError("Have not implemented this type yet: " + parameterTypes.get(i).className)
        }
      }
    }
    return methodParameters
  }

  /**
   * Parses an ASM method's return type and returns a Kotlin type
   *
   * @param method ASM MethodNode
   * @return Type corresponding to method's return type
   */
  private fun parseMethodReturnType(method: MethodNode): Type {
    val returnType = getReturnType(method.desc)

    when (returnType.className) {
      // Java byte type not implemented currently
      "boolean" -> return ObjCBoolType
      "char" -> return CharType
      "double" -> return FloatingType(size = 8, spelling = "double")
      "float" -> return FloatingType(size = 4, spelling = "float")
      "int" -> return IntegerType(size = 4, spelling = "int", isSigned = true)
      "long" -> return IntegerType(size = 8, spelling = "long", isSigned = true)
      "short" -> return IntegerType(size = 2, spelling = "short", isSigned = true)
      "void" -> return VoidType
      else -> {
        throw NotImplementedError("Have not implemented this type yet: " + returnType.className)
      }
    }
  }
}