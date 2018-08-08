package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.banner.IListenView;
import mitll.langtest.client.exercise.PlayAudioEvent;
import mitll.langtest.shared.exercise.AudioAttribute;

import java.util.*;
import java.util.logging.Logger;

public class HeadlessPlayAudio extends DivWidget implements AudioControl, IPlayAudioControl {
  protected final Logger logger = Logger.getLogger("HeadlessPlayAudio");
  protected final SoundManagerAPI soundManager;
  protected final Collection<AudioControl> listeners = new HashSet<>();
  final List<PlayListener> playListeners = new ArrayList<>();
  protected final int id;
  private IListenView listenView;
  private final HTML warnNoFlash = new HTML("<font color='red'>Flash is not activated. Do you have a flashblocker? " +
      "Please add this site to its whitelist.</font>");
  /**
   * @see #rememberAudio
   */
  String currentPath = null;
  /**
   *
   */
  protected AudioAttribute currentAudioAttr = null;
  private Sound currentSound = null;
  private SimpleAudioListener simpleAudioListener;
  private boolean playing = false;
  private static int counter = 0;

  private static final String FILE_MISSING = "FILE_MISSING";

  private static final boolean DEBUG = false;

  HeadlessPlayAudio(SoundManagerAPI soundManager) {
    id = counter++;
    this.soundManager = soundManager;
  }

  /**
   * @see mitll.langtest.client.scoring.DialogExercisePanel#makePlayAudio
   * @param soundManager
   * @param listenView
   */
  public HeadlessPlayAudio(SoundManagerAPI soundManager, IListenView listenView) {
    this(soundManager);
    this.listenView = listenView;
  }

  /**
   * Remember to destroy a sound once we are done with it, otherwise SoundManager
   * will maintain references to it, listener references, etc.
   */
  @Override
  protected void onUnload() {
    if (DEBUG) logger.info("onUnload : doing unload of play ------------------> " + this.getId());
    super.onUnload();

    cleanUp();
  }

  public void cleanUp() {
    if (DEBUG) logger.info("cleanUp : doing cleanUp of play ------------------> " + this.getId());
    doPause();
    destroySound();

    for (AudioControl listener : listeners) listener.reinitialize();    // remove playing line, if it's there
  }

  /**
   * @param playListener
   * @see mitll.langtest.client.exercise.RecordAudioPanel#addPlayListener(PlayListener)
   */
  public void addPlayListener(PlayListener playListener) {
    this.playListeners.add(playListener);
  }
  public void removePlayListener(PlayListener playListener) {
    this.playListeners.remove(playListener);
  }

  /**
   * @see PlayAudioPanel#makePlayButton
   * @see #doPlay
   */
  @Override
  public void doPlayPauseToggle() {
    //logger.info("PlayAudioPanel doPlayPauseToggle " + playing + " " +currentPath);

    if (currentSound != null) {
      if (isPlaying()) {
        if (DEBUG) logger.info("doPlayPauseToggle pause " + playing + " " + currentPath);
        //markNotPlaying();
        pause();  // somehow get exception here?
      } else {
        if (DEBUG) logger.info("doPlayPauseToggle start " + playing + " " + currentPath);
        startPlaying();
      }
    } else {
      doPlay();
    }
  }

  /**
   * @see #doPlayPauseToggle
   */
  private void startPlaying() {
    markPlaying();
    // tell other widgets to pause if they are playing audio

    // TODO : probably a bad idea
    LangTest.EVENT_BUS.fireEvent(new PlayAudioEvent(id));

    // logger.info("startPlaying tell " + playListeners.size() + " listeners play started");
    playListeners.forEach(PlayListener::playStarted);

    play();
  }

