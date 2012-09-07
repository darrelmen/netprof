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
import com.google.gwt.user.client.ui.HTML;
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
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class AudioPanel extends VerticalPanel implements RequiresResize {
  public static final int MIN_WIDTH = 256;
  public static final int HEIGHT = 128;
  public static final int RIGHT_MARGIN = 400;
  private String audioPath;
  private Image waveform,spectrogram;
  private int lastWidth = 0;
  private PopupPanel imageOverlay;
  private double songDuration;
  private Panel imageContainer;
  private AudioPositionPopup audioPositionPopup;
  protected LangTestDatabaseAsync service;
  protected SoundManagerAPI soundManager;
  private PlayAudioPanel playAudio;
  boolean debug = false;

  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @paramx e
   * @param service
   * @paramx userFeedback
   * @paramx controller
   */
  public AudioPanel(String path, LangTestDatabaseAsync service, SoundManagerAPI soundManager) {
    this.soundManager = soundManager;
    //super(e, service, userFeedback, controller);
    this.service = service;
  //  this.controller = controller;
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        int diff = Math.abs(event.getWidth() - lastWidth);
        //System.out.println("got resize " + getOffsetWidth() + " event " + event.getWidth() + " diff " + diff);
        if (lastWidth == 0 || ((float)diff /(float)lastWidth) > 0.2) {
          if (debug) System.out.println("new width " +  event.getWidth() + " vs old " + lastWidth);

          lastWidth = event.getWidth();
          getImages();
        }
      }
    });
    addWidgets(path);
  }

  public void onResize() {
    if (debug) System.out.println("got resize " + getOffsetWidth());
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   * @param path
   * @return
   */
  private void addWidgets(String path) {
    //  ResizeLayoutPanel resizeLayoutPanel = new ResizeLayoutPanel();
    //  imageContainer = new ResizeVP();
    // resizeLayoutPanel.add(imageContainer);
    imageContainer = new VerticalPanel();
    HorizontalPanel hp = new HorizontalPanel();
    hp.setWidth("100%");
    hp.setSpacing(5);


    playAudio = addButtonsToButtonRow(hp, path);
    lastWidth = Window.getClientWidth();

    HorizontalPanel controlPanel = new HorizontalPanel();
    waveform = new Image();
    addCheckbox(controlPanel, "Waveform", waveform);
    spectrogram = new Image();
    addCheckbox(controlPanel, "Spectrogram", spectrogram);
    hp.setCellHorizontalAlignment(controlPanel, HasHorizontalAlignment.ALIGN_RIGHT);
    hp.add(controlPanel);
    add(hp);

    imageContainer.add(waveform);
    imageContainer.add(spectrogram);
    add(imageContainer);

    if (path != null) { // TODO awkward... better way?
      getImagesForPath(path);
    }
  }

  public void getImagesForPath(String path) {
    getImages(path, waveform, spectrogram);
    this.audioPath = path;
    playAudio.startSong(path);
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

  private static class ResizeVP extends VerticalPanel implements RequiresResize {
    public void onResize() {
      System.out.println("Got resize width " + getOffsetWidth() + " height " + getOffsetHeight());
    }
  }

  private void getImages() {
    getImages(audioPath, waveform, spectrogram);
  }

  private void getImages(String path, Image waveform, Image spectrogram) {
    int width = Window.getClientWidth()- RIGHT_MARGIN;
    getImageURLForAudio(path, "Waveform", width, waveform);
    getImageURLForAudio(path, "Spectrogram", width, spectrogram);
  }

  private void getImageURLForAudio(String path, String type,int width, final Image waveform) {
    int toUse = Math.max(MIN_WIDTH, width);
    int height = HEIGHT;
    service.getImageForAudioFile(path, type, toUse, height, new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(String result) {
        waveform.setUrl(result);
        audioPositionPopup.reinitialize();
      }
    });
  }

  private void addCheckbox(HorizontalPanel controlPanel,String label, final Widget widget) {
    CheckBox w = new CheckBox();
    w.setValue(true);
    w.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        widget.setVisible(event.getValue());
      }
    });
    controlPanel.add(w);
    controlPanel.add(new Label(label));
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
        //System.out.println("showing at " + last);
        showAt(last);
      }
    }

    private void showAt(double position) {
      int offsetHeight = imageContainer.getOffsetHeight();
      imageOverlay.setSize("2px", offsetHeight + "px");
      //imageOverlay.getWidget().setSize("2px", offsetHeight + "px");

      int left = imageContainer.getAbsoluteLeft() + (int) (((double) imageContainer.getOffsetWidth()) * (position / songDuration));
      int top = imageContainer.getAbsoluteTop();
      //System.out.println("update " + position + " left " + left + " top " + top);
      imageOverlay.setPopupPosition(left, top);
      if (debug) System.out.println("showAt " + imageOverlay.isShowing() + " vis " + imageOverlay.isVisible() +
          " x " + imageOverlay.getPopupLeft() + " y " + imageOverlay.getPopupTop() + " dim " +
          imageOverlay.getOffsetWidth() + " x " + imageOverlay.getOffsetHeight());
    }
  }
}
