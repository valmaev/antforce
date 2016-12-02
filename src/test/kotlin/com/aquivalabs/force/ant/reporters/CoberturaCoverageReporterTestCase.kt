package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.codeCoverageResult
import com.aquivalabs.force.ant.codeLocation
import com.aquivalabs.force.ant.deployResult
import com.aquivalabs.force.ant.runTestsResult
import com.aquivalabs.force.ant.reporters.cobertura.Coverage
import com.aquivalabs.force.ant.reporters.cobertura.Packages
import com.sforce.soap.metadata.CodeCoverageResult
import org.hamcrest.MatcherAssert.*
import org.hamcrest.core.IsEqual.*
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File


class CoberturaCoverageReporterTestCase {
    @Test(dataProvider = "createCoberturaReportPackagesTestData")
    fun createCoberturaReport_forEachCodeCoverageType_shouldCreatePackage(
        codeCoverage: Array<CodeCoverageResult>,
        projectRootPath: String,
        expected: Packages,
        reason: String) {

        val sut = createSystemUnderTest(projectRootPath)
        val testResult = runTestsResult(codeCoverage = codeCoverage)
        val report = sut.createCoberturaReport(deployResult(testResult))

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
                    codeCoverageResult(type = "Class"),
                    codeCoverageResult(type = "Trigger")),
                "",
                Coverage().packages {
                    `package`("Class", `line-rate` = 1.0) {
                        classes {
                            `class`(`line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                    `package`("Trigger", `line-rate` = 1.0) {
                        classes {
                            `class`(`line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should create two packages for two types of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(type = null)),
                "",
                Coverage().packages {
                    `package`(name = "", `line-rate` = 1.0) {
                        classes {
                            `class`(`line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should create package with empty name if type of CodeCoverageResult is null"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        type = "Class",
                        name = "Book"),
                    codeCoverageResult(
                        type = "Class",
                        name = "BookBuilder",
                        namespace = "foo"),
                    codeCoverageResult(
                        type = "Trigger",
                        name = "AccountTrigger"),
                    codeCoverageResult(
                        type = "Trigger",
                        name = "BookTrigger",
                        namespace = "bar")),
                "",
                Coverage().packages {
                    `package`("Class", `line-rate` = 1.0) {
                        classes {
                            `class`(
                                name = "Book",
                                filename = "classes/Book.cls",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                            `class`(
                                name = "foo.BookBuilder",
                                filename = "classes/BookBuilder.cls",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                    `package`("Trigger", `line-rate` = 1.0) {
                        classes {
                            `class`(
                                name = "AccountTrigger",
                                filename = "triggers/AccountTrigger.trigger",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                            `class`(
                                name = "bar.BookTrigger",
                                filename = "triggers/BookTrigger.trigger",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should create class tag using name and namespace of CodeCoverageResult"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "BookBuilder",
                        type = "Class",
                        numLocations = 3,
                        locationsNotCovered = arrayOf(
                            codeLocation(line = 5, numExecutions = 0),
                            codeLocation(line = 26, numExecutions = 0),
                            codeLocation(line = 17, numExecutions = 0)))),
                "",
                Coverage().packages {
                    `package`("Class", `line-rate` = 0.0) {
                        classes {
                            `class`(
                                name = "BookBuilder",
                                filename = "classes/BookBuilder.cls",
                                `line-rate` = 0.0) {
                                methods()
                                lines {
                                    line(number = 5, hits = 0)
                                    line(number = 17, hits = 0)
                                    line(number = 26, hits = 0)
                                }
                            }
                        }
                    }
                },
                "Should create line for each not covered location in CodeCoverageResult " +
                    "in ascending order (numLocations == locationsNotCovered.size)"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "BookBuilder",
                        type = "Class",
                        numLocations = 13,
                        locationsNotCovered = arrayOf(
                            codeLocation(line = 5, numExecutions = 0),
                            codeLocation(line = 7, numExecutions = 0),
                            codeLocation(line = 10, numExecutions = 0)))),
                "",
                Coverage().packages {
                    `package`("Class", `line-rate` = 10.0 / 13) {
                        classes {
                            `class`(
                                name = "BookBuilder",
                                filename = "classes/BookBuilder.cls",
                                `line-rate` = 10.0 / 13) {
                                methods()
                                lines {
                                    line(number = 1, hits = 1)
                                    line(number = 2, hits = 1)
                                    line(number = 3, hits = 1)
                                    line(number = 4, hits = 1)
                                    line(number = 5, hits = 0)
                                    line(number = 6, hits = 1)
                                    line(number = 7, hits = 0)
                                    line(number = 8, hits = 1)
                                    line(number = 9, hits = 1)
                                    line(number = 10, hits = 0)
                                    line(number = 11, hits = 1)
                                    line(number = 12, hits = 1)
                                    line(number = 13, hits = 1)
                                }
                            }
                        }
                    }
                },
                "Should create line for each not covered location in CodeCoverageResult " +
                    "in ascending order (numLocations > locationsNotCovered.size; " +
                    "first notCoveredLocation on line 5)"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "BookBuilder",
                        type = "Class",
                        numLocations = 10,
                        locationsNotCovered = arrayOf(
                            codeLocation(line = 1, numExecutions = 0),
                            codeLocation(line = 2, numExecutions = 0),
                            codeLocation(line = 4, numExecutions = 0)))),
                "",
                Coverage().packages {
                    `package`("Class", `line-rate` = 0.7) {
                        classes {
                            `class`(
                                name = "BookBuilder",
                                filename = "classes/BookBuilder.cls",
                                `line-rate` = 0.7) {
                                methods()
                                lines {
                                    line(number = 1, hits = 0)
                                    line(number = 2, hits = 0)
                                    line(number = 3, hits = 1)
                                    line(number = 4, hits = 0)
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
                "Should create line for each not covered location in CodeCoverageResult " +
                    "in ascending order (numLocations > locationsNotCovered.size; " +
                    "first notCoveredLocation on line 1)"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "BookBuilder",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class", `line-rate` = 1.0) {
                        classes {
                            `class`(
                                filename = "classes/BookBuilder.cls",
                                name = "BookBuilder",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should properly construct file names for Classes – {projectRootPath}/classes/{name}.cls"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "BookTrigger",
                        type = "Trigger")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Trigger", `line-rate` = 1.0) {
                        classes {
                            `class`(
                                filename = "triggers/BookTrigger.trigger",
                                name = "BookTrigger",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should properly construct file names for Triggers – {projectRootPath}/triggers/{name}.trigger"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "BookBuilder",
                        namespace = "foo",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class", `line-rate` = 1.0) {
                        classes {
                            `class`(
                                filename = "classes/BookBuilder.cls",
                                name = "foo.BookBuilder",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should generate fileName for coverage results with non-empty namespace"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = null,
                        type = "Class"),
                    codeCoverageResult(
                        name = "",
                        type = "Class")),
                "/foo/bar/myDirectory",
                Coverage().packages {
                    `package`("Class", `line-rate` = 1.0) {
                        classes {
                            `class`(
                                filename = "",
                                name = "",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                            `class`(
                                filename = "",
                                name = "",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should not generate fileName for coverage results with null or empty name"),
            arrayOf(
                arrayOf(
                    codeCoverageResult(
                        name = "Book",
                        type = "Class")),
                "/foo/bar/myDirectory/",
                Coverage().packages {
                    `package`("Class", `line-rate` = 1.0) {
                        classes {
                            `class`(
                                name = "Book",
                                filename = "classes/Book.cls",
                                `line-rate` = 1.0) {
                                methods()
                                lines()
                            }
                        }
                    }
                },
                "Should properly handle trailing slash in projectRootPath"))
    }

    fun createSystemUnderTest(projectRootPath: String?) = CoberturaCoverageReporter(
        outputFile = File("foo"),
        projectRootPath = projectRootPath)
}