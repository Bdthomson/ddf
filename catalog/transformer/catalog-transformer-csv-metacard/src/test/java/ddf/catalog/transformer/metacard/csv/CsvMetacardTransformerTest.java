/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.metacard.csv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.gson.Gson;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.csv.CsvMetacardTransformer.AttributeConfig;
import ddf.catalog.transformer.metacard.csv.CsvMetacardTransformer.AttributeConfig.AttributeConfigItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class CsvMetacardTransformerTest {

  private CsvMetacardTransformer csvMetacardTransformer;

  public static final int METACARD_COUNT = 10;

  private static final Map<String, Serializable> METACARD_DATA_MAP = new HashMap<>();

  private static final List<AttributeDescriptor> ATTRIBUTE_DESCRIPTOR_LIST = new ArrayList<>();

  private static final List<Metacard> METACARD_LIST = new ArrayList<>();

  private static AttributeConfig attributeConfig;

  @Before
  public void setUp() {
    this.csvMetacardTransformer = new CsvMetacardTransformer();
    initMocks(this);

    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_DATA_MAP.clear();
    attributeConfig = null;

    buildMetacardDataMap();
    buildMetacardList();
    buildAttributeConfig();
  }

  private static final Object[][] ATTRIBUTE_DATA = {
    {"attribute1", "value1", BasicTypes.STRING_TYPE},
    {"attribute2", "value2", BasicTypes.STRING_TYPE},
    {"attribute3", 101, BasicTypes.INTEGER_TYPE},
    {"attribute4", 3.14159, BasicTypes.DOUBLE_TYPE},
    {"attribute5", "value,5", BasicTypes.STRING_TYPE},
    {"attribute6", "value6", BasicTypes.STRING_TYPE},
    {"attribute7", "OBJECT", BasicTypes.OBJECT_TYPE},
    {"attribute8", "BINARY", BasicTypes.BINARY_TYPE}
  };

  @Test
  public void testMetacardWithConfig() throws CatalogTransformerException {

    final String attributeConfigJson = toJson(attributeConfig);
    csvMetacardTransformer.setAttributeConfig(attributeConfigJson);

    List<BinaryContent> content = csvMetacardTransformer.transform(METACARD_LIST, null);
    BinaryContent bc = content.get(0);

    assertEquals(1, content.size());
    assertEquals(bc.getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    Scanner scanner = new Scanner(bc.getInputStream());
    scanner.useDelimiter("\\n|\\r|,");

    // Only attributes in "attributes" config field will exist in output.
    // OBJECT types, BINARY types and excluded attributes will be filtered out
    // excluded attribute take precedence over attributes that appear in requested attribute list.
    String[] expectedHeaders = {"column2", "column1", "attribute3", "attribute5"};
    validate(scanner, expectedHeaders);

    // The scanner will split "value,5" into two tokens even though the CSVPrinter will
    // handle it correctly.
    String[] expectedValues = {"", "value2", "value1", "101", "\"value", "5\""};

    for (int i = 0; i < METACARD_COUNT; i++) {
      validate(scanner, expectedValues);
    }

    // final new line causes an extra "" value at end of file
    assertThat(scanner.hasNext(), is(true));
    assertThat(scanner.next(), is(""));
    assertThat(scanner.hasNext(), is(false));
  }

  @Test
  public void testMetacardWithOnlyExcludedConfig() throws CatalogTransformerException {

    AttributeConfig attr = new AttributeConfig();
    attr.excluded = buildList(new String[] {"attribute1", "attribute2"});

    final String attributeConfigJson = toJson(attr);
    csvMetacardTransformer.setAttributeConfig(attributeConfigJson);

    List<BinaryContent> content = csvMetacardTransformer.transform(METACARD_LIST, null);
    BinaryContent bc = content.get(0);

    assertEquals(1, content.size());
    assertEquals(bc.getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    Scanner scanner = new Scanner(bc.getInputStream());
    scanner.useDelimiter("\\n|\\r|,");

    // Only attributes in "attributes" config field will exist in output.
    // OBJECT types, BINARY types and excluded attributes will be filtered out
    // excluded attribute take precedence over attributes that appear in requested attribute list.
    String[] expectedHeaders = {"attribute3", "attribute4", "attribute5", "attribute6"};
    validate(scanner, expectedHeaders);

    // The scanner will split "value,5" into two tokens even though the CSVPrinter will
    // handle it correctly by creating an entry surrounded by double quotes i.e. "value,5"
    String[] expectedValues = {"", "101", "3.14159", "\"value", "5\"", "value6"};

    for (int i = 0; i < METACARD_COUNT; i++) {
      validate(scanner, expectedValues);
    }

    // final new line causes an extra "" value at end of file
    assertThat(scanner.hasNext(), is(true));
    assertThat(scanner.next(), is(""));
    assertThat(scanner.hasNext(), is(false));
  }

  @Test
  public void testMetacardWithoutConfig() throws CatalogTransformerException {

    List<BinaryContent> content = csvMetacardTransformer.transform(METACARD_LIST, null);
    BinaryContent bc = content.get(0);

    assertEquals(1, content.size());
    assertEquals(bc.getMimeTypeValue(), CsvMetacardTransformer.DEFAULT_MIME_TYPE.getBaseType());

    Scanner scanner = new Scanner(bc.getInputStream());
    scanner.useDelimiter("\\n|\\r|,");

    // Only attributes in "attributes" config field will exist in output.
    // OBJECT types, BINARY types and excluded attributes will be filtered out
    // excluded attribute take precedence over attributes that appear in requested attribute list.
    String[] expectedHeaders = {
      "attribute1", "attribute2", "attribute3", "attribute4", "attribute5", "attribute6"
    };
    validate(scanner, expectedHeaders);

    // The scanner will split "value,5" into two tokens even though the CSVPrinter will
    // handle it correctly.
    String[] expectedValues = {
      "", "value1", "value2", "101", "3.14159", "\"value", "5\"", "value6"
    };

    for (int i = 0; i < METACARD_COUNT; i++) {
      validate(scanner, expectedValues);
    }

    // final new line causes an extra "" value at end of file
    assertThat(scanner.hasNext(), is(true));
    assertThat(scanner.next(), is(""));
    assertThat(scanner.hasNext(), is(false));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardList() throws CatalogTransformerException {
    final String attributeConfigJson = toJson(attributeConfig);
    csvMetacardTransformer.setAttributeConfig(attributeConfigJson);
    new CsvMetacardTransformer().transform(null, null);
  }

  // Verifies no exception is thrown if a single null metacard is given.
  @Test
  public void testNullMetacardInList() throws CatalogTransformerException {
    final String attributeConfigJson = toJson(attributeConfig);
    csvMetacardTransformer.setAttributeConfig(attributeConfigJson);

    new CsvMetacardTransformer().transform(Collections.singletonList(null), null);
  }

  @Test
  public void testSavingInvalidJsonConfigParseError() {
    CsvMetacardTransformer csvMetacardTransformer = new CsvMetacardTransformer();
    csvMetacardTransformer.setAttributeConfig("{{");

    assertThat(csvMetacardTransformer.getAttributeconfig(), is(nullValue()));
  }

  @Test
  public void testSavingInvalidJsonConfigInvalidConfigAlias() {
    AttributeConfig attributeConfig = new AttributeConfig();
    attributeConfig.attributes = new ArrayList<>();
    attributeConfig.attributes.add(new AttributeConfigItem(null, "invalid_alias"));
    String attributeConfigJson = toJson(attributeConfig);
    CsvMetacardTransformer csvMetacardTransformer = new CsvMetacardTransformer();
    csvMetacardTransformer.setAttributeConfig(attributeConfigJson);

    assertThat(csvMetacardTransformer.getAttributeconfig(), is(nullValue()));
  }

  @Test
  public void testGetMimeTypes() {
    assertThat(csvMetacardTransformer.getMimeTypes().size(), is(1));
  }

  private String toJson(AttributeConfig a) {
    return new Gson().toJson(a, AttributeConfig.class);
  }

  private void buildMetacardList() {
    METACARD_LIST.clear();
    for (int i = 0; i < METACARD_COUNT; i++) {
      METACARD_LIST.add(buildMetacard());
    }
  }

  private Metacard buildMetacard() {
    Metacard metacard = mock(Metacard.class);

    Answer<Serializable> answer =
        invocation -> {
          String key = invocation.getArgumentAt(0, String.class);
          return METACARD_DATA_MAP.get(key);
        };

    when(metacard.getAttribute(anyString())).thenAnswer(answer);
    MetacardType metacardType = buildMetacardType();
    when(metacard.getMetacardType()).thenReturn(metacardType);
    return metacard;
  }

  private MetacardType buildMetacardType() {
    MetacardType metacardType = mock(MetacardType.class);
    when(metacardType.getAttributeDescriptors())
        .thenReturn(new HashSet<>(ATTRIBUTE_DESCRIPTOR_LIST));
    return metacardType;
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

  private Attribute buildAttribute(String name, Serializable value) {
    Attribute attribute = mock(Attribute.class);
    when(attribute.getName()).thenReturn(name);
    when(attribute.getValue()).thenReturn(value);
    return attribute;
  }

  private AttributeDescriptor buildAttributeDescriptor(String name, AttributeType type) {
    AttributeDescriptor attributeDescriptor = mock(AttributeDescriptor.class);
    when(attributeDescriptor.getName()).thenReturn(name);
    when(attributeDescriptor.getType()).thenReturn(type);
    return attributeDescriptor;
  }

  private void buildAttributeConfig() {
    attributeConfig = new AttributeConfig();

    List<AttributeConfigItem> attributeConfigItems = new ArrayList<>();
    attributeConfigItems.add(new AttributeConfigItem("attribute2", "column2"));
    attributeConfigItems.add(new AttributeConfigItem("attribute1", "column1"));
    attributeConfigItems.add(new AttributeConfigItem("attribute3", null));
    attributeConfigItems.add(new AttributeConfigItem("attribute5", null));
    attributeConfigItems.add(new AttributeConfigItem("attribute6", "column6"));
    attributeConfig.attributes = attributeConfigItems;
    attributeConfig.excluded = buildList(new String[] {"attribute4", "attribute6"});
  }

  private ArrayList<String> buildList(String[] entries) {
    return new ArrayList<>(Arrays.asList(entries));
  }

  private void validate(Scanner scanner, String[] expectedValues) {
    for (int i = 0; i < expectedValues.length; i++) {
      assertThat(scanner.hasNext(), is(true));
      assertThat(scanner.next(), is(expectedValues[i]));
    }
  }
}
