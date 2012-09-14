package mitll.langtest.client.goodwave;

import com.goodwave.client.PlayAudioPanel;
import com.goodwave.client.sound.AudioControl;
import com.goodwave.client.sound.SoundManagerAPI;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
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
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class AudioPanel extends VerticalPanel implements RequiresResize {
  private static final int MIN_WIDTH = 256;
  private static final int HEIGHT = 96;
  private static final int ANNOTATION_HEIGHT = 20;
  private static final int RIGHT_MARGIN = 500;//1;//400;
  private String audioPath;
  private ImageAndCheck waveform,spectrogram,speech,phones,words;
  private int lastWidth = 0;
  private PopupPanel imageOverlay;
  private double songDuration;
  private Panel imageContainer;
  private AudioPositionPopup audioPositionPopup;
  protected LangTestDatabaseAsync service;
  protected SoundManagerAPI soundManager;
  private PlayAudioPanel playAudio;
  private boolean debug = false;
  private String refAudio;
  private ScoreListener scoreListener;
  private float screenPortion = 1.0f;

  /**
   * @see GoodwaveExercisePanel#getQuestionContent(mitll.langtest.shared.Exercise)
   * @see GoodwaveExercisePanel.RecordAudioPanel#RecordAudioPanel(mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.exercise.ExerciseController, mitll.langtest.shared.Exercise, mitll.langtest.client.exercise.ExerciseQuestionState, int, String)
   * @paramx e
   * @param service
   * @paramx userFeedback
   * @paramx controller
   */
  public AudioPanel(String path, LangTestDatabaseAsync service, SoundManagerAPI soundManager) {
    this.soundManager = soundManager;
    this.service = service;
    addWidgets(path);
  }

  public void onResize() {
    getImages();
  }

  public void addScoreListener(ScoreListener l) { this.scoreListener = l;}

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

    playAudio = addButtonsToButtonRow(hp, path);

    HorizontalPanel controlPanel = new HorizontalPanel();

    waveform = new ImageAndCheck();
    imageContainer.add(waveform.image);
    controlPanel.add(addCheckbox("Waveform", waveform));

    spectrogram = new ImageAndCheck();
    imageContainer.add(spectrogram.image);
    controlPanel.add(addCheckbox("Spectrogram", spectrogram));

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
    if (audioPath != null) { // TODO awkward... better way?
      getImagesForPath(audioPath);
    }
    else {
      System.out.println("onLoad : for " + this + " got no audio path");
    }
  }

  public void setScreenPortion(float screenPortion) {
    this.screenPortion = screenPortion;
  }

  private static class ImageAndCheck {
    Image image;
    Widget check;
    public ImageAndCheck() {
      image = new Image();
      image.setVisible(false);
    }
  }

  public void getImagesForPath(String path) {
    if (path != null) {
      this.audioPath = path;
    }

    getImages();
    playAudio.startSong(path);
  }

  public void setRefAudio(String path) {
    this.refAudio = path;
  }

  protected PlayAudioPanel addButtonsToButtonRow(HorizontalPanel hp, String path) {
    final PlayAudioPanel playAudio = makePlayAudioPanel();
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

  private void getImages() {
    int rightMargin = screenPortion == 1.0f ? RIGHT_MARGIN : (int)(screenPortion*((float)RIGHT_MARGIN));
    int width = (int) ((screenPortion*((float)Window.getClientWidth())) - rightMargin);
    int diff = Math.abs(Window.getClientWidth() - lastWidth);
    if (lastWidth == 0 || diff > 100) {
      lastWidth = Window.getClientWidth();

      System.out.println("getImages : offset width " + getOffsetWidth() + " width " + width + " path " + audioPath);
      getImageURLForAudio(audioPath, "Waveform", width, waveform);
      getImageURLForAudio(audioPath, "Spectrogram", width, spectrogram);
      if (refAudio != null) {
        getTranscriptImageURLForAudio(audioPath,refAudio,width,words,phones,speech);
      }
    }
    else {
      //System.out.println("getImages : not updating, offset width " + getOffsetWidth() + " width " + width + " path " + audioPath + " diff " + diff + " last " + lastWidth);
    }
  }

  private void getImageURLForAudio(String path, String type,int width, final ImageAndCheck waveform) {
    int toUse = Math.max(MIN_WIDTH, width);
    int height = HEIGHT;
    if (path != null) {
      //System.out.println("asking for width " + toUse);
      service.getImageForAudioFile(path, type, toUse, height, new AsyncCallback<String>() {
        public void onFailure(Throwable caught) {
        }

        public void onSuccess(String result) {
          waveform.image.setUrl(result);
          waveform.image.setVisible(true);
          waveform.check.setVisible(true);
          audioPositionPopup.reinitialize();
        }
      });
    }
    else {
      System.out.println("no audio path for " + type);
    }
  }

  private Set<String> tested = new HashSet<String>();
  /**
   * TODO configure for multi-ref
   * TODO : add multiple ref files
   * @param path
   * @param width
   * @param wordTranscript
   * @param phoneTranscript
   * @param speechTranscript
   */
  private void getTranscriptImageURLForAudio(final String path, final String ref, int width,
                                             final ImageAndCheck wordTranscript,
                                             final ImageAndCheck phoneTranscript, final ImageAndCheck speechTranscript) {
    int toUse = Math.max(MIN_WIDTH, width);
    int height = ANNOTATION_HEIGHT;
    if (false) {
      service.getScoreForAudioFile(path, toUse, height, new AsyncCallback<PretestScore>() {
        public void onFailure(Throwable caught) {}
        public void onSuccess(PretestScore result) {
          useResult(result, wordTranscript, phoneTranscript, speechTranscript, tested.contains(path));
          tested.add(path);
        }
      });
    } else {
      Collection<String> refs = new ArrayList<String>();
      refs.add(ref);
      service.getScoreForAudioFile(path, refs, toUse, height, new AsyncCallback<PretestScore>() {
        public void onFailure(Throwable caught) {}
        public void onSuccess(PretestScore result) {
          boolean contains = tested.contains(path);
          if (contains) System.out.println("already asked to score " + path );
          useResult(result, wordTranscript, phoneTranscript, speechTranscript, contains);
          tested.add(path);
        }
      });
    }
  }

  private void useResult(PretestScore result, ImageAndCheck wordTranscript, ImageAndCheck phoneTranscript, ImageAndCheck speechTranscript, boolean scoredBefore) {
    System.out.println("useResult got " + result);
    if (result.sTypeToImage.get(NetPronImageType.WORD_TRANSCRIPT) != null) {
      wordTranscript.image.setUrl(result.sTypeToImage.get(NetPronImageType.WORD_TRANSCRIPT));
      wordTranscript.image.setVisible(true);
      wordTranscript.check.setVisible(true);
    }
    if (result.sTypeToImage.get(NetPronImageType.PHONE_TRANSCRIPT) != null) {
      phoneTranscript.image.setUrl(result.sTypeToImage.get(NetPronImageType.PHONE_TRANSCRIPT));
      phoneTranscript.image.setVisible(true);
      phoneTranscript.check.setVisible(true);
    }
    if (result.sTypeToImage.get(NetPronImageType.SPEECH_TRANSCRIPT) != null) {
      speechTranscript.image.setUrl(result.sTypeToImage.get(NetPronImageType.SPEECH_TRANSCRIPT));
      speechTranscript.image.setVisible(true);
      speechTranscript.check.setVisible(true);
    }
    if (!scoredBefore) {
      System.out.println("new score returned " + result);
      scoreListener.gotScore(result);
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

  private class AudioPositionPopup implements AudioControl {
    double last = 0;
    public void reinitialize() {
      imageOverlay.hide();
      int left = imageContainer.getAbsoluteLeft();
      int top  = imageContainer.getAbsoluteTop();
      if (debug) System.out.println("reinit at " + left + ", " + top);
      imageOverlay.setPopupPosition(left, top);
    }

    public void songFirstLoaded(double durationEstimate) {
    //  System.out.println("songFirstLoaded");
    }

    public void songLoaded(final double duration) {
          songDuration = duration;
          int offsetHeight = imageContainer.getOffsetHeight();
          int left = imageContainer.getAbsoluteLeft();
          int top = imageContainer.getAbsoluteTop();
      if (debug) System.out.println("songLoaded " + duration + " height " + offsetHeight + " x " + left +
          " y " + top);
      imageOverlay.setSize("2px", offsetHeight + "px");
      imageOverlay.setPopupPosition(left, top);
      if (debug) System.out.println("songLoaded " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
          " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim " +
          imageOverlay.getOffsetWidth() + " x " + imageOverlay.getOffsetHeight());
  }

    public void update(double position) {
      if (!imageOverlay.isShowing()) {
        imageOverlay.show();
        if (debug) System.out.println("\tshowingPopup");
      }
      last = position;
      showAt(position);
    }

    public void show() {
      if (imageOverlay.isShowing()) {
        showAt(last);
      }
    }

    private void showAt(double position) {
      int offsetHeight = imageContainer.getOffsetHeight();
      imageOverlay.setSize("2px", offsetHeight + "px");

      int left = imageContainer.getAbsoluteLeft() + (int) (((double) imageContainer.getOffsetWidth()) * (position / songDuration));
      int top = imageContainer.getAbsoluteTop();
      imageOverlay.setPopupPosition(left, top);
      if (debug) System.out.println("showAt " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
          " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim " +
          imageOverlay.getOffsetWidth() + " x " + imageOverlay.getOffsetHeight());
    }
  }
}
