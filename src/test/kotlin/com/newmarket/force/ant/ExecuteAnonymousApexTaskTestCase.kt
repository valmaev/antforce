package com.newmarket.force.ant

import com.salesforce.ant.SFDCAntTask
import com.sforce.soap.apex.ExecuteAnonymousResult
import org.apache.tools.ant.BuildException
import org.apache.tools.ant.Location
import org.apache.tools.ant.Project
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.testng.Assert.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


class ExecuteAnonymousApexTaskTestCase {

    @Test fun sut_always_shouldDeriveFromProperBaseClass() =
        assertThat(createSystemUnderTest(), instanceOf(SFDCAntTask::class.java))

    @Test fun addText_always_shouldFollowAntNamingConventions() {
        assertThat(
            "Prefix 'create' is one of the Ant's conventions for nested elements declaration. " +
                "See the manual: http://ant.apache.org/manual/develop.html#nested-elements",
            ExecuteAnonymousApexTask::addText.name,
            startsWith("add"))
    }

    @Test(dataProvider = "executeBlankCodeTestData")
    fun execute_withBlankCode_shouldLogMessage(blankCode: String) {
        val projectMock = mock(Project::class.java)
        val sut = createSystemUnderTest(projectMock)
        sut.addText(blankCode)

        sut.execute()

        verify(projectMock).log(
            Mockito.eq(sut),
            Mockito.contains("Apex code wasn't specified"),
            Mockito.anyInt())
    }

    @DataProvider
    fun executeBlankCodeTestData(): Array<Array<Any?>> = arrayOf(
        arrayOf<Any?>(""),
        arrayOf<Any?>("   "),
        arrayOf<Any?>("\t"),
        arrayOf<Any?>("\n"),
        arrayOf<Any?>("\r\n"),
        arrayOf<Any?>("\n  \n"),
        arrayOf<Any?>("\r\n  \r\n"))

    @Test fun processResult_whenCodeIsNotCompiled_shouldThrowBuildException() {
        val input = createExecutionResult(
            compiled = false,
            compileProblem = "Some error occurred")
        val sut = createSystemUnderTest()

        try {
            sut.processResult(input)
        } catch(actual: BuildException) {
            assertThat(actual.message, containsString(input.compileProblem))
            return
        }

        fail("BuildException isn't thrown")
    }

    @Test fun processResult_whenApexExecutionWasNotSuccessful_shouldThrowBuildException() {
        val input = createExecutionResult(
            success = false,
            exceptionMessage = "ArgumentNullException: Parameter 'uniqueId' can't be null",
            exceptionStackTrace = "Class.Book.<init>: line 9, column 1\nClass.BookBuilder.build: line 22, column 1)")
        val sut = createSystemUnderTest()

        try {
            sut.processResult(input)
        } catch (actual: BuildException) {
            assertThat(actual.message, containsString(input.exceptionMessage))
            assertThat(actual.message, containsString(input.exceptionStackTrace))
            return
        }

        fail("BuildException isn't thrown")
    }

    @Test(dataProvider = "processResultLocationTestData")
    fun processResult_whenThrowsBuildException_shouldCorrectlySetLocation(
        lineNumberCase: String,
        apexCode: String,
        initialLocation: Location,
        input: ExecuteAnonymousResult,
        expected: Location) {

        val sut = createSystemUnderTest()
        sut.addText(apexCode)
        sut.location = initialLocation

        try {
            sut.processResult(input)
        } catch (actual: BuildException) {
            assertThat(
                "LineNumber should always account empty lines in Apex code ($lineNumberCase)",
                actual.location.lineNumber,
                equalTo(expected.lineNumber))
            assertThat(
                "ColumnName should always equal to column from execution result",
                actual.location.columnNumber,
                equalTo(input.column))
            assertThat(
                "Filename should always equal to task location",
                actual.location.fileName,
                equalTo(sut.location.fileName))
            return
        }

        fail("BuildException isn't thrown")
    }

