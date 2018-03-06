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

import static org.codice.ddf.spatial.kml.transformer.KmzInputTransformer.KML_EXTENSION;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.xml.sax.SAXException;

public class KmzTransformerTest {

  KmzTransformer kmzTransformer;

  @Mock private KMLTransformerImpl kmlTransformer;

  @Before
  public void setup() {
    initMocks(this);
    kmzTransformer = new KmzTransformer(kmlTransformer);

    final String kmlOutput =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<kml xmlns=\"http://www.opengis.net/kml/2.2\" "
            + "xmlns:ns2=\"http://www.google.com/kml/ext/2.2\" "
            + "xmlns:ns4=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2.0\" "
            + "xmlns:ns3=\"http://www.w3.org/2005/Atom\">"
            + "<Document id=\"f73129e5-7346-44de-a217-431b128825b4\">"
            + "<name>KML Metacard Export</name>"
            + "<open>0</open>"
            + "<Style id=\"bluenormal\"><LabelStyle><scale>0.0</scale></LabelStyle><LineStyle><color>33ff0000</color><width>3.0</width></LineStyle><PolyStyle><color>33ff0000</color><fill>1</fill></PolyStyle><BalloonStyle><text>&lt;h3&gt;&lt;b&gt;$[name]&lt;/b&gt;&lt;/h3&gt;&lt;table&gt;&lt;tr&gt;&lt;td width=\"400\"&gt;$[description]&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;</text></BalloonStyle></Style><Style id=\"bluehighlight\"><LabelStyle><scale>1.0</scale></LabelStyle><LineStyle><color>99ff0000</color><width>6.0</width></LineStyle><PolyStyle><color>99ff0000</color><fill>1</fill></PolyStyle><BalloonStyle><text>&lt;h3&gt;&lt;b&gt;$[name]&lt;/b&gt;&lt;/h3&gt;&lt;table&gt;&lt;tr&gt;&lt;td width=\"400\"&gt;$[description]&lt;/td&gt;&lt;/tr&gt;&lt;/table&gt;</text></BalloonStyle></Style><StyleMap id=\"default\"><Pair><key>normal</key><styleUrl>#bluenormal</styleUrl></Pair><Pair><key>highlight</key><styleUrl>#bluehighlight</styleUrl></Pair></StyleMap>"
            + "<Placemark id=\"Placemark-UUID-1\"><name>ASU Campus</name>"
            + "<description>&lt;!DOCTYPE html&gt;\n</description><TimeSpan><begin>2018-03-06T19:53:52</begin></TimeSpan><styleUrl>#default</styleUrl><Point><coordinates>-111.9281,33.4242</coordinates></Point>"
            + "</Placemark>"
            + "<Placemark id=\"Placemark-UUID-2\"><name>Cardinals Stadium</name><description>&lt;!DOCTYPE html&gt;\n"
            + "</description><TimeSpan><begin>2018-03-06T19:53:52</begin></TimeSpan><styleUrl>#default</styleUrl><Point><coordinates>-112.2626,33.5276</coordinates></Point>"
            + "</Placemark>"
            + "</Document></kml>";

    InputStream kmlInputStream =
        new ByteArrayInputStream(kmlOutput.getBytes(StandardCharsets.UTF_8));

    when(kmlTransformer.transformToInputStream(any(), any())).thenReturn(kmlInputStream);
  }

  @Before
  public void setupXpath() {
    Map<String, String> m = new HashMap<String, String>();
    m.put("m", "http://www.opengis.net/kml/2.2");
    NamespaceContext ctx = new SimpleNamespaceContext(m);
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  @Test
  public void testKmzTransform()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    // Response from kml transformer is mocked, metacard list passed in is not used.
    List<BinaryContent> transform = kmzTransformer.transform(Collections.emptyList(), null);
    assertThat(transform.size(), is(1));

    // BC is a kmz zip file containing a single kml file called doc.kml.
    // Optionally, relative file links will exist in folder called files
    BinaryContent bc = transform.get(0);

    ZipInputStream zipInputStream = new ZipInputStream(bc.getInputStream());

    ZipEntry entry;
    String outputKml = "";
    while ((entry = zipInputStream.getNextEntry()) != null) {

      // According to Google, a .kmz should only contain a single .kml file
      // so we stop at the first one we find.
      final String fileName = entry.getName();
      if (fileName.endsWith(KML_EXTENSION)) {
        assertThat(fileName, is("doc.kml"));
        outputKml = readContentsFromInputStream(zipInputStream);
        break;
      }
    }

    // Prefixing with a single slash indicates root. Two slashes means a PathExpression can match
    // anywhere no matter what the prefix is. For kml Xpath testing, the xmlns attribute of a kml
    // document must be set in the prefix map as 'm' in the @Before method and you must reference
    // fields in the document with that prefix like so.

    assertXpathExists("/m:kml", outputKml);
    assertXpathExists("//m:Document", outputKml);
    assertXpathEvaluatesTo("KML Metacard Export", "//m:Document/m:name", outputKml);
    assertXpathExists("//m:Placemark[@id='Placemark-UUID-1']/m:name", outputKml);
    assertXpathExists("//m:Placemark[@id='Placemark-UUID-2']/m:name", outputKml);
  }

  private String readContentsFromInputStream(ZipInputStream zipInputStream) throws IOException {

    // Create a place to store the input stream.
    TemporaryFileBackedOutputStream fileBackedOutputStream = new TemporaryFileBackedOutputStream();

    // Copy zip input stream to output stream.
    IOUtils.copy(zipInputStream, fileBackedOutputStream);

    String s = new String(fileBackedOutputStream.asByteSource().read());

    // Close the zip input stream.
    IOUtils.closeQuietly(zipInputStream);

    return s;
  }
}
