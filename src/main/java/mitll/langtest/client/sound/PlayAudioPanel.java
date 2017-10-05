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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.PlayAudioEvent;
import mitll.langtest.client.flashcard.MyCustomIconType;
import mitll.langtest.shared.exercise.CommonAudioExercise;
import mitll.langtest.shared.exercise.CommonExercise;

import java.util.*;
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
public class PlayAudioPanel extends DivWidget implements AudioControl {
  protected final Logger logger = Logger.getLogger("PlayAudioPanel");

  private static final boolean DEBUG = false;
  private static final boolean LOCAL_TESTING = false;
  protected static final IconType PLAY = IconType.PLAY;

  /**
   * @see #setPlayButtonText
   */
  private static final String PAUSE_LABEL = "pause";
  private static final String FILE_MISSING = "FILE_MISSING";
  private String currentPath = null;
  private Sound currentSound = null;
  private final SoundManagerAPI soundManager;

  protected String playLabel;
  private String pauseLabel = PAUSE_LABEL;
  protected IconAnchor playButton;
  private final boolean isSlow;

  private final HTML warnNoFlash = new HTML("<font color='red'>Flash is not activated. Do you have a flashblocker? " +
      "Please add this site to its whitelist.</font>");
  private final Collection<AudioControl> listeners = new HashSet<>();
  private SimpleAudioListener simpleAudioListener;
  private final List<PlayListener> playListeners = new ArrayList<>();
  private static int counter = 0;
  private final int id;
  private boolean playing = false;
  protected final ExerciseController controller;
  protected final CommonAudioExercise exercise;

  /**
   * @param soundManager
   * @param buttonTitle
   * @param optionalToTheRight
   * @param doSlow
   * @param controller
   * @param exercise
   * @param addButtonsNow
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   */
  public PlayAudioPanel(SoundManagerAPI soundManager,
                        String buttonTitle,
                        Widget optionalToTheRight,
                        boolean doSlow,
                        ExerciseController controller,
                        CommonAudioExercise exercise,
                        boolean addButtonsNow) {
    this.soundManager = soundManager;
    addStyleName("playButton");
    playLabel = buttonTitle;
    if (buttonTitle.isEmpty()) {
      pauseLabel = "";
    }
    id = counter++;
    getElement().setId("PlayAudioPanel_" + (doSlow ? "slow" : "") + id);

    isSlow = doSlow;

    this.controller = controller;
    this.exercise = exercise;

    if (addButtonsNow) {
      addButtons(optionalToTheRight);
    }

    LangTest.EVENT_BUS.addHandler(PlayAudioEvent.TYPE, authenticationEvent -> {
      if (authenticationEvent.getId() != id) {
        // logger.info("this " + getClass() + " instance " + instanceName + " updating progress " + authenticationEvent.getSource());
        if (isPlaying()) {
          pause();
          reinitialize();
        }
      }
    });
  }

  /**
   * @param soundManager
   * @param path
   * @param doSlow
   * @param controller
   * @param exercise
   * @seex PressAndHoldExercisePanel#getPlayAudioPanel
   * @deprecated only for amas and dialog
   */
  public PlayAudioPanel(SoundManagerAPI soundManager, String path, boolean doSlow, ExerciseController controller, CommonAudioExercise exercise) {
    this(soundManager, "", null, doSlow, controller, exercise, true);
    loadAudio(path);
  }

  public PlayAudioPanel(SoundManagerAPI soundManager, PlayListener playListener, ExerciseController controller, CommonAudioExercise exercise) {
    this(soundManager, playListener, "", null, controller, exercise, true);
  }

  /**
   * @param soundManager
   * @param playListener
   * @param buttonTitle
   * @param optionalToTheRight
   * @param controller
   * @param exercise
   * @param addButtonsNow
   * @see mitll.langtest.client.exercise.RecordAudioPanel.MyPlayAudioPanel#MyPlayAudioPanel(com.github.gwtbootstrap.client.ui.Image, com.github.gwtbootstrap.client.ui.Image, com.google.gwt.user.client.ui.Panel, String, Widget, ExerciseController, CommonExercise)
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   */
  public PlayAudioPanel(SoundManagerAPI soundManager,
                        PlayListener playListener,
                        String buttonTitle,
                        Widget optionalToTheRight,
                        ExerciseController controller,
                        CommonAudioExercise exercise, boolean addButtonsNow) {
    this(soundManager, buttonTitle, optionalToTheRight, false, controller, exercise, addButtonsNow);
    addPlayListener(playListener);
  }

