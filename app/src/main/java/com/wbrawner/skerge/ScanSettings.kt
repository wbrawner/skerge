package com.wbrawner.skerge

import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

data class ScanSettings(
    val version: String = "2.1",
    val intent: String = "Document",
    val scanRegions: List<ScanRegion> = listOf(ScanRegion()),
    val inputSource: String = "Platen",
    val documentFormatExt: String = "application/pdf",
    val xResolution: Int = 300,
    val yResolution: Int = 300,
    val colorMode: String = "RGB24",
    val compressionFactor: Int = 25,
    val brightness: Int = 1000,
    val contrast: Int = 1000
) {
    fun toXml(): String {
        val writer = StringWriter()
        XmlPullParserFactory.newInstance().newSerializer().apply {
            setOutput(writer)
            startDocument(null, null)
            for (prefix in Prefix.values()) {
                setPrefix(prefix.prefix, prefix.namespace)
            }
            writeTag("ScanSettings", Prefix.SCAN) {
                writeTag("Version", Prefix.PWG, version)
                writeTag("Intent", Prefix.SCAN, intent)
                writeTag("ScanRegions", Prefix.PWG) {
                    scanRegions.forEach { scanRegion ->
                        scanRegion.toXml(this)
                    }
                }
                writeTag("InputSource", Prefix.PWG, inputSource)
                writeTag("DocumentFormatExt", Prefix.SCAN, documentFormatExt)
                writeTag("XResolution", Prefix.SCAN, xResolution)
                writeTag("YResolution", Prefix.SCAN, yResolution)
                writeTag("ColorMode", Prefix.SCAN, colorMode)
                writeTag("CompressionFactor", Prefix.SCAN, compressionFactor)
                writeTag("Brightness", Prefix.SCAN, brightness)
                writeTag("Contrast", Prefix.SCAN, contrast)
            }
            endDocument()
        }
        // We have to remove the xml document header since that's how the web app sends it
        return writer.toString().replace("<?xml version='1.0' ?>", "")
    }
}

data class ScanRegion(
    val height: Int = 3300,
    val width: Int = 2550,
    val xOffset: Int = 0,
    val yOffset: Int = 0
) {
    fun toXml(serializer: XmlSerializer) = serializer.apply {
        writeTag("ScanRegion", Prefix.PWG) {
            writeTag("Height", Prefix.PWG) {
                text(height.toString())
            }
            writeTag("Width", Prefix.PWG) {
                text(width.toString())
            }
            writeTag("XOffset", Prefix.PWG) {
                text(xOffset.toString())
            }
            writeTag("YOffset", Prefix.PWG) {
                text(yOffset.toString())
            }
        }
    }
}
