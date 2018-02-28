package ddf.catalog.transformer.metacard.csv;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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
public class CsvMetacardTransformer implements MetacardTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvMetacardTransformer.class);

  protected static final MimeType DEFAULT_MIME_TYPE = new MimeType();

  static {
    try {
      DEFAULT_MIME_TYPE.setPrimaryType("text");
      DEFAULT_MIME_TYPE.setSubType("csv");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failure creating MIME type", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  // TODO: Get an example of expected output and expected input.
  // TODO: Figure out what arguments are.

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    final String csvText = convertToCsv(metacard);

    return new BinaryContentImpl(
        new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8)), DEFAULT_MIME_TYPE);
  }

  private String convertToCsv(Metacard metacard) throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException();
    }

    return "";
  }
}