  public void setPlayLabel(String label) {
    this.playLabel = label;
    setText();
    //return this;
  }

  protected void setText() {
    playButton.setText(playLabel);
  }

  /**
   * @param playListener
   * @see mitll.langtest.client.exercise.RecordAudioPanel#addPlayListener(PlayListener)
   */
  public void addPlayListener(PlayListener playListener) {
    this.playListeners.add(playListener);
  }

  /**
   * Remember to destroy a sound once we are done with it, otherwise SoundManager
   * will maintain references to it, listener references, etc.
   */
  @Override
  protected void onUnload() {
    if (DEBUG) logger.info("doing unload of play ------------------> ");
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

    playButton.addClickHandler(event -> doClick());

    showPlayIcon(playButton);
    stylePlayButton(playButton);

    toAddTo.add(playButton);
    return playButton;
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

  private void stylePlayButton(Button playButton) {
    playButton.setType(ButtonType.INFO);
    //if (isLarge) {
    //   playButton.setSize(ButtonSize.LARGE);
    // }
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

  /**
   * @see #addButtons(Widget)
   */
  protected void doClick() {
    //logger.info("PlayAudioPanel doClick " + playing + " " +currentPath);
    if (playButton.isVisible() && isEnabled()) {
      if (isPlaying()) {
        if (DEBUG) logger.info("PlayAudioPanel doClick pause " + playing + " " + currentPath);
        pause();  // somehow get exception here?
      } else {
        if (DEBUG) logger.info("PlayAudioPanel doClick start " + playing + " " + currentPath);
        startPlaying();
      }
    }
  }

  private void startPlaying() {
    if (LOCAL_TESTING) {
      logger.info("startPlaying " + currentPath);
      startSong(currentPath);
    }
    LangTest.EVENT_BUS.fireEvent(new PlayAudioEvent(id));

    for (PlayListener playListener : playListeners) playListener.playStarted();
    play();
  }


  public void doPause() {
    if (isPlaying()) {
      pause();
    }
  }

  /**
   * @param listener
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons
   */
  public void setListener(AudioControl listener) {
    listeners.clear();
    this.listeners.add(listener);
    //logger.info("setListener now has listener " + listeners.size());
  }

  private void addSimpleListener(SimpleAudioListener listener) {
    this.simpleAudioListener = listener;
  }

  /**
   * @see #doClick
   */
  private void play() {
    if (DEBUG || LOCAL_TESTING) {
      logger.info("PlayAudioPanel play " + playing + " " + currentPath);
    }
    playing = true;
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  private void setPlayButtonText() {
    boolean playing1 = isPlaying();
    playButton.setText(playing1 ? pauseLabel : playLabel);
    if (playing1) {
      playButton.setIcon(IconType.PAUSE);
    } else {
      logger.info("setPlayButtonText set cursor to play ");
      showPlayIcon(playButton);
    }
  }

  protected boolean isEnabled() {
    return playButton.isEnabled();
  }

  /**
   * @param val
   * @see
   */
  public void setEnabled(boolean val) {
    playButton.setEnabled(val);
  }

  private boolean isPlaying() {
    return playing;
  }

  private void setPlayLabel() {
    playing = false;
    if (DEBUG) logger.info(new Date() + " setPlayLabel playing " + playing);
    setText();

    showPlayIcon(playButton);

    for (PlayListener playListener : playListeners) playListener.playStopped();
  }

  // --- playing audio ---

  /**
   * @param startInSeconds
   * @param endInSeconds
   * @see mitll.langtest.client.scoring.AudioPanel#playSegment
   */
  @Override
  public void repeatSegment(float startInSeconds, float endInSeconds) {
    if (currentSound != null) {
      doPlaySegment(startInSeconds, endInSeconds);
    } else {
      if (DEBUG || currentPath == null) logger.info("repeatSegment - new path " + currentPath);

      addSimpleListener(new SimpleAudioListener() {
        @Override
        public void songLoaded(double duration) {
          if (DEBUG) logger.info("playAudio - songLoaded " + currentPath + " this " + this);
          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
              if (DEBUG) logger.info("playAudio - songLoaded calling doClick  " + currentPath);
              doPlaySegment(startInSeconds, endInSeconds);
            }
          });
        }

        @Override
        public void songFinished() {
        }
      });

      loadAudio(currentPath);
    }
  }

