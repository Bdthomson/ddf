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

import static junit.framework.TestCase.assertNotNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.transform.Source;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xmlunit.builder.Input;
import org.xmlunit.xpath.JAXPXPathEngine;

public class KMLTransformerImplTest {

  private static final String DEFAULT_STYLE_LOCATION = "/kml-styling/defaultStyling.kml";

  private static final String ID = "1234567890";

  private static final String TITLE = "myTitle";

  private static final String POINT_WKT = "POINT (-110.00540924072266 34.265270233154297)";

  private static final String LINESTRING_WKT = "LINESTRING (1 1,2 1)";

  private static final String POLYGON_WKT = "POLYGON ((1 1,2 1,2 2,1 2,1 1))";

  private static final String MULTIPOINT_WKT = "MULTIPOINT ((1 1), (0 0), (2 2))";

  private static final String MULTILINESTRING_WKT = "MULTILINESTRING ((1 1, 2 1), (1 2, 0 0))";

  private static final String MULTIPOLYGON_WKT =
      "MULTIPOLYGON (((1 1,2 1,2 2,1 2,1 1)), ((0 0,1 1,2 0,0 0)))";

  private static final String GEOMETRYCOLLECTION_WKT =
      "GEOMETRYCOLLECTION (" + POINT_WKT + ", " + LINESTRING_WKT + ", " + POLYGON_WKT + ")";

  private static final String ACTION_URL = "http://example.com/source/id?transform=resource";

  private static BundleContext mockContext = mock(BundleContext.class);

  private static Bundle mockBundle = mock(Bundle.class);

  private static KMLTransformerImpl kmlTransformer;

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  @BeforeClass
  public static void setUp() throws IOException {
    when(mockContext.getBundle()).thenReturn(mockBundle);
    URL url = KMLTransformerImplTest.class.getResource(DEFAULT_STYLE_LOCATION);
    when(mockBundle.getResource(any(String.class))).thenReturn(url);

    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    ActionProvider mockActionProvider = mock(ActionProvider.class);
    Action mockAction = mock(Action.class);
    when(mockActionProvider.getAction(any(Metacard.class))).thenReturn(mockAction);
    when(mockAction.getUrl()).thenReturn(new URL(ACTION_URL));
    kmlTransformer =
        new KMLTransformerImpl(
            mockContext, DEFAULT_STYLE_LOCATION, new KmlStyleMap(), mockActionProvider);
  }

