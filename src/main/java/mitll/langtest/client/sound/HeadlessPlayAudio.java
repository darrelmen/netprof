package mitll.langtest.client.sound;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.dialog.ExceptionHandlerDialog;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.exercise.PlayAudioEvent;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;

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

  /**
   *
   */
  private Sound currentSound = null;
  private SimpleAudioListener simpleAudioListener;
  private boolean playing = false;
  private static int counter = 0;

  private static final String FILE_MISSING = "FILE_MISSING";

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PLAY = false;
  private static final boolean DEBUG_DETAIL = false;

  HeadlessPlayAudio(SoundManagerAPI soundManager) {
    id = counter++;
    getElement().setId("HeadlessPlayAudio_" + id);

    this.soundManager = soundManager;
  }

  /**
   * @param soundManager
   * @param listenView
   * @see mitll.langtest.client.scoring.DialogExercisePanel#makePlayAudio
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

  private void cleanUp() {
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
   * @return true if paused
   * @see PlayAudioPanel#makePlayButton
   * @see #loadAndPlay
   */
  @Override
  public boolean doPlayPauseToggle() {
    //logger.info("PlayAudioPanel doPlayPauseToggle " + playing + " " +currentPath);

    if (hasSound()) {
      if (isPlaying()) {
        if (DEBUG_PLAY) logger.info("doPlayPauseToggle pause, is playing = " + playing + " " + currentPath);
        //markNotPlaying();
        pause();  // somehow get exception here?
        return true;
      } else {
        if (DEBUG_PLAY) logger.info("doPlayPauseToggle start, is playing = " + playing + " " + currentPath);
        startPlaying();
        return false;
      }
    } else {
      if (DEBUG_PLAY) {
        logger.info("doPlayPauseToggle no current sound, so calling loadAndPlay, is playing = " + playing + " " + currentPath);
      }

      loadAndPlay();
      return false;
    }
  }

  private boolean hasSound() {
    return currentSound != null;
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
  public void loadAndPlaySegment(float startInSeconds, float endInSeconds) {
    if (hasSound()) {
      doPlaySegment(startInSeconds, endInSeconds);
    } else {
      if (DEBUG || currentPath == null) logger.info("loadAndPlaySegment - new path " + currentPath);

      addSimpleListener(new SimpleAudioListener() {
        @Override
        public void songLoaded(double duration) {
          if (DEBUG) logger.info("loadAndPlaySegment - songLoaded " + currentPath + " this " + this);
          if (Scheduler.get() != null) {
            Scheduler.get().scheduleDeferred(() -> {
              if (DEBUG) logger.info("loadAndPlaySegment - songLoaded calling doPlaySegment with  " + currentPath);
              doPlaySegment(startInSeconds, endInSeconds);
            });
          }
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
   * @see #doPlaySegment
   */
  private void playSegment(float startInSeconds, float endInSeconds) {
    if (hasSound() & soundManager != null) {
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
    if (DEBUG_PLAY) logger.info("HeadlessPlayAudioPanel :pause");
    markNotPlaying();

    if (soundManager != null) {
      soundManager.pause(currentSound);
    }
  }

  public void update(double position) {
    // logger.info("update " +listeners.size() + " with " + position);
    listeners.forEach(audioControl -> audioControl.update(position));
  }

  protected void loadAndPlayOrPlayAudio(AudioAttribute audioAttribute) {
    rememberAudio(audioAttribute);
    loadAndPlay();
  }

  /**
   * @see mitll.langtest.client.scoring.ChoicePlayAudioPanel#configureButton2
   */
  protected void loadAndPlay() {
    if (currentPath == null) {
      logger.warning("loadAndPlay, current path is null?");

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("no path?"));
//      logger.info("logException stack " + exceptionAsString);
    } else {
      loadAndPlayOrPlayAudio(currentPath);
    }
  }

  /**
   * @param audioAttribute
   * @see mitll.langtest.client.scoring.DialogExercisePanel#makePlayAudio(ClientExercise, DivWidget)
   */
  public void rememberAudio(AudioAttribute audioAttribute) {
    this.currentAudioAttr = audioAttribute;
    if (currentAudioAttr != null) {
      rememberAudio(currentAudioAttr.getAudioRef());
    }
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
  private void loadAndPlayOrPlayAudio(String path) {
    if (currentPath.equals(path) && hasSound()) {
      if (DEBUG) logger.info("loadAndPlayOrPlayAudio - doPlayPauseToggle " + currentPath);
      doPlayPauseToggle();
    } else {
      if (DEBUG) logger.info("loadAndPlayOrPlayAudio - new path " + path);

//      String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception());
//      logger.info("logException stack " + exceptionAsString);

      addSimpleListener(new SimpleAudioListener() {
        @Override
        public void songLoaded(double duration) {
          if (DEBUG_PLAY) {
            logger.info("loadAndPlayOrPlayAudio - songLoaded " + path + " this " + this);
          }
//          Scheduler.get().scheduleDeferred(() -> doPlayPauseToggle());
          startPlaying();
        }

        // if (DEBUG) logger.info("loadAndPlayOrPlayAudio - songLoaded calling doPlayPauseToggle  " + path);
        @Override
        public void songFinished() {
          if (DEBUG) {
            logger.info("loadAndPlayOrPlayAudio - songFinished " + path + " this " + this);
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
   * @see #loadAndPlaySegment(float, float)
   * @see #loadAndPlayOrPlayAudio(String)
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
  public String rememberAudio(String path) {
     if (DEBUG_PLAY || path == null) {
       logger.info("rememberAudio - path " + path);
     }
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
      //logger.info("HeadlessPlayAudio.loadAudio - skipping " + path);
      if (DEBUG) logger.info("HeadlessPlayAudio : startSong : " + path);
      if (soundManager.isReady()) {
        //if (DEBUG) logger.info(new Date() + " Sound manager is ready.");
        if (soundManager.isOK()) {
          //  if (DEBUG)
          if (DEBUG_PLAY)
            logger.info("HeadlessPlayAudio : startSong : " + path + " destroy current sound " + currentSound);

          destroySound();
          createSound(path, doAutoload);
        } else {
          logger.warning(" Sound manager is not OK!.");
          warnNoFlash.setVisible(true);
        }
      } else {
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

    if (DEBUG_PLAY) {
      logger.info("createSound " + uniqueID +
          "" +
          ": (" + getId() + ")" +
          "\n\tfor           " + song +
          "\n\tcreated sound " + currentSound);
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
    if (hasSound()) {
      if (DEBUG_PLAY) {
        logger.info("HeadlessPlayAudio.destroySound : (" + getId() + ") destroy sound " + currentSound);

//        String exceptionAsString = ExceptionHandlerDialog.getExceptionAsString(new Exception("got destroy"));
//        logger.info("logException stack " + exceptionAsString);
      }
      this.soundManager.destroySound(currentSound);
      currentSound = null;
    }
  }

  /**
   * Does repeat audio if childCount > 0
   */
  public void reinitialize() {
    if (DEBUG_DETAIL /*|| LOCAL_TESTING*/) {
      logger.info("HeadlessPlayAudio :reinitialize " + getId());
    }

    resetAudio();

    if (DEBUG_DETAIL /*|| LOCAL_TESTING*/)
      logger.info("HeadlessPlayAudio :reinitialize - telling listener to reinitialize " + listeners);

    for (AudioControl listener : listeners) listener.reinitialize();

//    else {
//      logger.info("HeadlessPlayAudio :reinitialize - no listener");
//    }
  }

  /**
   * @see #reinitialize
   */
  protected void resetAudio() {
    // setPlayLabel();
    update(0);
    if (hasSound()) {
      soundManager.setPosition(currentSound, 0);
    }
  }

  /**
   * So this is the first message you'd get...
   *
   * @param durationEstimate
   */
  public void songFirstLoaded(double durationEstimate) {
    if (DEBUG_DETAIL) {
      logger.info("HeadlessPlayAudio.songFirstLoaded : " + this.getId());
    }

    for (AudioControl listener : listeners) listener.songFirstLoaded(durationEstimate);

//    if (listener != null && listener != this) {
//      listener.songFirstLoaded(durationEstimate);
//    } else if (listener != null) {
//      logger.info("HeadlessPlayAudio :songFirstLoaded - listener is me??? ");
//    }
    //setEnabled(true);
  }

  /**
   * @param duration
   * @see SoundManager#songLoaded(Sound, double)
   */
  public void songLoaded(double duration) {
    // if (DEBUG) logger.info("HeadlessPlayAudio.songLoaded : " + this);

//    if (listener != null) {
//      listener.songLoaded(duration);
//    }
    for (AudioControl listener : listeners) listener.songLoaded(duration);

    if (simpleAudioListener != null) {
      if (DEBUG) logger.info("HeadlessPlayAudio.songLoaded : " + this.getId());
      simpleAudioListener.songLoaded(duration);
    }
//    else {
//      logger.info("no listener for song loaded " + duration);
//    }
    // setEnabled(true);
    if (DEBUG_DETAIL) logger.info("song loaded : reinit");
    reinitialize();
  }

  /**
   * Called when the audio stops playing, also relays the message to the listener if there is one.
   */
  public void songFinished() {
    if (DEBUG) logger.info("HeadlessPlayAudio :songFinished " + getElement().getId() +
        ", tell " + listeners.size() + " listeners...");

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
    if (DEBUG_DETAIL) {
      logger.info("HeadlessPlayAudio playing now = " + isPlaying() + " path " + currentPath);
    }
    markPlaying();
    soundManager.play(currentSound);
  }

  private void markPlaying() {
    playing = true;
  }

  /**
   * @see #pause
   */
  private void markNotPlaying() {
    playing = false;
  }

  public boolean isPlaying() {
    return playing;
  }
}
