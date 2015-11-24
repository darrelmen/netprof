/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.scoring;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.sound.AudioControl;

/**
 * Makes a red line appear on top of the waveform/spectogram,etc. marking playback position.
 */
class AudioPositionPopup extends SimplePanel implements AudioControl  {
  private static int counter  = 0;
  private int id = 0;

  private float durationInMillis;
  private static final boolean debugPartial = false;
  private static final boolean debug = false;
  private final Panel imageContainer;

  /**
   * @see AudioPanel#addWidgets(String, String, String)
   */
  public AudioPositionPopup(Panel imageContainer) {
    id = counter++;
    this.imageContainer = imageContainer;
    setStyleName("ImageOverlay");
    getElement().setId("AudioPositionPopup");
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getImageURLForAudio(String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
   */
  public void reinitialize(double durationInSeconds) {
    setWavDurationInSeconds(durationInSeconds);
    if (debugPartial) System.out.println(this + "  : AudioPositionPopup.reinitialize dur = " + durationInSeconds + "/" + durationInMillis);
    reinitialize();
  }

  public void reinitialize() {
    if (debugPartial) System.out.println(this + "  : AudioPositionPopup.reinitialize ");
    songFinished();
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
    Widget parent = imageContainer;// getParent();
    setSize("2px", parent.getOffsetHeight() + "px");
    getElement().getStyle().setPosition(Style.Position.ABSOLUTE);
    getElement().getStyle().setLeft(0, Style.Unit.PX);
    getElement().getStyle().setTop(0, Style.Unit.PX);
/*    if (debug) System.out.println("songLoaded " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
        " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim " +
        imageOverlay.getOffsetWidth() + " x " + imageOverlay.getOffsetHeight());*/
  }

  @Override
  public void songFinished() {
    if (debugPartial) System.out.println(this + "  : AudioPositionPopup.songFinished ");

    setVisible(false);

    getElement().getStyle().setLeft(0, Style.Unit.PX);
    getElement().getStyle().setTop(0, Style.Unit.PX);
  }

  /**
   * @see mitll.langtest.client.sound.PlayAudioPanel#update(double)
   * @param position
   */
  public void update(double position) {
    if (!isVisible()) setVisible(true);
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
    Widget parent = imageContainer;//getParent();
    int offsetHeight = parent.getOffsetHeight();
    int width = parent.getOffsetWidth();

 /*   if (!parent.getElement().getId().equals(imageContainer.getElement().getId())) {
      System.err.println("parent " + parent.getElement().getId() + " but container " + imageContainer.getElement().getId());
    }*/
    //positionInMillis -= (double)ScoringAudioPanel.MP3_HEADER_OFFSET;
    float positionInMillisF = (float)positionInMillis - ScoringAudioPanel.MP3_HEADER_OFFSET*1000;
    float horizontalFraction = positionInMillisF / durationInMillis;
    if (horizontalFraction > 1f) horizontalFraction = 1f;
    int pixelProgress = (int) Math.min(width - 2, ((float) width) * horizontalFraction);

    getElement().getStyle().setLeft(pixelProgress/* + parent.getAbsoluteLeft()*/, Style.Unit.PX);
    //getElement().getStyle().setTop(parent.getAbsoluteTop() + 1, Style.Unit.PX);
    setSize("2px", offsetHeight + "px");

    if (debug) {
      System.out.println(this + " showAt " + positionInMillis + " millis " + "/" +pixelProgress
        +" parent " + parent.getElement().getId()+ " width " + width+
        " millis " +
        " dim x " +
        getOffsetWidth() +
        " y " + offsetHeight);
    }
  }

/*
  public void setHeightFromContainer() {
    Widget parent = imageContainer;//getParent();
    int offsetHeight = parent.getOffsetHeight();
    System.out.println("Set height to " + offsetHeight);
    if (offsetHeight > 0) {
      setSize("2px", offsetHeight + "px");
    }
  }
*/

  public String toString() {
    Widget parent = imageContainer;//getParent();
    return "popup #" +id + " inside " + parent.getElement().getId(); }
}
