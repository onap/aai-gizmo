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
package org.openecomp.schema;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.oxm.XMLField;
import org.onap.aaiutils.oxm.OxmModelLoader;
import org.openecomp.crud.entity.Vertex;
import org.openecomp.crud.exception.CrudException;
import org.openecomp.crud.util.CrudServiceUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response.Status;

public class OxmModelValidator {
  public enum Metadata {
    NODE_TYPE("aai-node-type"),
    URI("aai-uri"),
    CREATED_TS("aai-created-ts"),
    SOT("source-of-truth"),
    LAST_MOD_SOT("last-mod-source-of-truth");

    private final String propName;

    Metadata(String propName) {
      this.propName = propName;
    }

    public String propertyName() {
      return propName;
    }

    public static boolean isProperty(String property) {
      for (Metadata meta : Metadata.values()) {
        if (meta.propName.equals(property)) {
          return true;
        }
      }
      return false;
    }
  }


  public static Map<String, Object> resolveCollectionfilter(String version, String type,
                                                            Map<String, String> filter)
      throws CrudException {

    DynamicJAXBContext jaxbContext = null;
    try {
      jaxbContext = OxmModelLoader.getContextForVersion(version);
    } catch (Exception e) {
      throw new CrudException(e);
    }

    Map<String, Object> result = new HashMap<String, Object>();
    if (jaxbContext == null) {
      throw new CrudException("", Status.NOT_FOUND);
    }
    final DynamicType modelObjectType = jaxbContext.getDynamicType(CaseFormat.LOWER_CAMEL
        .to(CaseFormat.UPPER_CAMEL,
        CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, type)));

