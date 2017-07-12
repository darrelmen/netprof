package mitll.langtest.client.custom;

import mitll.langtest.client.list.Reloadable;

/**
 * @see mitll.langtest.client.banner.NewContentChooser
 * Created by go22670 on 4/10/17.
 */
public interface ExerciseListContent extends ContentView {
  void hideList();
  void loadExercise(int exid);
  Reloadable getReloadable();
}
