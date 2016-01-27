/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.exercise.ExerciseController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
 * User: go22670
 * Date: 8/30/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayAudioPanel extends HorizontalPanel implements AudioControl {
  private Logger logger = Logger.getLogger("PlayAudioPanel");

  /**
   * @see #setPlayButtonText
   */
  private static final String PAUSE_LABEL = "pause";
  private static final int MIN_WIDTH = 40;
  private static final boolean DEBUG = false;
  private static final String FILE_MISSING = "FILE_MISSING";
  private String currentPath = null;

  private Sound currentSound = null;
  private final SoundManagerAPI soundManager;

  private String playLabel;
  private String pauseLabel = PAUSE_LABEL;
  private int minWidth = MIN_WIDTH;
  private final Button playButton;

  private final HTML warnNoFlash = new HTML("<font color='red'>Flash is not activated. Do you have a flashblocker? " +
      "Please add this site to its whitelist.</font>");
  private AudioControl listener;
  private SimpleAudioListener simpleAudioListener;
  private final List<PlayListener> playListeners = new ArrayList<PlayListener>();
  private static int counter = 0;
  private final int id;
  private boolean playing = false;

  /**
   * @param soundManager
   * @param buttonTitle
   * @param optionalToTheRight
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   */
  public PlayAudioPanel(SoundManagerAPI soundManager, String buttonTitle, Widget optionalToTheRight) {
    this.soundManager = soundManager;
    setSpacing(10);
    setVerticalAlignment(ALIGN_MIDDLE);
    playLabel = buttonTitle;
    if (buttonTitle.isEmpty()) {
      minWidth = 12;
      pauseLabel = "";
    }
    playButton = new Button(playLabel);
    playButton.setIcon(IconType.PLAY);
    id = counter++;
    getElement().setId("PlayAudioPanel_" + id);
    addButtons(optionalToTheRight);
  }

  public PlayAudioPanel(ExerciseController controller, String path) {
    this(controller.getSoundManager(), "", null);
    loadAudio(path);
    this.currentPath = path;
  }

  public PlayAudioPanel setPlayLabel(String label) {
    this.playLabel = label;
    playButton.setText(playLabel);
    return this;
  }

  /**
   * @param soundManager
   * @param playListener
   * @param buttonTitle
   * @param optionalToTheRight
   * @see mitll.langtest.client.exercise.RecordAudioPanel.MyPlayAudioPanel#MyPlayAudioPanel(com.github.gwtbootstrap.client.ui.Image, com.github.gwtbootstrap.client.ui.Image, com.google.gwt.user.client.ui.Panel, String, com.google.gwt.user.client.ui.Widget)
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   */
  public PlayAudioPanel(SoundManagerAPI soundManager, PlayListener playListener, String buttonTitle, Widget optionalToTheRight) {
    this(soundManager, buttonTitle, optionalToTheRight);
    addPlayListener(playListener);
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
    if (DEBUG) System.out.println("doing unload of play ------------------> ");
    super.onUnload();

    doPause();
    destroySound();
    if (listener != null) listener.reinitialize();    // remove playing line, if it's there
  }

  /**
   * @param optionalToTheRight
   * @see #PlayAudioPanel
   */
  protected void addButtons(Widget optionalToTheRight) {
    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });
    playButton.setType(ButtonType.INFO);
    playButton.getElement().setId("PlayAudioPanel_playButton");
    playButton.addStyleName("leftFiveMargin");
    playButton.setEnabled(false);
    add(playButton);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);

    if (optionalToTheRight != null) {
      //  logger.info("adding " + optionalToTheRight.getElement().getId() + " to " + getElement().getId());
      add(optionalToTheRight);
    } else {
      // logger.info("NOT adding right optional thing  to " + getElement().getId());
    }
  }

  /**
   * Make sure play button doesn't squish down when it says "pause"
   */
  @Override
  protected void onLoad() {
    super.onLoad();

    int offsetWidth = playButton.getOffsetWidth();
    if (offsetWidth < minWidth && minWidth > 0) {
      offsetWidth = minWidth;
    }

    if (minWidth > 0) {
//      System.out.println("setting min width " + minWidth + " on " + playButton.getElement().getId());
      if (minWidth == 12) offsetWidth = 12;
      playButton.getElement().getStyle().setProperty("minWidth", offsetWidth + "px");
    }
  }

  /**
   * @see #addButtons(Widget)
   */
  private void doClick() {
    if (playButton.isVisible() && playButton.isEnabled()) {
      if (isPlaying()) {
        pause();
      } else {
        for (PlayListener playListener : playListeners) playListener.playStarted();
        play();
      }
    }
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
  public void addListener(AudioControl listener) {
    this.listener = listener;
  }

  public void addSimpleListener(SimpleAudioListener listener) {
    this.simpleAudioListener = listener;
  }

  /**
   * @see #doClick()
   */
  private void play() {
    if (DEBUG) System.out.println("PlayAudioPanel :play " + playing);
    playing = true;
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  private void setPlayButtonText() {
    boolean playing1 = isPlaying();
    String html = playing1 ? pauseLabel : playLabel;
    // System.out.println("setPlayButtonText now playing = " + isPlaying());
    playButton.setText(html);
    playButton.setIcon(playing1 ? IconType.PAUSE : IconType.PLAY);
  }

  private boolean isPlaying() {
    return playing;
  }

  private void setPlayLabel() {
    playing = false;
    if (DEBUG) logger.info(new Date() + " setPlayLabel playing " + playing);
    playButton.setText(playLabel);
    playButton.setIcon(IconType.PLAY);

    for (PlayListener playListener : playListeners) playListener.playStopped();
  }

  // --- playing audio ---

  /**
   * @param startInSeconds
   * @param endInSeconds
   * @see mitll.langtest.client.scoring.AudioPanel#playSegment
   */
  public void repeatSegment(float startInSeconds, float endInSeconds) {
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
    if (currentSound != null) {
      soundManager.pause(currentSound);
      float start1 = startInSeconds * 1000f;
      float end1 = endInSeconds * 1000f;
      int s = (int) start1;
      int e = (int) end1;

      //logger.info("playing from " + s + " to " + e);
      soundManager.playInterval(currentSound, s, e);
    }
  }

  /**
   * @see #reinitialize()
   */
  private void pause() {
    if (DEBUG) logger.info("PlayAudioPanel :pause");

    setPlayLabel();
    soundManager.pause(currentSound);
  }

  public void update(double position) {
    if (listener != null) {
      listener.update(position);
    }
    //else {
    //logger.info("PlayAudioPanel :update - no listener");
    //}
  }

  private String loadAudio(String path) {
    path = wavToMP3(path);
    path = ensureForwardSlashes(path);
    if (isPlaying()) pause();
    startSong(path);
    return path;
  }

  /**
   * @param path
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#getShowGroup(java.util.List)
   */
  public void playAudio(String path) {
    if (currentPath.equals(path)) {
      doClick();
    } else {
      logger.info("playAudio - " + path);

      loadAudio(path);
      this.currentPath = path;

      addSimpleListener(new SimpleAudioListener() {
        @Override
        public void songLoaded(double duration) {
          Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
              doClick();
            }
          });
        }

        @Override
        public void songFinished() {
        }
      });
    }
  }

  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;

  public void loadAudioAgain(String path) {
    loadAudio(path);
    setPath(path);
  }

  private void setPath(String path) {
    this.currentPath = path;
  }

  protected String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }


  /**
   * Check if soundmanager loaded properly, warn if it didn't.
   *
   * @param path to audio file on server
   * @see mitll.langtest.client.scoring.AudioPanel#getImagesForPath(String)
   */
  public void startSong(String path) {
    if (!path.equals(FILE_MISSING)) {
      //logger.info("PlayAudioPanel.loadAudio - skipping " + path);
      if (DEBUG) logger.info("PlayAudioPanel : start song : " + path);
      if (soundManager.isReady()) {
        //if (DEBUG) logger.info(new Date() + " Sound manager is ready.");
        if (soundManager.isOK()) {
          if (DEBUG)
            logger.info("PlayAudioPanel : startSong : " + path + " destroy current sound " + currentSound);

          destroySound();
          createSound(path);
        } else {
          logger.info(new Date() + " Sound manager is not OK!.");
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
      if (DEBUG)
        logger.info("PlayAudioPanel.destroySound : (" + getElement().getId() + ") destroy sound " + currentSound);

      this.soundManager.destroySound(currentSound);
    }
  }

  /**
   * Does repeat audio if count > 0
   */
  public void reinitialize() {
    if (DEBUG) {
      logger.info("PlayAudioPanel :reinitialize " + getElement().getId());
    }

    setPlayLabel();
    update(0);
    soundManager.setPosition(currentSound, 0);

    if (listener != null) {
      if (DEBUG) logger.info("PlayAudioPanel :reinitialize - telling listener to reinitialize ");

      listener.reinitialize();
    }
//    else {
//      logger.info("PlayAudioPanel :reinitialize - no listener");
//    }
  }

  /**
   * So this is the first message you'd get...
   *
   * @param durationEstimate
   */
  public void songFirstLoaded(double durationEstimate) {
    if (DEBUG) {
      logger.info("PlayAudioPanel.songFirstLoaded : " + this);
      //  new Exception().printStackTrace();
    }

    if (listener != null && listener != this) {
      listener.songFirstLoaded(durationEstimate);
    } else if (listener != null) {
      logger.info("PlayAudioPanel :songFirstLoaded - listener is me??? ");
    }
    setEnabled(true);
  }

  /**
   * @param duration
   * @see SoundManager#songLoaded(Sound, double)
   */
  public void songLoaded(double duration) {
    if (DEBUG) logger.info("PlayAudioPanel.songLoaded : " + this);

    if (listener != null) {
      listener.songLoaded(duration);
    }
    if (simpleAudioListener != null) {
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

    if (listener != null) {  // remember to delegate too
      listener.songFinished();
    }
    if (simpleAudioListener != null) {
      simpleAudioListener.songFinished();
    }
  }

  public void setEnabled(boolean val) {
    playButton.setEnabled(val);
  }

  /**
   * @return
   * @see mitll.langtest.client.exercise.RecordAudioPanel#getPlayButton()
   */
  public Button getPlayButton() {
    return playButton;
  }

  public void playCurrent() {
    if (DEBUG) logger.info("PlayAudioPanel :play " + playing);
    playing = true;
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  public PlayAudioPanel setPauseLabel(String pauseLabel) {
    this.pauseLabel = pauseLabel;
    return this;
  }

  public PlayAudioPanel setMinWidth(int minWidth) {
    this.minWidth = minWidth;
    return this;
  }

  public String toString() {
    return "PlayAudioPanel #" + id;
  }
}
