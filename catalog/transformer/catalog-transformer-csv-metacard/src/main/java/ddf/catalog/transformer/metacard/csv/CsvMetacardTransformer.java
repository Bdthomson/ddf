package ddf.catalog.transformer.metacard.csv;

import static ddf.catalog.transformer.csv.common.CsvTransformer.getOnlyRequestedAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.sortAttributes;
import static ddf.catalog.transformer.csv.common.CsvTransformer.writeSearchResultsToCsv;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;
import org.json.simple.JSONObject;
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

  private JSONObject attributeConfig;

  static {
    try {
      DEFAULT_MIME_TYPE.setPrimaryType("text");
      DEFAULT_MIME_TYPE.setSubType("csv");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failure creating MIME type", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  public void setAttributeConfig(String attributeConfig) {
    validateJSON(attributeConfig);
    this.attributeConfig = toJSON(attributeConfig);
  }

  private JSONObject toJSON(String attributeConfig) {

    return new JSONObject();
  }

  private void validateJSON(String attributeConfig) {}

  @Override
  public List<BinaryContent> transform(
      List<Metacard> metacards, Map<String, ? extends Serializable> arguments)
      throws CatalogTransformerException {

    // TODO: What are Arguments

    // TODO: Pull in config service to get what attribute names we want and what sort order.

    // Get requested attributes
    Set<AttributeDescriptor> onlyRequestedAttributes =
        getOnlyRequestedAttributes(metacards, Collections.emptySet());

    // Sort them
    List<String> attributeOrder =
        Optional.ofNullable((List<String>) arguments.get(COLUMN_ORDER_KEY))
            .orElse(Collections.emptyList());

    List<AttributeDescriptor> sortedAttributeDescriptors =
        sortAttributes(onlyRequestedAttributes, attributeOrder);

    Appendable csv = writeSearchResultsToCsv(metacards, null, sortedAttributeDescriptors);

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