    for (String key : filter.keySet()) {
      String keyJavaName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
      if (modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName) != null) {
        try {
          DatabaseMapping mapping = modelObjectType.getDescriptor()
              .getMappingForAttributeName(keyJavaName);
          Object value = CrudServiceUtil.validateFieldType(filter.get(key),
              mapping.getField().getType());
          result.put(key, value);
        } catch (Exception ex) {
          // Skip any exceptions thrown while validating the filter
          // key value
          continue;
        }
      }
    }

    return result;

  }

  public static String resolveCollectionType(String version, String type) throws CrudException {

    DynamicJAXBContext jaxbContext = null;
    try {
      jaxbContext = OxmModelLoader.getContextForVersion(version);
    } catch (Exception e) {
      throw new CrudException(e);
    }

    if (jaxbContext == null) {
      throw new CrudException("", Status.NOT_FOUND);
    }
    // Determine if the Object part is a collection type in the model
    // definition
    final DynamicType modelObjectType = jaxbContext.getDynamicType(CaseFormat.LOWER_CAMEL
        .to(CaseFormat.UPPER_CAMEL,
        CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, type)));

    if (modelObjectType == null) {
      throw new CrudException("", Status.NOT_FOUND);
    }

    if (modelObjectType.getDescriptor().getMappings().size() == 1
        && modelObjectType.getDescriptor().getMappings().get(0).isCollectionMapping()) {
      String childJavaObjectName = modelObjectType.getDescriptor().getMappings()
          .get(0).getAttributeName();
      childJavaObjectName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, childJavaObjectName);
      final DynamicType childObjectType = jaxbContext.getDynamicType(childJavaObjectName);
      if (childObjectType == null) {
        // Should not happen as child object is defined in oxm model
        // itself
        throw new CrudException("", Status.NOT_FOUND);
      }
      return childObjectType.getDescriptor().getTableName();
    } else {
      return modelObjectType.getDescriptor().getTableName();
    }

  }


  public static Vertex validateIncomingUpsertPayload(String id, String version, String type,
                                                     JsonElement properties)
      throws CrudException {

    try {
      type = resolveCollectionType(version, type);
      DynamicJAXBContext jaxbContext = OxmModelLoader.getContextForVersion(version);
      String modelObjectClass = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL,
          CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, type));

      final DynamicType modelObjectType = jaxbContext.getDynamicType(modelObjectClass);

      Set<Map.Entry<String, JsonElement>> payloadEntriesSet = properties.getAsJsonObject()
          .entrySet();

      //loop through input to validate against schema
      for (Map.Entry<String, JsonElement> entry : payloadEntriesSet) {
        String keyJavaName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entry.getKey());

        // check for valid field
        if (modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName) == null) {
          throw new CrudException("Invalid field: " + entry.getKey(), Status.BAD_REQUEST);
        }

      }

      Map<String, JsonElement> entriesMap = new HashMap<String, JsonElement>();
      for (Map.Entry<String, JsonElement> entry : payloadEntriesSet) {
        entriesMap.put(entry.getKey(), entry.getValue());
      }

      Vertex.Builder modelVertexBuilder = new Vertex.Builder(type);
      if (id != null) {
        modelVertexBuilder.id(id);
      }
      for (DatabaseMapping mapping : modelObjectType.getDescriptor().getMappings()) {
        if (mapping.isAbstractDirectMapping()) {
          DatabaseField field = mapping.getField();
          String defaultValue = mapping.getProperties().get("defaultValue") == null ? ""
              : mapping.getProperties().get("defaultValue").toString();

          String keyName = field.getName().substring(0, field.getName().indexOf("/"));

          if (((XMLField) field).isRequired() && !entriesMap.containsKey(keyName)
              && !defaultValue.isEmpty()) {
            modelVertexBuilder.property(keyName,
                CrudServiceUtil.validateFieldType(defaultValue, field.getType()));
          }
          // if schema field is required and not set then reject
          if (((XMLField) field).isRequired() && !entriesMap.containsKey(keyName)
              && defaultValue.isEmpty()) {
            throw new CrudException("Missing required field: " + keyName, Status.BAD_REQUEST);
          }
          // If invalid field then reject
          if (entriesMap.containsKey(keyName)) {
            Object value = CrudServiceUtil.validateFieldType(entriesMap.get(keyName)
                .getAsString(), field.getType());
            modelVertexBuilder.property(keyName, value);
          }

          // Set defaults
          if (!defaultValue.isEmpty() && !entriesMap.containsKey(keyName)) {
            modelVertexBuilder.property(keyName,
                CrudServiceUtil.validateFieldType(defaultValue, field.getType()));
          }
        }
      }

      return modelVertexBuilder.build();
    } catch (Exception e) {
      throw new CrudException(e.getMessage(), Status.BAD_REQUEST);
    }
  }

  public static Vertex validateIncomingPatchPayload(String id, String version, String type,
                                                    JsonElement properties, Vertex existingVertex)
      throws CrudException {

    try {
      type = resolveCollectionType(version, type);
      DynamicJAXBContext jaxbContext = OxmModelLoader.getContextForVersion(version);
      String modelObjectClass = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL,
          CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, type));

      final DynamicType modelObjectType = jaxbContext.getDynamicType(modelObjectClass);

      Set<Map.Entry<String, JsonElement>> payloadEntriesSet = properties.getAsJsonObject()
          .entrySet();

      // Loop through the payload properties and merge with existing
      // vertex props
      for (Map.Entry<String, JsonElement> entry : payloadEntriesSet) {

        String keyJavaName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entry.getKey());

        // check for valid field
        if (modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName) == null) {
          throw new CrudException("Invalid field: " + entry.getKey(), Status.BAD_REQUEST);
        }

        DatabaseField field = modelObjectType.getDescriptor()
            .getMappingForAttributeName(keyJavaName).getField();
        String defaultValue = modelObjectType.getDescriptor()
            .getMappingForAttributeName(keyJavaName)
            .getProperties().get("defaultValue") == null ? ""
            : modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName)
            .getProperties().get("defaultValue").toString();

        // check if mandatory field is not set to null
        if (((XMLField) field).isRequired() && entry.getValue() instanceof JsonNull
            && !defaultValue.isEmpty()) {
          existingVertex.getProperties().put(entry.getKey(),
              CrudServiceUtil.validateFieldType(defaultValue, field.getType()));
        } else if (((XMLField) field).isRequired() && entry.getValue() instanceof JsonNull
            && defaultValue.isEmpty()) {
          throw new CrudException("Mandatory field: " + entry.getKey()
              + " can't be set to null",
              Status.BAD_REQUEST);
        } else if (!((XMLField) field).isRequired() && entry.getValue() instanceof JsonNull
            && existingVertex.getProperties().containsKey(entry.getKey())) {
          existingVertex.getProperties().remove(entry.getKey());
        } else if (!(entry.getValue() instanceof JsonNull)) {
          // add/update the value if found in existing vertex
          Object value = CrudServiceUtil.validateFieldType(entry.getValue().getAsString(),
              field.getType());
          existingVertex.getProperties().put(entry.getKey(), value);
        }

      }

      return existingVertex;
    } catch (Exception e) {
      throw new CrudException(e.getMessage(), Status.BAD_REQUEST);
    }

  }

  private static DatabaseField getDatabaseField(String fieldName, DynamicType modelObjectType) {
    for (DatabaseField field : modelObjectType.getDescriptor().getAllFields()) {
      int ix = field.getName().indexOf("/");
      if (ix <= 0) {
        ix = field.getName().length();
      }

      String keyName = field.getName().substring(0, ix);
      if (fieldName.equals(keyName)) {
        return field;
      }
    }
    return null;
  }

  public static Vertex validateOutgoingPayload(String version, Vertex vertex) {

    Vertex.Builder modelVertexBuilder = new Vertex.Builder(vertex.getType())
        .id(vertex.getId().get());

    try {
      DynamicJAXBContext jaxbContext = OxmModelLoader.getContextForVersion(version);
      String modelObjectClass = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL,
          CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL,
              vertex.getProperties().get(Metadata.NODE_TYPE.propertyName()) != null
                  ? vertex.getProperties().get(Metadata.NODE_TYPE.propertyName()).toString() : vertex.getType()));
      final DynamicType modelObjectType = jaxbContext.getDynamicType(modelObjectClass);

      for (String key : vertex.getProperties().keySet()) {
        DatabaseField field = getDatabaseField(key, modelObjectType);
        if (field != null) {
          if (!Metadata.isProperty(key)) {
            modelVertexBuilder.property(key, vertex.getProperties().get(key));
          }
        }
      }
      return modelVertexBuilder.build();
    } catch (Exception ex) {
      return vertex;
    }

  }


}
