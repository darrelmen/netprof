package mitll.langtest.client.scoring;

import mitll.langtest.client.custom.INavigation;

/**
 * Created by go22670 on 2/21/17.
 */
public class ExerciseOptions {
  private final float screenPortion = 1.0f;
  private final boolean addKeyHandler = false;
  private final INavigation.VIEWS instance;
  private final boolean allowRecording = true;
  private final boolean includeListButtons = true;

  public ExerciseOptions(INavigation.VIEWS instance) {
    this.instance = instance;
  }

  float getScreenPortion() {
    return screenPortion;
  }
  boolean isAddKeyHandler() {
    return addKeyHandler;
  }
  public INavigation.VIEWS getInstance() {
    return instance;
  }
  boolean isAllowRecording() {
    return allowRecording;
  }
  boolean isIncludeListButtons() {
    return includeListButtons;
  }
}
