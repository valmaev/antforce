package com.newmarket.force.ant.dsl

import org.testng.annotations.Test
import org.hamcrest.MatcherAssert.assertThat
import org.xmlmatchers.XmlMatchers.*
import org.xmlmatchers.transform.XmlConverters.*
import java.time.LocalDateTime


public class JUnitDslTestCase {

    @Test fun testSuite_always_shouldContainsThreeChildElementsInStringRepresentation() {
        val actual = JUnitReport.testSuite(name = "TestSuite",
            tests = 5,
            errors = 2,
            failures = 1,
            time = 0.37,
            timestamp = LocalDateTime.MAX) {

            testCase(className = "FooTestClass", name = "test1", time = 0.25)
            testCase(className = "BarTestClass", name = "test2", time = 0.12)
        }.toString()

        val expected =
            "<testsuite name=\"TestSuite\" tests=\"5\" errors=\"2\" failures=\"1\" time=\"0.37\" timestamp=\"${LocalDateTime.MAX}\">" +
                "<testcase classname=\"FooTestClass\" name=\"test1\" time=\"0.25\" />" +
                "<testcase classname=\"BarTestClass\" name=\"test2\" time=\"0.12\" />" +
            "</testsuite>"

        assertThat(the(actual), isEquivalentTo(the(expected)))
    }
}