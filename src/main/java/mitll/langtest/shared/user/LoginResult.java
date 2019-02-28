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

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * LoginResult
 *
 * @author Raymond Budd <a href="mailto:raymond.budd@ll.mit.edu">raymond.budd@ll.mit.edu</a>
 * @since Oct 15, 2014 11:59:31 PM
 */

public class LoginResult implements IsSerializable {
  public enum ResultType implements IsSerializable {
    Success,
    Failed,
    BadPassword,
    //SessionNotRestored,
    //SessionExpired,
    MissingInfo,
    Updated,
    Added,
    /**
     * @see mitll.langtest.client.user.SignUpForm#handleAddUserResponse
     * @see mitll.langtest.server.services.OpenUserServiceImpl#addUser
     */
    Exists,
    ExistsValid,
    Multiple,
    Email,

    /**
     * ERROR
     */
    Unknown
  }

  private int id;
  private String userID;
  private String token;
  private User loggedInUser;
  private ResultType resultType = ResultType.Unknown;

  public LoginResult() {
  }

  /**
   * @see mitll.langtest.server.database.user.DominoUserDAOImpl#addUser(int, MiniUser.Gender, int, String, String, String, String, String, boolean, Collection, Kind, String, String, String, String, String, String, String)
   * @param id
   * @param token
   */
  public LoginResult(int id, String token) {
    this.id = id;
    this.token = token;
  }

  public LoginResult(ResultType resultType) {
    this(null, resultType);
  }

  public LoginResult(ResultType resultType, String userID) {
    this(null, resultType);
    this.userID = userID;
  }

  public LoginResult(User loggedInUser) {
    this(loggedInUser, ResultType.Success);
  }

  public LoginResult(User loggedInUser, ResultType resultType) {
    this.loggedInUser = loggedInUser;
    this.resultType = resultType;
  }

  public String getUserID() {
    return userID;
  }

  public LoginResult setUserID(String userID) {
    this.userID = userID;
    return this;
  }

  public int getId() {
    return id;
  }

  /**
   * @see mitll.langtest.server.database.user.BaseUserDAO#addUser
   * @return
   */
  public String getToken() {
    return token;
  }

  public User getLoggedInUser() {
    return loggedInUser;
  }

  public ResultType getResultType() {
    return resultType;
  }

  public String toString() {
    return "Login result " + resultType + " : " + loggedInUser + " or " + getUserID();
  }
}
