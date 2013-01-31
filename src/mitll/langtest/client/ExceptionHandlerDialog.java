package mitll.langtest.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.DialogBox;

/**
 * Show javascript exceptions
 * User: GO22670
 * Date: 1/30/13
 * Time: 7:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExceptionHandlerDialog {
  public ExceptionHandlerDialog(BrowserCheck browserCheck,Throwable throwable) {
    String text = "Uncaught exception: ";
    while (throwable != null) {
      StackTraceElement[] stackTraceElements = throwable.getStackTrace();
      text += throwable.toString() + "\n";
      for (StackTraceElement stackTraceElement : stackTraceElements) {
        text += "    at " + stackTraceElement + "\n";
      }
      throwable = throwable.getCause();
      if (throwable != null) {
        text += "Caused by: ";
      }
    }
    if (browserCheck.isIE7() && text.contains("Unknown runtime error")) { // hack for IE 7
      return;
    }
    DialogBox dialogBox = new DialogBox(true, false);
    DOM.setStyleAttribute(dialogBox.getElement(), "backgroundColor", "#ABCDEF");
    System.err.print(text);
    text = text.replaceAll(" ", "&nbsp;");
    dialogBox.setHTML("<pre>" + text + "</pre>");
    dialogBox.center();
  }
}
