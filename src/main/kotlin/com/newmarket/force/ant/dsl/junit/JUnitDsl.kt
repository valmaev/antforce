package com.newmarket.force.ant.dsl.junit

import com.newmarket.force.ant.dsl.xml.*
import java.time.LocalDateTime


class JUnitReportRoot : EmptyTag() {
    fun testSuite(
        name: String = "",
        tests: Int = 0,
        errors: Int = 0,
        failures: Int = 0,
        time: Double = 0.0,
        timestamp: LocalDateTime = LocalDateTime.now(),
        init: TestSuite.() -> Unit = {}): TestSuite {

        val suite = initTag(TestSuite(), init)
        suite.name = name
        suite.tests = tests
        suite.errors = errors
        suite.failures = failures
        suite.time = time
        suite.timestamp = timestamp
        return suite
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        render(builder, "")
        return builder.toString()
    }
}

class TestSuite : Tag("testsuite") {
    final val testCases: Iterable<TestCase>
        get() = children.filterIsInstance<TestCase>()

    var errors: Int by attributes
    var failures: Int by attributes
    var tests: Int by attributes
    var name: String by attributes
    var time: Double by attributes
    var timestamp: LocalDateTime by attributes

    fun properties(init: Properties.() -> Unit = {}): Properties =
        initTag(Properties(), init)

    fun testCase(
        classname: String = "",
        name: String = "",
        time: Double = 0.0,
        init: TestCase.() -> Unit = {}): TestCase {

        val case = initTag(TestCase(), init)
        case.classname = classname
        case.name = name
        case.time = time
        return case
    }
}

class Properties: Tag("properties") {
    fun fromMap(properties: Map<String, String>) =
        properties.forEach { property(name = it.key, value = it.value) }

    fun property(
        name: String = "",
        value: String = "",
        init: Property.() -> Unit = {}): Property {

        val property = initTag(Property(), init)
        property.name = name
        property.value = value
        return property
    }
}

class Property: Tag("property") {
    var name: String by attributes
    var value: String by attributes
}

class TestCase : Tag("testcase") {
    var classname: String by attributes
    var name: String by attributes
    var time: Double? by attributes

    fun failure(
        message: String = "",
        type: String = "",
        init: Failure.() -> Unit = {}): Failure {

        val failure = initTag(Failure(), init)
        failure.message = message
        failure.type = type
        return failure
    }
}

class Failure: TagWithCharacterData("failure") {
    var message: String by attributes
    var type: String by attributes
}