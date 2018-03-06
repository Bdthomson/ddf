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
package org.codice.ddf.spatial.kml.transformer;

import static com.google.common.base.Preconditions.checkNotNull;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmzTransformer implements MultiMetacardTransformer {

  private static final String ID = "kmz-multi-metacard-transformer";

  protected static final MimeType KMZ_MIMETYPE = new MimeType();

  private static final Logger LOGGER = LoggerFactory.getLogger(KmzTransformer.class);

  private KMLTransformerImpl kmlTransformer;

  static {
    try {
      KMZ_MIMETYPE.setPrimaryType("application");
      KMZ_MIMETYPE.setSubType("vnd.google-earth.kmz");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Unable to parse KMZ MimeType.", e);
    }
  }

  KmzTransformer(KMLTransformerImpl kmlTransformer) {
    this.kmlTransformer = checkNotNull(kmlTransformer);
  }

  @Override
  public List<BinaryContent> transform(
      List<Metacard> metacards, Map<String, ? extends Serializable> arguments)
      throws CatalogTransformerException {

    if (metacards == null) {
      throw new CatalogTransformerException("Metacard list cannot be null");
    }

    try {
      return Collections.singletonList(getBinaryContent(metacards, arguments));
    } catch (IOException e) {
      LOGGER.error(e.toString());
      throw new CatalogTransformerException("");
    }
  }

  private BinaryContentImpl getBinaryContent(
      List<Metacard> metacards, Map<String, ? extends Serializable> arguments) throws IOException {

    Map<String, Serializable> args = new HashMap<>();
    args.put("docName", "KML Metacard Export");

    // Get kml file input stream.
    InputStream inputStream = kmlTransformer.transformToInputStream(metacards, arguments);

    // Create a temporary file to hold kml document.
    TemporaryFileBackedOutputStream fileBackedOutputStream = new TemporaryFileBackedOutputStream();

    ZipOutputStream zipOutputStream = new ZipOutputStream(fileBackedOutputStream);
    zipOutputStream.putNextEntry(new ZipEntry("doc.kml"));
    zipOutputStream.write(IOUtils.toByteArray(inputStream));
    zipOutputStream.closeEntry();

    byte[] read = fileBackedOutputStream.asByteSource().read();
    zipOutputStream.close();

    return new BinaryContentImpl(new ByteArrayInputStream(read), KMZ_MIMETYPE);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public Set<MimeType> getMimeTypes() {
    return Collections.singleton(KMZ_MIMETYPE);
  }

  @Override
  public Map<String, Object> getProperties() {
    return Collections.emptyMap();
  }
}
