package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
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
  private static final int QUIET_BETWEEN_REPEATS = 300;
  // anything longer than this gets the long audio number of repeats
  private static final float LONG_AUDIO_THRESHOLD = 1f;
  private static final String PLAY_LABEL = "\u25ba play";
  private static final String PAUSE_LABEL = "|| pause";
//  public static final int SPACE_BAR = 32;
  private Sound currentSound = null;
  private SoundManagerAPI soundManager;
  private final Button playButton = new Button(PLAY_LABEL);
  private final HTML warnNoFlash = new HTML("<font color='red'>Flash is not activated. Do you have a flashblocker? " +
    "Please add this site to its whitelist.</font>");
  private AudioControl listener;
 // private HandlerRegistration keyHandler;
  private PlayListener playListener;
 // private boolean hasFocus;
  private static int counter = 0;
  private int id;
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
   * @see mitll.langtest.client.exercise.WaveformExercisePanel.RecordAudioPanel#makePlayAudioPanel(com.google.gwt.user.client.ui.Widget)
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
    super.onUnload();

    doPause();
    destroySound();
    if (listener != null) listener.reinitialize();    // remove playing line, if it's there
    if (DEBUG) System.out.println("doing unload of play ------------------> ");

    //if (keyHandler != null) keyHandler.removeHandler();
  }

  protected void addButtons() {
    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        doClick();
      }
    });
/*
    playButton.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        hasFocus = true;
      }
    });
    playButton.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        hasFocus = false;
      }
    });*/
    playButton.setType(ButtonType.INFO);
    playButton.getElement().setId("playButton");
    playButton.addStyleName("leftFiveMargin");

    // addKeyboardListener();
/*    if (keyHandler != null) {
      playButton.setTitle("Press the space bar to play/stop playing audio.");
    }*/
    playButton.setVisible(false);
    add(playButton);
    warnNoFlash.setVisible(false);
    add(warnNoFlash);
  }

/*  protected void addKeyboardListener() {
    keyHandler = Event.addNativePreviewHandler(new
                                                   Event.NativePreviewHandler() {

                                                     @Override
                                                     public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                       NativeEvent ne = event.getNativeEvent();
                                                       if (ne.getCharCode() == SPACE_BAR &&
                                                           "[object KeyboardEvent]".equals(ne.getString()) &&
                                                           !hasFocus && playButton.isVisible()) {
                                                         ne.preventDefault();

                                                         System.out.println(new Date() + " : Play click handler : Got " + event + " type int " +
                                                             event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                             " native " + event.getNativeEvent() + " source " + event.getSource());
                                                         doClick();
                                                       }
                                                     }
                                                   });
  }*/

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

  public void setPlayEnabled(boolean val) {
    playButton.setEnabled(val);
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons(com.google.gwt.user.client.ui.Widget)
   * @param listener
   */
  public void addListener(AudioControl listener) {
    this.listener = listener;
  }

  /**
   * @see #doClick()
   */
  private void play() {
    if (DEBUG) System.out.println("PlayAudioPanel :play");

    count = 0;
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  private void setPlayButtonText() {
    String html = isPlaying() ? PLAY_LABEL : PAUSE_LABEL ;
    playButton.setText(html);
  }

  private boolean isPlaying() {
    boolean isPlaying = playButton.getText().trim().equals(PAUSE_LABEL);
   // if (!isPlaying && DEBUG) System.out.println(new Date() + " isPlaying : " + playButton.getText() + " vs " + PAUSE_LABEL);
    return isPlaying;
  }

  private void setPlayLabel() {
    if (count == 0) {
      if (DEBUG) System.out.println(new Date() + " setPlayLabel");

      playButton.setText(PLAY_LABEL);
      if (playListener != null) {
        playListener.playStopped();
      }
    }
  }

  // --- playing audio ---

  private int count = 0;
  private float start, end;

  /**
   * Repeat the segment 2 or 3 times, depending on length
   * @see mitll.langtest.client.scoring.AudioPanel#playSegment
   * @param start
   * @param end
   * @param numRepeats
   */
  public void repeatSegment(float start, float end, int numRepeats) {
    setPlayButtonText();
    int times = (end - start > LONG_AUDIO_THRESHOLD) ? numRepeats-1 : numRepeats;
    times = Math.max(0,times);
    playSegment(start,end, times);
  }

  private void playSegment(float start, float end, int times) {
    count = times;
    this.start = start;
    this.end = end;

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

    count = 0;

    setPlayLabel();
    soundManager.pause(currentSound);
  }

  public void update(double position){
    if (listener != null) {
      listener.update(position);
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
    if (DEBUG) System.out.println("PlayAudioPanel.createSound : " + this + " created sound " + currentSound);

    soundManager.createSound(currentSound, song, song);
  }

  private void destroySound() {
    if (currentSound != null) {
      this.soundManager.destroySound(currentSound);
    }
  }

  /**
   * Does repeat audio if count > 0
   */
  public void reinitialize(){
    if (count == 0) setPlayLabel();
    update(0);
    soundManager.setPosition(currentSound, 0);

    if (listener != null) {
      listener.reinitialize();
    }

    if (count > 0) {
      Timer t = new Timer() {
        public void run() {
          playSegment(start, end, --count);
        }
      };

      t.schedule(QUIET_BETWEEN_REPEATS);
    }
  }

  public void songFirstLoaded(double durationEstimate){
    if (!playButton.isEnabled()) setPlayEnabled(true);
    playButton.setVisible(true);
    if (listener != null) {
      listener.songFirstLoaded(durationEstimate);
    }
  }

  /**
   * @see SoundManager#songLoaded(Sound, double)
   * @param duration
   */
  public void songLoaded(double duration){
    if (listener != null) {
      listener.songLoaded(duration);
    }
    else {
      System.out.println("no listener for song loaded " + duration);
    }
  }

 // public Button getPlayButton() { return playButton;  }

  public String toString() { return "PlayAudioPanel #" +id; }
}