  @Before
  public void setupXpath() {
    Map<String, String> m = new HashMap<String, String>();
    m.put("m", "http://www.opengis.net/kml/2.2");
//    m.put("m", "urn:catalog:metacard");
//    m.put("gml", "http://www.opengis.net/gml");
    NamespaceContext ctx = new SimpleNamespaceContext(m);
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testPerformDefaultTransformationNoLocation() throws CatalogTransformerException {
    Metacard metacard = createMockMetacard("1234567890");
    kmlTransformer.performDefaultTransformation(metacard, null);
  }

  @Test
  public void testPerformDefaultTransformationPointLocation() throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(POINT_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getStyleSelector().isEmpty(), is(true));
    assertThat(placemark.getStyleUrl(), nullValue());
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(Point.class));
  }

  @Test
  public void testPerformDefaultTransformationLineStringLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(LINESTRING_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(LineString.class));
  }

  @Test
  public void testPerformDefaultTransformationPolygonLocation() throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(POLYGON_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(Polygon.class));
  }

  @Test
  public void testPerformDefaultTransformationMultiPointLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(MULTIPOINT_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiPoint = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiPoint.getGeometry().size(), is(3));
    assertThat(multiPoint.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiPoint.getGeometry().get(1), instanceOf(Point.class));
    assertThat(multiPoint.getGeometry().get(2), instanceOf(Point.class));
  }

  @Test
  public void testPerformDefaultTransformationMultiLineStringLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(MULTILINESTRING_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiLineString = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiLineString.getGeometry().size(), is(2));
    assertThat(multiLineString.getGeometry().get(0), instanceOf(LineString.class));
    assertThat(multiLineString.getGeometry().get(1), instanceOf(LineString.class));
  }

  @Test
  public void testPerformDefaultTransformationMultiPolygonLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(MULTIPOLYGON_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiPolygon = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiPolygon.getGeometry().size(), is(2));
    assertThat(multiPolygon.getGeometry().get(0), instanceOf(Polygon.class));
    assertThat(multiPolygon.getGeometry().get(1), instanceOf(Polygon.class));
  }

  @Test
  public void testPerformDefaultTransformationGeometryCollectionLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(GEOMETRYCOLLECTION_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo2 = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiGeo2.getGeometry().size(), is(3));
    assertThat(multiGeo2.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo2.getGeometry().get(1), instanceOf(LineString.class));
    assertThat(multiGeo2.getGeometry().get(2), instanceOf(Polygon.class));
  }

  @Test
  public void testTransformMetacardGetsDefaultStyle()
      throws CatalogTransformerException, IOException {
    MetacardImpl metacard = createMockMetacard("1234567890");
    metacard.setLocation(POINT_WKT);
    BinaryContent content = kmlTransformer.transform(metacard, null);
    assertThat(content.getMimeTypeValue(), is(KMLTransformerImpl.KML_MIMETYPE.toString()));
    IOUtils.toString(content.getInputStream());
  }

  @Test
  public void testTransformMetacardList()
      throws CatalogTransformerException, IOException, XpathException, SAXException {
    List<Metacard> metacardList = new ArrayList<>();
    metacardList.add(createMockMetacardWithLocation("1"));
    metacardList.add(createMockMetacardWithLocation("2"));

    List<BinaryContent> bc = kmlTransformer.transform(metacardList, null);
    assertThat(bc.size(), is(1));

    BinaryContent file = bc.get(0);
    assertThat(file.getMimeTypeValue(), is(KMLTransformerImpl.KML_MIMETYPE.toString()));

    String outputKml = new String(file.getByteArray());

    final String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n"
            + "<Document id=\"docId\">"
            + "<open>0</open>"
            + "<Placemark id=\"Placemark-1\">"
            + "<name>NameValue</name>"
            + "</Placemark>"
            + "<Placemark id=\"Placemark-2\">"
            + "<name>NameValue</name>"
            + "</Placemark>"
            + "</Document>"
            + "</kml>";

    final String output =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<kml \n"
            + "  xmlns=\"http://www.opengis.net/kml/2.2\" \n"
            + "  xmlns:ns2=\"http://www.google.com/kml/ext/2.2\" \n"
            + "  xmlns:ns4=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\" \n"
            + "  xmlns:ns3=\"http://www.w3.org/2005/Atom\">\n"
            + "  <Document id=\"c991e572-c251-43d4-a039-cabcc2e56c43\">\n"
            + "    <name>Results (2)</name>\n"
            + "    <open>0</open>\n"
            + "    <Style id=\"bluenormal\">\n"
            + "      <LabelStyle>\n"
            + "        <scale>0.0</scale>\n"
            + "      </LabelStyle>\n"
            + "      <LineStyle>\n"
            + "        <color>33ff0000</color>\n"
            + "        <width>3.0</width>\n"
            + "      </LineStyle>\n"
            + "      <PolyStyle>\n"
            + "        <color>33ff0000</color>\n"
            + "        <fill>1</fill>\n"
            + "      </PolyStyle>\n"
            + "      <BalloonStyle>\n"
            + "        <text>&lt;h3&gt;&lt;b&gt;$[name]&lt;/b&gt;&lt;/h3&gt;&lt;table&gt;&lt;tr&gt;&lt;td width=\"400\"&gt;$[description]&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;</text>\n"
            + "      </BalloonStyle>\n"
            + "    </Style>\n"
            + "    <Style id=\"bluehighlight\">\n"
            + "      <LabelStyle>\n"
            + "        <scale>1.0</scale>\n"
            + "      </LabelStyle>\n"
            + "      <LineStyle>\n"
            + "        <color>99ff0000</color>\n"
            + "        <width>6.0</width>\n"
            + "      </LineStyle>\n"
            + "      <PolyStyle>\n"
            + "        <color>99ff0000</color>\n"
            + "        <fill>1</fill>\n"
            + "      </PolyStyle>\n"
            + "      <BalloonStyle>\n"
            + "        <text>&lt;h3&gt;&lt;b&gt;$[name]&lt;/b&gt;&lt;/h3&gt;&lt;table&gt;&lt;tr&gt;&lt;td width=\"400\"&gt;$[description]&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;</text>\n"
            + "      </BalloonStyle>\n"
            + "    </Style>\n"
            + "    <StyleMap id=\"default\">\n"
            + "      <Pair>\n"
            + "        <key>normal</key>\n"
            + "        <styleUrl>#bluenormal</styleUrl>\n"
            + "      </Pair>\n"
            + "      <Pair>\n"
            + "        <key>highlight</key>\n"
            + "        <styleUrl>#bluehighlight</styleUrl>\n"
            + "      </Pair>\n"
            + "    </StyleMap>\n"
            + "    <Placemark id=\"Placemark-1\">\n"
            + "      <name>myTitle</name>\n"
            + "      <description>&lt;!DOCTYPE html&gt;\n"
            + "&lt;html&gt;  &lt;head&gt;    &lt;meta content=\"text/html; charset=windows-1252\" http-equiv=\"content-type\"&gt;    &lt;style media=\"screen\" type=\"text/css\"&gt;      .label {        font-weight: bold      }      .linkTable {        width: 100%      }      .thumbnailDiv {        text-align: center      }      img {        max-width: 100px;        max-height: 100px;        border-style:none      }      tr:nth-child(even) {        background-color: #c0c0c0;      }    &lt;/style&gt;  &lt;/head&gt;  &lt;body&gt;\n"
            + "        &lt;table style=\"width:100%\"&gt;        &lt;tbody&gt;                                &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Created&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Effective&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Expiration&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Id&lt;/td&gt;                &lt;td&gt;1&lt;/td&gt;            &lt;/tr&gt;                                                                                    &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Metadata Content Type&lt;/td&gt;                &lt;td&gt;myContentType&lt;/td&gt;            &lt;/tr&gt;                                                                &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Modified&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Title&lt;/td&gt;                &lt;td&gt;myTitle&lt;/td&gt;            &lt;/tr&gt;                            &lt;/tbody&gt;    &lt;/table&gt;    &lt;table class=\"linkTable\"&gt;      &lt;tr&gt;        &lt;td&gt;&lt;a href=\"http://example.com/source/id?transform=resource\"&gt;Download...&lt;/a&gt;&lt;/td&gt;          &lt;td&gt;&lt;/td&gt;      &lt;/tr&gt;    &lt;/table&gt;  &lt;/body&gt;\n"
            + "&lt;/html&gt;\n"
            + "</description>\n"
            + "      <TimeSpan>\n"
            + "        <begin>2018-03-05T23:02:12</begin>\n"
            + "      </TimeSpan>\n"
            + "      <styleUrl>#default</styleUrl>\n"
            + "      <Point>\n"
            + "        <coordinates>30.0,10.0</coordinates>\n"
            + "      </Point>\n"
            + "    </Placemark>\n"
            + "    <Placemark id=\"Placemark-2\">\n"
            + "      <name>myTitle</name>\n"
            + "      <description>&lt;!DOCTYPE html&gt;\n"
            + "&lt;html&gt;  &lt;head&gt;    &lt;meta content=\"text/html; charset=windows-1252\" http-equiv=\"content-type\"&gt;    &lt;style media=\"screen\" type=\"text/css\"&gt;      .label {        font-weight: bold      }      .linkTable {        width: 100%      }      .thumbnailDiv {        text-align: center      }      img {        max-width: 100px;        max-height: 100px;        border-style:none      }      tr:nth-child(even) {        background-color: #c0c0c0;      }    &lt;/style&gt;  &lt;/head&gt;  &lt;body&gt;\n"
            + "        &lt;table style=\"width:100%\"&gt;        &lt;tbody&gt;                                &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Created&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Effective&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Expiration&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Id&lt;/td&gt;                &lt;td&gt;2&lt;/td&gt;            &lt;/tr&gt;                                                                                    &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Metadata Content Type&lt;/td&gt;                &lt;td&gt;myContentType&lt;/td&gt;            &lt;/tr&gt;                                                                &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Modified&lt;/td&gt;                &lt;td&gt;2018-03-05T16:02:12-0700&lt;/td&gt;            &lt;/tr&gt;                                            &lt;tr&gt;                &lt;td style=\"font-weight: bold;\"&gt;Title&lt;/td&gt;                &lt;td&gt;myTitle&lt;/td&gt;            &lt;/tr&gt;                            &lt;/tbody&gt;    &lt;/table&gt;    &lt;table class=\"linkTable\"&gt;      &lt;tr&gt;        &lt;td&gt;&lt;a href=\"http://example.com/source/id?transform=resource\"&gt;Download...&lt;/a&gt;&lt;/td&gt;          &lt;td&gt;&lt;/td&gt;      &lt;/tr&gt;    &lt;/table&gt;  &lt;/body&gt;\n"
            + "&lt;/html&gt;\n"
            + "</description>\n"
            + "      <TimeSpan>\n"
            + "        <begin>2018-03-05T23:02:12</begin>\n"
            + "      </TimeSpan>\n"
            + "      <styleUrl>#default</styleUrl>\n"
            + "      <Point>\n"
            + "        <coordinates>30.0,10.0</coordinates>\n"
            + "      </Point>\n"
            + "    </Placemark>\n"
            + "  </Document>\n"
            + "</kml>";

//    assertThat("Example didn't match", countElements(xml, "Placemark"), is(2));
//    assertThat("Results didn't match", countElements(outputKml, "Placemark"), is(2));

//    final String testId = "Placemark-1";
//    assertXpathEvaluatesTo(testId, "/m:Placemark/@id", xml);
//    assertXpathExists(
//        "/m:kml/m:Document[@name='metacard-tags']/m:value[text()='basic-tag']", xml);
//    "/m:kml/m:Document/m:value[text()='basic-tag']", xml);

    assertXpathExists("/m:kml", xml);
    assertXpathExists("//Document", xml);
    assertXpathExists("//Placemark[@id='Placemark-1']/name", xml);
    assertXpathExists("//Placemark[@id='Placemark-2']/name", xml);

//    assertThat(outputKml, hasXPath("//Placemark/@id", equalTo("id=Placemark-1")));
//    assertThat(outputKml, hasXPath("//Style/@id", equalTo("bluenormal")));

//    assertThat(outputKml, hasXPath("//Placemark/@id", equalTo("Placemark-1")));
//    assertThat(outputKml, hasXPath("//Placemark/@id", equalTo("Placemark-2")));
  }

  private int countElements(String xml, String element) {
    Source source = Input.from(xml).build();
    Iterable<Node> i = new JAXPXPathEngine().selectNodes(String.format("//%s", element), source);
    assertNotNull(i);
    int count = 0;
    for (Iterator<Node> it = i.iterator(); it.hasNext(); ) {
      count++;
      assertEquals(element, it.next().getNodeName());
    }

    return count;
  }

  private MetacardImpl createMockMetacard(String id) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setContentTypeName("myContentType");
    metacard.setContentTypeVersion("myVersion");
    metacard.setCreatedDate(Calendar.getInstance().getTime());
    metacard.setEffectiveDate(Calendar.getInstance().getTime());
    metacard.setExpirationDate(Calendar.getInstance().getTime());
    metacard.setId(id);
    // metacard.setLocation(wkt);
    metacard.setMetadata("<xml>Metadata</xml>");
    metacard.setModifiedDate(Calendar.getInstance().getTime());
    // metacard.setResourceSize("10MB");
    // metacard.setResourceURI(uri)
    metacard.setSourceId("sourceID");
    metacard.setTitle("myTitle");
    return metacard;
  }

  private MetacardImpl createMockMetacardWithLocation(String id) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setContentTypeName("myContentType");
    metacard.setContentTypeVersion("myVersion");
    metacard.setCreatedDate(Calendar.getInstance().getTime());
    metacard.setEffectiveDate(Calendar.getInstance().getTime());
    metacard.setExpirationDate(Calendar.getInstance().getTime());
    metacard.setId(id);
    metacard.setLocation("POINT (30 10)");
    metacard.setMetadata("<xml>Metadata</xml>");
    metacard.setModifiedDate(Calendar.getInstance().getTime());
    // metacard.setResourceSize("10MB");
    // metacard.setResourceURI(uri)
    metacard.setSourceId("sourceID");
    metacard.setTitle("myTitle");
    return metacard;
  }
}
