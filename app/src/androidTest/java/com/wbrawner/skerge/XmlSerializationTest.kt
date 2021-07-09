package com.wbrawner.skerge

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class XmlSerializationTest {
    @Test
    fun parseScannerStatusTest() {
        val scannerStatus = ScannerStatus(
            version = "2.63",
            state = "Processing",
            jobs = listOf(
                JobInfo(
                    jobUri = "/eSCL/ScanJobs/60d1ddd4-4862-40c0-8519-4e743ecaa0a4",
                    jobUuid = "60d1ddd4-4862-40c0-8519-4e743ecaa0a4",
                    age = 6,
                    imagesCompleted = 0,
                    imagesToTransfer = 1,
                    jobState = "Processing",
                    jobStateReasons = listOf("JobScanning")
                ),
                JobInfo(
                    jobUri = "/eSCL/ScanJobs/168f23f8-d8e5-496b-a6b3-52bf5ff6c348",
                    jobUuid = "168f23f8-d8e5-496b-a6b3-52bf5ff6c348",
                    age = 11933504,
                    imagesCompleted = 1,
                    imagesToTransfer = 0,
                    jobState = "Completed",
                    jobStateReasons = listOf("JobCompletedSuccessfully")
                )
            )
        )
        val parsedScannerStatus = ScannerStatus.fromXml(scannerStatusXml)
        assertEquals(scannerStatus, parsedScannerStatus)
    }

    @Test
    fun serializeScanSettingsTest() {
        val scanSettings = ScanSettings(
            scanRegions = listOf(ScanRegion())
        )
        assertEquals(scanSettingsXml, scanSettings.toXml())
    }
}

private val scanSettingsXml = """
<scan:ScanSettings xmlns:copy="http://www.hp.com/schemas/imaging/con/copy/2008/07/07" 
    xmlns:dd="http://www.hp.com/schemas/imaging/con/dictionaries/1.0/" 
    xmlns:dd3="http://www.hp.com/schemas/imaging/con/dictionaries/2009/04/06" 
    xmlns:fw="http://www.hp.com/schemas/imaging/con/firewall/2011/01/05" 
    xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm" 
    xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03" 
    xmlns:scc="http://schemas.hp.com/imaging/escl/2011/05/03">
    <pwg:Version>2.1</pwg:Version>
    <scan:Intent>Document</scan:Intent>
    <pwg:ScanRegions>
        <pwg:ScanRegion>
            <pwg:Height>3300</pwg:Height>
            <pwg:Width>2550</pwg:Width>
            <pwg:XOffset>0</pwg:XOffset>
            <pwg:YOffset>0</pwg:YOffset>
        </pwg:ScanRegion>
    </pwg:ScanRegions>
    <pwg:InputSource>Platen</pwg:InputSource>
    <scan:DocumentFormatExt>application/pdf</scan:DocumentFormatExt>
    <scan:XResolution>300</scan:XResolution>
    <scan:YResolution>300</scan:YResolution>
    <scan:ColorMode>RGB24</scan:ColorMode>
    <scan:CompressionFactor>25</scan:CompressionFactor>
    <scan:Brightness>1000</scan:Brightness>
    <scan:Contrast>1000</scan:Contrast>
</scan:ScanSettings>
""".trim().replace(Regex("\\s{2,}"), "").replace("\n", "").replace("\"x", "\" x")

private val scannerStatusXml = """
    <?xml version="1.0" encoding="UTF-8"?>
    <!-- THIS DATA SUBJECT TO DISCLAIMER(S) INCLUDED WITH THE PRODUCT OF ORIGIN. -->
    <scan:ScannerStatus xmlns:scan="http://schemas.hp.com/imaging/escl/2011/05/03" xmlns:pwg="http://www.pwg.org/schemas/2010/12/sm" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://schemas.hp.com/imaging/escl/2011/05/03 ../../schemas/eSCL.xsd">
    	<pwg:Version>2.63</pwg:Version>
    	<pwg:State>Processing</pwg:State>
    	<scan:Jobs>
    		<scan:JobInfo>
    			<pwg:JobUri>/eSCL/ScanJobs/60d1ddd4-4862-40c0-8519-4e743ecaa0a4</pwg:JobUri>
    			<pwg:JobUuid>60d1ddd4-4862-40c0-8519-4e743ecaa0a4</pwg:JobUuid>
    			<scan:Age>6</scan:Age>
    			<pwg:ImagesCompleted>0</pwg:ImagesCompleted>
    			<pwg:ImagesToTransfer>1</pwg:ImagesToTransfer>
    			<pwg:JobState>Processing</pwg:JobState>
    			<pwg:JobStateReasons>
    				<pwg:JobStateReason>JobScanning</pwg:JobStateReason>
    			</pwg:JobStateReasons>
    		</scan:JobInfo>
    		<scan:JobInfo>
    			<pwg:JobUri>/eSCL/ScanJobs/168f23f8-d8e5-496b-a6b3-52bf5ff6c348</pwg:JobUri>
    			<pwg:JobUuid>168f23f8-d8e5-496b-a6b3-52bf5ff6c348</pwg:JobUuid>
    			<scan:Age>11933504</scan:Age>
    			<pwg:ImagesCompleted>1</pwg:ImagesCompleted>
    			<pwg:ImagesToTransfer>0</pwg:ImagesToTransfer>
    			<pwg:JobState>Completed</pwg:JobState>
    			<pwg:JobStateReasons>
    				<pwg:JobStateReason>JobCompletedSuccessfully</pwg:JobStateReason>
    			</pwg:JobStateReasons>
    		</scan:JobInfo>
    	</scan:Jobs>
    </scan:ScannerStatus>
""".trim().replace(Regex("\\s{2,}"), "").replace("\n", "")
