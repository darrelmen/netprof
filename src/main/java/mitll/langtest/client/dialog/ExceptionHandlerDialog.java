/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.dialog;

import com.google.gwt.user.client.ui.DialogBox;
import mitll.langtest.client.initial.BrowserCheck;

/**
 * Show javascript exceptions
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/13
 * Time: 7:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExceptionHandlerDialog {
/*  public ExceptionHandlerDialog(Throwable throwable) { showException(null, throwable);  }

  private void showException(BrowserCheck browserCheck, Throwable throwable) {
    showExceptionInDialog(browserCheck, getExceptionAsString(throwable));
  }*/

/*
  private void showExceptionInDialog(BrowserCheck browserCheck, String text) {
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
*/

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
