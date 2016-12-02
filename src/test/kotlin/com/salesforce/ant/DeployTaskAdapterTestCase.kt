package com.salesforce.ant

import com.aquivalabs.force.ant.metadataConnection
import com.aquivalabs.force.ant.randomString
import com.nhaarman.mockito_kotlin.*
import com.sforce.soap.metadata.AsyncResult
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.lang.reflect.Modifier.isPublic
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.memberFunctions


class DeployTaskAdapterTestCase {
    @Test fun setZipBytes_withoutParameters_shouldBePublic() {
        val actual = DeployTaskAdapter::class.memberFunctions
            .single { it.name == DeployTask::setZipBytes.name && it.parameters.size == 1 }
        assertTrue(
            isPublic(actual.javaMethod!!.modifiers),
            "${actual.name} method should be public because it needs to be available from Kotlin code")
    }

    @Test fun setZipBytes_withByteArray_shouldSetBackingFieldValue() {
        val sut = DeployTaskAdapter()
        val expected = randomString().toByteArray()

        sut.setZipBytes(expected)

        assertEquals(sut.zipBytes, expected)
    }

    @Test fun getZipBytes_always_shouldReturnValueFromBackingField() {
        val sut = DeployTaskAdapter()
        sut.zipBytes = randomString().toByteArray()
        assertEquals(sut.getZipBytes(), sut.zipBytes)
    }

    @Test fun getDeployRoot_always_shouldReturnValueFromBackingField() {
        val sut = DeployTaskAdapter()
        sut.deployRoot = randomString()
        assertEquals(sut.getDeployRoot(), sut.deployRoot)
    }

    @Test fun handleResponse_always_shouldCorrectlyTransformStatusResultToAsyncResult() {
        val sut = spy<DeployTaskAdapter>()
        val inputConnection = metadataConnection()
        val inputResult = statusResult(id = randomString(),  isDone = true)

        sut.handleResponse(inputConnection, inputResult)

        verify(sut).handleResponse(
            same(inputConnection),
            argThat<AsyncResult>{ id == inputResult.id && isDone == inputResult.isDone })
    }

    private fun statusResult(id: String = "", isDone: Boolean = true): SFDCMDAPIAntTask.StatusResult {
        val result = mock<SFDCMDAPIAntTask.StatusResult>()
        doReturn(id).whenever(result).id
        doReturn(isDone).whenever(result).isDone
        return result
    }
}