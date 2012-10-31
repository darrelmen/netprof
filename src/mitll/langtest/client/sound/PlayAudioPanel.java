package mitll.langtest.client.sound;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;

/**
 * Two buttons, and an interface with the SoundManagerAPI to call off into the soundmanager.js
 * package to play, pause, and track the progress of audio.
 *
 * Tells an option AudioControl listener about the state transitions and current playing audio location.
 *
 * User: go22670
 * Date: 8/30/12
 * Time: 11:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlayAudioPanel extends HorizontalPanel implements AudioControl {
  private static final int QUIET_BETWEEN_REPEATS = 300;
  private static final int REPEATS_SHORT_AUDIO = 2;
  private static final int REPEATS_LONG_AUDIO = REPEATS_SHORT_AUDIO-1;
  // anything longer than this gets the long audio number of repeats
  private static final float LONG_AUDIO_THRESHOLD = 1f;
  private Sound currentSound;
  private SoundManagerAPI soundManager;
  private final Button playButton = new Button("\u25ba play");
  private final Button pauseButton = new Button("<b>||</b> pause");
  private AudioControl listener;
  private double durationInMillis = -1;

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
  }

  /**
   * Remember to destroy a sound once we are done with it, otherwise SoundManager
   * will maintain references to it, listener references, etc.
   */
  @Override
  protected void onUnload() {
    super.onUnload();
    destroySound();
  }

  private void destroySound() {
    if (currentSound != null) {
      System.out.println("destroySound : " + this + " so destroying sound " + currentSound);
      this.soundManager.destroySound(currentSound);
    }
  }

  protected void addButtons() {
    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        play();
      }
    });
    playButton.setEnabled(false);
    add(playButton);

    pauseButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        pause();
      }
    });
    pauseButton.setEnabled(false);
    add(pauseButton);
  }

  public void addListener(AudioControl listener) {
  //  System.out.println("addListener for " + this + " listener is " + listener);
    this.listener = listener;
  }

  private void play() {
    count = 0;
    pauseButton.setEnabled(true);
    playButton.setEnabled(false);
    soundManager.play(currentSound);
  }

  private int count = 0;
  private float start, end, wavDurInMillis;

  /**
   * Repeat the segment 2 or 3 times, depending on length
   * @param start
   * @param end
   * @param wavDurInMillis
   */
  public void repeatSegment(float start, float end, float wavDurInMillis) {
    playSegment(start,end,wavDurInMillis,(end-start > LONG_AUDIO_THRESHOLD) ? REPEATS_LONG_AUDIO : REPEATS_SHORT_AUDIO);
  }

  private void playSegment(float start, float end, float wavDurInMillis, int times) {
    count = times;
    this.start = start;
    this.end = end;
    this.wavDurInMillis = wavDurInMillis;

    playSegment(start,end,wavDurInMillis);
  }

  /**
   * Corrects for mp3/wav audio length disparity.
   *
   * @see mitll.langtest.client.scoring.AudioPanel#playSegment(float, float, float)
   * @param start
   * @param end
   * @param wavDurInMillis
   */
  private void playSegment(float start, float end, float wavDurInMillis) {
    float scalar = (float)durationInMillis/wavDurInMillis;
    playSegment(start*scalar,end*scalar);
  }

  private void playSegment(float startInSeconds, float endInSeconds) {
    soundManager.pause(currentSound);
    pauseButton.setEnabled(true);
    playButton.setEnabled(false);
//    System.out.println("play from " + startInSeconds + " to " + endInSeconds);
    soundManager.playInterval(currentSound, startInSeconds * 1000f, endInSeconds * 1000f);
  }

  /**
   * @see #reinitialize()
   */
  private void pause() {
    count = 0;

    pauseButton.setEnabled(false);
    playButton.setEnabled(true);

    soundManager.pause(currentSound);
  }

  public void update(double position){
    if (listener != null) {
     // if (count++ < 3) System.out.println("update for " + this + " listener is " + listener);
      listener.update(position);
    }
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getImagesForPath(String)
   * @param path
   */
  public void startSong(String path){
    destroySound();
    createSound(path);
    pauseButton.setEnabled(false);
    playButton.setEnabled(true);
  }

  /**
   * @see #startSong(String)
   * @param song
   */
  private void createSound(String song){
    currentSound = new Sound(this);
    soundManager.createSound(currentSound, song, song);
  }

  /**
   * Does repeat audio if count > 0
   */
  public void reinitialize(){
    final int localCount = count;
    pause();
    update(0);
    soundManager.setPosition(currentSound, 0);

    if (listener != null) {
      listener.reinitialize();
    }

    if (localCount > 0) {
      Timer t = new Timer() {
        public void run() {
          int times = localCount-1;
          playSegment(start, end, wavDurInMillis, times);
        }
      };

      t.schedule(QUIET_BETWEEN_REPEATS);
    }
  }

  public void songFirstLoaded(double durationEstimate){
    playButton.setEnabled(true);
    pauseButton.setEnabled(false);
    if (listener != null) {
     // System.out.println("songFirstLoaded for " + this + " listener is " + listener);
      listener.songFirstLoaded(durationEstimate);
    }
  }

  /**
   * @see SoundManager#songLoaded(Sound, double)
   * @param duration
   */
  public void songLoaded(double duration){
    durationInMillis = duration;
    if (listener != null) {
      listener.songLoaded(duration);
    }
    else {
      System.out.println("no listener for song loaded " + duration);
    }
  }

  private static int counter = 0;
  private int id;
  public String toString() { return "PlayAudioPanel #" +id; }
}


