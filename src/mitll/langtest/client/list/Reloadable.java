package mitll.langtest.client.list;

/**
 * Created by go22670 on 2/2/16.
 */
public interface Reloadable {
  void redraw();
  void reload();

  void reloadWithCurrent();

  void clearCachedExercise();

  String getCurrentExerciseID();

  void loadExercise(String itemID);
}
