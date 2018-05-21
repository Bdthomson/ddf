package org.codice.ddf.spatial.kml.converter;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Point;
import java.util.List;
import org.junit.Test;

public class MetacardToKmlTest {

  @Test
  public void point() throws CatalogTransformerException {
    final Point kmlGeoFromWkt = (Point) MetacardToKml.getKmlGeoFromWkt("POINT (2 3)");
    final List<Coordinate> coordinates = kmlGeoFromWkt.getCoordinates();
    assertThat(coordinates, hasSize(1));
    assertThat(coordinates.get(0).getLatitude(), is(3.0));
    assertThat(coordinates.get(0).getLongitude(), is(2.0));
  }
}
