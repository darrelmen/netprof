package mitll.langtest.client.sound;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;

import java.util.Date;

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
  private Sound currentSound;
  private SoundManagerAPI soundManager;
  private final Button playButton = new Button("\u25ba play");
  private final Button pauseButton = new Button("<b>||</b> pause");
  private AudioControl listener;
  private double durationInMillis = -1;

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#makePlayAudioPanel()
   * @param soundManager
   */
  public PlayAudioPanel(SoundManagerAPI soundManager) {
    this.soundManager = soundManager;
    setSpacing(5);
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
    if (currentSound != null) {
      //System.out.println("unloading " + this + " so destroying sound.");
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
    pauseButton.setEnabled(true);
    playButton.setEnabled(false);
    soundManager.play(currentSound);
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#playSegment(float, float, float)
   * @param start
   * @param end
   * @param wavDurInMillis
   */
  public void playSegment(float start, float end, float wavDurInMillis) {
    float scalar = (float)durationInMillis/wavDurInMillis;
    System.out.println("play from " + start + " to " + end + " scalar " + scalar);

    playSegment(start*scalar,end*scalar);
  }

  private void playSegment(float start, float end) {
    soundManager.pause(currentSound);
    pauseButton.setEnabled(true);
    playButton.setEnabled(false);
//    System.out.println("play from " + start + " to " + end);
    soundManager.playInterval(currentSound, start * 1000f, end * 1000f);
  }

  /**
   * @see #reinitialize()
   */
  private void pause() {
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
    createSound(path);
    pauseButton.setEnabled(false);
    playButton.setEnabled(true);
  }

  /**
   * @see #startSong(String)
   * @param song
   */
  private void createSound(String song){
    //System.out.println("PlayAudioPanel.createSound " + song);
    currentSound = new Sound(this);
    soundManager.createSound(currentSound, song, song);
  }

  public void reinitialize(){
    System.out.println(new Date() + " got reinitialize for " + this + " listener is " + listener);

    pause();
    update(0);
    soundManager.setPosition(currentSound, 0);

    if (listener != null) {
      //System.out.println("reinitialize for " + this + " listener is " + listener);
      listener.reinitialize();
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