  @Override
  public boolean doPause() {
    if (isPlaying()) {
      pause();
      return true;
    } else {
      return false;
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
   * @param startInSeconds
   * @param endInSeconds
   * @see mitll.langtest.client.scoring.WordTable#addClickHandler
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
          if (DEBUG) logger.info("doPlay - songLoaded " + currentPath + " this " + this);
          Scheduler.get().scheduleDeferred(() -> {
            if (DEBUG) logger.info("doPlay - songLoaded calling doPlayPauseToggle  " + currentPath);
            doPlaySegment(startInSeconds, endInSeconds);
          });
        }

        @Override
        public void songFinished() {
        }
      });

      loadAudio(currentPath);
    }
  }

  protected void doPlaySegment(float startInSeconds, float endInSeconds) {
    markPlaying();
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
  protected void pause() {
    if (DEBUG) logger.info("PlayAudioPanel :pause");
    markNotPlaying();
    //setPlayLabel();

    if (soundManager != null) {
      soundManager.pause(currentSound);
    }
  }

  public void update(double position) {
    // logger.info("update " +listeners.size() + " with " + position);
    listeners.forEach(audioControl -> audioControl.update(position));
  }

  protected void playAudio(AudioAttribute audioAttribute) {
    rememberAudio(audioAttribute);
    doPlay();
    // playAudio(audioAttribute.getAudioRef());
  }

  /**
   * @see mitll.langtest.client.scoring.ChoicePlayAudioPanel#configureButton2
   */
  protected void doPlay() {
    if (currentPath == null) {
      logger.warning("doPlay, current path is null?");
    } else {
      playAudio(currentPath);
    }
  }

  public void rememberAudio(AudioAttribute audioAttribute) {
    this.currentAudioAttr = audioAttribute;
    rememberAudio(currentAudioAttr.getAudioRef());
  }

  public AudioAttribute getCurrentAudioAttr() {
    return currentAudioAttr;
  }

  public Collection<Integer> getAllAudioIDs() {
    return currentAudioAttr == null ?
        Collections.emptySet() : Collections.singleton(currentAudioAttr.getUniqueID());
  }

  public Set<AudioAttribute> getAllPossible() {
    return currentAudioAttr == null ?
        Collections.emptySet() : Collections.singleton(currentAudioAttr);
  }

  /**
   * @param path
   * @see mitll.langtest.client.scoring.ChoicePlayAudioPanel#playAndRemember
   */
  protected void playAudio(String path) {
    if (currentPath.equals(path) && currentSound != null) {
      if (DEBUG) logger.info("playAudio - doPlayPauseToggle " + currentPath);
      doPlayPauseToggle();
    } else {
      if (DEBUG) logger.info("playAudio - new path " + path);

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
//      logger.info("logException stack " + exceptionAsString);

      addSimpleListener(new SimpleAudioListener() {
        @Override
        public void songLoaded(double duration) {
          if (DEBUG) {
            logger.info("playAudio - songLoaded " + path + " this " + this);
          }
          Scheduler.get().scheduleDeferred(() -> doPlayPauseToggle());
        }

        // if (DEBUG) logger.info("playAudio - songLoaded calling doPlayPauseToggle  " + path);
        @Override
        public void songFinished() {
          if (DEBUG) {
            logger.info("playAudio - songFinished " + path + " this " + this);
          }
        }
      });

      loadAudio(path);
    }
  }

  /**
   * Remember to convert the path (which might be .wav) to a browser dependent format - IE can't do ogg, only mp3.
   *
   * @param path
   * @see #repeatSegment(float, float)
   * @see #playAudio(String)
   */
  private void loadAudio(String path) {
    if (DEBUG) logger.info("loadAudio - path " + path);

    doPause();
    String fixedPath = rememberAudio(path);
    startSong(fixedPath, true);

    if (DEBUG) logger.info("loadAudio - finished " + fixedPath);
  }


  /**
   * @param path
   * @return
   * @see mitll.langtest.client.scoring.ChoicePlayAudioPanel#addChoices
   * @see #loadAudio
   */
  protected String rememberAudio(String path) {
    //  if (DEBUG || path == null) logger.info("rememberAudio - path " + path);
    destroySound();
    this.currentPath = CompressedAudio.getPath(path);
/*    if (DEBUG && path != null && path.endsWith(".wav")) {
      logger.info("rememberAudio convert" +
          "\nfrom " + path +
          "\nto   " + currentPath);
    }*/
    return currentPath;
  }

  /**
   * TODO : add optional volume
   *
   *
   * destroy any other current sound first...
   * <p>
   * Check if soundmanager loaded properly, warn if it didn't.
   *
   * @param path       to audio file on server
   * @param doAutoload
   * @see mitll.langtest.client.scoring.AudioPanel#getReadyToPlayAudio
   * @see mitll.langtest.client.scoring.SimpleRecordAudioPanel#getReadyToPlayAudio
   * @see #loadAudio
   */
  public void startSong(String path, boolean doAutoload) {
    if (path == null) logger.warning("no path given???");
    else if (!path.equals(FILE_MISSING)) {
      //logger.info("PlayAudioPanel.loadAudio - skipping " + path);
      if (DEBUG) logger.info("PlayAudioPanel : startSong : " + path);
      if (soundManager.isReady()) {
        //if (DEBUG) logger.info(new Date() + " Sound manager is ready.");
        if (soundManager.isOK()) {
          //  if (DEBUG)
          if (DEBUG)
            logger.info("PlayAudioPanel : startSong : " + path + " destroy current sound " + currentSound);

          destroySound();
          createSound(path, doAutoload);
        } else {
          logger.warning(" Sound manager is not OK!.");
          warnNoFlash.setVisible(true);
        }
      }
      else {
        logger.warning("sound manager is not ready???");
      }
    }
  }

  /**
   * @param song
   * @param doAutoload
   * @see #startSong(String, boolean)
   */
  private void createSound(String song, boolean doAutoload) {
    currentSound = new Sound(this);
    String uniqueID = song + "_" + getElement().getId(); // fix bug where multiple npf panels might load the same audio file and not load the second one seemingly


    if (DEBUG) {
      logger.info("HeadlessPlayAudioPanel.createSound  " + uniqueID +
          "" +
          ": (" + getElement().getId() + ") for " + song + " : " + this + " created sound " + currentSound);
    }

    soundManager.createSound(currentSound, uniqueID, song, doAutoload, getVolume());
  }

  private int getVolume() {
    return listenView == null ? 100 : listenView.getVolume();
  }

  /**
   * @see #onUnload()
   * @see #startSong(String, boolean)
   */
  public void destroySound() {
    if (currentSound != null) {
      //if (DEBUG)
     logger.info("HeadlessPlayAudio.destroySound : (" + getElement().getId() + ") destroy sound " + currentSound);
      this.soundManager.destroySound(currentSound);
      currentSound = null;
    }
  }

  /**
   * Does repeat audio if childCount > 0
   */
  public void reinitialize() {
    if (DEBUG /*|| LOCAL_TESTING*/) {
      logger.info("PlayAudioPanel :reinitialize " + getElement().getId());
    }

    resetAudio();

    if (DEBUG /*|| LOCAL_TESTING*/)
      logger.info("PlayAudioPanel :reinitialize - telling listener to reinitialize " + listeners);

    for (AudioControl listener : listeners) listener.reinitialize();

//    else {
//      logger.info("PlayAudioPanel :reinitialize - no listener");
//    }
  }

  /**
   * @see #reinitialize
   */
  protected void resetAudio() {
    // setPlayLabel();
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
    //setEnabled(true);
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
    // setEnabled(true);
    if (DEBUG) logger.info("song loaded : reinit");
    reinitialize();
  }

  /**
   * Called when the audio stops playing, also relays the message to the listener if there is one.
   */
  public void songFinished() {
    if (DEBUG) logger.info("PlayAudioPanel :songFinished " + getElement().getId() + " to " + listeners.size());

    markNotPlaying();
    //setPlayLabel();

//    if (listener != null) {  // remember to delegate too
//      listener.songFinished();
//    }

    listeners.forEach(SimpleAudioListener::songFinished);
    playListeners.forEach(PlayListener::playStopped);

    if (simpleAudioListener != null) {
      simpleAudioListener.songFinished();
    }
  }

  /**
   * @see #startPlaying
   * @see #doPlayPauseToggle
   */
  protected void play() {
    if (DEBUG) {
      logger.info("PlayAudioPanel playing now = " + isPlaying() + " path " + currentPath);
    }
    markPlaying();
    soundManager.play(currentSound);
  }

  private void markPlaying() {
    playing = true;
  }

  private void markNotPlaying() {
    playing = false;
  }

  public boolean isPlaying() {
    return playing;
  }
}
