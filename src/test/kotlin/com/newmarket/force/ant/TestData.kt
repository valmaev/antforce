package com.newmarket.force.ant

import org.apache.tools.ant.types.FileSet
import java.io.File

fun createFileSet(directory: File, fileNames: Iterable<String>): FileSet {
    val fileSet = FileSet()
    fileSet.dir = directory
    fileNames.forEach {
        val file = File("${directory.path}${File.separator}$it")
        file.createNewFile()
    }
    return fileSet
}

fun createFileSet(directory: File, vararg filesNames: String) =
    createFileSet(directory, filesNames.asIterable())