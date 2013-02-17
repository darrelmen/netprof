package mitll.langtest.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import mitll.langtest.client.table.PagerTable;
import mitll.langtest.client.user.UserManager;
import mitll.langtest.shared.Site;

import java.util.List;

public class DataCollectAdmin extends PagerTable {
  private UserManager userManager;
  private LangTestDatabaseAsync service;
  private long siteID;


  private ListDataProvider<Site> provider;
  private CellTable<Site> table;

  /**
   * @see mitll.langtest.client.LangTest#doDataCollectAdminView()
   * @param userManager
   * @param service
   */
  public DataCollectAdmin(UserManager userManager, LangTestDatabaseAsync service) {
    this.userManager = userManager;
    this.service = service;
  }

  void makeDataCollectNewSiteForm(Panel currentExerciseVPanel) {
   // currentExerciseVPanel.setWidth((Window.getClientWidth())-50 +"px");
    HTML child1 = new HTML("<h2>Current deployed sites</h2>");
    //child1.setWidth((Window.getClientWidth())-50 +"px");

    currentExerciseVPanel.add(child1);
    Widget table1 = getTable();
   // table1.setWidth((Window.getClientWidth())-50 +"px");
    currentExerciseVPanel.add(table1);
    currentExerciseVPanel.add(new HTML("<br></br>"));
    Button child = new Button("Add new site");
    currentExerciseVPanel.add(child);
    child.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showDialog();
      }
    });
  }

  private Widget getTable() {
    final CellTable<Site> table = new CellTable<Site>();
    TextColumn<Site> name = new TextColumn<Site>() {
      @Override
      public String getValue(Site answer) { return answer.name; }
    };
    name.setSortable(true);
    table.addColumn(name, "Site Name");

    TextColumn<Site> language = new TextColumn<Site>() {
      @Override
      public String getValue(Site answer) { return answer.language; }
    };
    language.setSortable(true);
    table.addColumn(language, "Language");

    TextColumn<Site> notes = new TextColumn<Site>() {
      @Override
      public String getValue(Site answer) { return answer.notes; }
    };
    notes.setSortable(true);
    table.addColumn(notes, "Notes");

    TextColumn<Site> exerciseFile = new TextColumn<Site>() {
      @Override
      public String getValue(Site answer) { return answer.exerciseFile; }
    };
    exerciseFile.setSortable(true);
    table.addColumn(exerciseFile, "File");

    Column<Site, SafeHtml> url = new Column<Site, SafeHtml>(
        new ClickableSafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Site answer) {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        sb.appendHtmlConstant(getLinkPrefix(answer) +
            "' target='_blank'>");
        sb.appendEscaped(answer.name);
        sb.appendHtmlConstant("</a>");
        return sb.toSafeHtml();
      }
    };

    table.addColumn(url, "Site URL");


    Column<Site, SafeHtml> url2 = new Column<Site, SafeHtml>(
        new ClickableSafeHtmlCell()) {
      @Override
      public SafeHtml getValue(Site answer) {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        sb.appendHtmlConstant(getLinkPrefix(answer) + "?admin"+
            "' target='_blank'>");
        sb.appendEscaped(answer.name);
        sb.appendHtmlConstant("</a>");
        return sb.toSafeHtml();
      }
    };

    table.addColumn(url2, "Site Monitoring URL");

    table.getColumnSortList().push(name);

    final ListDataProvider<Site> provider = createProvider(table);

    // We know that the data is sorted alphabetically by default.

    // Create a SimplePager.
    Panel pagerAndTable = getPagerAndTable(table, table, 10, 10);

    refresh(table, provider);
    this.provider = provider;
    this.table = table;
    return pagerAndTable;
  }

  private String getLinkPrefix(Site answer) {
    return "<a href='" + Window.Location.getProtocol() + "//" + Window.Location.getHost() + "/" + answer.name;
  }

  private class ClickableSafeHtmlCell extends AbstractCell<SafeHtml> {
    /**
     * Construct a new ClickableSafeHtmlCell.
     */
    public ClickableSafeHtmlCell() {
      super("click", "keydown");
    }

    @Override
    public void onBrowserEvent(Context context, com.google.gwt.dom.client.Element parent, SafeHtml value, NativeEvent event, ValueUpdater<SafeHtml> valueUpdater) {
      super.onBrowserEvent(context, parent, value, event, valueUpdater);
      if ("click".equals(event.getType())) {
        onEnterKeyDown(context, parent, value, event, valueUpdater);
      }
    }

    @Override
    public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
      if (value != null) {
        sb.append(value);
      }
    }

    @Override
    protected void onEnterKeyDown(Context context, com.google.gwt.dom.client.Element parent, SafeHtml value, NativeEvent event, ValueUpdater<SafeHtml> valueUpdater) {
      if (valueUpdater != null) {
        valueUpdater.update(value);
      }
    }
  }

  private void refresh() {
    refresh(table, provider);
  }

  private void refresh(final CellTable<Site> table, final ListDataProvider<Site> provider) {
    service.getSites(new AsyncCallback<List<Site>>() {
      @Override
      public void onFailure(Throwable caught) {}

      @Override
      public void onSuccess(List<Site> result) {
       // System.out.println("got sites num = " + result.size());

        provider.setList(result);
        table.setRowCount(result.size());
      }
    });
  }

  private ListDataProvider<Site> createProvider(CellTable<Site> table) {
    ListDataProvider<Site> dataProvider = new ListDataProvider<Site>();
    dataProvider.addDataDisplay(table);

    return dataProvider;
  }

  public void showDialog() {
    DialogBox dialogBox = new DialogBox(false, true);
    dialogBox.setTitle("Make new data collection site");
   // DOM.setStyleAttribute(dialogBox.getElement(), "backgroundColor", "#ABCDEF");
    VerticalPanel vp = new VerticalPanel();
    makeDataCollectNewSiteForm2(vp,dialogBox);
    dialogBox.add(vp);
    dialogBox.center();
  }
    /**
     * @see mitll.langtest.client.LangTest#doDataCollectAdminView()
     * @param currentExerciseVPanel
     */
  void makeDataCollectNewSiteForm2(Panel currentExerciseVPanel, final DialogBox dialogBox) {

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
    final TextBox siteName = new TextBox();
    siteName.setName("siteName");
    HTML w = new HTML("<h2>Create a new data collection site</h2>");
    w.addStyleName("blueColor");

    panel.add(w);
    HTML w1 = new HTML("<h3>Step 1: Choose a name</h3>");
    w1.addStyleName("blueColor");

    panel.add(w1);
    panel.add(siteName);
/*    siteName.addStyleName("blueColor");
    siteName.addStyleName("backgroundWhite");*/

    final TextBox languageBox = new TextBox();
    languageBox.setName("siteLanguage");
    HTML w2 = new HTML("<h3>Step 2: Choose a language</h3>");
    w2.addStyleName("blueColor");

    panel.add(w2);
    panel.add(languageBox);
/*
    languageBox.addStyleName("blueColor");
*/

    final TextBox notesBox = new TextBox();
    notesBox.setVisibleLength(50);
    notesBox.setName("siteNotes");
    HTML w3 = new HTML("<h3>Step 3: Add any notes for this upload.</h3>");
    w3.addStyleName("blueColor");

    panel.add(w3);
    panel.add(notesBox);
/*
    notesBox.addStyleName("blueColor");
*/
     // Create a FileUpload widget.
    final FileUpload upload = new FileUpload();
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
   // w6.addStyleName("blueColor");
    panel.add(w6);

    final HTML w7 = new HTML("");
    w7.addStyleName("blueColor");
    panel.add(w7);

    // Add a 'submit' button.
    final Button submit = new Button("Submit");
    submit.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        user.setText("" + userManager.getUser());
        form.submit();
      }
    });
    panel.add(submit);

    HTML w8 = new HTML("<h3>Step 6: Deploy new site.</h3>");
    w8.addStyleName("blueColor");

    panel.add(w8);

    // Add an event handler to the form.
    form.addSubmitHandler(new FormPanel.SubmitHandler() {
      public void onSubmit(FormPanel.SubmitEvent event) {
        // This event is fired just before the form is submitted. We can take
        // this opportunity to perform validation.
        if (siteName.getText().length() == 0) {
          Window.alert("Site name must not be empty");
          event.cancel();
        }
        else if (languageBox.getText().length() == 0) {
          Window.alert("Language must not be empty");
          event.cancel();
        }
        else if (upload.getFilename().length() == 0) {
          Window.alert("Please choose a word list file.");
          event.cancel();
        }
      }
    });
    final Button deployButton = new Button("Deploy!");
    deployButton.setEnabled(false);
    form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
      public void onSubmitComplete(FormPanel.SubmitCompleteEvent event) {
        // When the form submission is successfully completed, this event is
        // fired. Assuming the service returned a response of type text/html,
        // we can get the result text here (see the FormPanel documentation for
        // further explanation).
        deployButton.setEnabled(false);

        String results = event.getResults();
        if (results.contains("Invalid") || results.contains("invalid")) {
          if (results.startsWith("<")) {
            results = results.split(">")[1].split("<")[0];
          }
          Window.alert(results);
        } else {
          if (results.startsWith("<")) {
            results = results.split(">")[1].split("<")[0];
          }
          try {
            long id = 0;
            id = Long.parseLong(results.trim());

            submit.setEnabled(false);

            service.getSiteByID(id,new AsyncCallback<Site>() {
              @Override
              public void onFailure(Throwable caught) {
                submit.setEnabled(true);
              }

              @Override
              public void onSuccess(Site result) {
                submit.setEnabled(true);
                deployButton.setEnabled(true);
                w6.setHTML("<h4>" + result.getFeedback() + "</h4>");
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

    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.RIGHT);
    hp.add(deployButton);
    Button cancel = new Button("Cancel");
    hp.add(cancel);

    cancel.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });

    currentExerciseVPanel.add(hp);

    deployButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        deployButton.setEnabled(false);
        final PopupPanel pleaseWait = new PopupPanel();
        pleaseWait.setAutoHideEnabled(false);
        pleaseWait.add(new HTML("Please wait while site deploys..."));
        // this causes the image to be loaded into the DOM
        pleaseWait.center();
        service.deploySite(siteID, siteName.getText(), languageBox.getText(), notesBox.getText() , new AsyncCallback<Boolean>() {
          @Override
          public void onFailure(Throwable caught) {
            deployButton.setEnabled(true);
            pleaseWait.hide();
          }

          @Override
          public void onSuccess(Boolean result) {
            if (!result) Window.alert("Please choose another name : " +siteName.getText() + " exists or is invalid.");
            pleaseWait.hide();
            deployButton.setEnabled(true);
            dialogBox.hide();
            refresh();
          }
        });
      }
    });
  }
}