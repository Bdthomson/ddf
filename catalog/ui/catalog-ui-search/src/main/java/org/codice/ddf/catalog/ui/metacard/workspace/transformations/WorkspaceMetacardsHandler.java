package org.codice.ddf.catalog.ui.metacard.workspace.transformations;

import static java.util.stream.Collectors.toList;

import ddf.action.ActionRegistry;
import ddf.catalog.data.Metacard;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceValueTransformation;
import org.codice.ddf.configuration.SystemBaseUrl;

public class WorkspaceMetacardsHandler implements WorkspaceValueTransformation<List, List> {

  public static final String ACTIONS_KEY = "actions";

  private static final Set<String> EXTERNAL_LIST_ATTRIBUTES = Collections.singleton(ACTIONS_KEY);

  private final ActionRegistry actionRegistry;

  public WorkspaceMetacardsHandler(ActionRegistry actionRegistry) {
    this.actionRegistry = actionRegistry;
  }

  @Override
  public String getKey() {
    return WorkspaceAttributes.WORKSPACE_LISTS;
  }

  @Override
  public Class<List> getMetacardValueType() {
    return List.class;
  }

  @Override
  public Class<List> getJsonValueType() {
    return List.class;
  }

  @Override
  public Optional<List> metacardValueToJsonValue(
      WorkspaceTransformer transformer, List metacardXMLStrings) {

    final List<Map<String, Object>> listActions = getListActions(null);
    //    final List<Map<String, Object>> lists =
    //        (List<Map<String, Object>>) workspaceAsMap.get(WorkspaceAttributes.WORKSPACE_LISTS);

    //    if (lists != null) {
    //      lists.forEach(list -> list.put(ACTIONS_KEY, listActions));
    //    }

    //    return workspaceAsMap;

    //    return Optional.of(
    //        ((List<Object>) metacardXMLStrings)
    //            .stream()
    //            .map(transformer::xmlToMetacard)
    //            .map(transformer::transform)
    //            .collect(toList()));
    return Optional.empty();
  }

  @Override
  public Optional<List> jsonValueToMetacardValue(WorkspaceTransformer transformer, List jsonValue) {
    return Optional.empty();

    //    return map.entrySet()
    //        .stream()
    //        .filter(entry -> !EXTERNAL_LIST_ATTRIBUTES.contains(entry.getKey()))
    //        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

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
