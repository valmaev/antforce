package com.aquivalabs.force.ant

import org.testng.annotations.Test
import org.testng.Assert.*
import org.testng.annotations.DataProvider


class RunTestSuccessTestCase {

    @Test(dataProvider = "qualifiedClassNameTestData")
    fun qualifiedClassName_always_shouldReturnExpectedResult(
        namespace: String?,
        name: String?,
        expected: String) {

        val sut = runTestSuccess(name = name, namespace = namespace)
        assertEquals(sut.qualifiedClassName, expected)
    }

    @DataProvider
    fun qualifiedClassNameTestData() = qualifiedNameCommonTestData()
}

class RunTestFailureTestCase {

    @Test(dataProvider = "qualifiedClassNameTestData")
    fun qualifiedClassName_always_shouldReturnExpectedResult(
        namespace: String?,
        name: String?,
        expected: String) {

        val sut = runTestFailure(name = name, namespace = namespace)
        assertEquals(sut.qualifiedClassName, expected)
    }

    @DataProvider
    fun qualifiedClassNameTestData() = qualifiedNameCommonTestData()
}