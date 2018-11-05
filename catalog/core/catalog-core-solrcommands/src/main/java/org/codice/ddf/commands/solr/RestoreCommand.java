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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.codice.solr.factory.impl.HttpSolrClientFactory;

@Service
@Command(
  scope = SolrCommands.NAMESPACE,
  name = "restore",
  description = "Makes a backup of the selected Solr core/collection."
)
public class RestoreCommand extends SolrCommands {

  @Option(
    name = "-d",
    aliases = {"--dir"},
    multiValued = false,
    required = false,
    description =
        "The location of the backup snapshot file. If not specified, it looks for backups in Solr’s data directory."
  )
  String backupLocation;

  @Option(
    name = "-n",
    aliases = {"--name"},
    multiValued = false,
    required = false,
    description =
        "The name of the backed up index snapshot to be restored. If the name is not provided it looks for backups with snapshot.<timestamp> format in the location directory. It picks the latest timestamp backup in that case."
  )
  String backupName;

  @Option(
    name = "-c",
    aliases = {"--core"},
    multiValued = false,
    required = false,
    description = "Specify the Solr core to operate on (e.g. catalog)."
  )
  String coreName = DEFAULT_CORE_NAME;

  @Option(
    name = "-s",
    aliases = {"--restoreStatus"},
    multiValued = false,
    required = false,
    description =
        "Get the status of a SolrCloud asynchronous restore. Used in conjunction with --requestId."
  )
  boolean status;

  @Option(
    name = "-i",
    aliases = {"--requestId"},
    multiValued = false,
    required = false,
    description =
        "Request Id returned after performing a restore. This request Id is used to track"
            + " the status of a given restore. When requesting a restore status, --restoreStatus and --requestId"
            + " are both required options."
  )
  String requestId;

  @Option(
    name = "-a",
    aliases = {"--asyncRestore"},
    multiValued = false,
    required = false,
    description = "Perform an asynchronous restore."
  )
  boolean asyncRestore;

  @Option(
    name = "-f",
    aliases = {"--force"},
    multiValued = false,
    required = false,
    description = "Forces the restore. Will delete the collection if it already exists"
  )
  boolean force = false;

  private final int shardCount = NumberUtils.toInt(System.getProperty("solr.cloud.shardCount"), 2);

  private final int replicationFactor =
      NumberUtils.toInt(System.getProperty("solr.cloud.replicationFactor"), 2);

  private final int maximumShardsPerNode =
      NumberUtils.toInt(System.getProperty("solr.cloud.maxShardPerNode"), 2);

  @Override
  public Object execute() throws Exception {
    if (isSystemConfiguredWithSolrCloud()) {
      performSolrCloudRestore();

    } else {
      performSingleNodeSolrRestore();
    }

    return null;
  }

  private void performSingleNodeSolrRestore() throws URISyntaxException {
    String restoreUrl = getReplicationUrl(coreName);

    try {
      createSolrCore();

      URIBuilder uriBuilder = new URIBuilder(restoreUrl);
      uriBuilder.addParameter("command", "restore");

      if (StringUtils.isNotBlank(backupLocation)) {
        uriBuilder.addParameter("location", backupLocation);
      }

      URI restoreUri = uriBuilder.build();
      LOGGER.debug("Sending request to {}", restoreUri);

      HttpWrapper httpClient = getHttpClient();

      processResponse(httpClient.execute(restoreUri));
    } catch (IOException | SolrServerException e) {
      LOGGER.error("Unable to perform single node Solr restore", e);
    }
  }

  private void performSolrCloudRestore() throws IOException {
    SolrClient client = null;
    try {
      client = getCloudSolrClient();
      if (status) {
        verifyStatusInput();
        getStatus(client, requestId);
      } else {
        verifyRestoreInput();
        performSolrCloudRestore(client);
      }
    } finally {
      shutdown(client);
    }
  }

