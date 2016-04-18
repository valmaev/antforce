package com.newmarket.force.ant.dsl


class CoberturaReport : EmptyTag() {
    fun coverage(init: Coverage.() -> Unit = {}): Coverage = initTag(Coverage(), init)

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        render(builder, "")
        return builder.toString()
    }
}

class Coverage: Tag("coverage") {
    fun packages(init: Packages.() -> Unit = {}): Packages = initTag(Packages(), init)
}

class Packages: Tag("packages") {
    fun packageTag(name: String = "", init: Package.() -> Unit = {}): Package {
        val packageTag = initTag(Package(), init)
        packageTag.name = name
        return packageTag
    }
}

class Package: Tag("package") {
    var name: String
        get() = attributes["name"]!!
        set(value) { attributes["name"] = value }

    fun classes(init: Classes.() -> Unit = {}): Classes = initTag(Classes(), init)
}

class Classes: Tag("classes") {
    fun classTag(
        name: String = "",
        fileName: String = "",
        init: Class.() -> Unit = {}): Class {

        val classTag = initTag(Class(), init)
        classTag.name = name
        classTag.fileName = fileName
        return classTag
    }
}

class Class: Tag("class") {
    var name: String
        get() = attributes["name"]!!
        set(value) { attributes["name"] = value }

    var fileName: String
        get() = attributes["filename"]!!
        set(value) { attributes["filename"] = value }

    fun lines(init: Lines.() -> Unit = {}): Lines = initTag(Lines(), init)
}

class Lines: Tag("lines") {
    fun line(number: Int = 0, hits: Int = 0, init: Line.() -> Unit = {}): Line {
        val line = initTag(Line(), init)
        line.number = number
        line.hits = hits
        return line
    }
}

class Line: Tag("line") {
    var number: Int
        get() = attributes["number"]!!.toInt()
        set(value) { attributes["number"] = value.toString() }

    var hits: Int
        get() = attributes["hits"]!!.toInt()
        set(value) { attributes["hits"] = value.toString() }
}