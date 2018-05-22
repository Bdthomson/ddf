package org.codice.ddf.catalog.ui.metacard.workspace.transformations;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.Sets;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.codice.ddf.catalog.ui.metacard.workspace.ListMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.configuration.SystemBaseUrl;

public class EmbeddedListMetacardsHandler extends EmbeddedMetacardsHandler {

  public static final String ACTIONS_KEY = "actions";

  private static final Set<String> EXTERNAL_LIST_ATTRIBUTES = Sets.newHashSet(ACTIONS_KEY);
  private final ActionRegistry actionRegistry;

  public EmbeddedListMetacardsHandler(ActionRegistry actionRegistry) {
    super(WorkspaceAttributes.WORKSPACE_LISTS, ListMetacardImpl.TYPE);
    this.actionRegistry = actionRegistry;
  }

  /**
   * Add "actions" key to list metacards.
   *
   * @param workspaceMetacard
   * @param workspaceAsMap
   * @return
   */
  @Override
  public Optional<List> metacardValueToJsonValue(
      WorkspaceTransformer transformer, List metacardXMLStrings, Metacard workspaceMetacard) {

    final List<Map<String, Object>> listActions = getListActions(workspaceMetacard);

    final Optional<List> listMetacardOptional =
        super.metacardValueToJsonValue(transformer, metacardXMLStrings, workspaceMetacard);

    listMetacardOptional.ifPresent(
        listMetacards ->
            ((List<Object>) listMetacards)
                .stream()
                .filter(Metacard.class::isInstance)
                .map(Metacard.class::cast)
                .forEach(
                    listMetacard ->
                        listMetacard.setAttribute(
                            new AttributeImpl(ACTIONS_KEY, (Serializable) listActions))));

    return listMetacardOptional;
  }

  /**
   * TODO: Remove "actions" key from list metacard map.
   *
   * @param transformer
   * @param metacardJsonData
   * @return
   */
  @Override
  public Optional<List> jsonValueToMetacardValue(
      WorkspaceTransformer transformer, List metacardJsonData) {
    //

    //    ((List<Object>) metacardJsonData)
    //        .stream()
    //        .filter(Map.class::isInstance)
    //        .map(Map.class::cast)
    //        .map(m -> m.get)
    ////        .filter(entry -> entry.getKey)
    ////        .map(
    ////            queryJson -> {
    ////              final Metacard metacard = new MetacardImpl(metacardType);
    ////              transformer.transformIntoMetacard(queryJson, metacard);
    ////              return metacard;
    ////            })
    ////        .map(transformer::metacardToXml)
    ////        .collect(Collectors.toList()));
    //
    //
    //    return map.entrySet()
    //        .stream()
    //        .filter(entry -> !EXTERNAL_LIST_ATTRIBUTES.contains(entry.getKey()))
    //        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return super.jsonValueToMetacardValue(transformer, metacardJsonData);
  }

  /**
   * Given a {@link org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl}, get a list
   * of actions that can be executed on a list.
   *
   * @param workspaceMetacard
   * @return
   */
  private List<Map<String, Object>> getListActions(Metacard workspaceMetacard) {
    final String host =
        SystemBaseUrl.getProtocol() + SystemBaseUrl.getHost() + ":" + SystemBaseUrl.getPort();

    return actionRegistry
        .list(workspaceMetacard)
        .stream()
        .filter(action -> action.getId().startsWith("catalog.data.metacard.list"))
        .map(
            action -> {
              // Work-around for paths being behind VPCs with non-public DNS values
              final String url = action.getUrl().toString().replaceFirst(host, "");
              final Map<String, Object> actionMap = new HashMap<>();
              actionMap.put("id", action.getId());
              actionMap.put("url", url);
              actionMap.put("title", action.getTitle());
              actionMap.put("description", action.getDescription());
              return actionMap;
            })
        .collect(toList());
  }
}
