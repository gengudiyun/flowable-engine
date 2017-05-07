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
package org.flowable.http.examples;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;

/**
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskExampleTest extends PluggableFlowableTestCase {
    @Deployment
    public void testExampleUsage() throws Exception {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("requestTimeout", 10000);

        String procId = runtimeService.startProcessInstanceByKey("exampleUsage", variables).getId();
        assertProcessEnded(procId);
    }
}
