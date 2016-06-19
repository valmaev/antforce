package com.aquivalabs.force.ant.dsl.cobertura

import com.aquivalabs.force.ant.dsl.xml.*


class CoberturaReportRoot : EmptyTag() {
    fun coverage(init: Coverage.() -> Unit = {}): Coverage = initTag(Coverage(), init)

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        render(builder, "")
        return builder.toString()
    }
}

class Coverage : Tag("coverage") {
    fun sources(init: Sources.() -> Unit = {}): Sources = initTag(Sources(), init)
    fun packages(init: Packages.() -> Unit = {}): Packages = initTag(Packages(), init)
}

class Sources : Tag("sources") {
    fun source(init: Source.() -> Unit = {}): Source = initTag(Source(), init)
}

class Source : TagWithTextData("source")

class Packages : Tag("packages") {
    fun `package`(name: String = "", init: Package.() -> Unit = {}): Package {
        val packageTag = initTag(Package(), init)
        packageTag.name = name
        return packageTag
    }
}

class Package : Tag("package") {
    var name: String by attributes

    fun classes(init: Classes.() -> Unit = {}): Classes = initTag(Classes(), init)
}

class Classes : Tag("classes") {
    fun `class`(
        name: String = "",
        filename: String = "",
        init: Class.() -> Unit = {}): Class {

        val classTag = initTag(Class(), init)
        classTag.name = name
        classTag.filename = filename
        return classTag
    }
}

class Class : Tag("class") {
    var name: String by attributes
    var filename: String by attributes

    fun lines(init: Lines.() -> Unit = {}): Lines = initTag(Lines(), init)
}

class Lines : Tag("lines") {
    fun line(number: Int = 0, hits: Int = 0, init: Line.() -> Unit = {}): Line {
        val line = initTag(Line(), init)
        line.number = number
        line.hits = hits
        return line
    }
}

class Line : Tag("line") {
    var number: Int by attributes
    var hits: Int by attributes
}