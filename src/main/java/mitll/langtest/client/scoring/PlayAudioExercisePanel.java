/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.ListenViewHelper;
import mitll.langtest.client.sound.HeadlessPlayAudio;
import mitll.langtest.client.sound.IPlayAudioControl;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.shared.exercise.AudioAttribute;

import java.util.logging.Logger;

public class PlayAudioExercisePanel extends DivWidget implements IPlayAudioControl, IPlayAudioExercise {
  private final Logger logger = Logger.getLogger("PlayAudioExercisePanel");

  private HeadlessPlayAudio playAudio;

  private static final boolean DEBUG_PLAY_PAUSE = false;

  public void rememberAudio(AudioAttribute next) {
    //  logger.info("rememberAudio audio for " + this + "  " + next);
    playAudio.rememberAudio(next);
  }

  /**
   * @return
   * @see ListenViewHelper#playCurrentTurn
   */
  @Override
  public boolean doPlayPauseToggle() {
    if (playAudio != null) {
      if (DEBUG_PLAY_PAUSE) logger.info("doPlayPauseToggle on ");// + getExID());
//
//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("doing play for " +getExID()));
//      logger.info("logException stack " + exceptionAsString);

      return playAudio.doPlayPauseToggle();
    } else {
      logger.warning("doPlayPauseToggle no play audio???");
      return false;
    }
  }

  public boolean hasAudio() {
    return playAudio.hasAudio();
  }

  /**
   * What to show if you go to play audio but there is none there...
   */
  @Override
  public void showNoAudioToPlay() {
//    Style style = getElement().getStyle();
//    String before = style.getBackgroundColor();
//    style.setBackgroundColor("red");
    Widget widget = getChildren().get(0);
    widget.addStyleName("blink-target");


    Timer waitTimer = new Timer() {
      @Override
      public void run() {
        //   logger.info("scheduleWaitTimer timer expired..." + outer);
        widget.removeStyleName("blink-target");
      }
    };
    waitTimer.schedule(1000);
  }

  /**
   * @param playListener
   * @return
   * @see ListenViewHelper#getTurnPanel
   */
  public boolean addPlayListener(PlayListener playListener) {
    if (playAudio != null) {
      playAudio.addPlayListener(playListener);
      return true;
    } else {
      logger.warning("\n\naddPlayListener ignore adding play listener since no play audio...\n\n");
      return false;
    }
  }

  /**
   *
   * @return true if paused
   */
  @Override
  public boolean doPause() {
    return playAudio.doPause();
  }

  public boolean isPlaying() {
    return playAudio.isPlaying();
  }

  public void resetAudio() {
    playAudio.reinitialize();
  }

  @Override
  protected void onUnload() {
    if (playAudio != null) {
      playAudio.destroySound();
    }
  }

  protected HeadlessPlayAudio setPlayAudio(HeadlessPlayAudio playAudio) {
    this.playAudio = playAudio;
    return playAudio;
  }

  public HeadlessPlayAudio getPlayAudio() {
    return playAudio;
  }
}
