package com.newmarket.force.ant.dsl


public class CoberturaReport : Tag("") {
    public fun coverage(init: Coverage.() -> Unit = {}): Coverage = initTag(Coverage(), init)

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        render(builder, "")
        return builder.toString()
    }
}

public class Coverage: Tag("coverage") {
    public fun packages(init: Packages.() -> Unit = {}): Packages = initTag(Packages(), init)
}

public class Packages: Tag("packages") {
    public fun packageTag(name: String = "", init: Package.() -> Unit = {}): Package {
        val packageTag = initTag(Package(), init)
        packageTag.name = name
        return packageTag
    }
}

public class Package: Tag("package") {
    public var name: String
        get() = attributes["name"]!!
        set(value) { attributes["name"] = value }

    public fun classes(init: Classes.() -> Unit = {}): Classes = initTag(Classes(), init)
}

public class Classes: Tag("classes") {
    public fun classTag(
        name: String = "",
        fileName: String = "",
        init: Class.() -> Unit = {}): Class {

        val classTag = initTag(Class(), init)
        classTag.name = name
        classTag.fileName = fileName
        return classTag
    }
}

public class Class: Tag("class") {
    public var name: String
        get() = attributes["name"]!!
        set(value) { attributes["name"] = value }

    public var fileName: String
        get() = attributes["filename"]!!
        set(value) { attributes["filename"] = value }

    public fun lines(init: Lines.() -> Unit = {}): Lines = initTag(Lines(), init)
}

public class Lines: Tag("lines") {
    public fun line(number: Int = 0, hits: Int = 0, init: Line.() -> Unit = {}): Line {
        val line = initTag(Line(), init)
        line.number = number
        line.hits = hits
        return line
    }
}

public class Line: Tag("line") {
    public var number: Int
        get() = attributes["number"]!!.toInt()
        set(value) { attributes["number"] = value.toString() }

    public var hits: Int
        get() = attributes["hits"]!!.toInt()
        set(value) { attributes["hits"] = value.toString() }
}