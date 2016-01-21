package com.newmarket.force.ant

import com.salesforce.ant.DeployTask
import java.util.*


public class DeployWithTestReportsTask : DeployTask() {
    internal final val tests = HashSet<BatchTest>()

    public fun createBatchTest(): BatchTest {
        val batch = BatchTest(getProject())
        tests.add(batch)
        return batch
    }

    public override fun getRunTests(): Array<out String>? {
        val allTests = super.getRunTests().toArrayList()
        tests.forEach { allTests.addAll(it.getFileNames()) }
        return allTests.toTypedArray()
    }
}