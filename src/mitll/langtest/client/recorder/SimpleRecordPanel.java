/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.ExerciseController;
import mitll.langtest.client.ExerciseQuestionState;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.AudioAnswer;
import mitll.langtest.shared.Exercise;

/**
 * Roughly mimics the Cykod example at <a href='https://github.com/cykod/FlashWavRecorder/blob/master/html/index.html'>Cykod example html</a><p></p>
 *
 * @author Gordon Vidaver
 *
 */
public class SimpleRecordPanel extends HorizontalPanel {
  private static final String IMAGES_CHECKMARK = "images/checkmark.png";
  private static final String IMAGES_REDX_PNG = "images/redx.png";

  boolean recording = false;
  private final Image recordImage;
  private final Image stopImage;
  private Image check;
  private SimplePanel playback = new SimplePanel();

  /**
   * @see mitll.langtest.client.SimpleRecordExercisePanel#getAnswerWidget(mitll.langtest.shared.Exercise, mitll.langtest.client.LangTestDatabaseAsync, mitll.langtest.client.ExerciseController, int)
   */
	public SimpleRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,
                           final Exercise exercise, final ExerciseQuestionState questionState, final int index){
    // record
    final Panel outer = this;

    // make images
    recordImage = new Image("images/record.png");
    recordImage.setAltText("Record");
    stopImage = new Image("images/stop.png");
    stopImage.setAltText("Stop");

    final ImageAnchor record_button = new ImageAnchor();
    record_button.setResource(recordImage);

    record_button.getElement().setId("record_button");
    record_button.setTitle("Record");
    playback.setHeight("30px"); // for audio controls to show
    record_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (recording) {
          recording = false;
          stopClicked(record_button, controller, service, exercise, index, questionState, outer);

        } else {
          recording = true;
          playback.setWidget(new HTML(""));
          record_button.setResource(stopImage);

          controller.startRecording();
        }
      }
    });
    add(record_button);

    SimplePanel spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("10px");
    add(spacer);
    //playback

    this.check = new Image(IMAGES_CHECKMARK);
    check.getElement().setId("checkmark_" +index);
    check.setAltText("Audio Saved");

    spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("100px") ;

    add(spacer);

    add(check);
    check.setVisible(false);

    spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("10px");
    add(spacer);

    add(playback);
  }

  private void stopClicked(ImageAnchor record_button, ExerciseController controller, LangTestDatabaseAsync service,
                           Exercise exercise, int index, final ExerciseQuestionState questionState, final Panel outer) {
    record_button.setResource(recordImage);
    controller.stopRecording();

    service.writeAudioFile(controller.getBase64EncodedWavFile()
      ,exercise.getPlan(),exercise.getID(),""+index,""+controller.getUser(),new AsyncCallback<AudioAnswer>() {
      public void onFailure(Throwable caught) {}

      public void onSuccess(AudioAnswer result) {
        showAudioValidity(result.valid, questionState, outer);
        setAudioTag(result.path);
      }
    });
  }

  private void setAudioTag(String result) {
    playback.setWidget(new HTML("<audio preload=\"none\" controls=\"controls\" tabindex=\"0\">\n" +
      "<source type=\"audio/wav\" src=\"" +
      result +
      "\"></source>\n" +
      // "<source type=\"audio/ogg\" src=\"media/ac-LC1-009/ac-LC1-009-C.ogg\"></source>\n" +
      "Your browser does not support the audio tag.\n" +
      "</audio>"));
  }

/*
  private void addPlayButton(final ExerciseController controller) {
    final Image image2 = new Image("images/play.png");
    image2.setAltText("play");
    play_button = new ImageAnchor();
    play_button.setResource(image2);

    play_button.getElement().setId("play_button");
    play_button.setTitle("Play");
    play_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (playing) {
          image2.setUrl("images/play.png");
          image2.setAltText("Play recorded audio.");
        }
        else {
          image2.setUrl("images/squareStop.png");
          image2.setAltText("Click to stop playback.");
        }
        playing = !playing;
      //  controller.playBackAudio();
      }
    });
    add(play_button);
    play_button.setVisible(false);
  }
*/

  private void showAudioValidity(Boolean result, ExerciseQuestionState questionState, Panel outer) {
    check.setVisible(false);
    if (result) {
      check.setUrl(IMAGES_CHECKMARK);
      check.setAltText("Audio Saved");
      questionState.recordCompleted(outer);
    }
    else {
      check.setUrl(IMAGES_REDX_PNG);
      check.setAltText("Audio Invalid");
      questionState.recordIncomplete(outer);
    }
    check.setVisible(true);
  }

  private static class ImageAnchor extends Anchor {
    Image img = null;
    public ImageAnchor() {}
    public void setResource(Image img2) {
      if (this.img != null) {
        DOM.removeChild(getElement(),this.img.getElement());
      }
      this.img = img2;
      DOM.insertBefore(getElement(), img.getElement(), DOM.getFirstChild(getElement()));
    }
  }
}
