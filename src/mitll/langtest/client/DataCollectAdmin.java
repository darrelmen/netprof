package mitll.langtest.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Site;

public class DataCollectAdmin {
  private UserManager userManager;
  private LangTestDatabaseAsync service;
  private long siteID;
  public DataCollectAdmin(UserManager userManager, LangTestDatabaseAsync service) {
    this.userManager = userManager;
    this.service = service;
  }

  void makeDataCollectNewSiteForm(Panel currentExerciseVPanel) {

    // Create a FormPanel and point it at a service.
    final FormPanel form = new FormPanel();
    form.addStyleName("table-center");
    form.addStyleName("demo-FormPanel");

    form.setAction(GWT.getModuleBaseURL() + "langtestdatabase");
        // Because we're going to add a FileUpload widget, we'll need to set the
        // form to use the POST method, and multipart MIME encoding.
    form.setEncoding(FormPanel.ENCODING_MULTIPART);
    form.setMethod(FormPanel.METHOD_POST);

    // Create a panel to hold all of the form widgets.
    VerticalPanel panel = new VerticalPanel();
    form.setWidget(panel);

    // Create a TextBox, giving it a name so that it will be submitted.
    final TextBox tb = new TextBox();
    tb.setName("siteName");
    HTML w = new HTML("<h2>Create a new data collection site</h2>");
    w.addStyleName("blueColor");

    panel.add(w);
    HTML w1 = new HTML("<h3>Step 1: Choose a name</h3>");
    w1.addStyleName("blueColor");

    panel.add(w1);
    panel.add(tb);
/*    tb.addStyleName("blueColor");
    tb.addStyleName("backgroundWhite");*/

    final TextBox tb2 = new TextBox();
    tb2.setName("siteLanguage");
    HTML w2 = new HTML("<h3>Step 2: Choose a language</h3>");
    w2.addStyleName("blueColor");

    panel.add(w2);
    panel.add(tb2);
/*
    tb2.addStyleName("blueColor");
*/

    final TextBox tb3 = new TextBox();
    tb3.setName("siteNotes");
    HTML w3 = new HTML("<h3>Step 3: Add any notes for this upload.</h3>");
    w3.addStyleName("blueColor");

    panel.add(w3);
    panel.add(tb3);
/*
    tb3.addStyleName("blueColor");
*/
     // Create a FileUpload widget.
    FileUpload upload = new FileUpload();
    upload.setName("upload");
    HTML w4 = new HTML("<h3>Step 4: Upload an excel spreadsheet wordlist.</h3>");
    w4.addStyleName("blueColor");

    panel.add(w4);
    panel.add(upload);
    HTML w5 = new HTML("<h3>Step 5: Check upload data for errors.</h3>");
    w5.addStyleName("blueColor");

    panel.add(w5);
    upload.addStyleName("blueColor");

    final TextBox user = new TextBox();
    user.setName("user");
    user.setVisible(false);
    user.setText(""+userManager.getUser());
    panel.add(user);

    final HTML w6 = new HTML("");
    w6.addStyleName("blueColor");
    panel.add(w6);


    final HTML w7 = new HTML("");
    w7.addStyleName("blueColor");
    panel.add(w7);



    // Add a 'submit' button.
    panel.add(new Button("Submit", new ClickHandler() {
      public void onClick(ClickEvent event) {
        user.setText(""+userManager.getUser());
        form.submit();
      }
    }));

    HTML w8 = new HTML("<h3>Step 6: Deploy new site.</h3>");
    w8.addStyleName("blueColor");

    panel.add(w8);

    // Add an event handler to the form.
    form.addSubmitHandler(new FormPanel.SubmitHandler() {
      public void onSubmit(FormPanel.SubmitEvent event) {
        // This event is fired just before the form is submitted. We can take
        // this opportunity to perform validation.
        if (tb.getText().length() == 0) {
          Window.alert("The text box must not be empty");
          event.cancel();
        }
      }
    });
    form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
      public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
        // When the form submission is successfully completed, this event is
        // fired. Assuming the service returned a response of type text/html,
        // we can get the result text here (see the FormPanel documentation for
        // further explanation).
        String results = event.getResults();
        if (results.contains("Invalid")) {
          Window.alert(results);
        } else {
          if (results.startsWith("<")) {
            results = results.split(">")[1].split("<")[0];
          }
          try {
            long id = 0;
            id = Long.parseLong(results.trim());
            service.getSiteByID(id,new AsyncCallback<Site>() {
              @Override
              public void onFailure(Throwable caught) {}

              @Override
              public void onSuccess(Site result) {
                w6.setHTML("<h4>&npsp;" +
                    result.getFeedback() + "</h4>");
                 //   ", example exercise content :</h4>");
                //w7.setHTML(result.example.getContent());
                siteID = result.id;
              }
            });
          } catch (NumberFormatException e) {
            Window.alert("couldn't understand response " + results);
          }

        }
      }
    });

    currentExerciseVPanel.add(form);
    Button child = new Button("Deploy!");
    currentExerciseVPanel.add(child);
    child.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        service.deploySite(siteID, new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            //To change body of implemented methods use File | Settings | File Templates.
          }

          @Override
          public void onSuccess(Boolean result) {
            Window.alert("Site deployed!");
          }
        });
      }
    });

  }
}