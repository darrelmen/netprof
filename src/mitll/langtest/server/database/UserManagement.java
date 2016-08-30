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

package mitll.langtest.server.database;

import java.io.OutputStream;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 9/18/15.
 * @deprecated
 */
public class UserManagement {
 /* private static final Logger logger = Logger.getLogger(UserManagement.class);
  public static final String NPF_CLASSROOM_PREFIX = "https://np.ll.mit.edu/npfClassroom";
  public static final String NO_USER = "-1";

  private final int numExercises;
  private final UserDAO userDAO;
  private final ResultDAO resultDAO;
  private final UserListManager userListManager;

  public UserManagement(UserDAO userDAO, int numExercises, ResultDAO resultDAO, UserListManager userListManager) {
    this.userDAO = userDAO;
    this.numExercises = numExercises;
    this.resultDAO = resultDAO;
    this.userListManager = userListManager;
  }

  *//**
   * Check other sites to see if the user exists somewhere else, and if so go ahead and use that person
   * here.
   *
   * TODO : read the list of sites from a file
   *
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
   * @see mitll.langtest.client.user.UserPassLogin#makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   * @see mitll.langtest.server.LangTestDatabaseImpl#userExists(String, String)
   *//*
  public User userExists(HttpServletRequest request, String login, String passwordH, ServerProperties props) {
    User user = userDAO.getUser(login, passwordH);

    if (user == null && !passwordH.isEmpty()) {
      logger.debug("userExists : checking '" + login + "'");

      for (String site : props.getSites()) {
        String url = NPF_CLASSROOM_PREFIX + site.replaceAll("Mandarin", "CM") + "/scoreServlet";
        String json = new HTTPClient().readFromGET(url + "?hasUser=" + login + "&passwordH=" + passwordH);

        if (!json.isEmpty()) {
          try {
            JSONObject jsonObject = JSONObject.fromObject(json);
            Object userid = jsonObject.get(RestUserManagement.USERID);
            Object pc = jsonObject.get(RestUserManagement.PASSWORD_CORRECT);

            if (userid == null) {
              logger.warn("huh? got back " + json + " for req " + login + " pass " +passwordH);
            }
            else {
              if (!userid.toString().equals(NO_USER)) {
                logger.info(site + " : found user " + userid);

                if (pc.toString().equals("true")) {
                  logger.info("\tmatching password for " + site);

                  String ip = getIPInfo(request);
                  Object emailH = jsonObject.get(RestUserManagement.EMAIL_H);
                  Object kind = jsonObject.get(RestUserManagement.KIND);
                  User.Kind realKind = kind == null ? User.Kind.STUDENT : User.Kind.valueOf(kind.toString());

                  user = addUser(login, passwordH, emailH.toString(), "browser", ip, realKind, true);
                  break;
                }
              }
            }
          } catch (Exception e) {
            logger.error("Got " + e, e);
          }
        }
      }
    }

    return user;
  }

  *//**
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param deviceType
   * @param device
   * @return
   * @see mitll.langtest.server.ScoreServlet#doPost
   *//*
  public User addUser(String userID, String passwordH, String emailH, String deviceType, String device) {
    boolean isMale = true;
    User.Kind kind = User.Kind.STUDENT;
    return addUser(userID, passwordH, emailH, deviceType, device, kind, isMale);
  }

  private User addUser(String userID, String passwordH, String emailH, String deviceType, String device, User.Kind kind,
                       boolean isMale) {
    int age = 89;
    String dialect = "unk";
    return addUser(userID, passwordH, emailH, deviceType, device, kind, isMale, age, dialect);
  }

  public User addUser(String userID, String passwordH, String emailH, String deviceType, String device, User.Kind kind,
                      boolean isMale, int age, String dialect) {
    return addAndGetUser(userID, passwordH, emailH, kind, isMale, age, dialect, deviceType, device);
  }

  *//**
   * @param request
   * @param userID
   * @param passwordH
   * @param emailH
   * @param kind
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#addUser
   *//*
  public User addUser(HttpServletRequest request, String userID, String passwordH, String emailH, User.Kind kind,
                      boolean isMale, int age, String dialect, String device) {
    String ip = getIPInfo(request);
    return addUser(userID, passwordH, emailH, device, ip, kind, isMale, age, dialect);
  }

  private User addAndGetUser(String userID, String passwordH, String emailH, User.Kind kind, boolean isMale, int age, String dialect, String device, String ip) {
    User user = userDAO.addUser(userID, passwordH, emailH, kind, ip, isMale, age, dialect, device);
    if (user != null) {
      userListManager.createFavorites(user.getId());
    }
    return user;
  }

  *//**
   * @param user
   * @param doThrow
   * @return
   * @see DatabaseImpl#addUser(User)
   *//*
  public long addUser(User user, boolean doThrow) {
    long l;
    String userID = user.getUserID();
    if ((l = userDAO.userExists(userID)) == -1) {
      logger.debug("addUser " + userID + " : " +new Date(user.getTimestampMillis()));
      try {
        l = userDAO.addUser(user.getAge(),
            user.getGender() == 0 ? UserDAO.MALE : UserDAO.FEMALE,
            user.getExperience(),
            user.getIpaddr(),
            user.getNativeLang(),
            user.getDialect(),
            userID,
            user.isEnabled(),
            user.getPermissions(),
            user.getUserKind(),
            user.getPasswordHash(),
            user.getEmailHash(),
            user.getDevice(),
            user.getTimestampMillis(), doThrow);
      } catch (SQLException e) {
        logger.error("Got " + e,e);
      }
    }
    return l;
  }

  *//**
   * Somehow on subsequent runs, the ids skip by 30 or so?
   * <p/>
   * Uses return generated keys to get the user id
   *
   * JUST FOR TESTING
   *
   * @param age
   * @param gender
   * @param experience
   * @param ipAddr      user agent info
   * @param dialect     speaker dialect
   * @param permissions
   * @param device
   * @return assigned id
   *//*
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions,
                      String device) {
    logger.debug("addUser " + userID);
    long l = 0;
    try {
      l = userDAO.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID, false, permissions,
          User.Kind.STUDENT, "", "", device, System.currentTimeMillis(), false);
    } catch (SQLException e) {
      logger.error("got " +e,e);
    }
    userListManager.createFavorites(l);
    return l;
  }

  private String getIPInfo(HttpServletRequest request) {
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    return request.getRemoteHost() +*//*"/"+ request.getRemoteAddr()+*//*(header != null ? "/" + header : "") +
        " at " + format;
  }

  *//**
   * @see mitll.langtest.server.database.DatabaseImpl#usersToXLSX(OutputStream)
   * @param out
   *//*
  void usersToXLSX(OutputStream out) {  userDAO.toXLSX(out, getUsers());  }
  JSON usersToJSON() { return userDAO.toJSON(getUsers());  }

  *//**
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUsers()
   * @see #usersToXLSX(OutputStream)
   *//*
  public List<User> getUsers() {
    Map<Long, Float> userToRate = resultDAO.getSessions().userToRate;
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
          int size = idToCount.idToUniqueCount.get(u.getId()).size();
          boolean complete = size >= numExercises;
          u.setComplete(complete);
          u.setCompletePercent(Math.min(1.0f, (float) size / (float) numExercises));
*//*          logger.debug("user " +u + " : results "+numResults + " unique " + size +
            " vs total exercises " + total + " complete " + complete);*//*
        }
      }
    } catch (Exception e) {
      logger.error("Got " + e, e);
    }

    return users;
  }

  *//**
   * So multiple recordings for the same item are counted as 1.
   * @return
   * @see #getUsers
   *//*
  private Pair populateUserToNumAnswers() {
    Map<Long, Integer> idToCount = new HashMap<Long, Integer>();
    Map<Long, Set<String>> idToUniqueCount = new HashMap<Long, Set<String>>();
    for (UserAndTime result : resultDAO.getUserAndTimes()) {
      long userid = result.getUserid();
      String exerciseID = result.getExid();

      Integer count = idToCount.get(userid);
      if (count == null) idToCount.put(userid, 1);
      else idToCount.put(userid, count + 1);

      Set<String> uniqueForUser = idToUniqueCount.get(userid);
      if (uniqueForUser == null) idToUniqueCount.put(userid, uniqueForUser = new HashSet<String>());
      uniqueForUser.add(exerciseID);
    }
    return new Pair(idToCount, idToUniqueCount);
  }

  private static class Pair {
    final Map<Long, Integer> idToCount;
    final Map<Long, Set<String>> idToUniqueCount;

    public Pair(Map<Long, Integer> idToCount, Map<Long, Set<String>> idToUniqueCount) {
      this.idToCount = idToCount;
      this.idToUniqueCount = idToUniqueCount;
    }
  }*/
}
