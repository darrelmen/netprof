/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */
package mitll.langtest.client.common;

import com.github.gwtbootstrap.client.ui.Alert;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Modal;
import com.github.gwtbootstrap.client.ui.constants.AlertType;
import com.github.gwtbootstrap.client.ui.constants.BackdropType;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.event.HideEvent;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.exercise.ExceptionSupport;
import mitll.langtest.client.initial.UILifecycle;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static mitll.langtest.client.LangTest.LANGTEST_IMAGES;

public class MessageHelper {
  private static final Logger log = Logger.getLogger(MessageHelper.class.getName());
  private static final String DOMINO_ERROR = "NetProF Error!";

  public enum DDialogType {
    Loading, FatalError, NonFatalError,
    Info, RefreshToken, Warning
  }

  private final UILifecycle parentHelper;
  private final ExceptionSupport exceptionSupport;
  private final Set<Object> waitList = new HashSet<>();
  private Modal waitDialog = null;

  public MessageHelper(UILifecycle parentHelper, ExceptionSupport exceptionSupport) {
    this.parentHelper = parentHelper;
    this.exceptionSupport = exceptionSupport;
  }

  public void makeModalAlert(String message) {
    Window.alert(message);
  }

  /**
   * @return Return true if Okay is clicked, false if Cancel is clicked.
   */
  public boolean makeConfirmation(String message) {
    return Window.confirm(message);
  }

  /**
   * Start waiting. This creates a dialog when necessary. Return a token to use when we're done waiting.
   */
  public Object startWaiting() {
    return startWaiting("Loading...");
  }

  /**
   * Start waiting. This creates a dialog when necessary. Return a token to use when we're done waiting.
   */
  public Object startWaiting(String message) {
    if (waitDialog == null) {
      waitDialog = makeInternalDialog(message, DDialogType.Loading, true);
      waitDialog.addHideHandler(HideEvent::stopPropagation);
      waitDialog.show();
    }
    Object token = new Object();
    waitList.add(token);
    return token;
  }

  /**
   * Stop waiting dismissing the dialog if necessary. Callers should provide the token returned during startWaiting.
   */
  public void stopWaiting(Object token) {
    waitList.remove(token);
    if (waitList.isEmpty() && waitDialog != null) {
      waitDialog.hide();
      waitDialog = null;
    }
  }

/*  public Modal handleFatalError(String msg, Throwable throwable) {
    return handleError(msg, throwable, DDialogType.FatalError);
  }*/

  public void handleNonFatalError(String msg, Throwable throwable) {
    handleError(msg, throwable, DDialogType.NonFatalError);
  }

  private Modal handleError(String msg, Throwable throwable, DDialogType dType) {
    if (throwable instanceof DominoSessionException) {
      log.warning("Logout for session (" + msg + ") : t=" + throwable);
      parentHelper.logout();
    } else if (throwable instanceof RpcTokenException) {
      log.warning("Restoring session: t=" + throwable);
      logOnServer("Refreshing token ! Original message\n" + msg, throwable, false);
      makeInternalDialog("", DDialogType.RefreshToken, true);
    } else if (throwable instanceof RestrictedOperationException) {
      makeInternalDialog("Operation not Permitted! (" + msg + ")", DDialogType.NonFatalError, true);
    } else if (throwable instanceof IncompatibleRemoteServiceException) {
      makeInternalDialog("Your version of netprof is out of date. Please click refresh on your browser to get the latest version.<br/><br/>",
          DDialogType.NonFatalError, false);
    } else if (isReload(throwable)) {
      // check wrapped
      logOnServer("User reload during ongoing request", throwable, false);
    } else {
      boolean originatedOnServer = (throwable instanceof StatusCodeException &&
          ((StatusCodeException) throwable).getStatusCode() == 500);
      // email for messages that do not originate on the server.
      logOnServer(msg, throwable, !originatedOnServer);
      return makeInternalDialog(msg, dType, true);
    }
    return null;
  }

  private boolean isReload(Throwable t) {
    if (t != null) {
      if (t instanceof StatusCodeException &&
          ((StatusCodeException) t).getStatusCode() == 0) {
        return true;
      } else {
        return isReload(t.getCause());
      }
    }
    return false;
  }

  /**
   * Make a dialog box with an embedded alert (either error, or waiting).
   */
  private Modal makeInternalDialog(String msg,
                                   final DDialogType dType,
                                   boolean defaultSuffix) {
    if (waitDialog != null) {
      waitDialog.hide();
      waitDialog = null;
      waitList.clear();
    }

    cleanupInlineMessages();

    final Modal m = new Modal(false, true);

    if (msg == null) {
      msg = "";
    }
    StringBuilder fullMsg = new StringBuilder(msg);
    if (defaultSuffix) {
      fullMsg.append(getDefaultSuffix(dType));
    }
    final Alert a = new Alert(fullMsg.toString());
    if (dType == DDialogType.NonFatalError || dType == DDialogType.FatalError) {
      String heading = DOMINO_ERROR + (msg.isEmpty() ? "" : "<br/><br/>");
      a.setHeading(heading);
      a.setType(AlertType.ERROR);
    } else if (dType == DDialogType.Info) {
      a.setType(AlertType.INFO);
    } else if (dType == DDialogType.RefreshToken) {
      a.setHeading("Warning:<br/><br/>");
    } else if (dType == DDialogType.Warning) {
      // Do not user a default header for warning messages
      //a.setHeading("Warning:<br/><br/>");
    }

    if (dType != DDialogType.FatalError && dType != DDialogType.Loading) {
      a.setClose(true);
      a.addClosedHandler(event -> doHide(m));
    } else {
      a.setClose(false);
    }
    m.add(a);
    m.setKeyboard(false);
    if (dType != DDialogType.FatalError && dType != DDialogType.Loading) {
      Button closeBtn = new Button("Close");
      if (dType == DDialogType.NonFatalError) {
        closeBtn.setType(ButtonType.DANGER);
      } else if (dType == DDialogType.RefreshToken || dType == DDialogType.Warning) {
        closeBtn.setType(ButtonType.WARNING);
      } else {
        closeBtn.setType(ButtonType.INFO);
      }
      closeBtn.addClickHandler(arg0 -> doHide(m));
      a.add(closeBtn);
    }

    m.setBackdrop(BackdropType.STATIC);

    m.setCloseVisible(false);
    //	m.addStyleName("alert-modal");
    m.show();
    return m;
  }

