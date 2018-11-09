/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.scoring;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.sound.AudioControl;

/**
 * Makes a red line appear on top of the waveform/spectogram,etc. marking playback position.
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since
 */
class AudioPositionPopup extends SimplePanel implements AudioControl  {
  private static int counter  = 0;
  private int id = 0;

  private float durationInMillis;
//  private static final boolean debugPartial = false;
//  private static final boolean debug = false;
  private final Panel imageContainer;

  /**
   * @see AudioPanel#addWidgets(String, String)
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
//    if (debugPartial) System.out.println(this + "  : AudioPositionPopup.reinitialize dur = " + durationInSeconds + "/" + durationInMillis);
    reinitialize();
  }

  public void reinitialize() {
//    if (debugPartial) System.out.println(this + "  : AudioPositionPopup.reinitialize ");
    songFinished();
  }

  private void setWavDurationInSeconds(double durationInSeconds) {
    this.durationInMillis = (float)durationInSeconds*1000f;
  }

  public void songFirstLoaded(double durationEstimate) {}

  @Override
  public void loadAndPlaySegment(float startInSeconds, float endInSeconds) {

  }

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
 //   if (debugPartial) System.out.println(this + "  : AudioPositionPopup.songFinished ");

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

 /*   if (!parent.getElement().getExID().equals(imageContainer.getElement().getExID())) {
      System.err.println("parent " + parent.getElement().getExID() + " but container " + imageContainer.getElement().getExID());
    }*/
    //positionInMillis -= (double)ScoringAudioPanel.MP3_HEADER_OFFSET;
    float positionInMillisF = (float)positionInMillis;// - ScoringAudioPanel.MP3_HEADER_OFFSET*1000;
    float horizontalFraction = positionInMillisF / durationInMillis;
    if (horizontalFraction > 1f) horizontalFraction = 1f;
    int pixelProgress = (int) Math.min(width - 2, ((float) width) * horizontalFraction);

    getElement().getStyle().setLeft(pixelProgress/* + parent.getAbsoluteLeft()*/, Style.Unit.PX);
    //getElement().getStyle().setTop(parent.getAbsoluteTop() + 1, Style.Unit.PX);
    setSize("2px", offsetHeight + "px");

/*    if (debug) {
      System.out.println(this + " showAt " + positionInMillis + " millis " + "/" +pixelProgress
        +" parent " + parent.getElement().getExID()+ " width " + width+
        " millis " +
        " dim x " +
        getOffsetWidth() +
        " y " + offsetHeight);
    }*/
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

  public String toString() { return "popup #" +id + " inside " + imageContainer.getElement().getId(); }
}
