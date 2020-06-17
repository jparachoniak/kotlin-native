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

    @TaskAction
    fun run() {
        val plugin = project.convention.getPlugin(ExecCLionFormat::class.java)
        plugin.exec(files)
        /*
        fun check(target: String) {
            val outputStream = ByteArrayOutputStream()
            project.exec {
                it.executable = executable()
                it.args = listOf(target)
                it.standardOutput = outputStream
            }.assertNormalExitValue()
            val diffOutputStream = ByteArrayOutputStream()
            val diffResult = project.exec {
                it.commandLine = listOf("diff", "-u", target, "-")
                it.standardInput = ByteArrayInputStream(outputStream.toByteArray())
                it.standardOutput = diffOutputStream
                it.isIgnoreExitValue = true
            }
            if (diffResult.exitValue == 0) {
                return
            }
            throw GradleException("$target is not formatted:\n$diffOutputStream")
        }
        */
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
