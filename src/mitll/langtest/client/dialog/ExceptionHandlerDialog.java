/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.dialog;

import com.google.gwt.user.client.ui.DialogBox;
import mitll.langtest.client.BrowserCheck;

/**
 * Show javascript exceptions
 * User: GO22670
 * Date: 1/30/13
 * Time: 7:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExceptionHandlerDialog {
  public ExceptionHandlerDialog(Throwable throwable) { showException(null, throwable);  }

  public ExceptionHandlerDialog(){}

  private void showException(BrowserCheck browserCheck, Throwable throwable) {
    String text = getExceptionAsString(throwable);
    showExceptionInDialog(browserCheck, text);
  }

  public void showExceptionInDialog(BrowserCheck browserCheck, String text) {
    if (browserCheck != null && browserCheck.isIE7() && text.contains("Unknown runtime error")) { // hack for IE 7
      return;
    }
    DialogBox dialogBox = new DialogBox(true, false);
    dialogBox.setWidth("80%");
    dialogBox.getElement().getStyle().setBackgroundColor("#ABCDEF");
    System.err.print(text);
    text = text.replaceAll(" ", "&nbsp;");
    dialogBox.setHTML("<pre>" + text + "</pre>");
    dialogBox.center();
  }

  public static String getExceptionAsString(Throwable throwable) {
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
    return text;
  }
}
