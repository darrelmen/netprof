package mitll.langtest.client.goodwave;

import com.goodwave.client.PlayAudioPanel;
import com.goodwave.client.sound.AudioControl;
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
import com.google.gwt.user.client.ui.ProvidesResize;
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
public class GoodwaveExercisePanel extends ExercisePanel implements RequiresResize {
  public static final int MIN_WIDTH = 256;
  public static final int HEIGHT = 128;
  public static final int RIGHT_MARGIN = 400;
  private String audioPath;
  private Image waveform,spectrogram;
  private int lastWidth = 0;
  PopupPanel imageOverlay;// = new PopupPanel(false);
  private double songDuration;

  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GoodwaveExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
    super(e, service, userFeedback, controller);
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        int diff = Math.abs(event.getWidth() - lastWidth);
        //System.out.println("got resize " + getOffsetWidth() + " event " + event.getWidth() + " diff " + diff);
        if (lastWidth == 0 || ((float)diff /(float)lastWidth) > 0.2) {
          System.out.println("new width " +  event.getWidth() + " vs old " + lastWidth);

          lastWidth = event.getWidth();
          getImages();
        }
      }
    });
  }

  public void onResize() {
    System.out.println("got resize " + getOffsetWidth());
  }

  /**
   * Replace the html 5 audio tag with our fancy waveform widget.
   * @param e
   * @return
   */
  @Override
  protected Widget getQuestionContent(Exercise e) {
    String content = e.getContent();
    String path = null;
    if (content.contains("audio")) {
   //   System.err.println("content " + content);

      int i = content.indexOf("source src=");
      String s = content.substring(i + "source src=".length()+1).split("\\\"")[0];
      System.err.println("audio path '" + s + "'");
      path = s;

      int start = content.indexOf("<audio");
      int end = content.indexOf("audio>");
      content = content.substring(0,start) + content.substring(end + "audio>".length());

    //  System.err.println("after " + content);
    }

    VerticalPanel vp = new VerticalPanel();
    // TODO make a good wave panel that plays audio, displays the wave form...
    Widget questionContent = new HTML(content);//super.getQuestionContent(e);
    vp.add(questionContent);
   if (path != null) {
     final PlayAudioPanel playAudio = new PlayAudioPanel(controller.getSoundManager(), path);
     HorizontalPanel hp = new HorizontalPanel();
     final Panel outer = this;
     imageOverlay = new PopupPanel(false);
     playAudio.addListener(new AudioControl() {
       public void reinitialize() {
         imageOverlay.hide();
       }

       public void songFirstLoaded(double durationEstimate) {
         //To change body of implemented methods use File | Settings | File Templates.
       }

       public void songLoaded(double duration) {
         songDuration = duration;
         imageOverlay.show();
         imageOverlay.setSize("3px",outer.getOffsetHeight()+"px");
       }

       public void update(double position) {
         imageOverlay.setPopupPosition((int)(((double)outer.getOffsetHeight())*(position/songDuration)), getAbsoluteTop());
       }
     });
     hp.setWidth("100%");
     hp.setSpacing(5);

     hp.add(playAudio);
     lastWidth = Window.getClientWidth();

     HorizontalPanel controlPanel = new HorizontalPanel();
     waveform = new Image();
     addCheckbox(controlPanel,"Waveform",waveform);
     spectrogram = new Image();
     addCheckbox(controlPanel,"Spectrogram",spectrogram);
     hp.setCellHorizontalAlignment(controlPanel, HasHorizontalAlignment.ALIGN_RIGHT);
     hp.add(controlPanel);

     imageOverlay.setStyleName("ImageOverlay");
     imageOverlay.add(new SimplePanel());

     vp.add(hp);
     vp.add(waveform);
     vp.add(spectrogram);
     getImages(path, waveform, spectrogram);
     this.audioPath = path;
   }
    return vp;
  }

  public void setImageOverlayVisible(boolean visible){
    //getOffsetWidth()
    if(visible){
      imageOverlay.show();
    }
    else{
      imageOverlay.hide();
    }
  }

  private void getImages() {
    getImages(audioPath,waveform,spectrogram);
  }

  private void getImages(String path, Image waveform, Image spectrogram) {
    int width = Window.getClientWidth()- RIGHT_MARGIN;
    getImageURLForAudio(path, "Waveform", width, waveform);
    getImageURLForAudio(path, "Spectrogram", width, spectrogram);
  }

  private void getImageURLForAudio(String path, String type,int width, final Image waveform) {
    int toUse = Math.max(MIN_WIDTH, width);
    int height = HEIGHT;
    service.getImageForAudioFile(path, type, toUse, height,new AsyncCallback<String>() {
      public void onFailure(Throwable caught) {}
      public void onSuccess(String result) {
        waveform.setUrl(result);
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

  /**
   * Has a answerPanel mark to indicate when the saved audio has been successfully posted to the server.
   *
   *
   * @see mitll.langtest.client.exercise.ExercisePanel#ExercisePanel(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.user.UserFeedback, mitll.langtest.client.exercise.ExerciseController)
   * @param exercise
   * @param service
   * @param controller
   * @param index
   * @return
   */
  @Override
  protected Widget getAnswerWidget(Exercise exercise, LangTestDatabaseAsync service, ExerciseController controller, final int index) {

    GoodwaveRecordPanel widgets = new GoodwaveRecordPanel(service, controller, exercise, this, index);
    GoodWaveCaptionPanel testCaptionPanel = new GoodWaveCaptionPanel("User Recorder", widgets);
    testCaptionPanel.add(widgets);

    testCaptionPanel.setHeight("600px");
    return testCaptionPanel;
  }

  @Override
  protected String getQuestionPrompt(boolean promptInEnglish) {
    return getSpokenPrompt(promptInEnglish);
  }

  /**
   * on the server, notice which audio posts have arrived, and take the latest ones...
   * <br></br>
   * Move on to next exercise...
   *
   * @param service
   * @param userFeedback
   * @param controller
   * @param completedExercise
   */
  @Override
  protected void postAnswers(LangTestDatabaseAsync service, UserFeedback userFeedback, ExerciseController controller, Exercise completedExercise) {
    controller.loadNextExercise(completedExercise);
  }
}
