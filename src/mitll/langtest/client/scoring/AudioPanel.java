package mitll.langtest.client.scoring;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
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
import mitll.langtest.client.gauge.ASRScorePanel;
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
  private static final float WAVEFORM_HEIGHT = 128f;//96;
  private static final float SPECTROGRAM_HEIGHT = 96f;//96;
  private static final int RIGHT_MARGIN = ASRScorePanel.X_CHART_SIZE+150;//550;//1;//400;
  protected static final String WAVEFORM = "Waveform";
  protected static final String SPECTROGRAM = "Spectrogram";
  protected String audioPath;
  private final Map<String,Integer> reqs = new HashMap<String, Integer>();
  private int reqid;

  protected ImageAndCheck waveform,spectrogram,phones,words;
  /**
   * @deprecated
   */
  protected ImageAndCheck speech;
  private int lastWidth = 0;
  private PopupPanel imageOverlay;
  private double songDuration;
  private Panel imageContainer;
  private AudioPositionPopup audioPositionPopup;
  protected final LangTestDatabaseAsync service;
  protected final SoundManagerAPI soundManager;
  private PlayAudioPanel playAudio;
  private final boolean debug = false;
  private float screenPortion = 1.0f;
  private int rightMarginToUse;

  /**
   * @see GoodwaveExercisePanel#getQuestionContent(mitll.langtest.shared.Exercise)
   * @see ScoringAudioPanel#ScoringAudioPanel
   * @paramx e
   * @param service
   * @param useFullWidth
   */
  public AudioPanel(String path, LangTestDatabaseAsync service, SoundManagerAPI soundManager, boolean useFullWidth) {
    this.soundManager = soundManager;
    this.service = service;
    rightMarginToUse = useFullWidth ? RIGHT_MARGIN :  ASRScorePanel.X_CHART_SIZE+400;
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
    hp.setWidth("100%");
    hp.setSpacing(5);

    playAudio = addButtonsToButtonRow(hp);

    HorizontalPanel controlPanel = new HorizontalPanel();

    waveform = new ImageAndCheck();
    imageContainer.add(waveform.image);
    controlPanel.add(addCheckbox(WAVEFORM, waveform));

    spectrogram = new ImageAndCheck();
    imageContainer.add(spectrogram.image);
    controlPanel.add(addCheckbox(SPECTROGRAM, spectrogram));

    words = new ImageAndCheck();
    imageContainer.add(words.image);
    controlPanel.add(addCheckbox("words", words));

    phones = new ImageAndCheck();
    imageContainer.add(phones.image);
    controlPanel.add(addCheckbox("phones", phones));

    speech = new ImageAndCheck();
    imageContainer.add(speech.image);
    controlPanel.add(addCheckbox("speech", speech));

    hp.setCellHorizontalAlignment(controlPanel, HasHorizontalAlignment.ALIGN_RIGHT);
    hp.add(controlPanel);
    add(hp);

    add(imageContainer);

    this.audioPath = path;
  }

  @Override
  public void onLoad() {
    if (audioPath != null) {
      System.out.println("onLoad : audio path is " + audioPath);
      getImagesForPath(audioPath);
    }
    else {
      System.out.println("onLoad : for AudioPanel got no audio path?");
    }
  }

  public void setScreenPortion(float screenPortion) {
    this.screenPortion = screenPortion;
  }

  protected static class ImageAndCheck {
    final Image image;
    Widget check;
    public ImageAndCheck() {
      image = new Image();
      image.setVisible(false);
    }
  }

  /**
   * @see #onLoad()
   * @see mitll.langtest.client.scoring.GoodwaveExercisePanel.PostAudioRecordButton#stopRecording()
   * @param path
   */
  public void getImagesForPath(String path) {
    System.out.println("AudioPanel : getImagesForPath " +path);
    if (path != null) {
      this.audioPath = path;
    }
    lastWidth = 0;
    getImages();
    playAudio.startSong(path);
  }

  /**
   * @see #addWidgets(String)
   * @param hp
   * @return
   */
  private PlayAudioPanel addButtonsToButtonRow(HorizontalPanel hp) {
    PlayAudioPanel playAudio = makePlayAudioPanel();
    imageOverlay = new PopupPanel(false);
    imageOverlay.setStyleName("ImageOverlay");
    SimplePanel w = new SimplePanel();
    imageOverlay.add(w);
    w.setStyleName("ImageOverlay");

    audioPositionPopup = new AudioPositionPopup();
    playAudio.addListener(audioPositionPopup);

    hp.add(playAudio);
    return playAudio;
  }

  protected PlayAudioPanel makePlayAudioPanel() {
    return new PlayAudioPanel(soundManager);
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
    int rightMargin = screenPortion == 1.0f ? rightMarginToUse : (int)(screenPortion*((float)rightMarginToUse));
    int width = (int) ((screenPortion*((float)Window.getClientWidth())) - rightMargin);
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
    getImageURLForAudio(audioPath, WAVEFORM, width, waveform);
    getImageURLForAudio(audioPath, SPECTROGRAM, width, spectrogram);
  }

  /**
   * Worries about responses returning out of order for the same type of image request (as the window is resized)
   * and ignores all but the most recent request.
   *
   * @param path
   * @param type
   * @param width
   * @param waveform
   */
  private void getImageURLForAudio(String path, final String type,int width, final ImageAndCheck waveform) {
    int toUse = Math.max(MIN_WIDTH, width);
    float heightForType = type.equals(WAVEFORM) ? WAVEFORM_HEIGHT : SPECTROGRAM_HEIGHT;
    int height = (int) (((float)Window.getClientHeight())/1200f * heightForType);
    if (path != null) {
      int reqid = getReqID(type);

      System.out.println("getImageURLForAudio : req " + reqid + " path " + path + " type " + type + " width " + width);
      service.getImageForAudioFile(reqid, path, type, toUse, height, new AsyncCallback<ImageResponse>() {
        public void onFailure(Throwable caught) {}
        public void onSuccess(ImageResponse result) {
          System.out.println("getImageURLForAudio : onSuccess " + result);
          if (!result.successful) {
            System.err.println("got error for request for type " + type);
          }
          else if (isMostRecentRequest(type,result.req)) {
            waveform.image.setUrl(result.imageURL);
            waveform.image.setVisible(true);
            waveform.check.setVisible(true);
            audioPositionPopup.reinitialize();
          }
          else {
            System.out.println("getImageURLForAudio : ignoring out of sync response " + result.req + " for " + type);
          }
        }
      });
    }
    else {
      System.out.println("getImageURLForAudio : no audio path for " + type);
    }
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
      }
    });
    Panel p = new HorizontalPanel();
    p.add(w);
    p.add(new Label(label));
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
    public AudioPositionPopup() { id = counter++; }
    public String toString() { return "popup #" +id; }
    /**
     * @see AudioPanel#getImageURLForAudio(String, String, int, mitll.langtest.client.scoring.AudioPanel.ImageAndCheck)
     */
    public void reinitialize() {
      imageOverlay.hide();
      int left = imageContainer.getAbsoluteLeft();
      int top  = imageContainer.getAbsoluteTop();
      imageOverlay.setPopupPosition(left, top);
    }

    public void songFirstLoaded(double durationEstimate) {
    //  if (debug) System.out.println("songFirstLoaded " + durationEstimate);
    }

    /**
     * @param duration
     * @see PlayAudioPanel#songLoaded(double)
     */
    public void songLoaded(final double duration) {
      songDuration = duration;
      int offsetHeight = imageContainer.getOffsetHeight();
      int left = imageContainer.getAbsoluteLeft();
      int top = imageContainer.getAbsoluteTop();
      if (debug) System.out.println(this + " songLoaded " + duration + " height " + offsetHeight + " x " + left +
          " y " + top);
      imageOverlay.setSize("2px", offsetHeight + "px");
/*      if (!imageOverlay.isShowing()) {
        imageOverlay.show();
      }*/
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

    private void showAt(double position) {
      int offsetHeight = imageContainer.getOffsetHeight();
      imageOverlay.setSize("2px", offsetHeight + "px");

      int pixelProgress = (int) (((double) imageContainer.getOffsetWidth()) * (position / songDuration));
      int left = imageContainer.getAbsoluteLeft() + pixelProgress;
      int top = imageContainer.getAbsoluteTop();
      imageOverlay.setPopupPosition(left, top);

      if (debug) System.out.println(this + " showAt " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
          " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim x " +
          imageOverlay.getOffsetWidth() + " y " + imageOverlay.getOffsetHeight());
    }
  }
}
