package org.codice.ddf.catalog.ui.metacard.workspace.transformations;

import static java.util.stream.Collectors.toList;
import static org.codice.ddf.catalog.ui.metacard.MetacardApplication.ACTIONS_KEY;

import com.google.common.collect.Sets;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Metacard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.workspace.ListMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;
import org.codice.ddf.configuration.SystemBaseUrl;

public class EmbeddedListMetacardsHandler extends EmbeddedMetacardsHandler {

  private static final Set<String> EXTERNAL_LIST_ATTRIBUTES = Sets.newHashSet(ACTIONS_KEY);
  private final ActionRegistry actionRegistry;

  public EmbeddedListMetacardsHandler(ActionRegistry actionRegistry) {
    super(WorkspaceAttributes.WORKSPACE_LISTS, ListMetacardImpl.TYPE);
    this.actionRegistry = actionRegistry;
  }

  @Override
  protected Map<String, Object> metacardToJsonMapper(
      Metacard metacard, Map<String, Object> workspaceAsMap) {

    final List<Map<String, Object>> listActions = getListActions(metacard);
    final List<Map<String, Object>> lists =
        (List<Map<String, Object>>) workspaceAsMap.get(WorkspaceAttributes.WORKSPACE_LISTS);
    if (lists != null) {
      lists.forEach(list -> list.put(ACTIONS_KEY, listActions));
    }

    return workspaceAsMap;
  }

  @Override
  protected Map<String, Object> jsonToMetacardMapper(Map<String, Object> map) {
    return map.entrySet()
        .stream()
        .filter(entry -> !EXTERNAL_LIST_ATTRIBUTES.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
