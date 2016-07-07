package com.aquivalabs.force.ant.dsl

import com.sforce.soap.metadata.RunTestsResult
import java.io.File


interface Reporter<out T> {
    fun createReport(result: RunTestsResult): T
}

fun <T> Reporter<T>.saveReportToFile(result: RunTestsResult, directory: File, fileName: String): File {
    val reportFile = File("${directory.absolutePath}${File.separator}$fileName")
    reportFile.createNewFile()
    reportFile.writeText(this.createReport(result).toString())
    return reportFile
}