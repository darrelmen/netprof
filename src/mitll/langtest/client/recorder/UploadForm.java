/**
 * 
 */
package mitll.langtest.client.recorder;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * @author gregbramble
 *
 */
public class UploadForm extends FormPanel{
	private VerticalPanel mainPanel = new VerticalPanel();
	private Hidden authenticity_token = new Hidden(), upload_file = new Hidden(), format = new Hidden();
	private static Hidden currentPlanName, currentTestName;
	
	public UploadForm(){
		getElement().setId("uploadForm");			//this is so that the Recorder knows to use this form to upload
		
		setAction(GWT.getModuleBaseURL() + "upload");
		setEncoding(FormPanel.ENCODING_MULTIPART);
		setMethod(FormPanel.METHOD_POST);
		
		setWidget(mainPanel);
		
		authenticity_token.setValue("xxxxx");
		upload_file.setValue("1");
		format.setValue("json");
		currentPlanName = new Hidden("currentPlanName");
		currentTestName = new Hidden("currentTestName");
		
		mainPanel.add(authenticity_token);
		mainPanel.add(upload_file);
		mainPanel.add(format);
		mainPanel.add(currentPlanName);
		mainPanel.add(currentTestName);

    currentPlanName.setValue("testPlan");
    currentTestName.setValue("testExercise");
	}
	
	public static void setCurrentData(String planName, String testName){
		currentPlanName.setValue(planName);
		currentTestName.setValue(testName);
	}
}
