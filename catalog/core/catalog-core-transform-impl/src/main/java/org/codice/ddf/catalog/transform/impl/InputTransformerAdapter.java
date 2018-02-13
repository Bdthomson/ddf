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
package org.codice.ddf.catalog.transform.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.transform.MultiInputTransformer;

public class InputTransformerAdapter extends AbstractTransformerAdapter
    implements MultiInputTransformer {

  private InputTransformer inputTransformer;

  public InputTransformerAdapter(
      InputTransformer inputTransformer, Map<String, Object> properties) {
    super(properties);
    this.inputTransformer = inputTransformer;
  }

  @Override
  public List<Metacard> transform(InputStream input, Map<String, ? extends Serializable> arguments)
      throws IOException, CatalogTransformerException {
    return Collections.singletonList(inputTransformer.transform(input));
  }

  public InputTransformer getInputTransformer() {
    return inputTransformer;
  }
}
