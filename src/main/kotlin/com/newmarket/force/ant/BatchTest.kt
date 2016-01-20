package com.newmarket.force.ant

import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet
import org.apache.tools.ant.types.resources.Resources

public class BatchTest(val project: Project) {
    companion object {
        const val APEX_CLASS_FILE_EXTENSION = ".cls"
        const val NAMESPACE_SEPARATOR = "."
    }

    public final val resources = Resources()
    public var namespace = ""

    public fun addFileSet(fileSet: FileSet) {
        fileSet.project = project
        resources.add(fileSet)
    }

    public fun getFileNames(): List<String> =
        resources.filter {
            it.isExists && it.name.endsWith(APEX_CLASS_FILE_EXTENSION)
        }.map {
            val prefix = if (namespace.isBlank()) "" else "${namespace.trim()}$NAMESPACE_SEPARATOR"
            val testClassName = it.name.substring(0, it.name.length - APEX_CLASS_FILE_EXTENSION.length)
            prefix + testClassName
        }
}