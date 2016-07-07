package com.aquivalabs.force.ant.dsl

import com.aquivalabs.force.ant.classFileName
import com.aquivalabs.force.ant.dsl.cobertura.Classes
import com.aquivalabs.force.ant.dsl.cobertura.CoberturaReportRoot
import com.aquivalabs.force.ant.qualifiedName
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.RunTestsResult

class CoberturaCoverageReporter(var projectRootPath: String? = null) : Reporter<CoberturaReportRoot> {

    override fun createReport(result: RunTestsResult): CoberturaReportRoot {
        val coverageTypes = result.codeCoverage.groupBy { it.type ?: "" }

        val report = CoberturaReportRoot()
        report.coverage {
            sources {
                source { +projectRootPath.orEmpty() }
            }
            packages {
                coverageTypes.forEach { coverageType ->
                    `package`(name = coverageType.key) {
                        classes {
                            coverageType.value.forEach {
                                createClassTags(it)
                            }
                        }
                    }
                }
            }
        }
        return report
    }

    private fun Classes.createClassTags(result: CodeCoverageResult) {
        `class`(
            name = result.qualifiedName,
            filename = result.classFileName) {
            lines {
                val notCoveredLines = result.locationsNotCovered.orEmpty().associateBy { it.line }
                for (currentLine in 1..result.numLocations) {
                    val hits =
                        if (notCoveredLines.contains(currentLine))
                            notCoveredLines[currentLine]!!.numExecutions
                        else 1
                    line(number = currentLine, hits = hits)
                }
            }
        }
    }
}