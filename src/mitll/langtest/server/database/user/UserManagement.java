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

package mitll.langtest.server.database.user;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.custom.IUserListManager;
import mitll.langtest.server.database.excel.UserDAOToExcel;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.user.User;
import net.sf.json.JSON;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/18/15.
 */
public class UserManagement {
  private static final Logger logger = Logger.getLogger(UserManagement.class);
  private final IUserDAO userDAO;
  private final IResultDAO resultDAO;
  private final IUserListManager userListManager;

  /**
   * @see DatabaseImpl#makeDAO(String, String, String)
   * @param userDAO
   * @param resultDAO
   * @param userListManager
   */
  public UserManagement(IUserDAO userDAO, IResultDAO resultDAO, IUserListManager userListManager) {
    this.userDAO = userDAO;
    this.resultDAO = resultDAO;
    this.userListManager = userListManager;
  }


  /**
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param email
   *@param deviceType
   * @param device
   * @param projid    @return
   * @see mitll.langtest.server.ScoreServlet#doPost
   */
  public User addUser(String userID, String passwordH, String emailH, String email, String deviceType, String device,
                      int projid) {
    return addUser(userID, passwordH, emailH, email, deviceType, device, User.Kind.STUDENT, true, projid);
  }

  private User addUser(String userID, String passwordH, String emailH, String email, String deviceType, String device,
                       User.Kind kind,
                       boolean isMale, int projid) {
    int age = 89;
    String dialect = "unk";
    return addUser(userID, passwordH, emailH, email, deviceType, device, kind, isMale, age, dialect, projid);
  }

  /**
   * @see DatabaseImpl#addUser
   * @param userID
   * @param passwordH
   * @param emailH
   * @param email
   * @param deviceType
   * @param device
   * @param kind
   * @param isMale
   * @param age
   * @param dialect
   * @param projid
   * @return
   */
  public User addUser(String userID, String passwordH, String emailH, String email,
                      String deviceType, String device, User.Kind kind,
                      boolean isMale, int age, String dialect, int projid) {
    return addAndGetUser(userID, passwordH, emailH, email, kind, isMale, age, dialect, deviceType, device, projid);
  }

  /**
   * @param request
   * @param userID
   * @param passwordH
   * @param emailH
   * @param email
   * @param kind
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @return
   * @seex mitll.langtest.server.LangTestDatabaseImpl#addUser
   */
  public User addUser(HttpServletRequest request, String userID, String passwordH,
                      String emailH, String email, User.Kind kind,
                      boolean isMale, int age, String dialect, String device, int projid) {
    String ip = getIPInfo(request);
    return addUser(userID, passwordH, emailH, email, device, ip, kind, isMale, age, dialect, projid);
  }

  private User addAndGetUser(String userID, String passwordH, String emailH, String email, User.Kind kind, boolean isMale, int age,
                             String dialect, String device, String ip, int projid) {
    User user = userDAO.addUser(userID, passwordH, emailH, email, kind, ip, isMale, age, dialect, device);
    if (user != null) {
      userListManager.createFavorites(user.getId(), projid);
    }
    return user;
  }

  /**
   * @paramx user
   * @return
   * @see DatabaseImpl#addUser
   */
/*  public int addUser(User user) {
    int l;
    if ((l = userDAO.getIdForUserID(user.getUserID())) == -1) {
      logger.debug("addUser " + user);
      l = userDAO.addUser(user.getAge(), user.getGender() == 0 ? BaseUserDAO.MALE : BaseUserDAO.FEMALE,
          user.getExperience(), user.getIpaddr(), "", user.getNativeLang(), user.getDialect(), user.getUserID(), false,
          user.getPermissions(), User.Kind.STUDENT, "", "", "");
    }
    return l;
  }*/

  private String getIPInfo(HttpServletRequest request) {
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    return request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") +
        " at " + format;
  }

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#usersToXLSX(OutputStream)
   * @param out
   * @param language
   */
  public void usersToXLSX(OutputStream out, String language) { new UserDAOToExcel().toXLSX(out, getUsers(), language); }
  public JSON usersToJSON() { return new UserDAOToExcel().toJSON(getUsers());  }

  /**
   * TODO : come back and re-examine percent complete in context of a project
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUsers
   * @see #usersToXLSX(OutputStream, String)
   */
  public List<User> getUsers() {
    Map<Integer, Float> userToRate = resultDAO.getSessions().getUserToRate();
    List<User> users = null;
    try {
      Pair idToCount = populateUserToNumAnswers();
      users = userDAO.getUsers();
      //int total = exerciseDAO.getRawExercises().size();
      for (User u : users) {
        Integer numResults = idToCount.idToCount.get(u.getId());
        if (numResults != null) {
          u.setNumResults(numResults);

          if (userToRate.containsKey(u.getId())) {
            u.setRate(userToRate.get(u.getId()));
          }
        //  int size = idToCount.idToUniqueCount.get(u.getId()).size();

          if (false) {
   /*         boolean complete = size >= numExercises;
            u.setComplete(complete);
            u.setCompletePercent(Math.min(1.0f, (float) size / (float) numExercises));*/
          }
/*          logger.debug("user " +u + " : results "+numResults + " unique " + size +
            " vs total exercises " + total + " complete " + complete);*/
        }
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    return users;
  }

  /**
   * So multiple recordings for the same item are counted as 1.
   * @return
   * @see #getUsers
   */
  private Pair populateUserToNumAnswers() {
    Map<Integer, Integer> idToCount = new HashMap<>();
    Map<Integer, Set<Integer>> idToUniqueCount = new HashMap<>();
    for (UserAndTime result : resultDAO.getUserAndTimes()) {
      int userid = result.getUserid();
      int exerciseID = result.getExid();

      Integer count = idToCount.get(userid);
      if (count == null) idToCount.put(userid, 1);
      else idToCount.put(userid, count + 1);

      Set<Integer> uniqueForUser = idToUniqueCount.get(userid);
      if (uniqueForUser == null) idToUniqueCount.put(userid, uniqueForUser = new HashSet<>());
      uniqueForUser.add(exerciseID);
    }
    return new Pair(idToCount, idToUniqueCount);
  }

  private static class Pair {
    final Map<Integer, Integer> idToCount;
    final Map<Integer, Set<Integer>> idToUniqueCount;

    Pair(Map<Integer, Integer> idToCount, Map<Integer, Set<Integer>> idToUniqueCount) {
      this.idToCount = idToCount;
      this.idToUniqueCount = idToUniqueCount;
    }
  }
}
