package mitll.langtest.client.scoring;

import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.sound.AudioControl;

/**
 * Makes a red line appear on top of the waveform/spectogram,etc. marking playback position.
 */
class AudioPositionPopup implements AudioControl {
  private int id = 0;
  private PopupPanel imageOverlay;
  private float durationInMillis;
  private static int counter  = 0;
  private final boolean debugPartial = false;
  private final boolean debug = false;
  private Panel imageContainer;

  /**
   * @see AudioPanel#getPlayButtons
   */
  public AudioPositionPopup(Panel imageContainer) {
    id = counter++;
    this.imageContainer = imageContainer;
    imageOverlay = new PopupPanel(false);
    imageOverlay.setStyleName("ImageOverlay");
    SimplePanel w = new SimplePanel();
    imageOverlay.add(w);
    w.setStyleName("ImageOverlay");
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio(String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
   */
  public void reinitialize(double durationInSeconds) {
    if (debugPartial) System.out.println(this + "  : AudioPositionPopup.reinitialize dur = " + durationInSeconds);

    setWavDurationInSeconds(durationInSeconds);
    reinitialize();
  }

  public void reinitialize() {
    if (debugPartial) System.out.println(this + "  : AudioPositionPopup.reinitialize ");

    imageOverlay.hide();
    int left = imageContainer.getAbsoluteLeft();
    int top  = imageContainer.getAbsoluteTop();
    imageOverlay.setPopupPosition(left, top);
  }

  private void setWavDurationInSeconds(double durationInSeconds) {
    this.durationInMillis = (float)durationInSeconds*1000f;
  }

  public void songFirstLoaded(double durationEstimate) {}

  /**
   * @param durationInMillisIgnored
   * @see mitll.langtest.client.sound.PlayAudioPanel#songLoaded(double)
   */
  public void songLoaded(final double durationInMillisIgnored) {
    int offsetHeight = imageContainer.getOffsetHeight();
    int left = imageContainer.getAbsoluteLeft();
    int top = imageContainer.getAbsoluteTop();
    if (debugPartial) {
      System.out.println(this + " songLoaded " + durationInMillisIgnored + " height " + offsetHeight + " x " + left +
        " y " + top);
    }
    imageOverlay.setSize("2px", offsetHeight + "px");
    imageOverlay.setPopupPosition(left, top);
    if (debug) System.out.println("songLoaded " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
        " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim " +
        imageOverlay.getOffsetWidth() + " x " + imageOverlay.getOffsetHeight());
  }

  /**
   * @see mitll.langtest.client.sound.PlayAudioPanel#update(double)
   * @param position
   */
  public void update(double position) {
    if (!imageOverlay.isShowing()) {
      imageOverlay.show();
    }
    showAt(position);
  }

  /**
   * So the story here is that the wav->mp3 conversion adds about 0.1 sec to the end of the mp3 file
   * So to get the audio positionInMillis to translate to a positionInMillis on the image, we need to use the
   * wav file duration from the server and not the mp3 duration {@link #setWavDurationInSeconds(double)}
   *
   * @param positionInMillis where soundmanager tells us the audio cursor is during playback
   */
  private void showAt(double positionInMillis) {
    setHeightFromContainer();

    float horizontalFraction = (float) positionInMillis / durationInMillis;
    if (horizontalFraction > 1f) horizontalFraction = 1f;
    int pixelProgress = (int) (((float) imageContainer.getOffsetWidth()) * horizontalFraction);
    int left = imageContainer.getAbsoluteLeft() + pixelProgress;
    int top = imageContainer.getAbsoluteTop();
    imageOverlay.setPopupPosition(left, top);

    if (debug) System.out.println(this + " showAt " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
        " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim x " +
        imageOverlay.getOffsetWidth() + " y " + imageOverlay.getOffsetHeight());
  }

  public void setHeightFromContainer() {
    int offsetHeight = imageContainer.getOffsetHeight();
    imageOverlay.setSize("2px", offsetHeight + "px");
  }

  public String toString() { return "popup #" +id + " inside " +imageContainer.getElement().getId(); }
}
