package com.newmarket.force.ant

import com.newmarket.force.ant.dsl.JUnitReport
import com.newmarket.force.ant.dsl.TestSuite
import com.sforce.soap.metadata.RunTestsResult
import java.time.LocalDateTime


public class Reporter(val dateTimeProvider: () -> LocalDateTime) {

    public fun createJUnitReport(
        runTestsResult: RunTestsResult,
        suiteName: String = "",
        properties: Map<String, String>? = null): TestSuite {

        return JUnitReport().testSuite(
            name = suiteName,
            tests = runTestsResult.numTestsRun - runTestsResult.numFailures,
            failures = runTestsResult.numFailures,
            time = runTestsResult.totalTime / 1000,
            timestamp = dateTimeProvider()) {

            if (properties != null) {
                properties {
                    properties.forEach { property(name = it.key, value = it.value) }
                }
            }

            runTestsResult.successes.forEach {
                testCase(
                    className = if (it.namespace == null) it.name else "${it.namespace}.${it.name}",
                    name = it.methodName,
                    time = it.time / 1000)
            }

            runTestsResult.failures.forEach {
                testCase(
                    className = if (it.namespace == null) it.name else "${it.namespace}.${it.name}",
                    name = it.methodName,
                    time = it.time / 1000) {

                    failure(message = it.message, type = it.type) { +it.stackTrace }
                }
            }
        }
    }
}