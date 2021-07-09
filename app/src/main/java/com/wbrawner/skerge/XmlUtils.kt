package com.wbrawner.skerge

import org.xmlpull.v1.XmlSerializer

enum class Prefix(val prefix: String, val namespace: String) {
    COPY("copy", "http://www.hp.com/schemas/imaging/con/copy/2008/07/07"),
    DD("dd", "http://www.hp.com/schemas/imaging/con/dictionaries/1.0/"),
    DD3("dd3", "http://www.hp.com/schemas/imaging/con/dictionaries/2009/04/06"),
    FW("fw", "http://www.hp.com/schemas/imaging/con/firewall/2011/01/05"),
    PWG("pwg", "http://www.pwg.org/schemas/2010/12/sm"),
    SCAN("scan", "http://schemas.hp.com/imaging/escl/2011/05/03"),
    SCC("scc", "http://schemas.hp.com/imaging/escl/2011/05/03")
}

fun XmlSerializer.writeTag(
    name: String,
    prefix: Prefix? = null,
    content: XmlSerializer.() -> Unit
) {
    val tagName = prefix?.let { "${it.prefix}:${name}" } ?: name
    // XmlPullParser doesn't seem to like having multiple prefixes for the same namespace
    startTag(null, tagName)
    content()
    endTag(null, tagName)
}

fun XmlSerializer.writeTag(name: String, prefix: Prefix? = null, text: Any) =
    writeTag(name, prefix) {
        text(text.toString())
    }