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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.flashcard.MyCustomIconType;

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
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 8/30/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayAudioPanel extends DivWidget implements AudioControl {
  protected final Logger logger = Logger.getLogger("PlayAudioPanel");

  protected static final IconType PLAY = IconType.PLAY;

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

  protected String playLabel;
  private String pauseLabel = PAUSE_LABEL;
  private int minWidth = MIN_WIDTH;
  protected IconAnchor playButton;
  private boolean isSlow;

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
   * @param doSlow
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   */
  public PlayAudioPanel(SoundManagerAPI soundManager, String buttonTitle, Widget optionalToTheRight, boolean doSlow) {
    this.soundManager = soundManager;
    playLabel = buttonTitle;
    if (buttonTitle.isEmpty()) {
      minWidth = 12;
      pauseLabel = "";
    }
    id = counter++;
    getElement().setId("PlayAudioPanel_" + (doSlow ? "slow" : "") + id);

    isSlow = doSlow;

    addButtons(optionalToTheRight);
  }

  /**
   * @param soundManager
   * @param path
   * @param doSlow
   * @seex PressAndHoldExercisePanel#getPlayAudioPanel
   */
  public PlayAudioPanel(SoundManagerAPI soundManager, String path, boolean doSlow) {
    this(soundManager, "", null, doSlow);
    loadAudio(path);
  }

  public PlayAudioPanel setPlayLabel(String label) {
    this.playLabel = label;
    setText();
    return this;
  }

  protected void setText() {
    playButton.setText(playLabel);
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
    this(soundManager, buttonTitle, optionalToTheRight, false);
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
    if (DEBUG) logger.info("doing unload of play ------------------> ");
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
    playButton = makePlayButton(this);

//    add();

    if (false) {
      warnNoFlash.setVisible(false);
      add(warnNoFlash);
    }

    if (optionalToTheRight != null) {
      optionalToTheRight.addStyleName("floatLeft");
      //  logger.info("adding " + optionalToTheRight.getElement().getExID() + " to " + getElement().getExID());
      add(optionalToTheRight);
    } else {
      // logger.info("NOT adding right optional thing  to " + getElement().getExID());
    }
  }

  /**
   * @return
   * @see PlayAudioPanel#addButtons
   * @param toAddTo
   */
  protected IconAnchor makePlayButton(DivWidget toAddTo) {
    Button playButton = new Button(playLabel);

    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });

    showPlayIcon(playButton);
    stylePlayButton(playButton);
    toAddTo.add(playButton);
    return playButton;
  }

  protected void stylePlayButton(Button playButton) {
    playButton.setType(ButtonType.INFO);
    playButton.getElement().setId("PlayAudioPanel_playButton");
    playButton.addStyleName("leftFiveMargin");
    playButton.addStyleName("floatLeft");
    playButton.setEnabled(false);
  }

  protected void showPlayIcon(IconAnchor playButton) {
    if (isSlow) {
      playButton.setBaseIcon(MyCustomIconType.turtle);
      styleSlowIcon(playButton);
    } else {
      playButton.setIcon(PLAY);
    }
  }

  private void styleSlowIcon(Widget playButton) {
    Style style = playButton.getElement().getStyle();
    style.setPaddingBottom(3, Style.Unit.PX);
    style.setPaddingLeft(6, Style.Unit.PX);
    style.setPaddingRight(6, Style.Unit.PX);
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
//      logger.info("setting min width " + minWidth + " on " + playButton.getElement().getExID());
      if (minWidth == 12) offsetWidth = 12;
      playButton.getElement().getStyle().setProperty("minWidth", offsetWidth + "px");
    }
  }

  /**
   * @see #addButtons(Widget)
   */
  protected void doClick() {
    if (playButton.isVisible() && isEnabled()) {
      if (isPlaying()) {
        pause();  // somehow get exception here?
      } else {

        startPlaying();
      }
    }
  }

  protected void startPlaying() {
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
  public void addListener(AudioControl listener) {
    this.listener = listener;
  }

  private void addSimpleListener(SimpleAudioListener listener) {
    this.simpleAudioListener = listener;
  }

  /**
   * @see #doClick()
   */
  private void play() {
    if (DEBUG) logger.info("PlayAudioPanel :play " + playing);
    playing = true;
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  private void setPlayButtonText() {
    boolean playing1 = isPlaying();
    String html = playing1 ? pauseLabel : playLabel;
    // logger.info("setPlayButtonText now playing = " + isPlaying());
    playButton.setText(html);
    if (playing1) {
      playButton.setIcon(IconType.PAUSE);

      playButton.getElement().getStyle().setPaddingBottom(4, Style.Unit.PX);
      playButton.getElement().getStyle().setPaddingLeft(12, Style.Unit.PX);
      playButton.getElement().getStyle().setPaddingRight(12, Style.Unit.PX);
    } else {
      showPlayIcon(playButton);
    }
    //  playButton.setIcon(playing1 ? IconType.PAUSE : PLAY);
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
    if (listener != null) {
      listener.update(position);
    }
  }

  /**
   * @param path
   * @see mitll.langtest.client.custom.exercise.CommentNPFExercise#getShowGroup
   */
  public void playAudio(String path) {
    if (currentPath.equals(path)) {
      doClick();
    } else {
//      logger.info("playAudio - " + path);
      loadAudio(path);

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

  public void loadAudio(String path) {
    path = CompressedAudio.getPath(path);
    if (isPlaying()) pause();
    startSong(path);
    this.currentPath = path;
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
   * Does repeat audio if childCount > 0
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

  /**
   * @return
   * @see mitll.langtest.client.exercise.RecordAudioPanel#getPlayButton()
   */
  public Widget getPlayButton() {
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
