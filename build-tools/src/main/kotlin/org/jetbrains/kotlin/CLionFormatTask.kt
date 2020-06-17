/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.*

open class CLionFormatCheckTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    var files: Iterable<File> = ArrayList()

    private val outputDir = File(project.buildDir, "clionFormat")
    private val root = project.file(".")

    override fun configure(closure: Closure<Any>): Task {
        super.configure(closure)
        for (file in files) {
            val name = "copy_" + file.toRelativeString(root).replace('/', '_').replace('\\', '_')
            val outputFile = File(outputDir, file.toRelativeString(root)).parentFile
            (project.tasks.create(name, Copy::class.java) as Copy).into(outputFile).from(file)
            dependsOn(name)
        }
        return this
    }

    @TaskAction
    fun run() {
        val plugin = project.convention.getPlugin(ExecCLionFormat::class.java)

        plugin.exec(files.map { File(outputDir, it.toRelativeString(root)) })

        var failing = false
        val failingDiff = ByteArrayOutputStream()
        for (file in files) {
            val outputFile = File(outputDir, file.toRelativeString(root))
            val diffOutputStream = ByteArrayOutputStream()
            val diffResult = project.exec {
                it.commandLine = listOf("diff", "-u", file.absolutePath, outputFile.absolutePath)
                it.standardOutput = diffOutputStream
                it.isIgnoreExitValue = true
            }
            if (diffResult.exitValue != 0) {
                failing = true
                diffOutputStream.writeTo(failingDiff)
            }
        }

        if (failing) {
            throw GradleException("Code is not formatted:\n$failingDiff")
        }
    }
}

open class CLionFormatFixTask : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    var files: Iterable<File> = ArrayList()

    @TaskAction
    fun run() {
        val plugin = project.convention.getPlugin(ExecCLionFormat::class.java)
        plugin.exec(files)
    }
}
