package mitll.langtest.client.mail;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.ValueBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.LangTestDatabaseAsync;
import mitll.langtest.client.user.UserManager;

public class MailDialog {
  public static final String TYPE_AN_EMAIL_ADDRESS = "Type an email address";
  public static final String TYPE_AN_OPTIONAL_MESSAGE = "Type an optional message.";
  public static final String TYPE_AN_OPTIONAL_REPLY_TO_EMAIL = "Type an optional reply-to email.";
  private final LangTestDatabaseAsync service;
  private final UserManager userManager;

  public MailDialog(LangTestDatabaseAsync service, UserManager userManager) {
    this.service = service;
    this.userManager = userManager;
  }

  /**
   * @see LangTest#showEmail(String, String, String)
   * @param subject
   * @param linkTitle
   * @param token
   */
  public void showEmail(final String subject, final String linkTitle, final String token) {
    // Create the popup dialog box
    final DialogBox dialogBox = new DialogBox();
    dialogBox.setText("E-MAIL THIS");

    // Enable glass background.
    dialogBox.setGlassEnabled(true);

    Button closeButton = new Button("Cancel");
    Button sendButton = new Button("<b>Send</b>");
    Panel hp = makeButtonRow(closeButton, sendButton);
    final VerticalPanel dialogVPanel = new VerticalPanel();

    FormPanel form = new FormPanel();
    FlexTable grid = new FlexTable();
    form.setWidget(grid);
    grid.setStyleName("body");
    int row = 0;

    String href = "#" + token;
    System.out.println("href = " + href);
    Anchor title = new Anchor("<h2>" + subject + "</h2>", true, href, "_blank");
    title.setStyleName("bigLink");
    System.out.println("link element = " + title.getElement());
    grid.setWidget(row++, 0, title);
    grid.getFlexCellFormatter().setColSpan(row, 0, 2);
    grid.setHTML(row++, 0, "<b>To</b>");

    final TextArea toEmail = getEmailArea();

    VerticalPanel vp = new VerticalPanel();
    vp.add(toEmail);
    vp.add(new HTML("<font size='-1' style='grayText'>Separate multiple addresses with commas</font>"));
    grid.setWidget(row, 0, vp);
    grid.getFlexCellFormatter().setColSpan(row++, 0, 2);

    grid.setHTML(row++, 0, "");

    grid.setHTML(row++, 0, "<b>Reply To</b>");

    final TextArea replyEmail = getEmailArea(TYPE_AN_OPTIONAL_REPLY_TO_EMAIL);

    VerticalPanel vp2 = new VerticalPanel();
    vp2.add(replyEmail);
    vp2.add(new HTML("<font size='-1' style='grayText'>This is who would receive a reply to this email.</font>"));
    grid.setWidget(row++, 0, vp2);

    grid.setHTML(row++, 0, "<b>Message</b>");
    final TextArea messageBox = getMessageBox();
    grid.setWidget(row, 0, messageBox);
    grid.getFlexCellFormatter().setColSpan(row, 0, 2);

    final Panel ackPanel = new FlowPanel();
    final HTML ack = new HTML("");

    sendButton.addClickHandler(new SendClickHandler(toEmail, replyEmail, messageBox,
      subject, token, dialogBox, dialogVPanel, ack, ackPanel));

    dialogVPanel.add(form);
    dialogVPanel.add(hp);

    makeAckPanel(dialogBox, ackPanel, ack);

    ackPanel.setVisible(false);

    VerticalPanel twoPart = new VerticalPanel();
    twoPart.add(dialogVPanel);
    twoPart.add(ackPanel);
    dialogBox.setWidget(twoPart);

    // Add a handler to send the name to the server
    closeButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    dialogBox.center();
  }

