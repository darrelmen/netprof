/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.shared.Exercise;

/**
 * @author gregbramble
 *
 */
public class UploadForm extends FormPanel{
  public static final String PLAN = "plan";
  public static final String EXERCISE = "exercise";
  public static final String QUESTION = "question";
  public static final String USER = "user";
  private VerticalPanel mainPanel = new VerticalPanel();
	private Hidden authenticity_token = new Hidden(), upload_file = new Hidden(), format = new Hidden();
	private static Hidden plan, exercise, question, user;
	
	public UploadForm(){
		getElement().setId("uploadForm");			//this is so that the Recorder knows to use this form to upload
		
		setAction(GWT.getModuleBaseURL() + "upload");
		setEncoding(FormPanel.ENCODING_MULTIPART);
		setMethod(FormPanel.METHOD_POST);
		
		setWidget(mainPanel);
		
		authenticity_token.setValue("xxxxx");
		upload_file.setValue("1");
		format.setValue("json");
		plan = new Hidden(PLAN);
    exercise = new Hidden(EXERCISE);
    question = new Hidden(QUESTION);
    user = new Hidden(USER);

    mainPanel.add(authenticity_token);
		mainPanel.add(upload_file);
		mainPanel.add(format);

    // TODO : context info -- should add user too
		mainPanel.add(plan);
    mainPanel.add(exercise);
    mainPanel.add(question);
    mainPanel.add(user);
  }

  /**
   * @see FlashRecordPanel#setUpload
   * @param userID
   * @param e
   * @param id
   */
  public void setSlots(long userID, Exercise e, int id) {
    user.setValue(""+userID);
    plan.setValue(e.getPlan());
    exercise.setValue(e.getID());
    question.setValue(""+id);
  }
}
