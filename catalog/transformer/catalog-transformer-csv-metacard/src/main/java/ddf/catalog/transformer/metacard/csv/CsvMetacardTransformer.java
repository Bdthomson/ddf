package ddf.catalog.transformer.metacard.csv;

import static ddf.catalog.transformer.csv.common.CsvTransformer.getOnlyRequestedAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.sortAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.writeSearchResultsToCsv;

import com.google.gson.Gson;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.metacard.csv.CsvMetacardTransformer.AttributeConfig.MyAttribute;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvMetacardTransformer.class);

  private final String ID = "csv-metacard-transformer";

  protected static final MimeType DEFAULT_MIME_TYPE = new MimeType();

  private static final String COLUMN_ORDER_KEY = "columnOrder";

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
  public class AttributeConfig {
    List<MyAttribute> attributes;
    List<String> excluded;

    class MyAttribute {
      String name;
      String alias;

      String getName() {
        return this.name;
      }

      String getAlias() {
        return this.alias;
      }
    }
  }

  public void setAttributeConfig(String attributeConfig) {
    this.attributeConfig = fromJSON(attributeConfig);
  }

  private AttributeConfig fromJSON(String attributeConfigString) {
    Gson g = new Gson();
    AttributeConfig attributeConfig = g.fromJson(attributeConfigString, AttributeConfig.class);
    return attributeConfig;
  }

  private void validateJSON(String attributeConfig) {}

  @Override
  public List<BinaryContent> transform(
      List<Metacard> metacards, Map<String, ? extends Serializable> arguments)
      throws CatalogTransformerException {

    if (this.attributeConfig == null) {
      LOGGER.error(
          "AttributeConfig is null. Please set it in Admin Config -> Catalog -> Configuration -> Csv Metacard Configuration");
      throw new CatalogTransformerException("AttributeConfig not set.");
    }

    Map<String, String> aliasMap =
        this.attributeConfig
            .attributes
            .stream()
            .collect(Collectors.toMap(MyAttribute::getName, MyAttribute::getAlias));

    // Get requested attributes and filter excluded attributes
    Set<AttributeDescriptor> onlyRequestedAttributes =
        getOnlyRequestedAttributes(metacards, aliasMap.keySet(), this.attributeConfig.excluded);

    // Sort them
    //        List<String> attributeOrder =
    //            Optional.ofNullable((List<String>) arguments.get(COLUMN_ORDER_KEY))
    //                .orElse(Collections.emptyList());

    List<String> attributeOrder =
        this.attributeConfig
            .attributes
            .stream()
            .map(MyAttribute::getName)
            .collect(Collectors.toList());

    List<AttributeDescriptor> sortedAttributeDescriptors =
        sortAttributes(onlyRequestedAttributes, attributeOrder);

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
