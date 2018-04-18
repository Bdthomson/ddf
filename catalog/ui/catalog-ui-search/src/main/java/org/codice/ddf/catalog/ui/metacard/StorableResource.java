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
package org.codice.ddf.catalog.ui.metacard;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;

/** Call {@link #close()} if {@link #isError()} returns {@code false}. */
public class StorableResource implements AutoCloseable {

  private TemporaryFileBackedOutputStream temporaryFileBackedOutputStream;
  private String mimeType;
  private String filename;
  private String errorMessage;
  private List<InputStream> openInputStreams = new LinkedList<>();

  private StorableResource(@Nullable String mimeType, String filename) {
    temporaryFileBackedOutputStream = new TemporaryFileBackedOutputStream();
    this.mimeType = mimeType;
    this.filename = filename;
  }

  public StorableResource(InputStream inputStream, @Nullable String mimeType, String filename)
      throws IOException {
    this(mimeType, filename);
    IOUtils.copy(inputStream, temporaryFileBackedOutputStream);
  }

  public StorableResource(String input, @Nullable String mimeType, String filename)
      throws IOException {
    this(mimeType, filename);
    temporaryFileBackedOutputStream.write(input.getBytes());
  }

  public StorableResource(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public StorableResource(InputStream inputStream, String filename) throws IOException {
    this(inputStream, null, filename);
  }

  /**
   * The caller is not responsible for calling {@link InputStream#close()} on the returned stream.
   * However, the caller is responsible for calling {@link #close()}.
   *
   * @return
   * @throws IOException
   */
  public InputStream getInputStream() throws IOException {
    InputStream inputStream = temporaryFileBackedOutputStream.asByteSource().openStream();
    openInputStreams.add(inputStream);
    return inputStream;
  }

  public Optional<String> getMimeType() {
    return Optional.ofNullable(mimeType);
  }

  public String getFilename() {
    return filename;
  }

  public boolean isError() {
    return errorMessage != null;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  @Override
  public void close() {
    if (temporaryFileBackedOutputStream != null) {
      IOUtils.closeQuietly(temporaryFileBackedOutputStream);
      temporaryFileBackedOutputStream = null;
    }
    openInputStreams.forEach(IOUtils::closeQuietly);
    openInputStreams.clear();
  }
}
