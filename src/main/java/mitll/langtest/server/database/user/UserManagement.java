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

package mitll.langtest.server.database.user;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.user.SignUpUser;
import mitll.langtest.shared.user.User;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

import static mitll.langtest.server.services.MyRemoteServiceServlet.USER_AGENT;

public class UserManagement {
  private static final Logger logger = LogManager.getLogger(UserManagement.class);
  private final IUserDAO userDAO;

  /**
   * @param userDAO
   * @see DatabaseImpl#makeDAO
   */
  public UserManagement(IUserDAO userDAO) {  this.userDAO = userDAO;  }

  /**
   * @param request
   * @param user
   * @return
   * @see mitll.langtest.server.services.OpenUserServiceImpl#addUser
   */
  public User addUser(HttpServletRequest request, SignUpUser user) {
    return userDAO.addUser(user.setIp(getIPInfo(request)));
  }

  /**
   *
   *
   * @param user
   * @return
   * @see #addUser(HttpServletRequest, SignUpUser)
   * @see mitll.langtest.server.rest.RestUserManagement#addUser(HttpServletRequest, String, String, String, JSONObject)
   */
  public User addUser(SignUpUser user) {    return userDAO.addUser(user);  }

  /**
   * @param request
   * @return
   * @see #addUser(HttpServletRequest, SignUpUser)
   */
  private String getIPInfo(HttpServletRequest request) {
    String header = request.getHeader(USER_AGENT);
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    return request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") +
        " at " + format;
  }

  /**
   * TODO : come back and re-examine percent complete in context of a project
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   * <p>
   * TODO : percent complete should be done from audio table, not result table
   * TODO : passing in empty language - is that OK
   *
   * @return
   * @see mitll.langtest.server.database.DatabaseImpl#getUsers
   * @see #usersToXLSX
   */
/*  public List<User> getUsers() {
    // TODO : this is really expensive - right now we reparse all the json when we get it out of the database.
    //Map<Integer, Float> userToRate = resultDAO.getSessions("").getUserToRate();
    List<User> users = null;
    try {
      long then = System.currentTimeMillis();
      UserToCount idToCount = resultDAO.getUserToNumAnswers();
      long now = System.currentTimeMillis();
      if (now - then > 20) logger.info("getUsers took " + (now - then) + " millis to get user->num answers");
      then = now;

      users = userDAO.getUsers();
      now = System.currentTimeMillis();
      if (now - then > 20) logger.info("getUsers took " + (now - then) + " millis to get " + users.size() + " users");

      Collections.sort(users);

      //int total = exerciseDAO.getRawExercises().size();
      for (User u : users) {
        Integer numResults = idToCount.getIdToCount().get(u.getID());
        if (numResults != null) {
          //u.setNumResults(numResults);

          //    if (userToRate.containsKey(u.getID())) {
          //     u.setRate(userToRate.get(u.getID()));
          //  }
          //  int size = idToCount.idToUniqueCount.get(u.getID()).size();

          // TODO : put this back
//          if (false) {
   *//*         boolean complete = size >= numExercises;
            u.setComplete(complete);
            u.setCompletePercent(Math.min(1.0f, (float) size / (float) numExercises));*//*
  //        }
*//*          logger.debug("user " +u + " : results "+numResults + " unique " + size +
            " vs total exercises " + total + " complete " + complete);*//*
        }
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    return users;
  }*/

  /**
   * So multiple recordings for the same item are counted as 1.
   * @return
   * @see #getUsers
   */
/*  public Pair populateUserToNumAnswers() {
    Map<Integer, Integer> idToCount = new HashMap<>();
    Map<Integer, Set<Integer>> idToUniqueCount = new HashMap<>();
    for (UserAndTime result : resultDAO.getUserAndTimes()) {
      int userid = result.getUserid();
      int exerciseID = result.getExid();

      Integer childCount = idToCount.get(userid);
      if (childCount == null) idToCount.put(userid, 1);
      else idToCount.put(userid, childCount + 1);

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
  }*/
}
