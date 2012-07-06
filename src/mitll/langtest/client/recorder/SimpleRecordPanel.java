/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import mitll.langtest.client.ExerciseController;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.shared.Exercise;

/**
 * Roughly mimics the Cykod example at <a href='https://github.com/cykod/FlashWavRecorder/blob/master/html/index.html'>Cykod example html</a><p></p>
 *
 * Communicates with saveFeedback interface when a save(upload) completes {@link mitll.langtest.client.RecordExercisePanel.AnswerPanel}
 *
 * @author Gordon Vidaver
 *
 */
public class SimpleRecordPanel extends FlowPanel {

   private static final String IMAGES_CHECKMARK = "images/checkmark.png";
  private static final String IMAGES_REDX_PNG = "images/redx.png";

 // private boolean showStatus = false;
 // private boolean showUploadStatus = false;
//  private final UploadForm upload;
 // private static SaveNotification saveFeedback;  // remember for later
  //private InlineHTML save_button;
  private ImageAnchor play_button;
  private LangTestDatabaseAsync service;
  boolean recording = false;
  private final Image recordImage;
  private final Image stopImage;
  private Image check;

  /**
   * @see
   */
	public SimpleRecordPanel(final LangTestDatabaseAsync service, final ExerciseController controller,final Exercise exercise,final int index){
    this.service = service;
/*    save_button = new InlineHTML();
    save_button.getElement().setId("save_button");//"flashcontent");
    add(save_button);*/

/*    SimplePanel flashContent = new SimplePanel();
		flashContent.getElement().setId(id);//"flashcontent");
    flashContent.setSize(240+"px",160+"px");*/

  /*  InlineHTML inner = new InlineHTML();
    inner.setHTML("<p>ERROR: Your browser must have JavaScript enabled and the Adobe Flash Player installed.</p>");
    flashContent.add(inner);*/

	/*	SimplePanel statusPanel = new SimplePanel();
		statusPanel.getElement().setId("status");
		add(statusPanel);
    statusPanel.setVisible(showStatus);*/
    // record
    recordImage = new Image("images/record.png");
    recordImage.setAltText("Record");
    stopImage = new Image("images/stop.png");
    stopImage.setAltText("Stop");
    final ImageAnchor record_button = new ImageAnchor();
    record_button.setResource(recordImage);

    record_button.getElement().setId("record_button");
    record_button.setTitle("Record");
    record_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        if (recording) {
          recording = false;
          record_button.setResource(recordImage);
          controller.stopRecording();
          play_button.setVisible(true); // TODO : replace with link to audio on server

         // sendBase64EncodedArray(controller.getBase64EncodedWavFile());

          service.writeAudioFile(          controller.getBase64EncodedWavFile()
            ,exercise.getPlan(),exercise.getID(),""+index,""+controller.getUser(),new AsyncCallback<Boolean>() {
            public void onFailure(Throwable caught) {
            }

            public void onSuccess(Boolean result) {
              // if (result) {}

              check.setVisible(false);
              if (result) {
                check.setUrl(IMAGES_CHECKMARK);
                check.setAltText("Audio Saved");
                //   exercisePanel.recordCompleted(outer);    // TODO fill in
              }
              else {
                check.setUrl(IMAGES_REDX_PNG);
                check.setAltText("Audio Invalid");
                // exercisePanel.recordIncomplete(outer);
              }
              check.setVisible(true);
            }

          });
          
          
        } else {
          record_button.setResource(stopImage);
          recording = true;

          controller.startRecording();
        }
      }
    });
    add(record_button);

  //  add(flashContent);

    //playback

    Image image2 = new Image("images/play.png");
    image2.setAltText("Play");
    play_button = new ImageAnchor();
    play_button.setResource(image2);

    play_button.getElement().setId("play_button");
    play_button.setTitle("Play");
    play_button.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
    //    headless.playbackOnClick();
      }
    });
    add(play_button);
    play_button.setVisible(false);

 /*   SimplePanel uploadStatusPanel = new SimplePanel();
		uploadStatusPanel.getElement().setId("upload_status");
		add(uploadStatusPanel);
    uploadStatusPanel.setVisible(showUploadStatus);

    upload = new UploadForm();
    add(upload);*/


    this.check = new Image(IMAGES_CHECKMARK);
    check.getElement().setId("checkmark_" +index);
    check.setAltText("Audio Saved");

    SimplePanel spacer = new SimplePanel();
    spacer.setHeight("24px");
    spacer.setWidth("100px") ;

    add(spacer);

    add(check);
    check.setVisible(false);
  }

  /**
   * Doesn't seem to work
   */
  public void reset() {
    //GWT.log("reset widget state --- ");
    //save_button.setVisible(false);
    //play_button.setVisible(false);
  }

  /**
   * Remember widget so we can show it later when the save completes.
   * @see mitll.langtest.client.ExerciseController#showRecorder(mitll.langtest.shared.Exercise, int, com.google.gwt.user.client.ui.Widget, mitll.langtest.client.recorder.SaveNotification)
   * @param w
   */
  public static void setSaveCompleteFeedbackWidget(SaveNotification w) {
   // saveFeedback = w;
  }

  /**
   * @see mitll.langtest.client.ExerciseController#showRecorder(mitll.langtest.shared.Exercise, int, com.google.gwt.user.client.ui.Widget, mitll.langtest.client.recorder.SaveNotification)
   * @paramx userID
   * @paramx xe
   * @paramx qid
   */
  //public void setUpload(long userID, Exercise e, int qid) { upload.setSlots(userID, e,qid); }

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

  /*  public void setImage() {
      img.setUrl();
    }*/
  }

  private void sendBase64EncodedArray(String base64) {
    if (base64 == null) {
      System.err.println("got null array!");
      return;
    }
   // GWT.log("Got array of size " + array.length());
 //   JsArrayInteger array = getArray();

    service.postArray(base64,new AsyncCallback<Void>() {
      public void onFailure(Throwable caught) {
        GWT.log("sendBase64EncodedArray : got failure " + caught);
      }

      public void onSuccess(Void result) {
        GWT.log("sendBase64EncodedArray : got success " + result);
      }
    });
  }
}
