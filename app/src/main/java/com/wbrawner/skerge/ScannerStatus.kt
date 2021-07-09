package com.wbrawner.skerge

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParser.*
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

data class ScannerStatus(
    val version: String,
    val state: String,
    val jobs: List<JobInfo>
) {
    companion object {
        fun fromXml(xml: String): ScannerStatus {
            var version = ""
            var state = ""
            val jobs = mutableListOf<JobInfo>()
            XmlPullParserFactory.newInstance().newPullParser().apply {
                setInput(StringReader(xml))
                while (eventType != END_DOCUMENT) {
                    when (eventType) {
                        START_TAG -> {
                            when (name) {
                                "pwg:Version" -> version = nextText()
                                "pwg:State" -> state = nextText()
                                "scan:JobInfo" -> jobs.add(JobInfo.fromXml(this))
                            }
                        }
                    }
                    next()
                }
            }
            return ScannerStatus(version, state, jobs)
        }
    }
}

data class JobInfo(
    val jobUri: String,
    val jobUuid: String,
    val age: Int,
    val imagesCompleted: Int,
    val imagesToTransfer: Int,
    val jobState: String,
    val jobStateReasons: List<String>
) {
    val completed: Boolean
        get() = jobState == "Completed"

    val aborted: Boolean
        get() = jobState == "Aborted"

    companion object {
        fun fromXml(xmlPullParser: XmlPullParser): JobInfo {
            var uri = ""
            var uuid = ""
            var age = 0
            var imagesCompleted = 0
            var imagesToTransfer = 0
            var jobState = ""
            val reasons = mutableListOf<String>()
            xmlPullParser.apply {
                while (eventType != END_DOCUMENT) {
                    when (eventType) {
                        START_TAG -> {
                            when (name) {
                                "pwg:JobUri" -> uri = nextText()
                                "pwg:JobUuid" -> uuid = nextText()
                                "scan:Age" -> age = nextText().toInt()
                                "pwg:ImagesCompleted" -> imagesCompleted = nextText().toInt()
                                "pwg:ImagesToTransfer" -> imagesToTransfer = nextText().toInt()
                                "pwg:JobState" -> jobState = nextText()
                                "pwg:JobStateReason" -> reasons.add(nextText())
                            }
                        }
                        END_TAG -> {
                            if (name == "scan:JobInfo") break
                        }
                    }
                    next()
                }
            }
            return JobInfo(uri, uuid, age, imagesCompleted, imagesToTransfer, jobState, reasons)
        }
    }
}
