package ddf.catalog.transformer.metacard.csv;

import static ddf.catalog.transformer.csv.common.CsvTransformer.getAllRequestedAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.getOnlyRequestedAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.sortAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.writeSearchResultsToCsv;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.metacard.csv.CsvMetacardTransformer.AttributeConfig.AttributeConfigItem;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link MetacardTransformer} interface to transform a single {@link Metacard}
 * instance to CSV.
 *
 * <p>TODO: Cement description. // * This class places what is returned by {@link
 * Metacard#getLocation()} in the // * geometry JSON object in the GeoJSON output. The rest of the
 * attributes of the Metacard are placed // * in the properties object in the JSON. See geojson.org
 * for the GeoJSON specification.
 *
 * @author Blake Thomson
 * @author ddf.isgs@lmco.com
 * @see MetacardTransformer
 * @see Metacard
 * @see Attribute
 */
public class CsvMetacardTransformer implements MultiMetacardTransformer {

  private final String ID = "csv-metacard-transformer";

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvMetacardTransformer.class);

  protected static final MimeType DEFAULT_MIME_TYPE = new MimeType();

  private AttributeConfig attributeConfig;

  static {
    try {
      DEFAULT_MIME_TYPE.setPrimaryType("text");
      DEFAULT_MIME_TYPE.setSubType("csv");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failure creating MIME type", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  // TODO: Make both lists start out as empty?
  public static class AttributeConfig {

    List<AttributeConfigItem> attributes = new ArrayList<>();
    List<String> excluded = new ArrayList<>();

    public static class AttributeConfigItem {
      String name;
      String alias;

      AttributeConfigItem(String name, String alias) {
        this.name = name;
        this.alias = alias;
      }

      String getName() {
        return this.name;
      }

      String getAlias() {
        return this.alias;
      }
    }
  }

  public void setAttributeConfig(String attributeConfig) {
    AttributeConfig attrConfig;
    try {
      attrConfig = new Gson().fromJson(attributeConfig, AttributeConfig.class);
    } catch (JsonParseException e) {
      LOGGER.error("Invalid JSON for AttributeConfig.", e);
      return;
    }

    if (attrConfig == null) return;

    if (isValidConfig(attrConfig)) {
      this.attributeConfig = attrConfig;
    }
  }

  public AttributeConfig getAttributeconfig() {
    return this.attributeConfig;
  }

  private boolean isValidConfig(AttributeConfig attrConfig) {
    for (AttributeConfigItem attr : attrConfig.attributes) {
      if (attr.getName() == null) return false;
    }

    return true;
  }

  @Override
  public List<BinaryContent> transform(
      List<Metacard> metacards, Map<String, ? extends Serializable> arguments)
      throws CatalogTransformerException {

    if (metacards == null){
      throw new CatalogTransformerException("Metacard list cannot be null.");
    }

    if (this.attributeConfig == null) {
      this.attributeConfig = new AttributeConfig();
    }

    // Can't use collectors.toMap because alias could be null.
    Map<String, String> aliasMap = new HashMap<>();
    this.attributeConfig.attributes.forEach(attr -> aliasMap.put(attr.getName(), attr.getAlias()));

    // If attributes provided, only choose those. Otherwise, choose all and sort alphabetically.
    // In both cases, filter excluded attributes.
    Set<AttributeDescriptor> attributesToInclude;
    List<String> attributeOrder;

    // If requested attributes are not provided, include all attributes.
    if (this.attributeConfig.attributes.size() != 0) {
      attributesToInclude =
          getOnlyRequestedAttributes(metacards, aliasMap.keySet(), this.attributeConfig.excluded);
      attributeOrder =
          this.attributeConfig
              .attributes
              .stream()
              .map(AttributeConfigItem::getName)
              .collect(Collectors.toList());
    } else {
      attributesToInclude =
          getAllRequestedAttributes(metacards, new HashSet<>(this.attributeConfig.excluded));

      attributeOrder =
          attributesToInclude
              .stream()
              .sorted(Comparator.comparing(o -> aliasMap.getOrDefault(o.getName(), o.getName())))
              .map(AttributeDescriptor::getName)
              .collect(Collectors.toList());
    }

    List<AttributeDescriptor> sortedAttributeDescriptors =
        sortAttributes(attributesToInclude, attributeOrder);

    Appendable csv = writeSearchResultsToCsv(metacards, aliasMap, sortedAttributeDescriptors);

    return Collections.singletonList(getBinaryContent(csv));
  }

  private BinaryContentImpl getBinaryContent(Appendable CsvText) {
    return new BinaryContentImpl(
        new ByteArrayInputStream(CsvText.toString().getBytes(StandardCharsets.UTF_8)),
        DEFAULT_MIME_TYPE);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public Set<MimeType> getMimeTypes() {
    return Collections.singleton(DEFAULT_MIME_TYPE);
  }

  @Override
  public Map<String, Object> getProperties() {
    return Collections.emptyMap();
  }
}
