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

package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PlayAudioEvent;
import mitll.langtest.client.flashcard.MyCustomIconType;

import java.util.Date;
import java.util.logging.Logger;

/**
 * A play button, and an interface with the SoundManagerAPI to call off into the soundmanager.js
 * package to play, pause, and track the progress of audio.<br></br>
 * <p>
 * Tells an option AudioControl listener about the state transitions and current playing audio location.<br></br>
 * <p>
 * Warns if a flash blocker is preventing flash from loading, preventing soundmanager2 from loading.<br></br>
 * There is an option to use HTML5 Audio but that seems to have playback issues when clicking on words or phonemes.
 * <p>
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/30/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayAudioPanel extends HeadlessPlayAudio {
  protected final Logger logger = Logger.getLogger("PlayAudioPanel");

  private static final boolean DEBUG = true;

  protected static final IconType PLAY = IconType.PLAY;

  /**
   * @see #setPlayButtonText
   */
  private static final String PAUSE_LABEL = "pause";

  protected String playLabel;
  private String pauseLabel = PAUSE_LABEL;
  protected IconAnchor playButton;
  private final boolean isSlow;

  protected final ExerciseController controller;

  protected final int exid;

  /**
   * @param buttonTitle
   * @param optionalToTheRight
   * @param doSlow
   * @param controller
   * @param exid
   * @param addButtonsNow
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   */
  public PlayAudioPanel(String buttonTitle,
                        Widget optionalToTheRight,
                        boolean doSlow,
                        ExerciseController controller,
                        int exid,
                        boolean addButtonsNow) {
    super(controller.getSoundManager());
    addStyleName("playButton");
    playLabel = buttonTitle;
    if (buttonTitle.isEmpty()) {
      pauseLabel = "";
    }
    getElement().setId("PlayAudioPanel_" + (doSlow ? "slow" : "") + id);

    isSlow = doSlow;

    this.controller = controller;
    this.exid = exid;

    if (addButtonsNow) {
      addButtons(optionalToTheRight);
    }

    /**
     * If another play widget on the page is playing - stop!
     */
    LangTest.EVENT_BUS.addHandler(PlayAudioEvent.TYPE, authenticationEvent -> {
      if (authenticationEvent.getId() != id) {
        //logger.info("this " + getClass() + " got play audio event " + authenticationEvent.getSource());
        if (isPlaying()) {
          if (DEBUG) {
            logger.info("\t PAUSE : this " + getClass() + " got play audio event " + authenticationEvent.getSource());
          }
          pause();
          reinitialize();
        }
      }
    });
  }

  /**
   * @param soundManager
   * @param playListener
   * @param controller
   * @param exid
   * @see mitll.langtest.client.flashcard.BootstrapExercisePanel#showRecoOutput
   */
  public PlayAudioPanel(SoundManagerAPI soundManager, PlayListener playListener, ExerciseController controller,
                        int exid) {
    this(playListener, "", null, controller, exid, true);
  }

  /**
   * @param playListener
   * @param buttonTitle
   * @param optionalToTheRight
   * @param controller
   * @param exercise
   * @param addButtonsNow
   * @see mitll.langtest.client.exercise.RecordAudioPanel.MyPlayAudioPanel#MyPlayAudioPanel
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   */
  public PlayAudioPanel(PlayListener playListener,
                        String buttonTitle,
                        Widget optionalToTheRight,
                        ExerciseController controller,
                        int exercise,
                        boolean addButtonsNow) {
    this(buttonTitle, optionalToTheRight, false, controller, exercise, addButtonsNow);

    if (playListener != null) {
      addPlayListener(playListener);
    }
  }

  public void setPlayLabel(String label) {
    this.playLabel = label;
    setText();
  }

  protected void setText() {
    playButton.setText(playLabel);
  }

  /**
   * Remember to destroy a sound once we are done with it, otherwise SoundManager
   * will maintain references to it, listener references, etc.
   */
  @Override
  protected void onUnload() {
    if (DEBUG) logger.info("onUnload : doing unload of play ------------------> ");
    super.onUnload();

    doPause();
    destroySound();

    for (AudioControl listener : listeners) listener.reinitialize();    // remove playing line, if it's there
  }

  /**
   * @param optionalToTheRight
   * @see #PlayAudioPanel
   */
  protected void addButtons(Widget optionalToTheRight) {
    playButton = makePlayButton(this);

/*    if (false) {
      warnNoFlash.setVisible(false);
      add(warnNoFlash);
    }*/

    if (optionalToTheRight != null) {
      optionalToTheRight.addStyleName("floatLeft");
      //  logger.info("adding " + optionalToTheRight.getElement().getExID() + " to " + getElement().getExID());
      add(optionalToTheRight);
    } else {
      // logger.info("NOT adding right optional thing  to " + getElement().getExID());
    }
  }

  /**
   * @param toAddTo
   * @return
   * @see PlayAudioPanel#addButtons
   */
  protected IconAnchor makePlayButton(DivWidget toAddTo) {
    Button playButton = new Button(playLabel);

    playButton.addClickHandler(event -> {
      doPlayPauseToggle();
      controller.logEvent(playButton, "play audio", exid, "");
    });

    showPlayIcon(playButton);
    stylePlayButton(playButton);

    toAddTo.add(playButton);
    return playButton;
  }

  @Override
  public void doPlayPauseToggle() {
    if (playButton.isVisible() && isEnabled()) {
      super.doPlayPauseToggle();
    }
  }

  private void stylePlayButton(Button playButton) {
    playButton.setType(ButtonType.INFO);
    playButton.getElement().getStyle().setProperty("minWidth", "15px");
    playButton.getElement().setId("PlayAudioPanel_playButton");
    playButton.addStyleName("leftFiveMargin");
    playButton.addStyleName("floatLeft");
    playButton.setEnabled(false);
  }

  private void styleSlowIcon(Widget playButton) {
    Style style = playButton.getElement().getStyle();
    style.setPaddingBottom(3, Style.Unit.PX);
    style.setPaddingLeft(6, Style.Unit.PX);
    style.setPaddingRight(6, Style.Unit.PX);
  }


  protected boolean isEnabled() {
    return playButton.isEnabled();
  }

  /**
   * Don't change the  button state if the audio is playing.
   *
   * @param val
   * @see
   */
  public void setEnabled(boolean val) {
//    if (DEBUG) {
//      logger.info(" setEnabled val " + val);
//    //  logger.warning("askServerForExercise got " + getExceptionAsString(new Exception()));
//    }
    if (!isPlaying()) {
      playButton.setEnabled(val);
    }
  }

  @Override
  protected void doPlaySegment(float startInSeconds, float endInSeconds) {
    super.doPlaySegment(startInSeconds, endInSeconds);
    setPlayButtonText();
  }

  private void setPlayButtonText() {
    boolean playing1 = isPlaying();
    playButton.setText(playing1 ? pauseLabel : playLabel);
    if (playing1) {
      playButton.setIcon(IconType.PAUSE);
    } else {
//      logger.info("setPlayButtonText set cursor to play ");
      showPlayIcon(playButton);
    }
  }

  /**
   * @see #pause
   * @see #resetAudio
   */
  private void setPlayLabel() {
    // markNotPlaying();

    if (DEBUG) logger.info(new Date() + " setPlayLabel playing " + isPlaying());
    setText();

    showPlayIcon(playButton);

    playListeners.forEach(PlayListener::playStopped);
  }

  private void showPlayIcon(IconAnchor playButton) {
    if (isSlow) {
      playButton.setBaseIcon(MyCustomIconType.turtle);
      styleSlowIcon(playButton);
    } else {
      //logger.info("showPlayIcon ");
      playButton.setIcon(PLAY);
    }
  }

  // --- playing audio ---

  /**
   * @return
   * @see mitll.langtest.client.exercise.RecordAudioPanel#getPlayButton
   */
  public Widget getPlayButton() {
    return playButton;
  }

  public PlayAudioPanel setPauseLabel(String pauseLabel) {
    this.pauseLabel = pauseLabel;
    return this;
  }

  @Override
  public void songFirstLoaded(double durationEstimate) {
    super.songFirstLoaded(durationEstimate);
    setEnabled(true);
  }

  @Override
  public void songLoaded(double duration) {
    super.songLoaded(duration);
    setEnabled(true);
  }

  @Override
  public void songFinished() {
    super.songFinished();
    setPlayLabel();
  }

  @Override
  protected void resetAudio() {
    super.resetAudio();
    setPlayLabel();
  }

  protected void play() {
    super.play();
    setPlayButtonText();
  }

  @Override
  protected void pause() {
    super.pause();
    setPlayLabel();
  }

  public String toString() {
    return "PlayAudioPanel #" + id + " : " + currentPath;
  }
}
