package com.newmarket.force.ant.dsl.junit

import org.testng.annotations.Test
import org.hamcrest.MatcherAssert.assertThat
import org.xmlmatchers.XmlMatchers.*
import org.xmlmatchers.transform.XmlConverters.*
import java.time.LocalDateTime


class JUnitDslTestCase {

    @Test fun toString_always_shouldReturnsExpectedResult() {
        val actual = JUnitReportRoot()
        actual.testSuite(name = "TestSuite",
            tests = 5,
            errors = 2,
            failures = 1,
            time = 0.37,
            timestamp = LocalDateTime.MAX) {

            properties {
                property(name = "foo", value = "bar")
                fromMap(hashMapOf("baz" to "qux", "quux" to "fnag"))
            }

            testCase(className = "FooTestClass", name = "test1", time = 0.25)
            testCase(className = "BarTestClass", name = "test2", time = 0.12)
            testCase(className = "baz.BazTestClass", name = "test3", time = 1.067) {
                failure(message = "System.AssertionError", type = "SomeType") {
                    + "baz.BazTestClass.test3: line 9, column 1"
                }
            }
        }

        val expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<testsuite name=\"TestSuite\" tests=\"5\" errors=\"2\" failures=\"1\" time=\"0.37\" timestamp=\"${LocalDateTime.MAX}\">" +
                "<properties>" +
                    "<property name=\"foo\" value=\"bar\" />" +
                    "<property name=\"baz\" value=\"qux\" />" +
                    "<property name=\"quux\" value=\"fnag\" />" +
                "</properties>" +
                "<testcase classname=\"FooTestClass\" name=\"test1\" time=\"0.25\" />" +
                "<testcase classname=\"BarTestClass\" name=\"test2\" time=\"0.12\" />" +
                "<testcase classname=\"baz.BazTestClass\" name=\"test3\" time=\"1.067\">" +
                    "<failure message=\"System.AssertionError\" type=\"SomeType\">" +
                        "<![CDATA[baz.BazTestClass.test3: line 9, column 1]]>" +
                    "</failure>" +
                "</testcase>" +
            "</testsuite>"

        assertThat(the(actual.toString()), isEquivalentTo(the(expected)))
    }
}