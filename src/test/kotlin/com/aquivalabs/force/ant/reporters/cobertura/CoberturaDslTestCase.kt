package com.aquivalabs.force.ant.reporters.cobertura

import org.hamcrest.MatcherAssert.*
import org.testng.annotations.Test
import org.xmlunit.matchers.CompareMatcher.isSimilarTo


class CoberturaDslTestCase {

    @Test fun toString_always_shouldReturnValidExpectedXml() {
        val actual = CoberturaReportRoot()
        actual.coverage(
            `line-rate` = 0.5,
            `lines-valid` = 6,
            `lines-covered` = 3,
            `branch-rate` = 1.0,
            `branches-valid` = 1,
            `branches-covered` = 1,
            timestamp = "1234",
            complexity = 2,
            version = "1.0") {
            sources {
                source { +"/path/to/sourcecode" }
            }
            packages {
                `package`(
                    name = "fooPackage",
                    `line-rate` = 1.0,
                    `branch-rate` = 1.0,
                    complexity = 2) {
                    classes {
                        `class`(
                            name = "fooClass",
                            filename = "/c/bar/baz/fooClass.bar",
                            `line-rate` = 1.0,
                            `branch-rate` = 1.0,
                            complexity = 2) {
                            methods()
                            lines {
                                line(number = 1, hits = 2, branch = true, `condition-coverage` = 0)
                            }
                        }
                    }
                }
                `package`(
                    name = "barPackage",
                    `line-rate` = 1.0,
                    `branch-rate` = 1.0,
                    complexity = 2) {
                    classes {
                        `class`(
                            name = "barClass",
                            filename = "/c/qux/baz/barClass.bar",
                            `line-rate` = 1.0,
                            `branch-rate` = 1.0,
                            complexity = 2) {
                            methods()
                            lines {
                                line(number = 3, hits = 4)
                                line(number = 5, hits = 6)
                            }
                        }
                    }
                }
            }
        }

        val expected =
            """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE coverage SYSTEM "http://cobertura.sourceforge.net/xml/coverage-04.dtd">
<coverage lines-valid="6" lines-covered="3" line-rate="0.5" branches-valid="1"
          branches-covered="1" branch-rate="1.0" timestamp="1234" complexity="2"
          version="1.0">
    <sources>
        <source>
            /path/to/sourcecode
        </source>
    </sources>
    <packages>
        <package name="fooPackage" line-rate="1.0" branch-rate="1.0" complexity="2">
            <classes>
                <class name="fooClass" filename="/c/bar/baz/fooClass.bar" line-rate="1.0"
                       branch-rate="1.0" complexity="2">
                    <methods/>
                    <lines>
                        <line number="1" hits="2" branch="true" condition-coverage="0%"/>
                    </lines>
                </class>
            </classes>
        </package>
        <package name="barPackage" line-rate="1.0" branch-rate="1.0" complexity="2">
            <classes>
                <class name="barClass" filename="/c/qux/baz/barClass.bar" line-rate="1.0"
                       branch-rate="1.0" complexity="2">
                    <methods/>
                    <lines>
                        <line number="3" hits="4" branch="false" condition-coverage="100%"/>
                        <line number="5" hits="6" branch="false" condition-coverage="100%"/>
                    </lines>
                </class>
            </classes>
        </package>
    </packages>
</coverage>"""

        assertThat(actual.toString(), isSimilarTo(expected).ignoreWhitespace())
    }
}