/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.input.propertyjson;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.boon.json.JsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyJsonInputTransformer implements InputTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyJsonInputTransformer.class);

  private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  private final List<MetacardType> metacardTypeList = new LinkedList<>();

  private final MetacardType defaultMetacardType;

  private final AttributeInjector attributeInjector;

  public PropertyJsonInputTransformer(
      MetacardType defaultMetacardType, AttributeInjector attributeInjector) {

    this.attributeInjector = attributeInjector;
    this.defaultMetacardType = attributeInjector.injectAttributes(defaultMetacardType);
  }

  public void addMetacardType(MetacardType metacardType) {
    this.metacardTypeList.add(attributeInjector.injectAttributes(metacardType));
  }

  public void removeMetacardType(MetacardType metacardType) {
    this.metacardTypeList.removeIf(mt -> metacardType.getName().equals(mt.getName()));
  }

  @Override
  public Metacard transform(InputStream input) throws CatalogTransformerException {
    return transform(input, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Metacard transform(InputStream input, String id) throws CatalogTransformerException {

    Map<String, Object> rawMap = JsonFactory.create().readValue(input, Map.class);

    Object rawPropertiesMap = rawMap.get("properties");
    if (rawPropertiesMap instanceof Map) {
      Map<String, Object> propertiesMap = (Map<String, Object>) rawPropertiesMap;

      MetacardType metacardType = findMetacardType(propertiesMap.get("metacard-type"));

      MetacardImpl metacard = new MetacardImpl(metacardType);

      try {
        for (Map.Entry<String, Object> entry : propertiesMap.entrySet()) {
          String attributeName = entry.getKey();
          Object value = entry.getValue();
          AttributeDescriptor attributeDescriptor =
              metacardType.getAttributeDescriptor(attributeName);
          if (Core.SOURCE_ID.equals(attributeName) && value instanceof String) {
            metacard.setSourceId((String) value);
          } else if (attributeDescriptor != null) {
            AttributeType.AttributeFormat attributeFormat =
                attributeDescriptor.getType().getAttributeFormat();
            List<Serializable> convertedValues = convert(value, attributeFormat);
            metacard.setAttribute(new AttributeImpl(attributeName, convertedValues));
          } else {
            LOGGER.trace(
                "Unable to transform an unrecognized attribute. attributeName={}", attributeName);
          }
        }
      } catch (ParseException | NumberFormatException e) {
        throw new CatalogTransformerException("Unable to convert a json field.", e);
      }

      return metacard;
    }

    throw new CatalogTransformerException("Unable to parse the json to a map");
  }

  private List<Serializable> convert(Object obj, AttributeType.AttributeFormat attributeFormat)
      throws ParseException, NumberFormatException, CatalogTransformerException {
    List<Serializable> serializables = new LinkedList<>();
    if (obj instanceof List) {
      for (Object child : (List) obj) {
        if (child instanceof String) {
          serializables.add(convert((String) child, attributeFormat));
        } else if (child instanceof Date) {
          serializables.add((Date) child);
        } else if (child instanceof Integer) {
          serializables.add(convert((Integer) child, attributeFormat));
        } else if (child instanceof Double) {
          serializables.add(convert((Double) child, attributeFormat));
        } else {
          throw new CatalogTransformerException(
              String.format("Encountered an unrecognized datatype in a json list: [%s]", child));
        }
      }
    } else if (obj instanceof String) {
      serializables.add(convert((String) obj, attributeFormat));
    } else if (obj instanceof Integer) {
      serializables.add(convert((Integer) obj, attributeFormat));
    } else if (obj instanceof Double) {
      serializables.add(convert((Double) obj, attributeFormat));
    } else {
      throw new CatalogTransformerException(
          String.format(
              "Encountered a non-string, non-int, non-double and non-list in json: [%s]", obj));
    }

    return serializables;
  }

  private Serializable convert(String str, AttributeType.AttributeFormat attributeFormat)
      throws ParseException, NumberFormatException, CatalogTransformerException {
    switch (attributeFormat) {
      case BOOLEAN:
        return Boolean.parseBoolean(str);
      case DATE:
        return parseDate(str);
      case SHORT:
        return Short.parseShort(str);
      case INTEGER:
        return Integer.parseInt(str);
      case LONG:
        return Long.parseLong(str);
      case FLOAT:
        return Float.parseFloat(str);
      case DOUBLE:
        return Double.parseDouble(str);
      case STRING:
      case GEOMETRY:
      case BINARY:
      case XML:
      case OBJECT:
        return str;
      default:
        throw new CatalogTransformerException(
            String.format("Encountered unrecognized attribute format: [%s]", attributeFormat));
    }
  }

  private Serializable convert(Integer value, AttributeType.AttributeFormat attributeFormat)
      throws NumberFormatException, CatalogTransformerException {
    switch (attributeFormat) {
      case INTEGER:
        return value;
      case LONG:
        return value.longValue();
      case FLOAT:
        return value.floatValue();
      case DOUBLE:
        return value.doubleValue();
      case SHORT:
        return value.shortValue();
      case STRING:
        return value.toString();
      case BOOLEAN:
      case DATE:
      case GEOMETRY:
      case BINARY:
      case XML:
      case OBJECT:
      default:
        throw new CatalogTransformerException(
            String.format("Cannot convert an integer to %s: [%s]", attributeFormat, value));
    }
  }

  private Serializable convert(Double value, AttributeType.AttributeFormat attributeFormat)
      throws NumberFormatException, CatalogTransformerException {
    switch (attributeFormat) {
      case INTEGER:
        return (int) Math.round(value);
      case LONG:
        return Math.round(value);
      case FLOAT:
        return value.floatValue();
      case DOUBLE:
        return value;
      case SHORT:
        return (short) Math.round(value);
      case STRING:
        return value.toString();
      case BOOLEAN:
      case DATE:
      case GEOMETRY:
      case BINARY:
      case XML:
      case OBJECT:
      default:
        throw new CatalogTransformerException(
            String.format("Cannot convert a double to %s: [%s]", attributeFormat, value));
    }
  }

  private MetacardType findMetacardType(Object suppliedTypeName) {
    return metacardTypeList
        .stream()
        .filter(metacardType -> metacardType.getName().equals(suppliedTypeName))
        .findFirst()
        .orElse(defaultMetacardType);
  }

  private Date parseDate(String dateString) throws ParseException {
    SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormat.parse(dateString);
  }
}
