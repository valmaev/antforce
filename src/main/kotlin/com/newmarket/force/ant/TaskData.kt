package com.newmarket.force.ant

import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.Resource
import org.apache.tools.ant.types.resources.Resources


object Constants {
    const val APEX_CLASS_FILE_EXTENSION = ".cls"
    const val NAMESPACE_SEPARATOR = "."
}

class BatchTest(val project: Project) {
    final val resources = Resources()
    var namespace = ""
    private val prefix: String
        get() =
            if (namespace.isBlank()) ""
            else "${namespace.trim()}${Constants.NAMESPACE_SEPARATOR}"

    fun addFileSet(fileSet: FileSet) {
        fileSet.project = project
        resources.add(fileSet)
    }

    fun getFileNames(): List<String> =
        resources.filter {
            it.isExists && it.name.endsWith(Constants.APEX_CLASS_FILE_EXTENSION)
        }.map {
            prefix + getTestClassNameFrom(it)
        }

    private fun getTestClassNameFrom(resource: Resource) =
        resource.name.substring(
            0,
            resource.name.length - Constants.APEX_CLASS_FILE_EXTENSION.length)
}

data class JUnitReport(var file: String = "TEST-Apex.xml", var suiteName: String = "Apex")
data class CoberturaReport(var file: String = "Apex-Coverage.xml")
data class HtmlCoverageReport(var file: String = "Apex-Coverage.html")