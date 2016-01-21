package com.newmarket.force.ant

import org.apache.tools.ant.Project
import org.apache.tools.ant.Task
import org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers.*;
import org.testng.annotations.Test


public class DeployWithTestReportsTaskTestCase {

    @Test fun sut_always_shouldDeriveFromTaskClass() =
        assertThat(createSystemUnderTest(), instanceOf(Task::class.java))

    @Test fun createBatchTest_always_shouldAddNewBatchTestToTests() {
        val sut = createSystemUnderTest()
        val actual = sut.createBatchTest()
        assertThat(sut.tests, contains(actual))
        assertThat(sut.getProject(), sameInstance(actual.project))
    }

    @Test fun createBatchTest_always_shouldCreatePrefixInName() {
        assertThat(
            "Prefix 'create' is one of the Ant's conventions for nested elements declaration. " +
                "See the manual: http://ant.apache.org/manual/develop.html#nested-elements",
            DeployWithTestReportsTask::createBatchTest.name,
            startsWith("create"))
    }

    fun createSystemUnderTest(project: Project = createProject()): DeployWithTestReportsTask {
        val sut = DeployWithTestReportsTask()
        sut.setProject(project)
        return sut
    }

    fun createProject(name: String = "TestProject"): Project {
        val project = Project()
        project.name = name
        return project
    }
}