  private void processResponse(ResponseWrapper responseWrapper) {

    if (responseWrapper.getStatusCode() == HttpStatus.SC_OK) {
      printSuccessMessage(String.format("%nRestore of [%s] complete.%n", coreName));
    } else {
      printErrorMessage(String.format("Error restoring up Solr core: [%s]", coreName));
      printErrorMessage(
          String.format(
              "Restore command failed due to: %d - %s %n Request: %s",
              responseWrapper.getStatusCode(),
              responseWrapper.getStatusPhrase(),
              getReplicationUrl(coreName)));
    }
  }

  private boolean restore(
      SolrClient client, String collection, String backupLocation, String backupName)
      throws IOException, SolrServerException {
    if (canRestore(client, collection)) {
      CollectionAdminRequest.Restore restore =
          CollectionAdminRequest.restoreCollection(collection, backupName)
              .setLocation(backupLocation);
      CollectionAdminResponse response = restore.process(client, collection);
      LOGGER.debug("Restore status: {}", response.getStatus());
      boolean success = response.getStatus() == 0;
      if (!success) {
        printErrorMessage("Restore failed. ");
        printResponseErrorMessages(response);
      }
      return success;
    } else {
      LOGGER.warn("Unable to restore collection {}", collection);
      return false;
    }
  }

  private String restoreAsync(
      SolrClient client, String collection, String backupLocation, String backupName)
      throws IOException, SolrServerException {

    if (canRestore(client, collection)) {
      CollectionAdminRequest.Restore restore =
          CollectionAdminRequest.AsyncCollectionAdminRequest.restoreCollection(
                  collection, backupName)
              .setLocation(backupLocation);

      String requestId = restore.processAsync(client);
      LOGGER.debug("Restore request Id: {}", requestId);
      return requestId;
    } else {
      LOGGER.info("Unable to restore: {}, collection already exists", collection);
      return null;
    }
  }

  private void performSolrCloudRestore(SolrClient client) {
    LOGGER.debug("Restoring collection {} from {} / {}.", coreName, backupLocation, backupName);
    printInfoMessage(
        String.format(
            "Restoring collection [%s] from [%s] / [%s].", coreName, backupLocation, backupName));
    try {
      if (asyncRestore) {
        String requestId = restoreAsync(client, coreName, backupLocation, backupName);
        printInfoMessage("Restore request Id: " + requestId);
      } else {
        boolean isSuccess = restore(client, coreName, backupLocation, backupName);
        if (isSuccess) {
          printInfoMessage("Restore complete.");
        }
      }
    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : "";
      printErrorMessage(String.format("Restore failed. %s", message));
    }
  }

  private boolean canRestore(SolrClient client, String collection)
      throws IOException, SolrServerException {
    if (collectionExists(client, collection)) {
      if (force) {
        CollectionAdminRequest.Delete delete = CollectionAdminRequest.deleteCollection(collection);
        CollectionAdminResponse response = delete.process(client);
        return response.isSuccess();
      } else {
        return false;
      }
    }
    return true;
  }

  private void createSolrCore() throws IOException, SolrServerException {
    String url = HttpSolrClientFactory.getDefaultHttpsAddress();
    final HttpClientBuilder builder =
        HttpClients.custom()
            .setDefaultCookieStore(new BasicCookieStore())
            .setMaxConnTotal(128)
            .setMaxConnPerRoute(32);

    if (StringUtils.startsWithIgnoreCase(url, "https")) {
      builder.setSSLSocketFactory(
          new SSLConnectionSocketFactory(
              HttpSolrClientFactory.getSslContext(),
              HttpSolrClientFactory.getProtocols(),
              HttpSolrClientFactory.getCipherSuites(),
              SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER));
    }
    final String solrDataDir = HttpSolrClientFactory.getSolrDataDir();
    if (solrDataDir != null) {
      ConfigurationStore.getInstance().setDataDirectoryPath(solrDataDir);
    }
    HttpSolrClientFactory.createSolrCore(url, coreName, null, builder.build());
  }

  private void verifyStatusInput() {
    if (status && StringUtils.isBlank(requestId)) {
      throw new IllegalArgumentException(SEE_COMMAND_USAGE_MESSAGE);
    }
  }

  private void verifyRestoreInput() {
    if (StringUtils.isNotBlank(requestId)) {
      throw new IllegalArgumentException(SEE_COMMAND_USAGE_MESSAGE);
    }
  }
}
