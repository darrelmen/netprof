package mitll.langtest.client.project;

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.Fieldset;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.user.BasicDialog;

/**
 * Created by go22670 on 4/26/17.
 */
public class FileUploader extends BasicDialog {
  /**
   * Make a form panel
   *
   * @return
   */
  private FormPanel makeFormPanel() {
    final FormPanel form = new FormPanel();
    form.addStyleName("table-center");
    form.addStyleName("demo-FormPanel");

    form.setAction(GWT.getModuleBaseURL() + "langtestdatabase");
    // Because we're going to add a FileUpload widget, we'll need to set the
    // form to use the POST method, and multipart MIME encoding.
    form.setEncoding(FormPanel.ENCODING_MULTIPART);
    form.setMethod(FormPanel.METHOD_POST);
    return form;
  }

  private HTML feedback;

  public Widget getForm(int projectid) {
    final FormPanel form = makeFormPanel();
    Fieldset fieldset = new Fieldset();
    form.add(fieldset);
    com.github.gwtbootstrap.client.ui.FileUpload upload = new com.github.gwtbootstrap.client.ui.FileUpload();
    upload.setName("upload");

    final TextBox projectID = new TextBox();
    projectID.setName("projectid");
    projectID.setVisible(false);
    projectID.setText("" + projectid);
    fieldset.add(projectID);

    ControlGroup widgets = addControlGroupEntrySimple(
        fieldset,
        "Upload file (JSON)",
        upload);
    widgets.setWidth("366px");

    // Add a 'submit' button.
    final com.github.gwtbootstrap.client.ui.Button submit = new com.github.gwtbootstrap.client.ui.Button("Submit");
    submit.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        submit.setEnabled(false);
        form.submit();
      }
    });
    fieldset.add(submit);

    feedback = new HTML();
    feedback.addStyleName("topFiveMargin");
    feedback.addStyleName("bottomFiveMargin");
    fieldset.add(feedback);

    // Add an event handler to the form.
    form.addSubmitHandler(new FormPanel.SubmitHandler() {
      public void onSubmit(FormPanel.SubmitEvent event) {
        // This event is fired just before the form is submitted. We can take
        // this opportunity to perform validation.
        // checkForEmptyFormFields(event, siteName, languageBox, upload);
        if (upload.getFilename().isEmpty()) {
          markError(widgets, upload, upload, "Try Again", "Please select a file.");
          event.cancel();
          submit.setEnabled(true);
        }
      }
    });

    form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
      public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
        // When the form submission is successfully completed, this event is
        // fired. Assuming the service returned a response of type text/html,
        // we can get the result text here (see the FormPanel documentation for
        // further explanation).
        submit.setEnabled(true);
        String results = event.getResults();
        feedback.setHTML(results);
      }
    });

    return form;
  }
}
