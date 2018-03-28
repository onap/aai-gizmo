/**
 * ﻿============LICENSE_START=======================================================
 * Gizmo
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.crud.event.envelope;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.builder.EqualsBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class GraphEventHeader {

    @SerializedName("request-id")
    private String requestId;

    private String timestamp;

    @SerializedName("source-name")
    private String sourceName;

    @SerializedName("event-type")
    private String eventType;

    @SerializedName("validation-entity-type")
    private String validationEntityType;

    @SerializedName("validation-top-entity-type")
    private String validationTopEntityType;

    @SerializedName("entity-link")
    private String entityLink;

    private static final String MICROSERVICE_NAME = "GIZMO";
    private static final String EVENT_TYPE_PENDING_UPDATE = "pending-update";
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static class Builder {

        private String requestId;
        private String validationEntityType;
        private String validationTopEntityType;
        private String entityLink;

        public Builder requestId(String val) {
            requestId = val;
            return this;
        }

        public Builder validationEntityType(String val) {
            validationEntityType = val;
            return this;
        }

        public Builder validationTopEntityType(String val) {
            validationTopEntityType = val;
            return this;
        }

        public Builder entityLink(String val) {
            entityLink = val;
            return this;
        }

        public GraphEventHeader build() {
            return new GraphEventHeader(this);
        }
    }

    private GraphEventHeader(Builder builder) {
        requestId = builder.requestId != null ? builder.requestId : UUID.randomUUID().toString();
        timestamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").withZone(ZoneOffset.UTC).format(Instant.now());
        sourceName = MICROSERVICE_NAME;
        eventType = EVENT_TYPE_PENDING_UPDATE;

        validationEntityType = builder.validationEntityType;
        validationTopEntityType = builder.validationTopEntityType;
        entityLink = builder.entityLink;
    }

    /**
     * Serializes this object into a JSON string representation.
     *
     * @return a JSON format string representation of this object.
     */
    public String toJson() {
        return gson.toJson(this);
    }

    ///////////////////////////////////////////////////////////////////////////
    // GETTERS
    ///////////////////////////////////////////////////////////////////////////

    public String getRequestId() {
        return requestId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getEventType() {
        return eventType;
    }

    public String getValidationEntityType() {
        return validationEntityType;
    }

    public String getValidationTopEntityType() {
        return validationTopEntityType;
    }

    public String getEntityLink() {
        return entityLink;
    }

    ///////////////////////////////////////////////////////////////////////////
    // OVERRIDES
    ///////////////////////////////////////////////////////////////////////////

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.requestId, this.timestamp, this.sourceName, this.eventType, this.validationEntityType,
                this.validationTopEntityType, this.entityLink);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GraphEventHeader)) {
            return false;
        } else if (obj == this) {
            return true;
        }
        GraphEventHeader rhs = (GraphEventHeader) obj;
     // @formatter:off
     return new EqualsBuilder()
                  .append(requestId, rhs.requestId)
                  .append(timestamp, rhs.timestamp)
                  .append(sourceName, rhs.sourceName)
                  .append(eventType, rhs.sourceName)
                  .append(validationEntityType, rhs.validationEntityType)
                  .append(validationTopEntityType, rhs.validationTopEntityType)
                  .append(entityLink, rhs.entityLink)
                  .isEquals();
     // @formatter:on
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toJson();
    }

}
