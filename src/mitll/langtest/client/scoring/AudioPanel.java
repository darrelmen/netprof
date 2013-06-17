package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.Heading;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.sound.AudioControl;
import mitll.langtest.client.sound.PlayAudioPanel;
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
  protected static final int MIN_WIDTH = 256;
  private static final float WAVEFORM_HEIGHT = 80f;//96;
  private static final float SPECTROGRAM_HEIGHT = 50f;//96;
  //private static final int RIGHT_MARGIN = ASRScorePanel.X_CHART_SIZE+150;//550;//1;//400;
  protected static final String WAVEFORM = "Waveform";
  protected static final String SPECTROGRAM = "Spectrogram";
  public static final String WAVEFORM_TOOLTIP = "The waveform should only be used to determine when periods of silence" +
    " and speech occur, or whether the mic is working properly.";
  public static final int IMAGE_WIDTH_SLOP = 100;
  private final ScoreListener gaugePanel;
  protected String audioPath;
  private final Map<String,Integer> reqs = new HashMap<String, Integer>();
  private int reqid;
  private boolean useKeyboard = true;

  private ImageAndCheck waveform;
  private ImageAndCheck spectrogram;
  protected ImageAndCheck phones;
  protected ImageAndCheck words;

  private int lastWidth = 0;
  private Panel imageContainer;
  private AudioPositionPopup audioPositionPopup;
  protected final LangTestDatabaseAsync service;
  protected final SoundManagerAPI soundManager;
  private PlayAudioPanel playAudio;
  private final boolean debug = false;
  private float screenPortion = 1.0f;
  private final boolean logMessages;
  private ExerciseController controller;

  /**
   * @see mitll.langtest.client.exercise.WaveformExercisePanel.RecordAudioPanel#RecordAudioPanel(mitll.langtest.client.LangTestDatabaseAsync, int)
   * @see ScoringAudioPanel#ScoringAudioPanel
   * @param service
   * @param useKeyboard
   * @param gaugePanel
   */
  public AudioPanel(String path, LangTestDatabaseAsync service,
                    boolean useKeyboard, ExerciseController controller, ScoreListener gaugePanel) {
    this.soundManager = controller.getSoundManager();
    this.service = service;
    this.useKeyboard = useKeyboard;
    this.logMessages = controller.isLogClientMessages();
    this.controller = controller;
    this.gaugePanel = gaugePanel;
    addWidgets(path);
  }

  public void onResize() {
    getImages();
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   * @param path
   * @return
   */
  private void addWidgets(String path) {
    imageContainer = new VerticalPanel();

    HorizontalPanel hp = new HorizontalPanel();
    hp.setVerticalAlignment(ALIGN_MIDDLE);

    Widget beforePlayWidget = getBeforePlayWidget();
    playAudio = getPlayButtons(beforePlayWidget);
    hp.add(playAudio);
    hp.setCellHorizontalAlignment(playAudio,HorizontalPanel.ALIGN_LEFT);

    HorizontalPanel controlPanel = new HorizontalPanel();

    waveform = new ImageAndCheck();
    imageContainer.add(getWaveform().image);
    controlPanel.add(addCheckbox(WAVEFORM, getWaveform()));
    getWaveform().image.setAltText(WAVEFORM_TOOLTIP);
    getWaveform().image.setTitle(WAVEFORM_TOOLTIP);
    spectrogram = new ImageAndCheck();
    imageContainer.add(getSpectrogram().image);
    controlPanel.add(addCheckbox(SPECTROGRAM, getSpectrogram()));

    words = new ImageAndCheck();
    imageContainer.add(words.image);
    controlPanel.add(addCheckbox("words", words));

    phones = new ImageAndCheck();
    imageContainer.add(phones.image);
    controlPanel.add(addCheckbox("phones", phones));

    hp.add(controlPanel);
    hp.setCellHorizontalAlignment(controlPanel, HorizontalPanel.ALIGN_RIGHT);
    hp.setWidth("100%");

    add(hp);

    add(new Heading(6));

    add(imageContainer);

    this.audioPath = path;
  }

  public void doPause() { playAudio.doPause(); }

  /**
   * This is sort of a hack -- so we can get left justify...
   * @return
   */
  protected Widget getBeforePlayWidget() {
    return null;
  }

  @Override
  public void onLoad() {
    if (audioPath != null) {
      if (debug) System.out.println("onLoad : audio path is " + audioPath);
      getImagesForPath(audioPath);
    }
    else {
      if (debug) System.out.println("onLoad : for AudioPanel got no audio path?");
    }
  }

  @Override
  protected void onUnload() {
    audioPositionPopup.reinitialize();
  }

  public void setScreenPortion(float screenPortion) {
    this.screenPortion = screenPortion;
  }

  public ImageAndCheck getWaveform() {
    return waveform;
  }

  public ImageAndCheck getSpectrogram() {
    return spectrogram;
  }

  public static class ImageAndCheck {
    final Image image;
    Widget check;
    public ImageAndCheck() {
      image = new Image();
      image.setVisible(false);
    }
    public void setVisible(boolean visible) { image.setVisible(visible); }
    public void setUrl(String url) { image.setUrl(url); }
  }

  /**
   * @see #onLoad()
   * @see mitll.langtest.client.scoring.PostAudioRecordButton#stopRecording()
   * @param path
   */
  public void getImagesForPath(String path) {
    //System.out.println("AudioPanel : getImagesForPath " +path);
    if (path != null) {
      this.audioPath = path;
    }
    lastWidth = 0;
    getImages();
    playAudio.startSong(path);
  }

  /**
   * @see ScoringAudioPanel.TranscriptEventClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
   * @param start
   * @param end
   * @paramx waveDurInSeconds
   * @param numRepeats
   */
  public void playSegment(float start, float end, int numRepeats) {
    if (start >= end) {
      System.err.println("bad segment " + start + "-" + end);
    }
    else {
      playAudio.repeatSegment(start,end,numRepeats);
    }
  }

    /**
    * @see #addWidgets(String)
    * @return PlayAudioPanel
    */
  private PlayAudioPanel getPlayButtons(Widget toTheLeftWidget) {
    PlayAudioPanel playAudio = makePlayAudioPanel(toTheLeftWidget);
    audioPositionPopup = new AudioPositionPopup();
    playAudio.addListener(audioPositionPopup);
    return playAudio;
  }

  protected PlayAudioPanel makePlayAudioPanel(final Widget toTheLeftWidget) {
    return new PlayAudioPanel(soundManager) {
      @Override
      protected void addButtons() {
        if (toTheLeftWidget != null) {
          add(toTheLeftWidget);
        }
        super.addButtons();
      }

      @Override
      protected void addKeyboardListener() {
        if (useKeyboard) {
          super.addKeyboardListener();
        }
      }
    };
  }

  /**
   * Ask the server for images of the waveform, spectrogram, and transcripts.
   * <p></p>
   * TODO uses the window width to determine the image width -- ideally we could find out what the width
   * of this component is and then use that.  getOffsetWidth doesn't work well, although perhaps using a parent
   * component's getOffsetWidth would.
   *
   * @see #getImagesForPath(String)
   * @see #onResize()
   */
  private void getImages() {
    int leftColumnWidth = Math.min(175,controller.getLeftColumnWidth()) + IMAGE_WIDTH_SLOP;
    //int rightMargin = screenPortion == 1.0f ? leftColumnWidth : (int)(screenPortion*((float)rightMarginToUse));
    int rightSide = gaugePanel != null ? gaugePanel.getOffsetWidth() : 0;
    int width = (int) ((screenPortion*((float)Window.getClientWidth())) - leftColumnWidth) - rightSide;

  //  System.out.println("getImages : leftColumnWidth " + leftColumnWidth + " width " + width + " vs window width " + Window.getClientWidth());

    //int width = getOffsetWidth();
    int diff = Math.abs(Window.getClientWidth() - lastWidth);
    if (lastWidth == 0 || diff > 100) {
      lastWidth = Window.getClientWidth();

      //System.out.println("getImages : offset width " + getOffsetWidth() + " width " + width + " path " + audioPath);
      getEachImage(width);
    }
    else {
      //System.out.println("getImages : not updating, offset width " + getOffsetWidth() + " width " + width + " path " + audioPath + " diff " + diff + " last " + lastWidth);
    }
  }

  protected void getEachImage(int width) {
    getImageURLForAudio(audioPath, WAVEFORM, width, getWaveform());
    getImageURLForAudio(audioPath, SPECTROGRAM, width, getSpectrogram());
  }

  /**
   * Worries about responses returning out of order for the same type of image request (as the window is resized)
   * and ignores all but the most recent request.
   *
   * @param path
   * @param type
   * @param width
   * @param imageAndCheck
   */
  private void getImageURLForAudio(final String path, final String type,int width, final ImageAndCheck imageAndCheck) {
    int toUse = Math.max(MIN_WIDTH, width);
    float heightForType = type.equals(WAVEFORM) ? WAVEFORM_HEIGHT : SPECTROGRAM_HEIGHT;
    int height = Math.max(10,(int) (((float)Window.getClientHeight())/1200f * heightForType));
    if (path != null) {
      int reqid = getReqID(type);
      final long then = System.currentTimeMillis();

      //System.out.println("getImageURLForAudio : req " + reqid + " path " + path + " type " + type + " width " + width);
      service.getImageForAudioFile(reqid, path, type, toUse, height, new AsyncCallback<ImageResponse>() {
        public void onFailure(Throwable caught) {
          long now = System.currentTimeMillis();
          System.out.println("getImageURLForAudio : (failure) took " +(now-then) + " millis");
          Window.alert("getImageForAudioFile Couldn't contact server. Please check network connection.");
        }
        public void onSuccess(ImageResponse result) {
          long now = System.currentTimeMillis();
          long roundtrip = now - then;

          if (!result.successful) {
            System.err.println("got error for request for type " + type);
            Window.alert("missing audio file on server " + path);
          }
          else if (isMostRecentRequest(type,result.req)) {
            imageAndCheck.image.setUrl(result.imageURL);
            imageAndCheck.image.setVisible(true);
            audioPositionPopup.reinitialize(result.durationInSeconds);

            // attempt to have the check not become visible until the image comes back from the server
            Timer t = new Timer() {
              public void run() {
                imageAndCheck.check.setVisible(true);
              }
            };

            t.schedule(50);
          }
          else {
            System.out.println("getImageURLForAudio : ignoring out of sync response " + result.req + " for " + type);
          }
          if (logMessages) {
            logMessage(result, roundtrip);
          }
        }
      });
    }
    else {
     // System.out.println("getImageURLForAudio : no audio path for " + type);
    }
  }

  private void logMessage(ImageResponse result, long roundtrip) {
    String message = "getImageURLForAudio : (success) " + result + " took " + roundtrip + " millis, audio dur " +
      (result.durationInSeconds * 1000f) + " millis, " +
      " " + ((float) roundtrip / (float) (result.durationInSeconds * 1000f)) + " roundtrip/audio duration ratio.";
   // System.out.println(message);
    service.logMessage(message, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(Void result) {
      }
    });
  }

  protected int getReqID(String type) {
    synchronized (this) {
      int current = reqid++;
      reqs.put(type, current);
    //  System.out.println("req map now " + reqs);
      return current;
    }
  }

  protected boolean isMostRecentRequest(String type,int responseReqID) {
    synchronized (this) {
      Integer mostRecentIDForType = reqs.get(type);
      if (mostRecentIDForType == null) {
        System.err.println("huh? couldn't find req " + reqid + " in " +reqs);
        return false;
      }
      else {
       // System.out.println("req for " + type + " = " + mostRecentIDForType);
        return mostRecentIDForType == responseReqID;
      }
    }
  }

  private Panel addCheckbox(String label, final ImageAndCheck widget) {
    CheckBox w = new CheckBox();
    w.setValue(true);
    w.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        widget.image.setVisible(event.getValue());
        audioPositionPopup.setHeightFromContainer();
      }
    });
    Panel p = new HorizontalPanel();
    p.add(w);
    p.add(new Label(label));
    p.addStyleName("buttonMargin");
    p.setVisible(false);
    widget.check = p;
    return p;
  }

  private static int counter  = 0;

  /**
   * Makes a red line appear on top of the waveform/spectogram,etc. marking playback position.
   */
  private class AudioPositionPopup implements AudioControl {
    private int id = 0;
    private PopupPanel imageOverlay;
    private float durationInMillis;

    /**
     * @see mitll.langtest.client.scoring.AudioPanel#getPlayButtons
     */
    public AudioPositionPopup() {
      id = counter++;
      imageOverlay = new PopupPanel(false);
      imageOverlay.setStyleName("ImageOverlay");
      SimplePanel w = new SimplePanel();
      imageOverlay.add(w);
      w.setStyleName("ImageOverlay");
    }

    /**
     * @see AudioPanel#getImageURLForAudio(String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
     */
    public void reinitialize(double durationInSeconds) {
      setWavDurationInSeconds(durationInSeconds);
      reinitialize();
    }

    public void reinitialize() {
      if (debug) System.out.println(this + "  : AudioPositionPopup.reinitialize ");

      imageOverlay.hide();
      int left = imageContainer.getAbsoluteLeft();
      int top  = imageContainer.getAbsoluteTop();
      imageOverlay.setPopupPosition(left, top);
    }

    private void setWavDurationInSeconds(double durationInSeconds) {
      this.durationInMillis = (float)durationInSeconds*1000f;
    }

    public void songFirstLoaded(double durationEstimate) {}

    /**
     * @param durationInMillisIgnored
     * @see PlayAudioPanel#songLoaded(double)
     */
    public void songLoaded(final double durationInMillisIgnored) {
      int offsetHeight = imageContainer.getOffsetHeight();
      int left = imageContainer.getAbsoluteLeft();
      int top = imageContainer.getAbsoluteTop();
      if (debug) System.out.println(this + " songLoaded " + durationInMillisIgnored + " height " + offsetHeight + " x " + left +
          " y " + top);
      imageOverlay.setSize("2px", offsetHeight + "px");
      imageOverlay.setPopupPosition(left, top);
      if (debug) System.out.println("songLoaded " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
          " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim " +
          imageOverlay.getOffsetWidth() + " x " + imageOverlay.getOffsetHeight());
    }

    /**
     * @see PlayAudioPanel#update(double)
     * @param position
     */
    public void update(double position) {
      if (!imageOverlay.isShowing()) {
        imageOverlay.show();
      }
      showAt(position);
    }

    /**
     * So the story here is that the wav->mp3 conversion adds about 0.1 sec to the end of the mp3 file
     * So to get the audio positionInMillis to translate to a positionInMillis on the image, we need to use the
     * wav file duration from the server and not the mp3 duration {@link #setWavDurationInSeconds(double)}
     *
     * @param positionInMillis where soundmanager tells us the audio cursor is during playback
     */
    private void showAt(double positionInMillis) {
      setHeightFromContainer();

      float horizontalFraction = (float) positionInMillis / durationInMillis;
      if (horizontalFraction > 1f) horizontalFraction = 1f;
      int pixelProgress = (int) (((float) imageContainer.getOffsetWidth()) * horizontalFraction);
      int left = imageContainer.getAbsoluteLeft() + pixelProgress;
      int top = imageContainer.getAbsoluteTop();
      imageOverlay.setPopupPosition(left, top);

      if (debug) System.out.println(this + " showAt " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
          " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim x " +
          imageOverlay.getOffsetWidth() + " y " + imageOverlay.getOffsetHeight());
    }

    public void setHeightFromContainer() {
      int offsetHeight = imageContainer.getOffsetHeight();
      imageOverlay.setSize("2px", offsetHeight + "px");
    }

    public String toString() { return "popup #" +id; }
  }
}
