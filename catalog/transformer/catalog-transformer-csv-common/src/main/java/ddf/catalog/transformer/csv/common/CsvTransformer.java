package ddf.catalog.transformer.csv.common;

import ddf.catalog.data.*;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(CsvTransformer.class);

  public static BinaryContent createResponse(final Appendable csv) {
    InputStream inputStream =
        new ByteArrayInputStream(csv.toString().getBytes(Charset.defaultCharset()));
    BinaryContent binaryContent = new BinaryContentImpl(inputStream);
    return binaryContent;
  }

  public static Appendable writeSearchResultsToCsv(
      final List<Metacard> metacards,
      Map<String, String> columnAliasMap,
      List<AttributeDescriptor> sortedAttributeDescriptors)
      throws CatalogTransformerException {
    StringBuilder stringBuilder = new StringBuilder();

    try {
      CSVPrinter csvPrinter = new CSVPrinter(stringBuilder, CSVFormat.RFC4180);
      printColumnHeaders(csvPrinter, sortedAttributeDescriptors, columnAliasMap);

      metacards.stream().forEach(mc -> printMetacard(csvPrinter, mc, sortedAttributeDescriptors));

      return csvPrinter.getOut();
    } catch (IOException ioe) {
      throw new CatalogTransformerException(ioe.getMessage(), ioe);
    }
  }

  private static void printMetacard(
      final CSVPrinter csvPrinter,
      final Metacard metacard,
      final List<AttributeDescriptor> attributeDescriptors) {

    Iterator<Serializable> metacardIterator = new MetacardIterator(metacard, attributeDescriptors);

    printData(csvPrinter, metacardIterator);
  }

  private static void printColumnHeaders(
      final CSVPrinter csvPrinter,
      final List<AttributeDescriptor> attributeDescriptors,
      Map<String, String> aliasMap) {
    Iterator<String> columnHeaderIterator =
        new ColumnHeaderIterator(attributeDescriptors, aliasMap);

    printData(csvPrinter, columnHeaderIterator);
  }

  /**
   * @param csvPrinter
   * @param iterator
   */
  private static void printData(final CSVPrinter csvPrinter, final Iterator iterator) {
    try {
      csvPrinter.printRecord(() -> iterator);
    } catch (IOException ioe) {
      LOGGER.error(ioe.getMessage(), ioe);
    }
  }

  public static List<AttributeDescriptor> sortAttributes(
      final Set<AttributeDescriptor> attributeSet, final List<String> attributeOrder) {
    CsvAttributeDescriptorComparator attributeComparator =
        new CsvAttributeDescriptorComparator(attributeOrder);

    return attributeSet.stream().sorted(attributeComparator).collect(Collectors.toList());
  }

  public static Set<AttributeDescriptor> getAllRequestedAttributes(
      final List<Metacard> metacards, final Set<String> hiddenFields) {

    Set<AttributeDescriptor> allAttributes = new HashSet<>();

    metacards
        .stream()
        .map(Metacard::getMetacardType)
        .map(MetacardType::getAttributeDescriptors)
        .forEach(
            descriptorSet ->
                descriptorSet
                    .stream()
                    .filter(
                        desc ->
                            !AttributeType.AttributeFormat.BINARY.equals(
                                desc.getType().getAttributeFormat()))
                    .filter(
                        desc ->
                            !AttributeType.AttributeFormat.OBJECT.equals(
                                desc.getType().getAttributeFormat()))
                    .filter(desc -> !hiddenFields.contains(desc.getName()))
                    .forEach(allAttributes::add));

    return allAttributes;
  }

  /**
   * Given a list of metacards and a string set of requested attributes, returns a set of
   * AttributeDescriptors.
   *
   * @param metacards
   * @param requestedAttributes
   * @return
   */
  public static Set<AttributeDescriptor> getOnlyRequestedAttributes(
      final List<Metacard> metacards, final Set<String> requestedAttributes) {

    Set<AttributeDescriptor> attributes = new HashSet<>();

    metacards
        .stream()
        .map(Metacard::getMetacardType)
        .map(MetacardType::getAttributeDescriptors)
        .forEach(
            descriptorSet ->
                descriptorSet
                    .stream()
                    .filter(
                        desc ->
                            // TODO: Change this boolean?
                            !AttributeType.AttributeFormat.BINARY.equals(
                                desc.getType().getAttributeFormat()))
                    .filter(
                        desc ->
                            // TODO: Change this boolean?
                            !AttributeType.AttributeFormat.OBJECT.equals(
                                desc.getType().getAttributeFormat()))
                    .filter(desc -> requestedAttributes.contains(desc.getName()))
                    .forEach(attributes::add));

    return attributes;
  }
}
