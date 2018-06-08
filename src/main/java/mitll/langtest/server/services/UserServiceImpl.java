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

package mitll.langtest.server.services;

import mitll.hlt.domino.server.util.ServletUtil;
import mitll.langtest.client.domino.user.ChangePasswordView;
import mitll.langtest.client.initial.InitialUI;
import mitll.langtest.client.services.UserService;
import mitll.langtest.shared.common.DominoSessionException;
import mitll.langtest.shared.common.RestrictedOperationException;
import mitll.langtest.shared.user.ActiveUser;
import mitll.langtest.shared.user.FirstLastUser;
import mitll.langtest.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * These calls require a session.
 */
@SuppressWarnings("serial")
public class UserServiceImpl extends MyRemoteServiceServlet implements UserService {
  private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

  @Override
  public List<ActiveUser> getUsersSince(long when) throws DominoSessionException, RestrictedOperationException {
    int userIDFromSessionOrDB = getUserIDFromSessionOrDB();
    if (hasAdminPerm(userIDFromSessionOrDB)) {
      return securityManager.getActiveSince(when);
    } else return Collections.emptyList();
  }

  /**
   * @see InitialUI#logout
   */
  public void logout() throws DominoSessionException {
    int sessionUserID = getSessionUserID();
    if (sessionUserID == -1) {
      logger.warn("logout : no session user on logout?");
    } else {
      logger.info("logout : logging out " + sessionUserID);
      securityManager.logoutUser(getThreadLocalRequest(), sessionUserID, true);
    }
  }

  /**
   * consider stronger passwords like in domino.
   *
   * @param currentHashedPassword
   * @param newHashedPassword
   * @return
   * @see ChangePasswordView#changePassword
   */
  public boolean changePasswordWithCurrent(String currentHashedPassword, String newHashedPassword) throws DominoSessionException {
    int userIDFromSession = getUserIDFromSessionOrDB();
    User userWhereResetKey = db.getUserDAO().getByID(userIDFromSession);
    return
        userWhereResetKey != null &&
            (db.getUserDAO().changePasswordWithCurrent(userIDFromSession, currentHashedPassword, newHashedPassword, getBaseURL()));

  }

  /**
   * Try to make it easier to mark users as teachers via netprof-help.
   *
   * @throws DominoSessionException
   */
  @Override
  public void sendTeacherRequest() throws DominoSessionException {
    User userFromSession = getUserFromSession();
    getMailSupport().sendHTMLEmail(serverProps.getHelpEmail(), serverProps.getMailReplyTo(),
        "Instructor Status Request",
        "Hi,<br/>" +
            " A Netprof user<br/>" +
            "<br/> * named <b>" + userFromSession.getName() + "</b>" +
            "<br/> * user id <b>" + userFromSession.getID() + "</b>" +
            "<br/> * email <b>" + userFromSession.getEmail() + "</b>" +
            " has requested instructor permissions in Netprof." +
            "<br/><br/>If this person is an instructor, please go to " +

            "<a href='https://" + serverProps.getDominoServer() + "'>" + serverProps.getDominoServer() + "</a>" +

            " and " +
            "<br/> find their user profile, choose Roles, and click on Teacher." +
            "<br/>Finally, perhaps consider sending them a confirmation email." +
            "<br/>Thanks,<br/> Netprof Administrator"
    );
  }

  private String getBaseURL() {
    return ServletUtil.get().getBaseURL(getThreadLocalRequest());
  }
}