  private FlowPanel makeButtonRow(Button closeButton, Button sendButton) {
    sendButton.addStyleName("sendButtonBlue");
    closeButton.setEnabled(true);
    closeButton.getElement().setId("closeButton");
    FlowPanel hp = new FlowPanel();
    hp.getElement().getStyle().setFloat(Style.Float.LEFT);
    hp.add(sendButton);
    hp.add(closeButton);
    return hp;
  }

  private void makeAckPanel(final DialogBox dialogBox, Panel ackPanel, HTML ack) {
    ackPanel.add(ack);
    Button close = new Button("Close");
    ackPanel.add(new HTML("<br></br>"));
    ackPanel.add(close);
    close.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
  }

  private void showAck(Panel dialogVPanel, HTML ack, String realToEmail, Panel ackPanel) {
    int offsetHeight = dialogVPanel.getOffsetHeight();
    dialogVPanel.setVisible(false);
    ack.setHTML(getAck(realToEmail));
    ackPanel.setHeight(offsetHeight + "px");
    ackPanel.setVisible(true);
  }

  private TextArea getMessageBox() {
    final TextArea messageBox = new TextArea();
    messageBox.setCharacterWidth(50);
    messageBox.setVisibleLines(3);
    messageBox.setText(TYPE_AN_OPTIONAL_MESSAGE);
    messageBox.setStyleName("grayText");
    addGrayStateKeyHandler(messageBox);
    return messageBox;
  }

  private TextArea getEmailArea() {
    String initialPrompt = TYPE_AN_EMAIL_ADDRESS;
    return getEmailArea(initialPrompt);
  }

  private TextArea getEmailArea(String initialPrompt) {
    final TextArea toEmail = new TextArea();
    toEmail.setCharacterWidth(50);
    toEmail.setText(initialPrompt);

    toEmail.setStyleName("grayText");
    addGrayStateKeyHandler(toEmail);
    return toEmail;
  }

  private String getAck(String destEmail) {
    return "<b>Thank You!</b><br></br>You've sent this lesson to <b>" + destEmail + "</b>&nbsp;";
  }

  private void addGrayStateKeyHandler(final TextBoxBase toEmail) {
    toEmail.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        if (toEmail.getStyleName().equals("grayText")){
          toEmail.setText("");
          toEmail.setStyleName("normalText");
        }
      }
    });
  }

  private class SendClickHandler implements ClickHandler {
    private final ValueBoxBase toEmail;
    private final ValueBoxBase replyEmail;
    private final ValueBoxBase messageBox;
    private final String subject;
    private final String token;
    //private final String linkTitle;
    private final DialogBox dialogBox;
    private final Panel dialogVPanel;
    private final HTML ack;
    private final Panel ackPanel;

    public SendClickHandler(ValueBoxBase toEmail, ValueBoxBase replyEmail, ValueBoxBase messageBox,
                            String subject, String token,
                          //  String linkTitle,
                            DialogBox dialogBox, Panel dialogVPanel,
                            HTML ack, Panel ackPanel) {
      this.toEmail = toEmail;
      this.replyEmail = replyEmail;
      this.messageBox = messageBox;
      this.subject = subject;
      this.token = token;
    //  this.linkTitle = linkTitle;
      this.dialogBox = dialogBox;
      this.dialogVPanel = dialogVPanel;
      this.ack = ack;
      this.ackPanel = ackPanel;
    }

    @Override
    public void onClick(ClickEvent event) {
    /*  if (fromEmail.getText().length() == 0 || !fromEmail.getText().contains("@")) {
        Window.alert("Please enter valid from email.");
      } else*/
      String toEmailContents = toEmail.getText();
      if (toEmailContents.equals(TYPE_AN_EMAIL_ADDRESS)) {
        toEmailContents = "";
      }
      final String realToEmail = toEmailContents;
      String messageBoxText = messageBox.getText();
      String replyToText = replyEmail.getText();
      if (messageBoxText.equals(TYPE_AN_OPTIONAL_MESSAGE)) messageBoxText = "";
      if (replyToText.equals(TYPE_AN_OPTIONAL_REPLY_TO_EMAIL)) replyToText = "";
      if (toEmailContents.length() == 0 || !toEmailContents.contains("@")) {
        Window.alert("Please enter valid to email.");
      } else {
        service.sendEmail(
          userManager.getUser(),
          toEmailContents,
          replyToText,
          subject,
          messageBoxText,
          token,
          new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert("Couldn't contact server.");
            dialogBox.hide();
          }

          @Override
          public void onSuccess(Void result) {
            showAck(dialogVPanel, ack, realToEmail, ackPanel);
          }
        }
        );
      }
    }
  }



