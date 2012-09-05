package mitll.langtest.client.goodwave;

import com.goodwave.client.HidePanelsControlPanel;
import com.goodwave.client.PlayAudioPanel;
import com.goodwave.client.songimage.SongImageController;
import com.goodwave.client.songimage.SongImageManagerPanel;
import com.google.gwt.user.client.ui.*;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.ExercisePanel;
import mitll.langtest.client.recorder.SimpleRecordPanel;
import mitll.langtest.client.user.UserFeedback;
import mitll.langtest.shared.Exercise;

import java.util.Arrays;

/**
 * Mainly delegates recording to the {@link mitll.langtest.client.recorder.SimpleRecordPanel}.
 * User: GO22670
 * Date: 5/11/12
 * Time: 11:51 AM
 * To change this template use File | Settings | File Templates.
 */
public class GoodwaveExercisePanel extends ExercisePanel {
  /**
   * @see mitll.langtest.client.exercise.ExercisePanelFactory#getExercisePanel(mitll.langtest.shared.Exercise)
   * @param e
   * @param service
   * @param userFeedback
   * @param controller
   */
  public GoodwaveExercisePanel(final Exercise e, final LangTestDatabaseAsync service, final UserFeedback userFeedback,
                               final ExerciseController controller) {
    super(e,service,userFeedback,controller);
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

     hp.setWidth("100%");
     hp.setSpacing(5);

     hp.add(playAudio);

     SongImageManagerPanel parent = new SongImageManagerPanel(new SongImageController() {
       public void startSong() {
         playAudio.play();
       }

       public int getOffsetWidth() {
         return getOffsetWidth();
       }

       public void addToImageLoadPanel(Widget widget) {
         //To change body of implemented methods use File | Settings | File Templates.
       }

       public void removeFromImageLoadPanel(Widget widget) {
         //To change body of implemented methods use File | Settings | File Templates.
       }
     });
     HidePanelsControlPanel controlPanel = new HidePanelsControlPanel(parent);   // TODO
     controlPanel.init(Arrays.asList(SongImageManagerPanel.GoodWaveImageType.WAVEFORM,SongImageManagerPanel.GoodWaveImageType.SPECTROGRAM));
     hp.add(controlPanel);
     hp.setCellHorizontalAlignment(controlPanel, HasHorizontalAlignment.ALIGN_RIGHT);

     vp.add(parent);

     vp.add(hp);
   }
    return vp;    //To change body of overridden methods use File | Settings | File Templates.
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
