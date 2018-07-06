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
package org.onap.crud.service;

import java.util.Objects;
import java.util.TimerTask;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.event.api.EventConsumer;
import org.onap.crud.logging.CrudServiceMsgs;

public class CrudAsyncResponseConsumer extends TimerTask {

    private static Logger logger = LoggerFactory.getInstance().getLogger(CrudAsyncResponseConsumer
        .class.getName());

    private final EventConsumer asyncResponseConsumer;
    private final GraphEventUpdater graphEventUpdater;


    public CrudAsyncResponseConsumer(EventConsumer asyncResponseConsumer, GraphEventUpdater graphEventUpdater) {
        Objects.requireNonNull(asyncResponseConsumer);
        Objects.requireNonNull(graphEventUpdater);
        this.asyncResponseConsumer = asyncResponseConsumer;
        this.graphEventUpdater = graphEventUpdater;
        logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO,
            "CrudAsyncResponseConsumer initialized SUCCESSFULLY! with event consumer "
                + asyncResponseConsumer.getClass().getName());
    }


    @Override
    public void run() {

        logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO, "Listening for graph events");

        try {
            Iterable<String> events = asyncResponseConsumer.consume();
            processEvents(events);
            asyncResponseConsumer.commitOffsets();
        } catch (Exception e) {
            logger.error(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_ERROR, e, e.getMessage());
        }
    }

    private void processEvents(Iterable<String> events) {
        if (areEventsAvailable(events)) {
            for (String event : events) {
                graphEventUpdater.update(event);
            }
            logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO, "No events recieved");
        } else {
            logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO, "No events recieved");
        }
    }

    private boolean areEventsAvailable(Iterable<String> events) {
        return !(events == null || !events.iterator().hasNext());
    }

}