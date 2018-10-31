package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.base.ListItem;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.shared.exercise.MatchInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public interface ChoicesContainer {
  @NotNull
  ListItem getTypeContainer(String type);

  Panel addChoices(Map<String, Set<MatchInfo>> typeToValues, String type);
}
