package com.aquivalabs.force.ant.dsl

import com.aquivalabs.force.ant.createCodeCoverageResult
import com.aquivalabs.force.ant.createCodeLocation
import com.aquivalabs.force.ant.createRunTestsResult
import com.aquivalabs.force.ant.dsl.cobertura.Coverage
import com.aquivalabs.force.ant.dsl.cobertura.Packages
import com.sforce.soap.metadata.CodeCoverageResult
import org.hamcrest.MatcherAssert.*
import org.hamcrest.core.IsEqual.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test


class CoberturaCoverageReporterTestCase {
    @Test(dataProvider = "createCoberturaReportPackagesTestData")
    fun createCoberturaReport_forEachCodeCoverageType_shouldCreatePackage(
        codeCoverage: Array<CodeCoverageResult>,
        projectRootPath: String,
        expected: Packages,
        reason: String) {

        val sut = createSystemUnderTest()
        sut.projectRootPath = projectRootPath
        val testResult = createRunTestsResult(codeCoverage = codeCoverage)
        val report = sut.createReport(testResult)

        val actual = report
            .children.filterIsInstance<Coverage>().single()
            .children.filterIsInstance<Packages>().single()

        assertThat(reason, actual, equalTo(expected))
    }

    @DataProvider
    fun createCoberturaReportPackagesTestData(): Array<Array<out Any>> {
        return arrayOf(
            arrayOf(
                arrayOf<CodeCoverageResult>(),
                "",
                Packages(),
                "Should create empty packages element for empty array of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(type = "Class"),
                    createCodeCoverageResult(type = "Trigger")),
                "",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`() {
                                lines()
                            }
                        }
                    }
                    `package`("Trigger") {
                        classes {
                            `class`() {
                                lines()
                            }
                        }
                    }
                },
                "Should create two packages for two types of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(type = null)),
                "",
                Coverage().packages {
                    `package`(name = "") {
                        classes {
                            `class`() {
                                lines()
                            }
                        }
                    }
                },
                "Should create package with empty name if type of CodeCoverageResult is null"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        type = "Class",
                        name = "Book"),
                    createCodeCoverageResult(
                        type = "Class",
                        name = "BookBuilder",
                        namespace = "foo"),
                    createCodeCoverageResult(
                        type = "Trigger",
                        name = "AccountTrigger"),
                    createCodeCoverageResult(
                        type = "Trigger",
                        name = "BookTrigger",
                        namespace = "bar")),
                "",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                name = "Book",
                                filename = "classes/Book.cls") {
                                lines()
                            }
                            `class`(
                                name = "foo.BookBuilder",
                                filename = "") {
                                lines()
                            }
                        }
                    }
                    `package`("Trigger") {
                        classes {
                            `class`(
                                name = "AccountTrigger",
                                filename = "triggers/AccountTrigger.trigger") {
                                lines()
                            }
                            `class`(
                                name = "bar.BookTrigger",
                                filename = "") {
                                lines()
                            }
                        }
                    }
                },
                "Should create class tag using name and namespace of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        type = "Class",
                        numLocations = 10,
                        locationsNotCovered = arrayOf(
                            createCodeLocation(line = 1, numExecutions = 0),
                            createCodeLocation(line = 2, numExecutions = 0),
                            createCodeLocation(line = 4, numExecutions = 3)))),
                "",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                name = "BookBuilder",
                                filename = "classes/BookBuilder.cls") {
                                lines {
                                    line(number = 1, hits = 0)
                                    line(number = 2, hits = 0)
                                    line(number = 3, hits = 1)
                                    line(number = 4, hits = 3)
                                    line(number = 5, hits = 1)
                                    line(number = 6, hits = 1)
                                    line(number = 7, hits = 1)
                                    line(number = 8, hits = 1)
                                    line(number = 9, hits = 1)
                                    line(number = 10, hits = 1)
                                }
                            }
                        }
                    }
                },
                "Should create line for each not covered location in CodeCoverageResult and for each covered location"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                filename = "classes/BookBuilder.cls",
                                name = "BookBuilder") { lines() }
                        }
                    }
                },
                "Should properly construct file names for Classes – {projectRootPath}/classes/{name}.cls"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookTrigger",
                        type = "Trigger")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Trigger") {
                        classes {
                            `class`(
                                filename = "triggers/BookTrigger.trigger",
                                name = "BookTrigger") { lines() }
                        }
                    }
                },
                "Should properly construct file names for Triggers – {projectRootPath}/triggers/{name}.trigger"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "BookBuilder",
                        namespace = "foo",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                filename = "",
                                name = "foo.BookBuilder") { lines() }
                        }
                    }
                },
                "Should not generate fileName for coverage results with non-empty namespace"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = null,
                        type = "Class"),
                    createCodeCoverageResult(
                        name = "",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                filename = "",
                                name = "") { lines() }
                            `class`(
                                filename = "",
                                name = "") { lines() }
                        }
                    }
                },
                "Should not generate fileName for coverage results with null or empty name"),
            arrayOf(
                arrayOf(
                    createCodeCoverageResult(
                        name = "Book",
                        type = "Class")),
                "/foo/bar/myDirectory/",
                Coverage().packages {
                    `package`("Class") {
                        classes {
                            `class`(
                                name = "Book",
                                filename = "classes/Book.cls") { lines() }
                        }
                    }
                },
                "Should properly handle trailing slash in projectRootPath"))
    }

    fun createSystemUnderTest() = CoberturaCoverageReporter()
}