  private void doPlaySegment(float startInSeconds, float endInSeconds) {
    playing = true;
    setPlayButtonText();
    playSegment(startInSeconds, endInSeconds);
  }

  /**
   * Checks to see if the sound was created properly before trying to play it.
   *
   * @param startInSeconds
   * @param endInSeconds
   */
  private void playSegment(float startInSeconds, float endInSeconds) {
    if (currentSound != null & soundManager != null) {
      soundManager.pause(currentSound);
      float start1 = startInSeconds * 1000f;
      float end1 = endInSeconds * 1000f;
      int s = Math.round(start1);
      int e = Math.round(end1);

      //  logger.info("playing from " + s + " to " + e);
      soundManager.playInterval(currentSound, s, e);
    }
  }

  /**
   * @see #reinitialize()
   */
  private void pause() {
    if (DEBUG) logger.info("PlayAudioPanel :pause");

    setPlayLabel();
    if (soundManager != null) soundManager.pause(currentSound);
  }

  public void update(double position) {
    for (AudioControl listener : listeners) listener.update(position);
  }

  /**
   * @see mitll.langtest.client.scoring.ChoicePlayAudioPanel#configureButton2
   */
  protected void playAudio() {
    if (currentPath == null) {
      logger.warning("playAudio " + currentPath);
    } else {
      playAudio(currentPath);
    }
  }

