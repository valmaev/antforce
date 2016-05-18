package com.newmarket.force.ant

import com.salesforce.ant.SFDCAntTask
import com.sforce.soap.apex.ExecuteAnonymousResult
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Location


class ExecuteAnonymousApexTask : SFDCAntTask() {
    private var _code = ""
    var expandProperties: Boolean = true

    fun addText(code: String) {
        _code = code
    }

    override fun execute() {
        var trimmedCode = _code.trim()
        if (trimmedCode.isNullOrBlank()) {
            log("Apex code wasn't specified")
            return
        }

        if (expandProperties)
            trimmedCode = getProject().replaceProperties(trimmedCode)

        val result = apexConnection.executeAnonymous(trimmedCode)
        processResult(result)
    }

    internal fun processResult(result: ExecuteAnonymousResult) = when {
        !result.compiled -> throw BuildException(
            result.toCompilerError(),
            getBuildFileErrorLocation(result))
        !result.success -> throw BuildException(
            result.toExceptionMessage(),
            getBuildFileErrorLocation(result))
        else -> log("Apex code was successfully executed")
    }

    private fun ExecuteAnonymousResult.toCompilerError() =
        "*********** APEX EXECUTION FAILED ***********${System.lineSeparator()}" +
            "Error: ${this.compileProblem}" +
            " (line ${this.line}, column ${this.column})${System.lineSeparator()}" //+

    private fun ExecuteAnonymousResult.toExceptionMessage() =
        "*********** APEX EXECUTION FAILED ***********${System.lineSeparator()}" +
            "${this.exceptionMessage}${System.lineSeparator()}" +
            "Stack trace: ${this.exceptionStackTrace}"

    private fun getBuildFileErrorLocation(result: ExecuteAnonymousResult): Location {
        val lines = _code.lines()
        val linesCount = lines.count()
        val lineOffset = lines.takeWhile { it.isBlank() }.count()
        val codeLineNumber = if (result.line > linesCount) linesCount else (result.line - 1)
        val lineNumber = getLocation().lineNumber + lineOffset + codeLineNumber
        return Location(getLocation().fileName, lineNumber, result.column)
    }
}