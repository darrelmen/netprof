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

/*
  public ExerciseOptions() {
  }
*/

  public ExerciseOptions(INavigation.VIEWS instance) {
    this.instance = instance;
  }

/*
  public ExerciseOptions(float screenPortion,
                         boolean addKeyHandler,
                         INavigation.VIEWS instance,
                         boolean allowRecording,
                         boolean includeListButtons) {
    this.screenPortion = screenPortion;
    this.addKeyHandler = addKeyHandler;
    this.includeListButtons = includeListButtons;
    this.instance = instance;
    this.allowRecording = allowRecording;
  }
*/

  float getScreenPortion() {
    return screenPortion;
  }
  public boolean isAddKeyHandler() {
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
