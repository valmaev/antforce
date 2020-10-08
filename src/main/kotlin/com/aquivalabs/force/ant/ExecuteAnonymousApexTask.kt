package com.aquivalabs.force.ant

import com.salesforce.ant.SFDCAntTask
import com.sforce.soap.apex.ExecuteAnonymousResult
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Location


class ExecuteAnonymousApexTask : SFDCAntTask() {
    companion object {
        const val ERROR_HEADER = "*********** APEX EXECUTION FAILED ***********"
    }

    private var _code = ""
    private var expandProperties: Boolean = true

    fun addText(code: String) {
        _code = code
    }

    override fun execute() {
        var trimmedCode = _code.trim()
        if (trimmedCode.isBlank()) {
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
        "$ERROR_HEADER${System.lineSeparator()}" +
            "Error: $compileProblem (line $line, column $column)}"

    private fun ExecuteAnonymousResult.toExceptionMessage() =
        "$ERROR_HEADER${System.lineSeparator()}" +
            "$exceptionMessage${System.lineSeparator()}" +
            "Stack trace: $exceptionStackTrace"

    private fun getBuildFileErrorLocation(result: ExecuteAnonymousResult): Location {
        val lines = _code.lines()
        val linesCount = lines.count()
        val emptyLinesBeforeCodeCount = lines.takeWhile(String::isBlank).count()
        val emptyLinesAfterCodeCount = lines.reversed().takeWhile(String::isBlank).count()
        val codeLinesCount = linesCount - emptyLinesBeforeCodeCount - emptyLinesAfterCodeCount

        val lineOffset = when {
            codeLinesCount + emptyLinesAfterCodeCount < result.line -> linesCount - 1
            else -> emptyLinesBeforeCodeCount + result.line - 1
        }

        val lineNumber = getLocation().lineNumber + lineOffset
        return Location(getLocation().fileName, lineNumber, result.column)
    }
}