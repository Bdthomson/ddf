package org.codice.ddf.spatial.kml.converter;

import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public class MetacardToKml {

  private static final String POINT_TYPE = "Point";

  private static final String LINES_STRING_TYPE = "LineString";

  private static final String POLYGON_TYPE = "Polygon";

  /**
   * Convert wkt string into JTS Geometry and then from JTS to KML Geo.
   *
   * @param wkt
   * @return
   * @throws CatalogTransformerException
   */
  public static Geometry getKmlGeoFromWkt(final String wkt) throws CatalogTransformerException {
    if (StringUtils.isBlank(wkt)) {
      throw new CatalogTransformerException(
          "WKT was null or empty. Unable to preform KML Transform on Metacard.");
    }

    com.vividsolutions.jts.geom.Geometry geo = readGeoFromWkt(wkt);
    Geometry kmlGeo = createKmlGeo(geo);
    if (!POINT_TYPE.equals(geo.getGeometryType())) {
      kmlGeo = addPointToKmlGeo(kmlGeo, geo.getCoordinate());
    }
    return kmlGeo;
  }

  private static Geometry createKmlGeo(com.vividsolutions.jts.geom.Geometry geo)
      throws CatalogTransformerException {
    Geometry kmlGeo;
    if (POINT_TYPE.equals(geo.getGeometryType())) {
      Point jtsPoint = (Point) geo;
      kmlGeo = KmlFactory.createPoint().addToCoordinates(jtsPoint.getX(), jtsPoint.getY());

    } else if (LINES_STRING_TYPE.equals(geo.getGeometryType())) {
      LineString jtsLS = (LineString) geo;
      de.micromata.opengis.kml.v_2_2_0.LineString kmlLS = KmlFactory.createLineString();
      List<Coordinate> kmlCoords = kmlLS.createAndSetCoordinates();
      for (com.vividsolutions.jts.geom.Coordinate coord : jtsLS.getCoordinates()) {
        kmlCoords.add(new Coordinate(coord.x, coord.y));
      }
      kmlGeo = kmlLS;
    } else if (POLYGON_TYPE.equals(geo.getGeometryType())) {
      Polygon jtsPoly = (Polygon) geo;
      de.micromata.opengis.kml.v_2_2_0.Polygon kmlPoly = KmlFactory.createPolygon();
      List<Coordinate> kmlCoords =
          kmlPoly.createAndSetOuterBoundaryIs().createAndSetLinearRing().createAndSetCoordinates();
      for (com.vividsolutions.jts.geom.Coordinate coord : jtsPoly.getCoordinates()) {
        kmlCoords.add(new Coordinate(coord.x, coord.y));
      }
      kmlGeo = kmlPoly;
    } else if (geo instanceof GeometryCollection) {
      List<Geometry> geos = new ArrayList<>();
      for (int xx = 0; xx < geo.getNumGeometries(); xx++) {
        geos.add(createKmlGeo(geo.getGeometryN(xx)));
      }
      kmlGeo = KmlFactory.createMultiGeometry().withGeometry(geos);
    } else {
      throw new CatalogTransformerException(
          "Unknown / Unsupported Geometry Type '"
              + geo.getGeometryType()
              + "'. Unale to preform KML Transform.");
    }
    return kmlGeo;
  }

  private static com.vividsolutions.jts.geom.Geometry readGeoFromWkt(final String wkt)
      throws CatalogTransformerException {
    WKTReader reader = new WKTReader();
    try {
      return reader.read(wkt);
    } catch (ParseException e) {
      throw new CatalogTransformerException("Unable to parse WKT to Geometry.", e);
    }
  }

  private static Geometry addPointToKmlGeo(
      Geometry kmlGeo, com.vividsolutions.jts.geom.Coordinate vertex) {
    if (null != vertex) {
      de.micromata.opengis.kml.v_2_2_0.Point kmlPoint =
          KmlFactory.createPoint().addToCoordinates(vertex.x, vertex.y);
      return KmlFactory.createMultiGeometry().addToGeometry(kmlPoint).addToGeometry(kmlGeo);
    } else {
      return null;
    }
  }
}
