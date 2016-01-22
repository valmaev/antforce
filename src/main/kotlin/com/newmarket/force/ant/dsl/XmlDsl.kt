package com.newmarket.force.ant.dsl


public interface Element {
    fun render(builder: StringBuilder, indent: String)
}

public abstract class Tag(val tagName: String) : Element {
    val children = arrayListOf<Element>()
    val attributes = hashMapOf<String, String>()

    protected fun <T : Element> initTag(tag: T, init: T.() -> Unit): T {
        tag.init()
        children.add(tag)
        return tag
    }

    override fun render(builder: StringBuilder, indent: String) {
        if (children.isEmpty())
            builder.append("$indent<$tagName${renderAttributes()} />\n")
        else {
            builder.append("$indent<$tagName${renderAttributes()}>\n")
            children.forEach { it.render(builder, indent + "  ") }
            builder.append("$indent</$tagName>\n")
        }
    }

    private fun renderAttributes(): String? {
        val builder = StringBuilder()
        attributes.keys.forEach { builder.append(" $it=\"${attributes[it]}\"") }
        return builder.toString()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        render(builder, "")
        return builder.toString()
    }
}