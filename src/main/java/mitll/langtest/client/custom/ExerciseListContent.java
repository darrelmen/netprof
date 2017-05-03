package mitll.langtest.client.custom;

import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.list.Reloadable;

/**
 * Created by go22670 on 4/10/17.
 */
public interface ExerciseListContent {
  void showContent(Panel listContent, String instanceName);
 // void showContent(Panel listContent, INavigation.VIEWS instanceName);

  void hideList();

  void loadExercise(int exid);

  Reloadable getReloadable();
}
