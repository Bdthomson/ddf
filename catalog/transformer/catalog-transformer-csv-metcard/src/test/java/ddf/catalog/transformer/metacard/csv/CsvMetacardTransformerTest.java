package ddf.catalog.transformer.metacard.csv;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import static org.mockito.MockitoAnnotations.initMocks;

public class CsvMetacardTransformerTest {

  private CsvMetacardTransformer csvMetacardTransformer;

  @Before
  public void Before() {
    csvMetacardTransformer = new CsvMetacardTransformer();
    initMocks(this);

    //    when().thenReturn();
  }

  @Test
  public void testMetacard() throws CatalogTransformerException, IOException, ParseException {

    MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("Test Location");

    // Calling function under test
    BinaryContent content = csvMetacardTransformer.transform(metacard, null);

    assertEquals(
        content.getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());
  }

  @Test
  public void testMetacardWithAttributes()
      throws CatalogTransformerException, IOException, ParseException {

    MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("Test Location");

    final Map attributes = new HashMap<String, Serializable>();

    // Calling function under test
    BinaryContent content = csvMetacardTransformer.transform(metacard, attributes);

    assertEquals(
        content.getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacard() throws CatalogTransformerException {
    new CsvMetacardTransformer().transform(null, null);
  }
}
