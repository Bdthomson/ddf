package org.codice.ddf.spatial.kml.util;

import static java.util.Collections.singletonMap;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import java.io.IOException;
import java.io.InputStream;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class KmlMarshallerTest {

  private KmlMarshaller kmlMarshaller;

  @Before
  public void setupXpath() {
    NamespaceContext ctx =
        new SimpleNamespaceContext(singletonMap("m", "http://www.opengis.net/kml/2.2"));
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  @Before
  public void setup() {
    kmlMarshaller = new KmlMarshaller();
  }

  @Test
  public void marshall() throws SAXException, IOException, XpathException {

    Placemark placemark = new Placemark();
    placemark.setName("a");

    Kml kml = new Kml();
    kml.setFeature(placemark);
    final String kmlString = kmlMarshaller.marshal(kml);

    assertXpathExists("/m:kml", kmlString);
    assertXpathEvaluatesTo("a", "//m:Placemark/m:name", kmlString);
  }

  @Test
  public void unmarshall() {
    final InputStream resourceAsStream = this.getClass().getResourceAsStream("/kmlPoint.kml");
    final Kml kml = kmlMarshaller.unmarshal(resourceAsStream).get();
    final Feature feature = kml.getFeature();

    assertThat(feature.getName(), is("Simple placemark"));
  }
}
