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
package org.codice.ddf.commands.solr;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

public abstract class SolrCommandTest {
  protected static final String DEFAULT_ZK_HOSTS =
      "zookeeperhost1:2181,zookeeperhost2:2181,zookeeperhost3:2181";

  protected static final String DDF_HOME_PROP = "ddf.home";

  protected static final String DDF_ETC_PROP = "ddf.etc";

  protected static final String DEFAULT_DDF_HOME = "/opt/ddf";

  @Mock protected SolrClient mockSolrClient;

  @Rule @ClassRule public static TemporaryFolder baseDir = new TemporaryFolder();

  @Rule @ClassRule public static TemporaryFolder backupLocation = new TemporaryFolder();

  protected HttpWrapper mockHttpWrapper;

  protected ConsoleOutput consoleOutput;

  protected static MiniSolrCloudCluster miniSolrCloud;

  protected static String cipherSuites;

  protected static String protocols;

  protected static Path getBaseDirPath() {
    return baseDir.getRoot().toPath();
  }

  protected String getBackupLocation() {
    return backupLocation.getRoot().getPath();
  }

  protected void setupSystemProperties(String solrClientType) {
    setupSolrClientType(solrClientType);
    setupZkHost();
  }

  protected static void setDdfHome() {
    System.setProperty(DDF_HOME_PROP, DEFAULT_DDF_HOME);
  }

  protected static void setDdfEtc() {
    System.setProperty(DDF_ETC_PROP, Paths.get(DEFAULT_DDF_HOME, "etc").toString());
  }

  protected void setupSolrClientType(String solrClientType) {
    System.setProperty(SolrCommands.SOLR_CLIENT_PROP, solrClientType);
  }

  protected void setupZkHost() {
    System.setProperty(SolrCommands.ZOOKEEPER_HOSTS_PROP, DEFAULT_ZK_HOSTS);
  }
}
