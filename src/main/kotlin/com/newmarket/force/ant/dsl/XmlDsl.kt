package com.newmarket.force.ant.dsl

import java.util.*


public interface Element {
    fun render(builder: StringBuilder, indent: String)
}

public class CharacterDataElement(val text: String) : Element {
    override fun render(builder: StringBuilder, indent: String) {
        builder.append("$indent<![CDATA[$text]]>\n")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null || javaClass != other.javaClass)
            return false
        val element = other as CharacterDataElement
        return Objects.equals(text, element.text)
    }

    override fun hashCode(): Int = text.hashCode()
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

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other == null || javaClass != other.javaClass)
            return false
        val tag = other as Tag
        return Objects.equals(tagName, tag.tagName)
            && Objects.equals(children, tag.children)
            && Objects.equals(attributes, tag.attributes)
    }

    override fun hashCode(): Int = Objects.hash(tagName, children, attributes)
}

public abstract class EmptyTag : Tag("") {
    override fun render(builder: StringBuilder, indent: String) =
        children.forEach { it.render(builder, indent + "  ") }
}

public abstract class TagWithCharacterData(name: String) : Tag(name) {
    operator fun String.unaryPlus() {
        children.add(CharacterDataElement(this))
    }
}