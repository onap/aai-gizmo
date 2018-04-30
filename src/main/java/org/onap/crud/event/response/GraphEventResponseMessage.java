/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.crud.event.response;

import java.text.MessageFormat;

/**
 * Graph event handling messages, including logging and exceptions.
 */
public enum GraphEventResponseMessage {

    //@formatter:off
    BASE_OPERATION_LOG_MESSAGE("Event response received: {0} with key: {1}, transaction-id: {2}, operation: {3}, result: {4}"),
    OPERATION_ERROR_LOG_MESSAGE("{0}, error: {1}"),
    OPERATION_ERROR_EXCEPTION_MESSAGE("Operation Failed with transaction-id: {0}. Error: {1}"),
    POLICY_VIOLATION_LOG_MESSAGE("Event response received: transaction-id: {0}, event source: {1}, event type: {2}, object key: {3}, object type: {4}, operation: {5}, result: policy violations detected in request, error: {6}"),
    POLICY_VIOLATION_EXCEPTION_MESSAGE("Operation Failed with transaction-id {0}. Error: policy violations detected in request, {1}");
    //@formatter:on

    private String message;

    private GraphEventResponseMessage(String message) {
        this.message = message;
    }

    /**
     * @param args to be formatted
     * @return the formatted error message
     */
    public String getMessage(Object... args) {
        MessageFormat formatter = new MessageFormat("");
        formatter.applyPattern(this.message);
        return formatter.format(args);
    }
}
