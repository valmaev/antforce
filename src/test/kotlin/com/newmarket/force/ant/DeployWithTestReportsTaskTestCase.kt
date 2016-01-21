package com.newmarket.force.ant

import org.apache.tools.ant.Task
import org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers.*;
import org.testng.annotations.Test


public class DeployWithTestReportsTaskTestCase {

    @Test fun sut_always_shouldDeriveFromTaskClass() =
        assertThat(DeployWithTestReportsTask(), instanceOf(Task::class.java))
}