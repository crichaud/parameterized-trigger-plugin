/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Tom Huybrechts
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.parameterizedtrigger.test;

import hudson.model.Build;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Queue;
import hudson.plugins.parameterizedtrigger.BuildTrigger;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.PredefinedBuildParameters;
import hudson.plugins.parameterizedtrigger.ResultCondition;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.SleepBuilder;
import org.jvnet.hudson.test.UnstableBuilder;

public class ResultConditionTest extends HudsonTestCase {

    public void testTriggerByStableBuild() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.FAILED);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertEquals(2, projectB.getLastBuild().getNumber());
        
        schedule(projectA, projectB, ResultCondition.UNSTABLE);
        assertEquals(2, projectB.getLastBuild().getNumber());
        
        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(2, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(3, projectB.getLastBuild().getNumber());
    }

    public void testTriggerByUnstableBuild() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new UnstableBuilder());
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());

        schedule(projectA, projectB, ResultCondition.FAILED);
        assertNull(projectB.getLastBuild());

        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.UNSTABLE);
        assertEquals(2, projectB.getLastBuild().getNumber());
        
        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(3, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(4, projectB.getLastBuild().getNumber());
    }

    private void schedule(Project projectA, Project projectB, ResultCondition condition)
            throws IOException, InterruptedException, ExecutionException {
        projectA.getPublishersList().replace(new BuildTrigger(new BuildTriggerConfig("projectB", condition, new PredefinedBuildParameters(""))));
        hudson.rebuildDependencyGraph();
        projectA.scheduleBuild2(0).get();
        Queue.Item q = hudson.getQueue().getItem(projectB);
        if (q != null) q.getFuture().get();
    }

    private Build<?,?> waitForBuildStarts(Project<?,?> project, long timeoutMillis) throws Exception {
        long current = System.currentTimeMillis();
        while(project.getLastBuild() == null || !project.getLastBuild().isBuilding()) {
            assertTrue(System.currentTimeMillis() - current < timeoutMillis);
            Thread.sleep(100);
        }
        Build<?,?> build = project.getLastBuild();
        assertTrue(build.isBuilding());
        assertNotNull(build.getExecutor());
        
        return build;
    }
    
    private void scheduleAndAbort(Project<?,?> projectA, Project<?,?> projectB, ResultCondition condition)
            throws Exception {
        projectA.getPublishersList().replace(new BuildTrigger(new BuildTriggerConfig(projectB.getFullName(), condition, new PredefinedBuildParameters(""))));
        hudson.rebuildDependencyGraph();
        projectA.scheduleBuild2(0);
        Build<?,?> build = waitForBuildStarts(projectA, 5000);
        build.getExecutor().interrupt();
        waitUntilNoActivity();
        assertBuildStatus(Result.ABORTED, build);
    }

    public void testTriggerByFailedBuild() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new FailureBuilder());
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());

        schedule(projectA, projectB, ResultCondition.FAILED);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.UNSTABLE);
        assertEquals(1, projectB.getLastBuild().getNumber());
        
        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(2, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(3, projectB.getLastBuild().getNumber());
    }

    public void testTriggerByAbortedBuild() throws Exception {
        Project projectA = createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new AbortedBuilder());
        Project projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);

        schedule(projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());

        schedule(projectA, projectB, ResultCondition.FAILED);
        assertNull(projectB.getLastBuild());

        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertNull(projectB.getLastBuild());

        schedule(projectA, projectB, ResultCondition.UNSTABLE);
        assertNull(projectB.getLastBuild());

        schedule(projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());

        schedule(projectA, projectB, ResultCondition.ALWAYS);
        assertEquals(2, projectB.getLastBuild().getNumber());
    }

    public void testTriggerByAbortedByInturrupted() throws Exception {
        FreeStyleProject projectA = createFreeStyleProject("projectA");
        projectA.getBuildersList().add(new SleepBuilder(10000));
        FreeStyleProject projectB = createFreeStyleProject("projectB");
        projectB.setQuietPeriod(1);
        
        scheduleAndAbort(projectA, projectB, ResultCondition.SUCCESS);
        assertNull(projectB.getLastBuild());
        
        scheduleAndAbort(projectA, projectB, ResultCondition.FAILED);
        assertNull(projectB.getLastBuild());
        
        scheduleAndAbort(projectA, projectB, ResultCondition.UNSTABLE_OR_BETTER);
        assertNull(projectB.getLastBuild());
        
        scheduleAndAbort(projectA, projectB, ResultCondition.UNSTABLE);
        assertNull(projectB.getLastBuild());
        
        scheduleAndAbort(projectA, projectB, ResultCondition.UNSTABLE_OR_WORSE);
        assertEquals(1, projectB.getLastBuild().getNumber());
        
        scheduleAndAbort(projectA, projectB, ResultCondition.FAILED_OR_BETTER);
        assertEquals(1, projectB.getLastBuild().getNumber());
        
        scheduleAndAbort(projectA, projectB, ResultCondition.ALWAYS);
        assertEquals(2, projectB.getLastBuild().getNumber());
    }

}