/*  public void setMailtoWithHistory(String type, String section, String historyToken) {
    String url = GWT.getModuleBaseURL() + "#"+historyToken;
    setMailto("Mail this lesson.",type + " " + section,"Hi,"+
    "Here is a link to the lesson " +type +" " +section + " : " +
      url
    );
     System.out.println("set mail to " + type + " " + section + " " + historyToken);

 *//*   SafeHtmlBuilder sb = new SafeHtmlBuilder();
    String name = answer.savedExerciseFileName;
    sb.appendHtmlConstant("<a href='" +
      "config/dataCollectAdmin/uploads/" +
      name +
      "'" +
      ">");
    sb.appendEscaped(answer.exerciseFile);
    sb.appendHtmlConstant("</a>");
    return sb.toSafeHtml();*//*

  }*/
/*  public void setMailto(String subject,String body) {
    setMailto("Mail this lesson.",subject,body);
  }*/
/*  public void setMailto(String linkTitle,String subject,String body) {
    //String linkTitle = "mail link";
    String toList = "recipients@mail.mil";
   // String subject = "subject";
   // String body = "";
    String s = "<a href=\"mailto:" + toList + "?" +
      "subject=" + subject + "&" +
      "cc=cc@example.com" +
      "&body=" + body +
      "\">" +
      linkTitle +
      "</a>";

    SafeHtmlBuilder sb = new SafeHtmlBuilder();
   // String name = answer.savedExerciseFileName;
    sb.appendHtmlConstant("<h3><a href=\"mailto:" + toList + "?" +
      "subject=" + subject + "&" +
      "cc=cc@example.com" +
      "&body=" + body +
      "\">");
    sb.appendEscaped(linkTitle);
    sb.appendHtmlConstant("</a></h3>");
    SafeHtml safeHtml = sb.toSafeHtml();
   // return safeHtml;

 //   lineBelowTitle.setHTML(safeHtml);
   // return s;
  }*/


/*  private Widget getEmailWidget() {
    FlexTable g = new FlexTable();
    Anchor widget = new Anchor("E-MAIL");
    int row = 0;
    //g.setText(row,0, "");
    g.setWidget(row, 0, new HTML("<h3>Share</h3>"));
    g.getFlexCellFormatter().setColSpan(row, 0, 2);
    row++;
    g.setText(row,0, "");
    g.setWidget(row,1, widget);
    widget.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        showEmail(emailSubject, emailMessage, emailToken);
      }
    });

    FlowPanel widgets = new FlowPanel();
    widgets.add(g);
    return widgets;
  }*/

  // Create the popup dialog box
  //final Triple triple = getTriple(History.getToken());

  // Enable glass background.


  //  HTMLPanel htmlPanel = new HTMLPanel("h2",title.getHTML());
//    title.setStyleName();
  // HTML html = new HTML("<h2></h2>");
  // html.getElement().appendChild(title.getElement());

  //  grid.setHTML(row++, 0, "<font size='-1' style='grayText'>Separate multiple addresses with commas</font>");
//    grid.getFlexCellFormatter().setColSpan(row, 0, 2);

  //String url = GWT.getHostPageBaseURL() + "#"+History.getToken();

  // Add a handler to send the name to the server
}