/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2018 Nokia
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
package org.onap.crud.service;

import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import javax.naming.OperationNotSupportedException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.aai.event.api.EventConsumer;


@RunWith(MockitoJUnitRunner.class)
public class CrudAsyncResponseConsumerTest {

    private static final ArrayList<String> EVENTS = Lists.newArrayList("event_json1", "event_json2");

    @Mock
    private EventConsumer eventConsumer;
    @Mock
    private GraphEventUpdater graphEventUpdater;

    private CrudAsyncResponseConsumer crudAsyncResponseConsumer;

    @Before
    public void setUp() {
        crudAsyncResponseConsumer = new CrudAsyncResponseConsumer(eventConsumer, graphEventUpdater);
    }

    @Test
    public void shouldCommitOnlyOffsetsWhenEventsCollectionIsEmpty() throws Exception {
        // given
        when(eventConsumer.consume()).thenReturn(new ArrayList<>());

        // when
        crudAsyncResponseConsumer.run();

        // then
        verify(graphEventUpdater, never()).update(anyString());
        verify(eventConsumer, times(1)).commitOffsets();
    }

    @Test
    public void shouldCommitOnlyOffsetsWhenThereIsNoEventsToProcess() throws Exception {
        // given
        when(eventConsumer.consume()).thenReturn(null);

        // when
        crudAsyncResponseConsumer.run();

        // then
        verify(graphEventUpdater, never()).update(anyString());
        verify(eventConsumer, times(1)).commitOffsets();
    }

    @Test
    public void shouldProcessEventsWhenConsumerProvidesListOfEvents() throws Exception {
        // given
        when(eventConsumer.consume()).thenReturn(EVENTS);

        // when
        crudAsyncResponseConsumer.run();

        // then
        verify(graphEventUpdater, times(2)).update(anyString());
        verify(eventConsumer, times(1)).commitOffsets();
    }

    @Test
    public void shouldHandleAnyErrorCaseDuringCommitOffsets() throws Exception {
        // given
        when(eventConsumer.consume()).thenReturn(EVENTS);
        doThrow(OperationNotSupportedException.class).when(eventConsumer).commitOffsets();

        // when
        try {
            crudAsyncResponseConsumer.run();
        } catch (Exception e) {
            fail("Any error reported by run method is wrong!");
        }

        // then
        verify(graphEventUpdater, times(2)).update(anyString());

    }
}