    @DataProvider
    fun processResultLocationTestData(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf<Any?>(
                "Apex code: 1 line, Execution result line number: 1, Expected line: 1",
                "System.debug('Hello');",
                createLocation("build.xml", lineNumber = 1, columnNumber = 2),
                createExecutionResult(success = false, line = 1, column = 1),
                createLocation("build.xml", lineNumber = 1, columnNumber = 1)),

            // Case when Execution line number more than code lines count is possible
            // when we're miss semicolon in Apex code
            // Apex compiler returns error that points to next line number after erroneous one
            arrayOf<Any?>(
                "Apex code: 1 line, Execution result line number: 2, Expected line: 1",
                "System.debug('Hello')",
                createLocation("build.xml", lineNumber = 1, columnNumber = 2),
                createExecutionResult(success = false, line = 2, column = 1),
                createLocation("build.xml", lineNumber = 1, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 lines, Execution result line number: 1, Expected line: 1",
                "System.debug('Hello');\nSystem.debug('Hello')",
                createLocation("build.xml", lineNumber = 1, columnNumber = 2),
                createExecutionResult(success = false, line = 1, column = 1),
                createLocation("build.xml", lineNumber = 1, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 lines, Execution result line number: 2, Expected line: 2",
                "System.debug('Hello');\nSystem.debug('Hello')",
                createLocation("build.xml", lineNumber = 1, columnNumber = 2),
                createExecutionResult(success = false, line = 2, column = 1),
                createLocation("build.xml", lineNumber = 2, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 lines, Execution result line number: 2, Expected line: 2",
                "System.debug('Hello');\nSystem.debug('Hello')",
                createLocation("build.xml", lineNumber = 1, columnNumber = 2),
                createExecutionResult(success = false, line = 3, column = 1),
                createLocation("build.xml", lineNumber = 2, columnNumber = 1)),

            arrayOf<Any?>(
                "Apex code: 2 Unix empty lines before 1 line of code, Execution result line number: 1, Expected line: 3",
                "\n\nSystem.debug('Hello');",
                createLocation("build.xml", lineNumber = 1, columnNumber = 5),
                createExecutionResult(success = false, line = 1, column = 1),
                createLocation("build.xml", lineNumber = 3, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 Windows empty lines before 1 line of code, Execution result line number: 1, Expected line: 3",
                "\r\n\r\nSystem.debug('Hello');",
                createLocation("build.xml", lineNumber = 1, columnNumber = 3),
                createExecutionResult(success = false, line = 1, column = 1),
                createLocation("build.xml", lineNumber = 3, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 Unix empty lines before 2 lines of code, Execution result line number: 2, Expected line: 4",
                "\n\nSystem.debug('Hello');\nSystem.debug('Hello')",
                createLocation("build.xml", lineNumber = 1, columnNumber = 2),
                createExecutionResult(success = false, line = 2, column = 1),
                createLocation("build.xml", lineNumber = 4, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 Unix empty lines before 2 lines of code, Execution result line number: 3, Expected line: 4",
                "\n\nSystem.debug('Hello');\nSystem.debug('Hello')",
                createLocation("build.xml", lineNumber = 1, columnNumber = 2),
                createExecutionResult(success = false, line = 3, column = 1),
                createLocation("build.xml", lineNumber = 4, columnNumber = 1)),

            arrayOf<Any?>(
                "Apex code: 2 Unix empty lines after 1 line of code, Execution result line number: 1, Expected line: 1",
                "System.debug('Hello');\n\n",
                createLocation("build.xml", lineNumber = 1, columnNumber = 5),
                createExecutionResult(success = false, line = 1, column = 1),
                createLocation("build.xml", lineNumber = 1, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 1 Unix empty line after 1 line of code, Execution result line number: 2, Expected line: 2",
                "System.debug('Hello')\n",
                createLocation("build.xml", lineNumber = 1, columnNumber = 5),
                createExecutionResult(success = false, line = 2, column = 1),
                createLocation("build.xml", lineNumber = 2, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 Unix empty lines after 1 line of code, Execution result line number: 2, Expected line: 2",
                "System.debug('Hello')\n\n",
                createLocation("build.xml", lineNumber = 1, columnNumber = 5),
                createExecutionResult(success = false, line = 2, column = 1),
                createLocation("build.xml", lineNumber = 2, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 1 Unix empty line before and after 1 line of code, Execution result line number: 2, Expected line: 3",
                "\nSystem.debug('Hello');\n",
                createLocation("build.xml", lineNumber = 1, columnNumber = 5),
                createExecutionResult(success = false, line = 2, column = 1),
                createLocation("build.xml", lineNumber = 3, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 Unix empty lines before and after 2 lines of code, Execution result line number: 2, Expected line: 4",
                "\n\nSystem.debug('Hello');\nSystem.debug('Hello');\n\n",
                createLocation("build.xml", lineNumber = 1, columnNumber = 5),
                createExecutionResult(success = false, line = 2, column = 1),
                createLocation("build.xml", lineNumber = 4, columnNumber = 1)),
            arrayOf<Any?>(
                "Apex code: 2 Unix empty lines before and after 2 lines of code, Execution result line number: 3, Expected line: 4",
                "\n\nSystem.debug('Hello');\nSystem.debug('Hello')\n\n",
                createLocation("build.xml", lineNumber = 1, columnNumber = 5),
                createExecutionResult(success = false, line = 3, column = 1),
                createLocation("build.xml", lineNumber = 5, columnNumber = 1)))
    }

    fun createSystemUnderTest(project: Project = createProject()): ExecuteAnonymousApexTask {
        val sut = ExecuteAnonymousApexTask()
        sut.project = project
        return sut
    }

    fun createLocation(
        fileName: String = "build.xml",
        lineNumber: Int = 0,
        columnNumber: Int = 0) : Location = Location(fileName, lineNumber, columnNumber)

    fun createExecutionResult(
        compiled: Boolean = true,
        compileProblem: String? = null,
        success: Boolean = true,
        exceptionMessage: String? = null,
        exceptionStackTrace: String? = null,
        line: Int = 0,
        column: Int = 0): ExecuteAnonymousResult {

        val result = ExecuteAnonymousResult()
        result.compiled = compiled
        result.compileProblem = compileProblem
        result.success = success
        result.exceptionMessage = exceptionMessage
        result.exceptionStackTrace = exceptionStackTrace
        result.line = line
        result.column = column
        return result
    }
}