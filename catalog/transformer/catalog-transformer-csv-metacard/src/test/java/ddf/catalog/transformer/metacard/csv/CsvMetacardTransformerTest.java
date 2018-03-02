package ddf.catalog.transformer.metacard.csv;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CsvMetacardTransformerTest {

  private CsvMetacardTransformer csvMetacardTransformer;

  private static final String NAME_ATTRIBUTE_KEY = "name";
  private static final String CREATED_AT_ATTRIBUTE_KEY = "created_at";
  private static final String EXCLUDE_THIS_ATTRIBUTE_KEY = "exclude_this";

  private static final String ATTRIBUTE_CONFIG =
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
            + "  \"excluded\": [\"excludeThis\"]\n"
            + "}";

  @Mock MetacardType metacardType;

  @Before
  public void setUp() {
    csvMetacardTransformer = new CsvMetacardTransformer();
    initMocks(this);

    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();



    //    when().thenReturn();
  }

  private static final Object[][] ATTRIBUTE_DATA = {
      {"attribute1", "value1", BasicTypes.STRING_TYPE},
      {"attribute2", 101, BasicTypes.INTEGER_TYPE},
      {"attribute3", 3.14159, BasicTypes.DOUBLE_TYPE},
      {"attribute4", "value,4", BasicTypes.STRING_TYPE},
      {"attribute5", "value5", BasicTypes.STRING_TYPE},
      {"attribute6", "OBJECT", BasicTypes.OBJECT_TYPE},
      {"attribute7", "BINARY", BasicTypes.BINARY_TYPE}
  };

  private static final Map<String, Serializable> METACARD_DATA_MAP = new HashMap<>();

  private static final List<AttributeDescriptor> ATTRIBUTE_DESCRIPTOR_LIST = new ArrayList<>();

  @Test
  public void testMetacard() throws CatalogTransformerException, IOException, ParseException {

    Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();
    attributeDescriptors.add(
        new AttributeDescriptorImpl("other", false, false, false, false, BasicTypes.STRING_TYPE));
    attributeDescriptors.add(
        new AttributeDescriptorImpl(
            NAME_ATTRIBUTE_KEY, false, false, false, false, BasicTypes.STRING_TYPE));
    attributeDescriptors.add(
        new AttributeDescriptorImpl(
            CREATED_AT_ATTRIBUTE_KEY, false, false, false, false, BasicTypes.STRING_TYPE));
    attributeDescriptors.add(
        new AttributeDescriptorImpl(
            EXCLUDE_THIS_ATTRIBUTE_KEY, false, false, false, false, BasicTypes.STRING_TYPE));

    when(metacardType.getAttributeDescriptors()).thenReturn(attributeDescriptors);

    // Create a metacard
    Metacard metacard = new MetacardImpl(metacardType);

    //
    metacard.setAttribute(new AttributeImpl(NAME_ATTRIBUTE_KEY, "name_value"));
    metacard.setAttribute(new AttributeImpl(CREATED_AT_ATTRIBUTE_KEY, "1033223423423"));
    metacard.setAttribute(new AttributeImpl(EXCLUDE_THIS_ATTRIBUTE_KEY, "do_not_show"));

    csvMetacardTransformer.setAttributeConfig(ATTRIBUTE_CONFIG);

    // Calling function under test
    List<BinaryContent> content =
        csvMetacardTransformer.transform(Collections.singletonList(metacard), null);

    assertEquals(1, content.size());
    assertEquals(
        content.get(0).getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());
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
    csvMetacardTransformer.setAttributeConfig(ATTRIBUTE_CONFIG);
    new CsvMetacardTransformer().transform(null, null);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testWhenAttributeConfigNull() throws CatalogTransformerException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute("name", "ThisIsMyName");
    metacard.setAttribute("alias", "Name");
    metacard.setAttribute("excludeThis", "DATA");

    new CsvMetacardTransformer().transform(Collections.singletonList(metacard), null);
  }

  private void buildMetacardDataMap() {
    for (Object[] entry : ATTRIBUTE_DATA) {
      String attributeName = entry[0].toString();
      AttributeType attributeType = (AttributeType) entry[2];
      Serializable attributeValue = (Serializable) entry[1];
      Attribute attribute = buildAttribute(attributeName, attributeValue);
      METACARD_DATA_MAP.put(attributeName, attribute);
      ATTRIBUTE_DESCRIPTOR_LIST.add(buildAttributeDescriptor(attributeName, attributeType));
    }
  }

  private getCsv
}