  private void doHide(Modal m) {
    m.hide();
    Scheduler.get().scheduleDeferred(parentHelper::getUserPermissions);
  }

  private String getDefaultSuffix(DDialogType dType) {
    switch (dType) {
      case FatalError:
        return "<br/><br/>An administrator has been notified.<br/>" +
            "Please wait a few minutes and reload your browser window.<br/>";
      case Loading:
        return "<br/><br/><img src='" +
            LANGTEST_IMAGES+
            "ajax-loader.gif'/>";
      case NonFatalError:
        return "<br/><br/>An administrator has been notified.<br/>" +
            "Please reload your browser if you <br/>" +
            "continue to experience this issue.<br/><br/>";
      case Info:
        return "<br/><br/>";
      case Warning:
        return "<br/><br/>";
      case RefreshToken:
        return "The last operation could not be completed " +
            "due to session expiration. Press Close to " +
            " reinitialize and return to the project list. " +
            " <br/><br/>If you continue to get this message, " +
            "log out, reload your browser, and log back in " +
            "again.<br/><br/>";
    }
    return "";
  }

  public Alert makeLoggedInlineMessage(String message, boolean autofade,
                                       AlertType aType) {
    return makeLoggedInlineMessage(message, new Throwable(), autofade, aType);
  }

  private Alert makeLoggedInlineMessage(String message, Throwable throwable,
                                        boolean autofade, AlertType aType) {
    logOnServer(message, throwable, true);
    return makeInlineMessage(message, autofade, aType);
  }

  private Alert makeInlineMessage(String message, boolean autofade,
                                  AlertType aType) {
    final Alert messageAlert = new Alert(message);
    //messageAlert.addStyleName("inline-message");
    if (aType == AlertType.WARNING) {
      messageAlert.setHeading("Warning: ");
    } else if (aType == AlertType.ERROR) {
      messageAlert.setHeading("Error: ");
    }
    messageAlert.setClose(true);
    messageAlert.setType(aType);
    messageAlert.setAnimation(true);
    addAlert(messageAlert, autofade);
    return messageAlert;
  }

  /**
   * Add the alert to the root pane and configure the autofade if necessary
   */
  private void addAlert(final Alert msgAlert, boolean autofade) {
    msgAlert.addCloseHandler(event -> RootPanel.get().remove(msgAlert));
    RootPanel.get().add(msgAlert);
    if (autofade) {
      int hideDelay = 3 * 1000; // 3 seconds.
      Timer t = new Timer() {
        public void run() {
          msgAlert.close();
        }
      };
      // Schedule the timer to run in 2 seconds.
      t.schedule(hideDelay);
    }
  }

  /**
   * Clean up any old inline messages.
   */
  private static void cleanupInlineMessages() {
    RootPanel rp = RootPanel.get();
    for (Widget child : rp) {
      if (child instanceof Alert) {
        ((Alert) child).close();
      }
    }
  }

  public void setupExceptionHandler() {
    GWT.setUncaughtExceptionHandler(e -> handleNonFatalError("", e));
  }

  public void logOnServer(Throwable throwable, boolean email) {
    logOnServer("", throwable, email);
  }

  private void logOnServer(String message, Throwable throwable, boolean email) {
    log.log(Level.INFO, "log to server " + throwable.getMessage() + " doing " + message);
    exceptionSupport.logMessageOnServer(message, "", email);
  }

/*	private CMessage makeMessage(String msgStr, Throwable throwable) {
		Throwable unwrapped = unwrap(throwable);
		log.log(Level.SEVERE, "Unwrapped", unwrapped);
		
		CMessage cMsg = new CMessage(msgStr, unwrapped, true);
		if (parentHelper != null && parentHelper.getState() != null) {
			User u = parentHelper.getState().getCurrentUser();
			if (u != null) {
				cMsg.setUserDBID(u.getDocumentDBID());
				cMsg.setUserId(u.getUserId());
			}
			ClientProject p = parentHelper.getState().getCurrentProject();
			if (p != null) {
				cMsg.setProjId(p.getId());
				cMsg.setProjName(p.getName());
			}
			if (parentHelper.getState().getStandardMetadata() != null) {
				cMsg.setClientBuildDate(parentHelper.getState().getStandardMetadata().getBuildDate());
				cMsg.setClientBuildVers(parentHelper.getState().getStandardMetadata().getBuildVers());
			}

		}
		return cMsg;	
	}*/

 /* private Throwable unwrap(Throwable e) {
    if (e instanceof UmbrellaException) {
      UmbrellaException ue = (UmbrellaException) e;
      if (ue.getCauses().size() == 1) {
        return unwrap(ue.getCauses().iterator().next());
      }
    }
    return e;
  }*/
}
