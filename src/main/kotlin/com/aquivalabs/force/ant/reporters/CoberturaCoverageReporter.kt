package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.classFileName
import com.aquivalabs.force.ant.reporters.cobertura.Classes
import com.aquivalabs.force.ant.reporters.cobertura.CoberturaReportRoot
import com.aquivalabs.force.ant.qualifiedName
import com.sforce.soap.metadata.CodeCoverageResult
import com.sforce.soap.metadata.DeployResult
import java.io.File

class CoberturaCoverageReporter(
    var outputFile: File,
    var projectRootPath: String? = null) : Reporter<File> {

    override fun createReport(deployResult: DeployResult): File {
        val report = createCoberturaReport(deployResult)
        outputFile.writeText(report.toString())
        return outputFile
    }

    internal fun createCoberturaReport(deployResult: DeployResult): CoberturaReportRoot {
        val coverageTypes = deployResult.details.runTestResult.codeCoverage.groupBy { it.type ?: "" }

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
                val locations = sortedMapOf<Int, Int>()
                result.locationsNotCovered?.forEach { locations[it.line] = it.numExecutions }

                var currentLine = 0
                while (locations.size != result.numLocations) {
                    if (locations.contains(++currentLine))
                        continue
                    locations[currentLine] = 1
                }

                locations.forEach {
                    line(number = it.key, hits = it.value)
                }
            }
        }
    }
}