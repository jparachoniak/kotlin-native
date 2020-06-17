/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import java.io.File
import org.gradle.api.Project

class ExecCLionFormat(private val project: Project) {
    fun exec(targets: Iterable<File>) {
        project.exec {
            it.executable = "open"
            it.args = listOf("-na", "CLion.app", "--args", "format" , "-s", File(project.rootProject.projectDir, "CLionFormat.xml").absolutePath) + targets.map { it.absolutePath }
        }
    }
}
