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
package org.onap.crud.util;

import org.eclipse.persistence.dynamic.DynamicEntity;
import org.eclipse.persistence.jaxb.JAXBMarshaller;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;

import java.io.StringReader;
import java.io.StringWriter;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

public class CrudJaxbTransformation {
  /**
   * Marshal a dynamic entity into a string.
   *
   * @param entity      the dynamic entity
   * @param jaxbContext the dynamic jaxb context
   * @return the marshaled entity
   * @throws RouterException on error
   */
  public static String marshal(MediaType mediaType, final DynamicEntity entity,
                               final DynamicJAXBContext jaxbContext) throws JAXBException {

    final JAXBMarshaller marshaller = jaxbContext.createMarshaller();
    marshaller.setProperty(JAXBMarshaller.JAXB_FORMATTED_OUTPUT, false);

    if (MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType)) {
      marshaller.setProperty("eclipselink.media-type", "application/json");
      marshaller.setProperty("eclipselink.json.include-root", false);
      marshaller.setProperty(MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, Boolean.FALSE);
    }

    final StringWriter writer = new StringWriter();
    marshaller.marshal(entity, writer);
    return writer.toString();

  }

  /**
   * @param type
   * @param json
   * @param mediaType
   * @return
   * @throws JAXBException
   * @throws Exception
   */
  public static Object unmarshal(String javaClass, String content, MediaType mediaType,
                                 final DynamicJAXBContext jaxbContext) throws JAXBException {
    Object clazz = null;
    Unmarshaller unmarshaller = null;

    clazz = jaxbContext.newDynamicEntity(javaClass);

    unmarshaller = jaxbContext.createUnmarshaller();
    if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
      unmarshaller.setProperty(UnmarshallerProperties.MEDIA_TYPE, "application/json");
      unmarshaller.setProperty(UnmarshallerProperties.JSON_INCLUDE_ROOT, false);
      unmarshaller.setProperty(UnmarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
    }

    return unmarshaller.unmarshal(new StreamSource(new StringReader(content)),
        clazz.getClass()).getValue();
  }

}
