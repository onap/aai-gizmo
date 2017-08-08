package org.openecomp.schema;

import org.junit.Test;
import org.openecomp.crud.exception.CrudException;

import static org.junit.Assert.assertTrue;


public class OxmModelLoaderTest {

  @Test
  public void loadModels() {
    try {
      OxmModelLoader.loadModels();
    } catch (CrudException e) {
      e.printStackTrace();
      assertTrue(false);
    }
    assertTrue(OxmModelLoader.getVersionContextMap().size() > 0);
  }
}
