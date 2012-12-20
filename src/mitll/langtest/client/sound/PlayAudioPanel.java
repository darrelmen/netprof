package mitll.langtest.client.sound;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;

import java.util.Date;

/**
 * A play button, and an interface with the SoundManagerAPI to call off into the soundmanager.js
 * package to play, pause, and track the progress of audio.<br></br>
 *
 * Tells an option AudioControl listener about the state transitions and current playing audio location.<br></br>
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
  private static final String PAUSE_LABEL = "<b>||</b> pause";
  private Sound currentSound;
  private SoundManagerAPI soundManager;
  private final Button playButton = new Button(PLAY_LABEL);
  private AudioControl listener;

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
    System.out.println("doing unload of play ------------------> ");

    logHandler.removeHandler();
  }

  protected void addButtons() {
    playButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
/*        System.out.println(new Date() + " : PlayButton : Got click " + event + " type int " +
            *//*event.getTypeInt() +*//* " assoc " + event.getAssociatedType() +
            " native " + event.getNativeEvent() + " source " + event.getSource());*/
        doClick();
      }
    });

    logHandler = Event.addNativePreviewHandler(new
                                                   Event.NativePreviewHandler() {

                                                     @Override
                                                     public void onPreviewNativeEvent(Event.NativePreviewEvent event) {
                                                       NativeEvent ne = event.getNativeEvent();

                                                    //   System.out.println("got key " + ne.getCharCode() + " string '" + ne.getString() +"'");
                                                       if (ne.getCharCode() == 32 && "[object KeyboardEvent]".equals(ne.getString())) {
                                                         ne.preventDefault();

                                                         System.out.println(new Date() + " : Play click handler : Got " + event + " type int " +
                                                             event.getTypeInt() + " assoc " + event.getAssociatedType() +
                                                             " native " + event.getNativeEvent() + " source " + event.getSource());
                                                         doClick();
                                                       }
                                                     }
                                                   });
    add(playButton);
    playButton.setVisible(false);
  }

  private HandlerRegistration logHandler;

  private void doClick() {
    if (playButton.isVisible() && playButton.isEnabled()) {
      if (isPlaying())
        pause();
      else
        play();
    }
  }

  /**
   * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons(com.google.gwt.user.client.ui.Widget)
   * @param listener
   */
  public void addListener(AudioControl listener) {
    this.listener = listener;
  }

  private void play() {
    count = 0;
    setPlayButtonText();
    soundManager.play(currentSound);
  }

  private void setPlayButtonText() {
    String html = isPlaying() ? PLAY_LABEL : PAUSE_LABEL ;
    playButton.setHTML(html);
  }

  private boolean isPlaying() {
    return playButton.getHTML().equals(PAUSE_LABEL);
  }

  private void setPlayLabel() {
    if (count == 0) playButton.setHTML(PLAY_LABEL);
  }

  // --- playing audio ---

  private int count = 0;
  private float start, end, wavDurInMillis;

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

  private void playSegment(float startInSeconds, float endInSeconds) {
    soundManager.pause(currentSound);
    float start1 = startInSeconds * 1000f;
    float end1 = endInSeconds * 1000f;
    int s = (int) start1;
    int e = (int) end1;

    soundManager.playInterval(currentSound, s, e);
  }

  /**
   * @see #reinitialize()
   */
  private void pause() {
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
   * @see mitll.langtest.client.scoring.AudioPanel#getImagesForPath(String)
   * @param path
   */
  public void startSong(String path){
    destroySound();
    createSound(path);
  }

  /**
   * @see #startSong(String)
   * @param song
   */
  private void createSound(String song){
    currentSound = new Sound(this);
    System.out.println("createSound : " + this + " created sound " + currentSound);

    soundManager.createSound(currentSound, song, song);
  }

  private void destroySound() {
    if (currentSound != null) {
      System.out.println("destroySound : " + this + " so destroying sound " + currentSound);
      this.soundManager.destroySound(currentSound);
      //currentSound = null;
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
    System.out.println("songFirstLoaded for " + this + " listener is " + listener);
    playButton.setVisible(true);
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