  /**
   * @param path
   * @seex mitll.langtest.client.custom.exercise.CommentNPFExercise#getShowGroup
   * @see mitll.langtest.client.scoring.ChoicePlayAudioPanel#playAndRemember
   */
  public void playAudio(String path) {
    if (currentPath.equals(path) && currentSound != null) {
      doClick();
    } else {
      if (DEBUG) logger.info("playAudio - new path " + path);

      addSimpleListener(new SimpleAudioListener() {
        @Override
        public void songLoaded(double duration) {
          if (DEBUG) logger.info("playAudio - songLoaded " + path + " this " + this);
          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
              if (DEBUG) logger.info("playAudio - songLoaded calling doClick  " + path);
              doClick();
            }
          });
        }

        @Override
        public void songFinished() {
        }
      });

      loadAudio(path);
    }
  }

  public void loadAudio(String path) {
    if (DEBUG) logger.info("playAudio - loadAudio " + path);
    String fixedPath = rememberAudio(path);
    if (isPlaying()) pause();
    startSong(path);
    if (DEBUG) logger.info("playAudio - loadAudio finished " + fixedPath);
  }

  protected String rememberAudio(String path) {
    if (DEBUG || path == null) logger.info("rememberAudio - path " + path);
    destroySound();
    this.currentPath = CompressedAudio.getPath(path);
    return currentPath;
  }

  /**
   * destroy any other current sound first...
   * <p>
   * Check if soundmanager loaded properly, warn if it didn't.
   *
   * @param path to audio file on server
   * @see mitll.langtest.client.scoring.AudioPanel#getReadyToPlayAudio
   * @see mitll.langtest.client.scoring.SimpleRecordAudioPanel#getReadyToPlayAudio
   * @see #loadAudio
   */
  public void startSong(String path) {
    if (path == null) logger.warning("no path given???");
    else if (!path.equals(FILE_MISSING)) {
      //logger.info("PlayAudioPanel.loadAudio - skipping " + path);
      if (DEBUG) logger.info("PlayAudioPanel : startSong : " + path);
      if (soundManager.isReady()) {
        //if (DEBUG) logger.info(new Date() + " Sound manager is ready.");
        if (soundManager.isOK()) {
          //  if (DEBUG)
          if (DEBUG) logger.info("PlayAudioPanel : startSong : " + path + " destroy current sound " + currentSound);

          destroySound();
          createSound(path);
        } else {
          logger.info(" Sound manager is not OK!.");
          warnNoFlash.setVisible(true);
        }
      }
    }
  }

  /**
   * @param song
   * @see #startSong(String)
   */
  private void createSound(String song) {
    if (LOCAL_TESTING) {
      currentPath = song;
    }

    currentSound = new Sound(this);
    if (DEBUG) {
      logger.info("PlayAudioPanel.createSound  : (" + getElement().getId() + ") for " + song + " : " + this + " created sound " + currentSound);
    }

    String uniqueID = song + "_" + getElement().getId(); // fix bug where multiple npf panels might load the same audio file and not load the second one seemingly
    soundManager.createSound(currentSound, uniqueID, song);
  }

  /**
   * @see #onUnload()
   * @see #startSong(String)
   */
  private void destroySound() {
    if (currentSound != null) {
      //if (DEBUG)
      //  logger.info("PlayAudioPanel.destroySound : (" + getElement().getId() + ") destroy sound " + currentSound);
      this.soundManager.destroySound(currentSound);
      currentSound = null;
    }
  }

  /**
   * Does repeat audio if childCount > 0
   */
  public void reinitialize() {
    if (DEBUG || LOCAL_TESTING) {
      logger.info("PlayAudioPanel :reinitialize " + getElement().getId());
    }

    resetAudio();

    if (DEBUG || LOCAL_TESTING)
      logger.info("PlayAudioPanel :reinitialize - telling listener to reinitialize " + listeners);

    for (AudioControl listener : listeners) listener.reinitialize();

//    else {
//      logger.info("PlayAudioPanel :reinitialize - no listener");
//    }
  }

  private void resetAudio() {
    setPlayLabel();
    update(0);
    if (currentSound != null) {
      soundManager.setPosition(currentSound, 0);
    }
  }

  /**
   * So this is the first message you'd get...
   *
   * @param durationEstimate
   */
  public void songFirstLoaded(double durationEstimate) {
    if (DEBUG) {
      logger.info("PlayAudioPanel.songFirstLoaded : " + this);
    }

    for (AudioControl listener : listeners) listener.songFirstLoaded(durationEstimate);

//    if (listener != null && listener != this) {
//      listener.songFirstLoaded(durationEstimate);
//    } else if (listener != null) {
//      logger.info("PlayAudioPanel :songFirstLoaded - listener is me??? ");
//    }
    setEnabled(true);
  }

  /**
   * @param duration
   * @see SoundManager#songLoaded(Sound, double)
   */
  public void songLoaded(double duration) {
    // if (DEBUG) logger.info("PlayAudioPanel.songLoaded : " + this);

//    if (listener != null) {
//      listener.songLoaded(duration);
//    }
    for (AudioControl listener : listeners) listener.songLoaded(duration);

    if (simpleAudioListener != null) {
      if (DEBUG) logger.info("PlayAudioPanel.songLoaded : " + this);
      simpleAudioListener.songLoaded(duration);
    }
//    else {
//      logger.info("no listener for song loaded " + duration);
//    }
    setEnabled(true);
    reinitialize();
  }

  /**
   * Called when the audio stops playing, also relays the message to the listener if there is one.
   */
  public void songFinished() {
    if (DEBUG) logger.info("PlayAudioPanel :songFinished " + getElement().getId());

    setPlayLabel();

//    if (listener != null) {  // remember to delegate too
//      listener.songFinished();
//    }
    for (AudioControl listener : listeners) listener.songFinished();


    if (simpleAudioListener != null) {
      simpleAudioListener.songFinished();
    }
  }

  /**
   * @return
   * @see mitll.langtest.client.exercise.RecordAudioPanel#getPlayButton
   */
  public Widget getPlayButton() {
    return playButton;
  }

  public void playCurrent() {
    if (DEBUG) logger.info("PlayAudioPanel :playCurrent " + playing);
    playing = true;
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  public PlayAudioPanel setPauseLabel(String pauseLabel) {
    this.pauseLabel = pauseLabel;
    return this;
  }

  public PlayAudioPanel setMinWidth(int minWidth) {
    // this.minWidth = minWidth;
    return this;
  }

  public String toString() {
    return "PlayAudioPanel #" + id + " : " + currentPath;
  }
}
