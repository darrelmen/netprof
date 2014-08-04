package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.AudioTag;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.PlayAudioPanel;
import mitll.langtest.client.sound.PlayListener;
import mitll.langtest.client.sound.SoundManagerAPI;
import mitll.langtest.shared.ImageResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Does audio playback and fetches and shows various audio images (waveform, spectrogram, etc.) with a red line
 * indicating audio position during playback.<br></br>
 *
 * Subclasses include the capability to record audio and post it to the server.<br></br>
 *
 * On window resize, requests new images compatible with current window size.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class AudioPanel extends VerticalPanel implements RequiresResize {
  static final int MIN_WIDTH = 256;
  private static final float WAVEFORM_HEIGHT = 80f;//96;
  private static final float SPECTROGRAM_HEIGHT = 50f;//96;
  private static final String WAVEFORM = "Waveform";
  private static final String SPECTROGRAM = "Spectrogram";
  private static final String WAVEFORM_TOOLTIP = "The waveform should only be used to determine when periods of silence" +
    " and speech occur, or whether the mic is working properly.";
  private static final String WAV = ".wav";
  private static final String MP3 = "." + AudioTag.COMPRESSED_TYPE;

  private static final boolean WARN_ABOUT_MISSING_AUDIO = false;
  private static final int WINDOW_SIZE_CHANGE_THRESHOLD = 50;
  private static final int IMAGE_WIDTH_SLOP = 70 + WINDOW_SIZE_CHANGE_THRESHOLD/2;

  private final ScoreListener gaugePanel;
  protected final String exerciseID;
  protected String audioPath;
  private final Map<String,Integer> reqs = new HashMap<String, Integer>();
  private int reqid;

  private ImageAndCheck waveform;
  private ImageAndCheck spectrogram;
  ImageAndCheck words;
  ImageAndCheck phones;

  private int lastWidth = 0;
  protected AudioPositionPopup audioPositionPopup;
  protected final LangTestDatabaseAsync service;
  protected final SoundManagerAPI soundManager;
  private PlayAudioPanel playAudio;
  private float screenPortion = 1.0f;
  private final boolean logMessages;
  protected final ExerciseController controller;
  private boolean showSpectrogram = false;
  private final int rightMargin;
  private static final boolean debug = false;
  private static final boolean DEBUG_GET_IMAGES = false;

  /**
   * @see ScoringAudioPanel#ScoringAudioPanel(String, String, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, boolean, ScoreListener, int, String, String)
   * @param service
   * @param showSpectrogram
   * @param gaugePanel
   * @param rightMargin
   * @param playButtonSuffix
   * @param exerciseID
   */
  AudioPanel(String path, LangTestDatabaseAsync service,
             ExerciseController controller, boolean showSpectrogram, ScoreListener gaugePanel, int rightMargin,
             String playButtonSuffix, String audioType, String exerciseID) {
    this(service, controller, showSpectrogram, gaugePanel, 1.0f, rightMargin, exerciseID);
    this.audioPath = path;

    addWidgets(playButtonSuffix, audioType, "Record");
    if (playAudio != null) {
      controller.register(getPlayButton(), exerciseID);
    }
  }

  public Button getPlayButton() {return playAudio.getPlayButton();}

  protected AudioPanel(LangTestDatabaseAsync service,
                       ExerciseController controller, boolean showSpectrogram, ScoreListener gaugePanel,
                       float screenPortion, int rightMargin, String exerciseID) {
    this.screenPortion = screenPortion;
    this.soundManager = controller.getSoundManager();
    this.service = service;
    this.logMessages = controller.isLogClientMessages();
    this.controller = controller;
    this.gaugePanel = gaugePanel;
    if (debug) System.out.println("AudioPanel : gauge panel " + gaugePanel);
    this.showSpectrogram = showSpectrogram;
    this.rightMargin = rightMargin;
    this.exerciseID = exerciseID;
    getElement().setId("AudioPanel_exercise_"+exerciseID);
  }

  public void onResize() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        getImages();
      }
    });
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   * @see mitll.langtest.client.exercise.RecordAudioPanel#RecordAudioPanel(mitll.langtest.shared.CommonExercise, mitll.langtest.client.exercise.ExerciseController, com.google.gwt.user.client.ui.Panel, mitll.langtest.client.LangTestDatabaseAsync, int, boolean, String)
   * @see mitll.langtest.client.scoring.AudioPanel#AudioPanel
   * @return
   * @param playButtonSuffix
   * @param recordButtonTitle
   */
  protected void addWidgets(String playButtonSuffix, String audioType, String recordButtonTitle) {
    //System.out.println("AudioPanel.addWidgets " + audioType + " title " + recordButtonTitle + " suffix = " + playButtonSuffix);
    DivWidget divWithRelativePosition = new DivWidget();  // need this for audio position div to work properly
    divWithRelativePosition.getElement().getStyle().setPosition(Style.Position.RELATIVE);
    Panel imageContainer = new VerticalPanel();
    divWithRelativePosition.add(imageContainer);
    imageContainer.getElement().setId("AudioPanel_imageContainer");

    HorizontalPanel hp = new HorizontalPanel();
    hp.setVerticalAlignment(ALIGN_MIDDLE);
    hp.getElement().setId("AudioPanel_hp");

    // add widgets to left of play button
    Widget toTheRightWidget = getAfterPlayWidget();
    audioPositionPopup = new AudioPositionPopup(imageContainer);
    imageContainer.add(audioPositionPopup);

    if (hasAudio()) {
      playAudio = getPlayButtons(toTheRightWidget, playButtonSuffix, audioType, recordButtonTitle);
      hp.add(playAudio);
      hp.setCellHorizontalAlignment(playAudio, HorizontalPanel.ALIGN_LEFT);
    }
    else {
      hp.add(toTheRightWidget);
      audioPositionPopup.setVisible(false);
    }

    waveform = new ImageAndCheck();
    imageContainer.add(getWaveform().image);
    //if (ADD_CHECKBOX) controlPanel.add(addCheckbox(WAVEFORM, getWaveform()));

    getWaveform().image.setAltText(WAVEFORM_TOOLTIP);
    getWaveform().image.setTitle(WAVEFORM_TOOLTIP);

    spectrogram = new ImageAndCheck();
    if (showSpectrogram) {
      imageContainer.add(getSpectrogram().image);
      //if (ADD_CHECKBOX) controlPanel.add(addCheckbox(SPECTROGRAM, getSpectrogram()));
    }

    words = new ImageAndCheck();
    imageContainer.add(words.image);
    words.image.getElement().setId("Transcript_Words");
    //if (ADD_CHECKBOX) controlPanel.add(addCheckbox("words", words));

    phones = new ImageAndCheck();
    imageContainer.add(phones.image);
    phones.image.getElement().setId("Transcript_Phones");
    //if (ADD_CHECKBOX) controlPanel.add(addCheckbox("phones", phones));

    hp.setWidth("100%");

    add(hp);
    hp.addStyleName("bottomFiveMargin");

    add(divWithRelativePosition);
  }

  boolean hasAudio() {  return true;  }

  public boolean isAudioPathSet() { return audioPath != null; }
  public void doPause() { if (playAudio != null) playAudio.doPause(); }

  /**
   * This is sort of a hack --
   * @return
   */
  protected Widget getAfterPlayWidget() { return null;  }

  @Override
  public void onLoad() {
    if (debug) System.out.println("onLoad : id=" + getElement().getId() + " audio path is " + audioPath);
    if (audioPath != null) {
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        public void execute() {
          getImagesForPath(audioPath);
        }
      });
    }
  }

  @Override
  protected void onUnload() {
    audioPositionPopup.reinitialize();
  }

  public void setScreenPortion(float screenPortion) {
    if (debug) System.out.println("AudioPanel.setScreenPortion : screenPortion " + screenPortion);
    this.screenPortion = screenPortion;
  }

  public ImageAndCheck getWaveform() {
    return waveform;
  }
  public ImageAndCheck getSpectrogram() {
    return spectrogram;
  }

  public void addPlayListener(PlayListener playListener) {
    if (playAudio != null) {
      playAudio.addPlayListener(playListener);
    }
  }

  public static class ImageAndCheck {
    final Image image;
    //private Widget check;
    public ImageAndCheck() {
      image = new Image();
      image.setVisible(false);
    }

    public void setVisible(boolean visible) {
      image.setVisible(visible);
      //if (check != null) check.setVisible(visible);
    }

    public void setUrl(String url) { image.setUrl(url); }

/*    public Widget getCheck() {
      return check;
    }
    public void setCheck(Widget check) {
      this.check = check;
    }*/
  }

  /**
   * Ask the server for the URLs to the mp3 for this audio
   * @see #onLoad()
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.FastAndSlowASRScoringAudioPanel#showAudio(mitll.langtest.shared.AudioAttribute)
   * @param path to audio on the server
   */
  public String getImagesForPath(String path) {
    path = wavToMP3(path);
    path = ensureForwardSlashes(path);
    if (debug) System.out.println("AudioPanel : " + getElement().getId()+ " getImagesForPath " +path);
    if (path != null) {
      this.audioPath = path;
    }
    lastWidth = 0;
    if (playAudio != null) {
      playAudio.startSong(path);
    }

    // wait for rest of page to do layout before asking for the images
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        getImages();
      }
    });

    return path;
  }


  private String ensureForwardSlashes(String wavPath) {
    return wavPath.replaceAll("\\\\", "/");
  }

  private String wavToMP3(String path) {
    return (path.endsWith(WAV)) ? path.replace(WAV, MP3) : path;
  }

  /**
   * Note this is currently not very accurate with soundmanager2.
   *
   * @see ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   * @param start from here
   * @param end to here
   * @paramx waveDurInSeconds
   */
  void playSegment(float start, float end) {
    if (start >= end) {
      System.err.println("bad segment " + start + "-" + end);
    }
    else {
      //System.out.println("playSegment segment " + start + "-" + end);
      playAudio.repeatSegment(start,end);
    }
  }

  /**
   * Adds a listener to the audio panel - this way it can update its position as the audio plays
   *
   * @return PlayAudioPanel
   * @see #addWidgets(String, String, String)
   */
  private PlayAudioPanel getPlayButtons(Widget toTheRightWidget, String playButtonSuffix, String audioType, String recordButtonTitle) {
    PlayAudioPanel playAudio = makePlayAudioPanel(toTheRightWidget, playButtonSuffix, audioType, recordButtonTitle);
    playAudio.addListener(audioPositionPopup);
    return playAudio;
  }

  protected PlayAudioPanel makePlayAudioPanel(final Widget toTheRightWidget, String buttonTitle, String audioType,
                                              String recordButtonTitle) {
    return new PlayAudioPanel(soundManager, buttonTitle, toTheRightWidget);
  }

  /**
   * Ask the server for images of the waveform, spectrogram, and transcripts.
   * <p/>
   * Complicated -- could be improved -- tries to ask for an image of that would fit horizontally.
   * <p/>
   * TODO uses the window width to determine the image width -- ideally we could find out what the width
   * of this component is and then use that.  getOffsetWidth doesn't work well, although perhaps using a parent
   * component's getOffsetWidth would.
   *
   * @see #getImagesForPath(String)
   * @see #onResize()
   */
  private void getImages() {
    int leftColumnWidth1 = controller.getLeftColumnWidth();
    int leftColumnWidth =  Math.max(225, leftColumnWidth1) + IMAGE_WIDTH_SLOP;
    int rightSide = gaugePanel != null ? gaugePanel.getOffsetWidth() : rightMargin;
    if (gaugePanel != null && rightSide == 0 /*&& !controller.getProps().isNoModel()*/) {
      rightSide = 180; // TODO : hack!!!
    }
    int width = (int) ((screenPortion * ((float) Window.getClientWidth())) - leftColumnWidth) - rightSide;
    int i = width / 5;
    width = (i-1) *5;
    width -= 4;
    if (DEBUG_GET_IMAGES) {
      System.out.println("AudioPanel.getImages : leftColumnWidth " + leftColumnWidth + "(" +leftColumnWidth1+
        ") width " + width + " (screen portion = " + screenPortion +
        ") vs window width " + Window.getClientWidth() + " right side " + rightSide);
    }

    int diff = Math.abs(Window.getClientWidth() - lastWidth);
    if (lastWidth == 0 || diff > WINDOW_SIZE_CHANGE_THRESHOLD) {
      lastWidth = Window.getClientWidth();

      if (DEBUG_GET_IMAGES) {
        System.out.println("\t---> AudioPanel.getImages : offset width " + getOffsetWidth() +
          " request width " + width + " path " + audioPath);
      }
      getEachImage(width);
    } else {
      if (DEBUG_GET_IMAGES) System.out.println("\tAudioPanel.getImages : not updating, offset width " +
        getOffsetWidth() + " width " + width + " path " + audioPath + " diff " + diff + " last " + lastWidth);
    }
  }

  /**
   * Get images of all types.
   * @param width
   */
  protected void getEachImage(int width) {
    System.out.println("AudioPanel.getEachImage : " + getElement().getId()+ " path " + audioPath);
    getImageURLForAudio(audioPath, WAVEFORM, width, getWaveform());
    if (showSpectrogram) {
      getImageURLForAudio(audioPath, SPECTROGRAM, width, getSpectrogram());
    }
  }

  /**
   * Worries about responses returning out of order for the same type of image request (as the window is resized)
   * and ignores all but the most recent request.
   *
   * Call to controller getImage checks if the requested image is in the cache before requesting it.
   *
   * @see mitll.langtest.client.LangTest#getImage(int, String, String, int, int, String, com.google.gwt.user.client.rpc.AsyncCallback)
   * @param path
   * @param type
   * @param width
   * @param imageAndCheck
   */
  private void getImageURLForAudio(final String path, final String type, int width, final ImageAndCheck imageAndCheck) {
    final int toUse = Math.max(MIN_WIDTH, width);
    float heightForType = type.equals(WAVEFORM) ? getWaveformHeight() : SPECTROGRAM_HEIGHT;
    int height = Math.max(10, (int) (((float) Window.getClientHeight()) / 1200f * heightForType));
    if (path != null) {
      int reqid = getReqID(type);
      final long then = System.currentTimeMillis();

//      System.out.println("getImageURLForAudio : req " + reqid + " path " + path + " type " + type + " width " + width);
      controller.getImage(reqid, path, type, toUse, height, exerciseID, new AsyncCallback<ImageResponse>() {
        public void onFailure(Throwable caught) {
          long now = System.currentTimeMillis();
          System.out.println("getImageURLForAudio : (failure) took " + (now - then) + " millis");
          if (!caught.getMessage().trim().equals("0")) {
            // Window.alert("getImageForAudioFile Couldn't contact server. Please check network connection.");
            controller.logMessageOnServer("getImageFailed for " + path + " " + type + " width" + toUse, "onFailure");
          }
          System.out.println("message " + caught.getMessage() + " " + caught);
        }

        public void onSuccess(ImageResponse result) {
          long now = System.currentTimeMillis();
          long roundtrip = now - then;

          if (!result.successful) {
            System.err.println("got error for request for type " + type);
            if (WARN_ABOUT_MISSING_AUDIO) Window.alert("missing audio file on server " + path);
          } else if (result.req == -1 || isMostRecentRequest(type, result.req)) { // could be cached
            showResult(result, imageAndCheck);
          } else {
            System.out.println("\tgetImageURLForAudio : ignoring out of sync response " + result.req + " for " + type);
          }
          if (logMessages) {
            logMessage(result, roundtrip);
          }
        }
      });
    }
  }

  protected float getWaveformHeight() {
    return WAVEFORM_HEIGHT;
  }

  private void showResult(ImageResponse result, final ImageAndCheck imageAndCheck) {
   // System.out.println("\tgetImageURLForAudio : using new url " + result.imageURL);
    imageAndCheck.image.setUrl(result.imageURL);
    imageAndCheck.image.setVisible(true);
    audioPositionPopup.reinitialize(result.durationInSeconds);

/*    if (ADD_CHECKBOX) {
      // attempt to have the check not become visible until the image comes back from the server
      Timer t = new Timer() {
        public void run() {
          imageAndCheck.getCheck().setVisible(true);
        }
      };

      t.schedule(50);
    }*/
  }

  private void logMessage(ImageResponse result, long roundtrip) {
    String message = "getImageURLForAudio : (success) " + result + " took " + roundtrip + " millis, audio dur " +
      (result.durationInSeconds * 1000f) + " millis, " +
      " " + ((float) roundtrip / (float) (result.durationInSeconds * 1000f)) + " roundtrip/audio duration ratio.";
    service.logMessage(message, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  int getReqID(String type) {
    synchronized (this) {
      int current = reqid++;
      reqs.put(type, current);
      return current;
    }
  }

  boolean isMostRecentRequest(String type, int responseReqID) {
    synchronized (this) {
      Integer mostRecentIDForType = reqs.get(type);
      if (mostRecentIDForType == null) {
        System.err.println("huh? couldn't find req " + reqid + " in " +reqs);
        return false;
      }
      else {
        //System.out.println("\tisMostRecentRequest: req for " + type + " = " + mostRecentIDForType + " compared to " + responseReqID);
        return mostRecentIDForType == responseReqID;
      }
    }
  }

  /**
   * @see #addWidgets(String, String, String)
   * @param label
   * @param widget
   * @return
   */
/*  private Panel addCheckbox(final String label, final ImageAndCheck widget) {
    final CheckBox checkBox = new CheckBox();
    checkBox.getElement().setId("CheckBox_for_"+label);
    checkBox.setValue(true);
    checkBox.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        widget.image.setVisible(event.getValue());
        audioPositionPopup.setHeightFromContainer();
        controller.logEvent(checkBox, "CheckBox", exerciseID, "CheckBox_for_" + label);
      }
    });
    Panel p = new HorizontalPanel();
    p.add(checkBox);
    p.add(new Label(label));
    p.addStyleName("buttonMargin");
    p.setVisible(false);
    //widget.setCheck(p);
    return p;
  }*/
}
