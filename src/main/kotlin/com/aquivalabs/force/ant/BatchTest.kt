package com.aquivalabs.force.ant

import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Resource
import org.apache.tools.ant.types.resources.Resources

class BatchTest(val project: Project) {
    val resources = Resources()
    var namespace = ""
    private val prefix: String
        get() = if (namespace.isBlank()) "" else "${namespace.trim()}$NAMESPACE_SEPARATOR"

    fun addFileSet(fileSet: FileSet) {
        fileSet.project = project
        resources.add(fileSet)
    }

    fun getFileNames(): List<String> = resources
        .filter { it.isExists && it.name.endsWith(APEX_CLASS_FILE_EXTENSION) }
        .map { prefix + getTestClassNameFrom(it) }

    private fun getTestClassNameFrom(resource: Resource) =
        resource.name.substring(0, resource.name.length - APEX_CLASS_FILE_EXTENSION.length)
}