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
package ddf.catalog.transformer.csv.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

public class CsvTransformerTest {

  private static final List<AttributeDescriptor> ATTRIBUTE_DESCRIPTOR_LIST = new ArrayList<>();
  private static final Map<String, Serializable> METACARD_DATA_MAP = new HashMap<>();

  private static final List<Metacard> METACARD_LIST = new ArrayList<>();
  private static final int METACARD_COUNT = 2;

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

  @Before
  public void setup() {
    ATTRIBUTE_DESCRIPTOR_LIST.clear();
    METACARD_LIST.clear();
    buildMetacardDataMap();
    buildMetacardList();
  }

  @Test
  public void getAllAttributes() {
    Set<String> hiddenFields = new HashSet<>();
    hiddenFields.add("attribute1");

    Set<AttributeDescriptor> allRequestedAttributes =
        CsvTransformer.getAllRequestedAttributes(METACARD_LIST, hiddenFields);

    assertThat(allRequestedAttributes.size(), is(5));
  }

  @Test
  public void getOnlyRequestedAttributes() {
    Set<String> requestedAttributes = new HashSet<>();
    requestedAttributes.add("attribute1");

    Set<AttributeDescriptor> onlyRequestedAttributes =
        CsvTransformer.getOnlyRequestedAttributes(
            METACARD_LIST, requestedAttributes, Collections.emptyList());

    assertThat(onlyRequestedAttributes.size(), is(1));
  }

  @Test
  public void writeSearchResultsToCsv() throws CatalogTransformerException {

    List<AttributeDescriptor> requestedAttributes = new ArrayList<>();
    requestedAttributes.add(buildAttributeDescriptor("attribute1", BasicTypes.STRING_TYPE));

    Appendable csvText =
        CsvTransformer.writeSearchResultsToCsv(
            METACARD_LIST, Collections.emptyMap(), requestedAttributes);

    Scanner scanner = new Scanner(csvText.toString());
    scanner.useDelimiter("\\n|\\r|,");

    // Only attributes in "attributes" config field will exist in output.
    // OBJECT types, BINARY types and excluded attributes will be filtered out
    // excluded attribute take precedence over attributes that appear in requested attribute list.
    String[] expectedHeaders = {"attribute1"};
    validate(scanner, expectedHeaders);

    // The scanner will split "value,5" into two tokens even though the CSVPrinter will
    // handle it correctly.
    String[] expectedValues = {"", "value1"};

    for (int i = 0; i < METACARD_COUNT; i++) {
      validate(scanner, expectedValues);
    }

    // final new line causes an extra "" value at end of file
    assertThat(scanner.hasNext(), is(true));
    assertThat(scanner.next(), is(""));
    assertThat(scanner.hasNext(), is(false));
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

  private void buildMetacardList() {
    METACARD_LIST.clear();
    for (int i = 0; i < METACARD_COUNT; i++) {
      METACARD_LIST.add(buildMetacard());
    }
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

  private void validate(Scanner scanner, String[] expectedValues) {
    for (int i = 0; i < expectedValues.length; i++) {
      assertThat(scanner.hasNext(), is(true));
      assertThat(scanner.next(), is(expectedValues[i]));
    }
  }
}
