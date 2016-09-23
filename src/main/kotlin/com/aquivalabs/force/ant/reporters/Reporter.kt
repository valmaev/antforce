package com.aquivalabs.force.ant.reporters

import com.sforce.soap.metadata.DeployResult

interface Reporter<out T> {
    fun createReport(deployResult: DeployResult): T
}