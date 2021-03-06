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
package org.onap.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response.Status;
import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.oxm.XMLField;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.crud.util.CrudServiceConstants;
import org.onap.crud.util.CrudServiceUtil;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class OxmModelValidator {
  private static Logger logger = LoggerFactory.getInstance().getLogger(OxmModelValidator.class.getName());
  private static final String OXM_LOAD_ERROR = "Error loading oxm model";
  
  public enum Metadata {
    NODE_TYPE("aai-node-type"),
    URI("aai-uri"),
    CREATED_TS("aai-created-ts"),
    UPDATED_TS("aai-last-mod-ts"),
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

  public static Map<String, Object> resolveCollectionfilter(String version, String type, Map<String, String> filter)
      throws CrudException {

    DynamicJAXBContext jaxbContext = null;
    try {
      jaxbContext = OxmModelLoader.getContextForVersion(version);
    } catch (Exception e) {
      throw new CrudException(e);
    }

    Map<String, Object> result = new HashMap<String, Object>();
    if (jaxbContext == null) {
      logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, OXM_LOAD_ERROR + ": " + version);
      throw new CrudException(OXM_LOAD_ERROR + ": " + version, Status.NOT_FOUND);
    }
    final DynamicType modelObjectType = OxmModelLoader.getDynamicTypeForVersion(version, type);
    final DynamicType reservedObjectType = jaxbContext.getDynamicType("ReservedPropNames");

    for (String key : filter.keySet()) {
        if ((key == CrudServiceConstants.CRD_RESERVED_VERSION )  || key == CrudServiceConstants.CRD_RESERVED_NODE_TYPE ) {
          result.put ( key, filter.get ( key ) );
          continue;
        }
      String keyJavaName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);
      DatabaseMapping mapping = modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName);

      // Try both the model for the specified type and the reserved properties for our key
      if (mapping == null) {
        mapping = reservedObjectType.getDescriptor().getMappingForAttributeName(keyJavaName);
      }
      if (mapping != null) {
        try {
          Object value = CrudServiceUtil.validateFieldType(filter.get(key), mapping.getField().getType());
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
    } catch (CrudException ce) {
      throw new CrudException(ce.getMessage(), ce.getHttpStatus());
    } catch (Exception e) {
      throw new CrudException(e);
    }

    if (jaxbContext == null) {
      logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, OXM_LOAD_ERROR + ": " + version);
      throw new CrudException(OXM_LOAD_ERROR + ": " + version, Status.NOT_FOUND);
    }
    // Determine if the Object part is a collection type in the model
    // definition
    final DynamicType modelObjectType = OxmModelLoader.getDynamicTypeForVersion(version, type);

    if (modelObjectType == null) {
      logger.error(CrudServiceMsgs.INVALID_OXM_FILE, "Object of collection type not found: " + type);
      throw new CrudException("Object of collection type not found: " + type, Status.NOT_FOUND);
    }

    if (modelObjectType.getDescriptor().getMappings().size() == 1
        && modelObjectType.getDescriptor().getMappings().get(0).isCollectionMapping()) {
      String childJavaObjectName = modelObjectType.getDescriptor().getMappings().get(0).getAttributeName();
      childJavaObjectName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, childJavaObjectName);
      final DynamicType childObjectType = jaxbContext.getDynamicType(childJavaObjectName);
      if (childObjectType == null) {
        // Should not happen as child object is defined in oxm model itself
        logger.error(CrudServiceMsgs.INVALID_OXM_FILE, "Child Object Type for Java Object not found: " + childJavaObjectName);
        throw new CrudException("Child Object Type for Java Object not found: " + childJavaObjectName, Status.NOT_FOUND);
      }
      return childObjectType.getDescriptor().getTableName();
    } else {
      return modelObjectType.getDescriptor().getTableName();
    }

  }

  public static Vertex validateIncomingUpsertPayload(String id, String version, String type, JsonElement properties)
      throws CrudException {

    try {
      type = resolveCollectionType(version, type);
      DynamicJAXBContext jaxbContext = OxmModelLoader.getContextForVersion(version);

      final DynamicType modelObjectType = OxmModelLoader.getDynamicTypeForVersion(version, type);
      final DynamicType reservedType = jaxbContext.getDynamicType("ReservedPropNames");

      Set<Map.Entry<String, JsonElement>> payloadEntriesSet = properties.getAsJsonObject().entrySet();

      // loop through input to validate against schema
      for (Map.Entry<String, JsonElement> entry : payloadEntriesSet) {
        String keyJavaName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entry.getKey());

        // check for valid field
        if (modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName) == null) {
          if (reservedType.getDescriptor().getMappingForAttributeName(keyJavaName) == null) {
            throw new CrudException("Invalid field: " + entry.getKey(), Status.BAD_REQUEST);
          }
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

          if (((XMLField) field).isRequired() && !entriesMap.containsKey(keyName) && !defaultValue.isEmpty()) {
            modelVertexBuilder.property(keyName, CrudServiceUtil.validateFieldType(defaultValue, field.getType()));
          }
          // if schema field is required and not set then reject
          if (((XMLField) field).isRequired() && !entriesMap.containsKey(keyName) && defaultValue.isEmpty()) {
            throw new CrudException("Missing required field: " + keyName, Status.BAD_REQUEST);
          }
          // If invalid field then reject
          if (entriesMap.containsKey(keyName)) {
            Object value = CrudServiceUtil.validateFieldType(entriesMap.get(keyName).getAsString(), field.getType());
            modelVertexBuilder.property(keyName, value);
          }

          // Set defaults
          if (!defaultValue.isEmpty() && !entriesMap.containsKey(keyName)) {
            modelVertexBuilder.property(keyName, CrudServiceUtil.validateFieldType(defaultValue, field.getType()));
          }
        }
      }

      // Handle reserved properties
      for (DatabaseMapping mapping : reservedType.getDescriptor().getMappings()) {
        if (mapping.isAbstractDirectMapping()) {
          DatabaseField field = mapping.getField();
          String keyName = field.getName().substring(0, field.getName().indexOf("/"));

          if (entriesMap.containsKey(keyName)) {
            Object value = CrudServiceUtil.validateFieldType(entriesMap.get(keyName).getAsString(), field.getType());
            modelVertexBuilder.property(keyName, value);
          }
        }
      }

      return modelVertexBuilder.build();
    } catch (CrudException ce) {
      throw new CrudException(ce.getMessage(), ce.getHttpStatus());
    } catch (Exception e) {
      throw new CrudException(e.getMessage(), Status.BAD_REQUEST);
    }
  }

  public static Vertex validateIncomingPatchPayload(String id, String version, String type, JsonElement properties,
      Vertex existingVertex) throws CrudException {
    try {
      type = resolveCollectionType(version, type);
      DynamicJAXBContext jaxbContext = OxmModelLoader.getContextForVersion(version);
      
      final DynamicType modelObjectType =  OxmModelLoader.getDynamicTypeForVersion(version, type);
      final DynamicType reservedType = jaxbContext.getDynamicType("ReservedPropNames");

      Set<Map.Entry<String, JsonElement>> payloadEntriesSet = properties.getAsJsonObject().entrySet();

      // Loop through the payload properties and merge with existing
      // vertex props
      for (Map.Entry<String, JsonElement> entry : payloadEntriesSet) {

        String keyJavaName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, entry.getKey());

        DatabaseField field = null;
        String defaultValue = null;

        if (modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName) != null) {
          field = modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName).getField();
          defaultValue = modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName).getProperties()
              .get("defaultValue") == null ? ""
                  : modelObjectType.getDescriptor().getMappingForAttributeName(keyJavaName).getProperties()
                      .get("defaultValue").toString();
        } else if (reservedType.getDescriptor().getMappingForAttributeName(keyJavaName) != null) {
          field = reservedType.getDescriptor().getMappingForAttributeName(keyJavaName).getField();
          defaultValue = "";
        }

        if (field == null) {
          throw new CrudException("Invalid field: " + entry.getKey(), Status.BAD_REQUEST);
        }

        // check if mandatory field is not set to null
        if (((XMLField) field).isRequired() && entry.getValue() instanceof JsonNull && !defaultValue.isEmpty()) {
          existingVertex.getProperties().put(entry.getKey(),
              CrudServiceUtil.validateFieldType(defaultValue, field.getType()));
        } else if (((XMLField) field).isRequired() && entry.getValue() instanceof JsonNull && defaultValue.isEmpty()) {
          throw new CrudException("Mandatory field: " + entry.getKey() + " can't be set to null", Status.BAD_REQUEST);
        } else if (!((XMLField) field).isRequired() && entry.getValue() instanceof JsonNull
            && existingVertex.getProperties().containsKey(entry.getKey())) {
          existingVertex.getProperties().remove(entry.getKey());
        } else if (!(entry.getValue() instanceof JsonNull)) {
          // add/update the value if found in existing vertex
          Object value = CrudServiceUtil.validateFieldType(entry.getValue().getAsString(), field.getType());
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
    Vertex.Builder modelVertexBuilder = new Vertex.Builder(vertex.getType()).id(vertex.getId().get());

    try {
      DynamicJAXBContext jaxbContext = OxmModelLoader.getContextForVersion(version);

      final DynamicType modelObjectType = OxmModelLoader.getDynamicTypeForVersion(version,
          vertex.getProperties().get(Metadata.NODE_TYPE.propertyName()) != null
              ? vertex.getProperties().get(Metadata.NODE_TYPE.propertyName()).toString()
              : vertex.getType());
      final DynamicType reservedObjectType = jaxbContext.getDynamicType("ReservedPropNames");

      for (String key : vertex.getProperties().keySet()) {
        DatabaseField field = getDatabaseField(key, modelObjectType);
        if (field == null) {
          field = getDatabaseField(key, reservedObjectType);
        }
        if (field != null) {
          modelVertexBuilder.property(key, vertex.getProperties().get(key));
        }
      }

      return modelVertexBuilder.build();
    } catch (Exception ex) {
      return vertex;
    }

  }

}
