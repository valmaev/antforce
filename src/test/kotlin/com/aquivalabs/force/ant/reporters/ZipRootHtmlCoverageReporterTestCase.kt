package com.aquivalabs.force.ant.reporters

import com.aquivalabs.force.ant.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.zip.ZipOutputStream


class ZipRootHtmlCoverageReporterTestCase : HtmlCoverageReporterTestCase<ZipRootHtmlCoverageReporter>() {
    override fun createSystemUnderTest(
        sourceDir: File?,
        outputDir: File,
        codeHighlighting: Boolean,
        dateTimeProvider: () -> LocalDateTime) =
        ZipRootHtmlCoverageReporter(sourceDir, outputDir, codeHighlighting, dateTimeProvider)

    override fun withDeployRoot(files: Map<String, String>, test: (File) -> Unit) = withTestDirectory {

        val zip = sourcesRoot(it)
        val fileOutput = FileOutputStream(zip)
        ZipOutputStream(fileOutput).use { zipOutput ->
            zipOutput.addEntry("classes", "")
            zipOutput.addEntry("triggers", "")
            files.forEach { zipOutput.addEntry(it.key, it.value) }
        }
        test(zip.parentFile)
    }

    override fun sourcesRoot(dir: File) = File(dir, "src.zip")
}