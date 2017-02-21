package mitll.langtest.client.scoring;

/**
 * Created by go22670 on 2/21/17.
 */
public class ExerciseOptions {
  private float screenPortion = 1.0f;
  private boolean addKeyHandler = false;
  private String instance = "";
  private boolean allowRecording = true;
  private boolean includeListButtons = true;

  public ExerciseOptions() {
  }

  public ExerciseOptions(String instance) {
    this.instance = instance;
  }

  public ExerciseOptions(float screenPortion,
                         boolean addKeyHandler,
                         String instance,
                         boolean allowRecording,
                         boolean includeListButtons) {
    this.screenPortion = screenPortion;
    this.addKeyHandler = addKeyHandler;
    this.includeListButtons = includeListButtons;
    this.instance = instance;
    this.allowRecording = allowRecording;
  }

  public float getScreenPortion() {
    return screenPortion;
  }

  public ExerciseOptions setScreenPortion(float screenPortion) {
    this.screenPortion = screenPortion;
    return this;
  }

  public boolean isAddKeyHandler() {
    return addKeyHandler;
  }

  public ExerciseOptions setAddKeyHandler(boolean addKeyHandler) {
    this.addKeyHandler = addKeyHandler;
    return this;
  }

  public String getInstance() {
    return instance;
  }

  public ExerciseOptions setInstance(String instance) {
    this.instance = instance;
    return this;
  }

  public boolean isAllowRecording() {
    return allowRecording;
  }

  public ExerciseOptions setAllowRecording(boolean allowRecording) {
    this.allowRecording = allowRecording;
    return this;
  }

  public boolean isIncludeListButtons() {
    return includeListButtons;
  }

  public ExerciseOptions setIncludeListButtons(boolean includeListButtons) {
    this.includeListButtons = includeListButtons;
    return this;
  }
}
