package mitll.langtest.client.sound;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
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
  private Sound currentSound;
  private SoundManagerAPI soundManager;
  private final Button playButton = new Button("\u25ba play");
  private final Button pauseButton = new Button("<b>||</b> pause");
  private AudioControl listener;

  public PlayAudioPanel(SoundManagerAPI soundManager) {
    this.soundManager = soundManager;
    setSpacing(5);
    addButtons();
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

  public void addListener(AudioControl listener) {this.listener = listener;}

  private void play() {
    pauseButton.setEnabled(true);
    playButton.setEnabled(false);

    soundManager.play(currentSound);
  }

  private void pause() {
    pauseButton.setEnabled(false);
    playButton.setEnabled(true);

    soundManager.pause(currentSound);
  }

  public void update(double position){
    if (listener != null) {
      listener.update(position);
    }
  }

  /**
   * @see
   * @param path
   */
  public void startSong(String path){
    createSound(path);
    pauseButton.setEnabled(false);
    playButton.setEnabled(true);
  }

  private void createSound(String song){
    currentSound = new Sound(this);
    soundManager.createSound(currentSound, song, song);
  }

  public void reinitialize(){
    pause();
    update(0);
    soundManager.setPosition(currentSound, 0);

    if (listener != null) {
      //System.out.println("reinitialize " );
      listener.reinitialize();
    }
  }

  public void songFirstLoaded(double durationEstimate){
    playButton.setEnabled(true);
    pauseButton.setEnabled(false);
    if (listener != null) {
      listener.songFirstLoaded(durationEstimate);
    }
  }

  public void songLoaded(double duration){
    if (listener != null) {
      listener.songLoaded(duration);
    }
  }
}


