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
  private VerticalPanel mainPanel = new VerticalPanel();
	private Hidden authenticity_token = new Hidden(), upload_file = new Hidden(), format = new Hidden();
	private static Hidden plan, exercise, question;
	
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

    mainPanel.add(authenticity_token);
		mainPanel.add(upload_file);
		mainPanel.add(format);
		mainPanel.add(plan);
		mainPanel.add(exercise);
	}

  public void setSlots(Exercise e, int id) {
    plan.setValue(e.getPlan());
    exercise.setValue(e.getID());
    question.setValue(""+id);
  }
}
