package com.aquivalabs.force.ant.reporters.cobertura

import com.aquivalabs.force.ant.reporters.xml.*


class CoberturaReportRoot : EmptyTag() {
    fun coverage(
        `lines-valid`: Int = 0,
        `lines-covered`: Int = 0,
        `line-rate`: Double = 0.0,
        `branches-valid`: Int = 0,
        `branches-covered`: Int = 0,
        `branch-rate`: Double = 0.0,
        timestamp: String = "",
        complexity: Int = 0,
        version: String = "0",
        init: Coverage.() -> Unit = {}): Coverage {

        val coverage = initTag(Coverage(), init)
        coverage.`lines-valid` = `lines-valid`
        coverage.`lines-covered` = `lines-covered`
        coverage.`line-rate` = `line-rate`
        coverage.`branches-valid` = `branches-valid`
        coverage.`branches-covered` = `branches-covered`
        coverage.`branch-rate` = `branch-rate`
        coverage.timestamp = timestamp
        coverage.complexity = complexity
        coverage.version = version
        return coverage
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        builder.append("<!DOCTYPE coverage SYSTEM \"http://cobertura.sourceforge.net/xml/coverage-04.dtd\">\n")

        render(builder, "")
        return builder.toString()
    }
}

class Coverage : Tag("coverage") {
    var `lines-valid`: Int by attributes
    var `lines-covered`: Int by attributes
    var `line-rate`: Double by attributes
    var `branches-valid`: Int by attributes
    var `branches-covered`: Int by attributes
    var `branch-rate`: Double by attributes
    var timestamp: String by attributes
    var complexity: Int by attributes
    var version: String by attributes

    fun sources(init: Sources.() -> Unit = {}): Sources = initTag(Sources(), init)
    fun packages(init: Packages.() -> Unit = {}): Packages = initTag(Packages(), init)
}

class Sources : Tag("sources") {
    fun source(init: Source.() -> Unit = {}): Source = initTag(Source(), init)
}

class Source : TagWithTextData("source")

class Packages : Tag("packages") {
    fun `package`(
        name: String = "",
        `line-rate`: Double = 0.0,
        `branch-rate`: Double = 0.0,
        complexity: Int = 0,
        init: Package.() -> Unit = {}): Package {

        val packageTag = initTag(Package(), init)
        packageTag.name = name
        packageTag.`line-rate` = `line-rate`
        packageTag.`branch-rate` = `branch-rate`
        packageTag.complexity = complexity
        return packageTag
    }
}

class Package : Tag("package") {
    var name: String by attributes
    var `line-rate`: Double by attributes
    var `branch-rate`: Double by attributes
    var complexity: Int by attributes

    fun classes(init: Classes.() -> Unit = {}): Classes = initTag(Classes(), init)
}

class Classes : Tag("classes") {
    fun `class`(
        name: String = "",
        filename: String = "",
        `line-rate`: Double = 0.0,
        `branch-rate`: Double = 0.0,
        complexity: Int = 0,
        init: Class.() -> Unit = {}): Class {

        val classTag = initTag(Class(), init)
        classTag.name = name
        classTag.filename = filename
        classTag.`line-rate` = `line-rate`
        classTag.`branch-rate` = `branch-rate`
        classTag.complexity = complexity
        return classTag
    }
}

class Class : Tag("class") {
    var name: String by attributes
    var filename: String by attributes
    var `line-rate`: Double by attributes
    var `branch-rate`: Double by attributes
    var complexity: Int by attributes

    fun methods(init: Methods.() -> Unit = {}): Methods = initTag(Methods(), init)
    fun lines(init: Lines.() -> Unit = {}): Lines = initTag(Lines(), init)
}

class Methods : Tag("methods")

class Lines : Tag("lines") {
    fun line(
        number: Int = 0,
        hits: Int = 0,
        branch: Boolean = false,
        `condition-coverage`: Int = 100,
        init: Line.() -> Unit = {}): Line {

        val line = initTag(Line(), init)
        line.number = number
        line.hits = hits
        line.branch = branch
        line.`condition-coverage` = "$`condition-coverage`%"
        return line
    }
}

class Line : Tag("line") {
    var number: Int by attributes
    var hits: Int by attributes
    var branch: Boolean by attributes
    var `condition-coverage`: String by attributes
}