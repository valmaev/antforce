package com.aquivalabs.force.ant

import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Resource
import org.apache.tools.ant.types.ZipFileSet
import org.apache.tools.ant.types.resources.Resources
import java.io.File

class BatchTest(val project: Project) {
    val resources = Resources()
    var namespace = ""
    private val prefix: String
        get() = if (namespace.isBlank()) "" else "${namespace.trim()}$NAMESPACE_SEPARATOR"

    fun addFileSet(fileSet: FileSet) {
        fileSet.project = project
        resources.add(fileSet)
    }

    fun addZipFileSet(zipFileSet: ZipFileSet) = addFileSet(zipFileSet)

    fun getFileNames(): Set<String> = resources
        .filter { it.isExists && it.name.endsWith(APEX_CLASS_FILE_EXTENSION) }
        .map { prefix + getTestClassNameFrom(it) }
        .toSet()

    private fun getTestClassNameFrom(resource: Resource) = resource.name
        .split(File.pathSeparator).last()
        .split(File.separator).last()
        .dropLast(APEX_CLASS_FILE_EXTENSION.length)
}