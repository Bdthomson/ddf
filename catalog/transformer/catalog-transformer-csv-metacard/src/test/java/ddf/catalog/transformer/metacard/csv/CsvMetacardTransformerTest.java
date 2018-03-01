package ddf.catalog.transformer.metacard.csv;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

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
    metacard.setAttribute("name", "ThisIsMyName");
    metacard.setAttribute("alias", "Name");

    final String ATTRIBUTE_CONFIG =
        "{\n"
            + "  \"attributes\": [\n"
            + "    {\n"
            + "      \"name\": \"name\",\n"
            + "      \"alias\": \"Name\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"name\": \"created_at\",\n"
            + "      \"alias\": \"Created At\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"excluded\": [\"string\"]\n"
            + "}";

    csvMetacardTransformer.setAttributeConfig(ATTRIBUTE_CONFIG);

    // Calling function under test
    List<BinaryContent> content =
        csvMetacardTransformer.transform(Collections.singletonList(metacard), null);

    ////    assertEquals(
    //        content.getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());
  }

  @Test
  public void testMetacardWithAttributes()
      throws CatalogTransformerException, IOException, ParseException {
    //
    //    MetacardImpl metacard = new MetacardImpl();
    //    metacard.setLocation("Test Location");
    //
    //    final Map attributes = new HashMap<String, Serializable>();
    //
    //    // Calling function under test
    //    BinaryContent content = csvMetacardTransformer.transform(metacard, attributes);
    //
    //    assertEquals(
    //        content.getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacard() throws CatalogTransformerException {
    new CsvMetacardTransformer().transform(null, null);
  }
}
