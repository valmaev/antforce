package com.aquivalabs.force.ant

import com.salesforce.ant.SFDCAntTask
import com.sforce.soap.metadata.AsyncResult
import com.sforce.soap.metadata.DeployStatus


fun SFDCAntTask.waitFor(maxPoll: Int = 200, pollWaitMillis: Int = 1000, action: () -> AsyncResult) {
    try {
        val result = action()
        log("Request ID for the current deploy task: ${result.id}")
        log("Waiting for server to finish processing the request...")

        repeat(maxPoll) {
            val deployStatus = metadataConnection.checkDeployStatus(result.id, false)
            if (deployStatus.done) {
                log("Request Status: ${deployStatus.status}")
                if (deployStatus.status == DeployStatus.Succeeded
                    || deployStatus.status == DeployStatus.SucceededPartial)
                    log("Finished request ${result.id} successfully.")
                return
            } else {
                log("Request Status: InProgress")
                Thread.sleep(pollWaitMillis.toLong())
            }
        }
    } catch (ex: Exception) {
        log("Request status: Failed")
        throw ex
    }
}