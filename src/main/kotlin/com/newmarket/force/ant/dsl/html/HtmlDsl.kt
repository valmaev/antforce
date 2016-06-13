package com.newmarket.force.ant.dsl.html

import com.newmarket.force.ant.dsl.xml.*


class HtmlReportRoot : EmptyTag() {
    fun html(init: Html.() -> Unit = {}) = initTag(Html(), init)

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("<!DOCTYPE html>\n")
        render(builder, "")
        return builder.toString()
    }
}

class Html() : TagWithTextData("html") {
    fun head(init: Head.() -> Unit) = initTag(Head(), init)
    fun body(init: Body.() -> Unit) = initTag(Body(), init)
}

class Head() : TagWithTextData("head") {
    fun title(init: Title.() -> Unit) = initTag(Title(), init)

    fun style(type: String = "text/css", init: Style.() -> Unit) {
        val style = initTag(Style(), init)
        style.type = type
    }

    fun link(rel: String, href: String, init: Link.() -> Unit) {
        val link = initTag(Link(), init)
        link.rel = rel
        link.href = href
    }
}

class Title() : TagWithTextData("title")

class Link() : Tag("link") {
    var rel: String
        get() = attributes["rel"]!!
        set(value) { attributes["rel"] = value }
    var href: String
        get() = attributes["href"]!!
        set(value) { attributes["href"] = value }
}

class Style() : TagWithTextData("style") {
    var type: String
        get() = attributes["type"]!!
        set(value) { attributes["type"] = value }
}

abstract class BodyTag(name: String) : TagWithTextData(name) {
    fun div(init: Div.() -> Unit) = initTag(Div(), init)
    fun table(init: Table.() -> Unit) = initTag(Table(), init)

    fun span(init: Span.() -> Unit) = initTag(Span(), init)
    fun h1(init: H1.() -> Unit) = initTag(H1(), init)

    var id: String
        get() = attributes["id"]!!
        set(value) { attributes["id"] = value }

    var `class`: String
        get() = attributes["class"]!!
        set(value) { attributes["class"] = value }

    var style: String
        get() = attributes["style"]!!
        set(value) { attributes["style"] = value }
}

class Body() : BodyTag("body")
class Div() : BodyTag("div")
class Span() : BodyTag("span")
class H1() : BodyTag("h1")

class Table() : BodyTag("table") {
    fun thead(init: THead.() -> Unit) = initTag(THead(), init)
    fun tbody(init: TBody.() -> Unit) = initTag(TBody(), init)
}

class THead() : BodyTag("thead") {
    fun th(init: TH.() -> Unit) = initTag(TH(), init)
}

class TH() : BodyTag("th")

class TBody() : BodyTag("tbody") {
    fun tr(init: TR.() -> Unit) = initTag(TR(), init)
}

class TR() : BodyTag("tr") {
    fun td(init: TD.() -> Unit) = initTag(TD(), init)
}

class TD() : BodyTag("td")