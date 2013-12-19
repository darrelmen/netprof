package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;

import java.util.Date;

/**
 * A play button, and an interface with the SoundManagerAPI to call off into the soundmanager.js
 * package to play, pause, and track the progress of audio.<br></br>
 *
 * Tells an option AudioControl listener about the state transitions and current playing audio location.<br></br>
 *
 * Warns if a flash blocker is preventing flash from loading, preventing soundmanager2 from loading.<br></br>
 * There is an option to use HTML5 Audio but that seems to have playback issues when clicking on words or phonemes.
 *
 * User: go22670
 * Date: 8/30/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayAudioPanel extends HorizontalPanel implements AudioControl {
  private static final String PLAY_LABEL = "\u25ba play";
  private static final String PAUSE_LABEL = "|| pause";
  private Sound currentSound = null;
  private final SoundManagerAPI soundManager;
  private final Button playButton = new Button(PLAY_LABEL);
  private final HTML warnNoFlash = new HTML("<font color='red'>Flash is not activated. Do you have a flashblocker? " +
    "Please add this site to its whitelist.</font>");
  private AudioControl listener;
  private PlayListener playListener;
  private static int counter = 0;
  private final int id;
  private static final boolean DEBUG = false;

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel
   * @param soundManager
   */
  public PlayAudioPanel(SoundManagerAPI soundManager) {
    this.soundManager = soundManager;
    setSpacing(10);
    setVerticalAlignment(ALIGN_MIDDLE);
    addButtons();
    id = counter++;
    getElement().setId("PlayAudioPanel_"+id);
  }

  /**
   * @see mitll.langtest.client.exercise.RecordAudioPanel.MyPlayAudioPanel#MyPlayAudioPanel(com.github.gwtbootstrap.client.ui.Image, com.github.gwtbootstrap.client.ui.Image, com.google.gwt.user.client.ui.Panel)
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.ASRRecordAudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget)
   * @param soundManager
   * @param playListener
   */
  public PlayAudioPanel(SoundManagerAPI soundManager, PlayListener playListener) {
    this(soundManager);
    this.playListener = playListener;
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

  protected void addButtons() {
    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });
    playButton.setType(ButtonType.INFO);
    playButton.getElement().setId("playButton");
    playButton.addStyleName("leftFiveMargin");
    playButton.setEnabled(false);
    add(playButton);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
  }

  private void doClick() {
    if (playButton.isVisible() && playButton.isEnabled()) {
      if (isPlaying()) {
        pause();
      }
      else {
        if (playListener != null) playListener.playStarted();
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
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons(com.google.gwt.user.client.ui.Widget)
   * @param listener
   */
  public void addListener(AudioControl listener) { this.listener = listener;  }

  /**
   * @see #doClick()
   */
  private void play() {
    if (DEBUG) System.out.println("PlayAudioPanel :play");
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  private void setPlayButtonText() {
    String html = isPlaying() ? PLAY_LABEL : PAUSE_LABEL ;
    playButton.setText(html);
  }

  private boolean isPlaying() {
    return playButton.getText().trim().equals(PAUSE_LABEL);
  }

  private void setPlayLabel() {
    if (DEBUG) System.out.println(new Date() + " setPlayLabel");

    playButton.setText(PLAY_LABEL);
    if (playListener != null) {
      playListener.playStopped();
    }
  }

  // --- playing audio ---

  /**
   * Repeat the segment 2 or 3 times, depending on length
   * @see mitll.langtest.client.scoring.AudioPanel#playSegment
   * @param start
   * @param end
   */
  public void repeatSegment(float start, float end) {
    setPlayButtonText();
    playSegment(start,end);
  }

  /**
   * Checks to see if the sound was created properly before trying to play it.
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

      soundManager.playInterval(currentSound, s, e);
    }
  }

  /**
   * @see #reinitialize()
   */
  private void pause() {
    if (DEBUG) System.out.println("PlayAudioPanel :pause");
    setPlayLabel();
    soundManager.pause(currentSound);
  }

  public void update(double position){
    if (listener != null) {
      listener.update(position);
    }
    else {
      System.out.println("PlayAudioPanel :update - no listener");
    }
  }

  /**
   * Check if soundmanager loaded properly, warn if it didn't.
   *
   * @see mitll.langtest.client.scoring.AudioPanel#getImagesForPath(String)
   * @param path to audio file on server
   */
  public void startSong(String path){
    if (DEBUG) System.out.println("PlayAudioPanel : start song : " + path);
    if (soundManager.isReady()) {
      if (DEBUG) System.out.println(new Date() + " Sound manager is ready.");
      if (soundManager.isOK()) {
        destroySound();
        createSound(path);
      } else {
        System.out.println(new Date() + " Sound manager is not OK!.");
        warnNoFlash.setVisible(true);
      }
    }
  }

  /**
   * @see #startSong(String)
   * @param song
   */
  private void createSound(String song){
    currentSound = new Sound(this);
    if (DEBUG) {
      System.out.println("PlayAudioPanel.createSound : (" + getElement().getId()+ ") for " +song +" : "+ this + " created sound " + currentSound);
    }

    String uniqueID = song + "_" + getElement().getId(); // fix bug where multiple npf panels might load the same audio file and not load the second one seemingly
    soundManager.createSound(currentSound, uniqueID, song);
  }

  /**
   *
   */
  private void destroySound() {
    if (currentSound != null) {
      System.out.println("PlayAudioPanel.destroySound : (" + getElement().getId()+ ") destroy sound " + currentSound);

      this.soundManager.destroySound(currentSound);
    }
  }

  /**
   * Does repeat audio if count > 0
   */
  public void reinitialize(){
    if (DEBUG) {
      System.out.println("PlayAudioPanel :reinitialize "+ getElement().getId());
      new Exception().printStackTrace();
    }

    setPlayLabel();
    update(0);
    soundManager.setPosition(currentSound, 0);

    if (listener != null) {
      System.out.println("PlayAudioPanel :reinitialize - telling listener to reinitialize ");

      listener.reinitialize();
    }
    else {
      System.out.println("PlayAudioPanel :reinitialize - no listener");
    }
  }

  public void songFirstLoaded(double durationEstimate){
    if (DEBUG) System.out.println("PlayAudioPanel.songFirstLoaded : " + this);

    if (listener != null) {
      listener.songFirstLoaded(durationEstimate);
    }
    else {
      System.out.println("PlayAudioPanel :songFirstLoaded - no listener");
    }
    setPlayEnabled(true);
  }

  /**
   * @see SoundManager#songLoaded(Sound, double)
   * @param duration
   */
  public void songLoaded(double duration) {
    if (DEBUG) System.out.println("PlayAudioPanel.songLoaded : " + this);

    if (listener != null) {
      listener.songLoaded(duration);
    } else {
      System.out.println("no listener for song loaded " + duration);
    }
    setPlayEnabled(true);
    reinitialize();
  }

  public void songFinished() {
    if (DEBUG) System.out.println("PlayAudioPanel :songFinished "+ getElement().getId());

    setPlayLabel();

    if (listener != null) {  // remember to delegate too
      listener.songFinished();
    }
  }

  public void setPlayEnabled(boolean val) { playButton.setEnabled(val); }

  public String toString() { return "PlayAudioPanel #" +id; }
}