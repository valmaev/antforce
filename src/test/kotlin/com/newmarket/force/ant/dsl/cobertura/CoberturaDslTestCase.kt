package com.newmarket.force.ant.dsl.cobertura

import org.hamcrest.MatcherAssert.*
import org.testng.annotations.Test
import org.xmlmatchers.XmlMatchers.*
import org.xmlmatchers.transform.XmlConverters.*


class CoberturaDslTestCase {

    @Test fun toString_always_shouldReturnsExpectedResult() {
        val actual = CoberturaReportRoot()
        actual.coverage {
            sources {
                source { +"/path/to/sourcecode" }
            }
            packages {
                packageTag(name = "fooPackage") {
                    classes {
                        classTag(name = "fooClass", fileName = "/c/bar/baz/fooClass.bar") {
                            lines {
                                line(number = 1, hits = 2)
                            }
                        }
                    }
                }
                packageTag(name = "barPackage") {
                    classes {
                        classTag(name = "barClass", fileName = "/c/qux/baz/barClass.bar") {
                            lines {
                                line(number = 3, hits = 4)
                                line(number = 5, hits = 6)
                            }
                        }
                    }
                }
            }
        }

        val expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<coverage>" +
                "<sources>" +
                    "<source>/path/to/sourcecode</source>" +
                "</sources>" +
                "<packages>" +
                    "<package name=\"fooPackage\">" +
                        "<classes>" +
                            "<class name=\"fooClass\" filename=\"/c/bar/baz/fooClass.bar\">" +
                                "<lines>" +
                                    "<line number=\"1\" hits=\"2\" />" +
                                "</lines>" +
                            "</class>" +
                        "</classes>" +
                     "</package>" +
                     "<package name=\"barPackage\">" +
                        "<classes>" +
                            "<class name=\"barClass\" filename=\"/c/qux/baz/barClass.bar\">" +
                                "<lines>" +
                                    "<line number=\"3\" hits=\"4\" />" +
                                    "<line number=\"5\" hits=\"6\" />" +
                                "</lines>" +
                            "</class>" +
                        "</classes>" +
                    "</package>" +
                "</packages>" +
            "</coverage>"

        assertThat(the(actual.toString()), isEquivalentTo(the(expected)))
    }
}