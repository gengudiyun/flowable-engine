/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.test.bpmn.event.escalation;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class BoundaryEscalationEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testCatchEscalationOnEmbeddedSubprocess() {
        runtimeService.startProcessInstanceByKey("boundaryEscalationOnEmbeddedSubprocess");

        // After process start, usertask in subprocess should exist
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("subprocessTask");

        // After task completion, escalation end event is reached and caught
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("task after catching the escalation");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/event/escalation/BoundaryEscalationEventTest.testCatchEscalationOnCallActivity-parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/escalation/BoundaryEscalationEventTest.subprocess.bpmn20.xml" })
    public void testCatchEscalationOnCallActivity() {
        String procId = runtimeService.startProcessInstanceByKey("catchEscalationOnCallActivity").getId();
        Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Task in subprocess");

        // Completing the task will reach the end error event,
        // which is caught on the call activity boundary
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("Escalated Task");

        // Completing the task will end the process instance
        taskService.complete(task.getId());
        assertProcessEnded(procId);
    }
    
    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/bpmn/event/escalation/BoundaryEscalationEventTest.testCatchEscalationOnCallActivitySuspendedParent.parent.bpmn20.xml",
            "org/flowable/engine/test/bpmn/event/escalation/BoundaryEscalationEventTest.testCatchEscalationOnCallActivitySuspendedParent.child.bpmn20.xml" })
    public void testCatchEscalationOnCallActivitySuspendedParent() {
        ProcessInstance escalationParent = runtimeService.startProcessInstanceByKey("escalationParent");
        String parentProcId = escalationParent.getId();
        String childProcId = runtimeService.createProcessInstanceQuery().processDefinitionKey("escalationChild").singleResult().getId();
        Execution boundaryEventExecution = runtimeService.createExecutionQuery().activityId("boundaryEventId").singleResult();
        String boundaryEventExecutionId = boundaryEventExecution.getId();

        runtimeService.suspendProcessInstanceById(parentProcId);

        // Propagates escalation from the child process instance
        ThrowingCallable propagateEscalation = () -> managementService
                        .executeJob(managementService.createJobQuery().processInstanceId(childProcId).singleResult().getId());
        assertThatThrownBy(propagateEscalation)
                .isInstanceOf(FlowableException.class)
                .hasMessage(
                        "Cannot propagate escalation 'testChildEscalation' with code 'testEscalationCode', because Execution[ id '%s' ] - definition '%s' - activity 'boundaryEventId' - parent '%s' is suspended".formatted(
                                boundaryEventExecutionId, escalationParent.getProcessDefinitionId(), boundaryEventExecution.getParentId()));
    }
}
