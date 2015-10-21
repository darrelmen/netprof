package mitll.langtest.server.database;

import mitll.langtest.server.audio.HTTPClient;
import mitll.langtest.server.database.custom.UserListManager;
import mitll.langtest.server.database.exercise.ExerciseDAO;
import mitll.langtest.server.rest.RestUserManagement;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.User;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by go22670 on 9/18/15.
 */
public class UserManagement {
  private static final Logger logger = Logger.getLogger(UserManagement.class);

  private final List<String> sites = Arrays.asList("Dari", "Egyptian", "English", "Farsi", "Korean", "Levantine",
      "Mandarin",
      "MSA", "Pashto1", "Pashto2", "Pashto3", "Spanish", "Sudanese", "Tagalog", "Urdu");

  private final ExerciseDAO exerciseDAO;
  private final UserDAO userDAO;
  private final ResultDAO resultDAO;
  private final UserListManager userListManager;

  public UserManagement(UserDAO userDAO, ExerciseDAO exerciseDAO, ResultDAO resultDAO, UserListManager userListManager) {
    this.userDAO = userDAO;
    this.exerciseDAO = exerciseDAO;
    this.resultDAO = resultDAO;
    this.userListManager = userListManager;
  }

  /**
   * Check other sites to see if the user exists somewhere else, and if so go ahead and use that person
   * here.
   *
   * @param login
   * @param passwordH
   * @return
   * @see mitll.langtest.client.user.UserPassLogin#gotLogin
   * @see mitll.langtest.client.user.UserPassLogin#makeSignInUserName(com.github.gwtbootstrap.client.ui.Fieldset)
   * @see mitll.langtest.server.LangTestDatabaseImpl#userExists(String, String)
   */
  public User userExists(HttpServletRequest request, String login, String passwordH) {
    User user = userDAO.getUser(login, passwordH);

    if (user == null && !passwordH.isEmpty()) {
      logger.debug("userExists : checking '" + login + "'");

      for (String site : sites) {
        String url = "https://np.ll.mit.edu/npfClassroom" + site.replaceAll("Mandarin", "CM") + "/scoreServlet";
        String json = new HTTPClient("", 0).readFromGET(url + "?hasUser=" + login + "&passwordH=" + passwordH);

        if (!json.isEmpty()) {
          try {
            JSONObject jsonObject = JSONObject.fromObject(json);
            Object userid = jsonObject.get(RestUserManagement.USERID);
            Object pc = jsonObject.get(RestUserManagement.PASSWORD_CORRECT);

            if (!userid.toString().equals("-1")) {
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
          } catch (Exception e) {
            logger.error("Got " + e, e);
          }
        }
      }
    }

    return user;
  }

  /**
   *
   * @param userID
   * @param passwordH
   * @param emailH
   * @param deviceType
   * @param device
   * @return
   * @see mitll.langtest.server.ScoreServlet#doPost
   */
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

  /**
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
   */
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

  /**
   * @param user
   * @return
   * @seex mitll.langtest.server.database.ImportCourseExamples#copyUser
   */
  public long addUser(User user) {
    long l;
    if ((l = userDAO.userExists(user.getUserID())) == -1) {
      logger.debug("addUser " + user);
      l = userDAO.addUser(user.getAge(), user.getGender() == 0 ? UserDAO.MALE : UserDAO.FEMALE,
          user.getExperience(), user.getIpaddr(), user.getNativeLang(), user.getDialect(), user.getUserID(), false,
          user.getPermissions(), User.Kind.STUDENT, "", "", "");
    }
    return l;
  }

  /**
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
   */
  public long addUser(int age, String gender, int experience, String ipAddr,
                      String nativeLang, String dialect, String userID, Collection<User.Permission> permissions, String device) {
    logger.debug("addUser " + userID);
    long l = userDAO.addUser(age, gender, experience, ipAddr, nativeLang, dialect, userID, false, permissions, User.Kind.STUDENT, "", "", device);
    userListManager.createFavorites(l);
    return l;
  }

  private String getIPInfo(HttpServletRequest request) {
    String header = request.getHeader("User-Agent");
    SimpleDateFormat sdf = new SimpleDateFormat();
    String format = sdf.format(new Date());
    return request.getRemoteHost() +/*"/"+ request.getRemoteAddr()+*/(header != null ? "/" + header : "") + " at " + format;
  }

  /**
   * @see mitll.langtest.server.DownloadServlet
   * @param out
   */
  public void usersToXLSX(OutputStream out) {  userDAO.toXLSX(out, getUsers());  }

  /**
   * Adds some sugar -- sets the answers and rate per user, and joins with dli experience data
   *
   * @return
   * @see mitll.langtest.server.LangTestDatabaseImpl#getUsers()
   */
  public List<User> getUsers() {
    Map<Long, Float> userToRate = resultDAO.getSessions().userToRate;
    List<User> users = null;
    try {
      Pair idToCount = populateUserToNumAnswers();
      users = userDAO.getUsers();
      int total = exerciseDAO.getRawExercises().size();
      for (User u : users) {
        Integer numResults = idToCount.idToCount.get(u.getId());
        if (numResults != null) {
          u.setNumResults(numResults);

          if (userToRate.containsKey(u.getId())) {
            u.setRate(userToRate.get(u.getId()));
          }
          int size = idToCount.idToUniqueCount.get(u.getId()).size();
          boolean complete = size >= total;
          u.setComplete(complete);
          u.setCompletePercent(Math.min(1.0f, (float) size / (float) total));
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
   */
  private Pair populateUserToNumAnswers() {
    Map<Long, Integer> idToCount = new HashMap<Long, Integer>();
    Map<Long, Set<String>> idToUniqueCount = new HashMap<Long, Set<String>>();
    for (Result r : resultDAO.getResults()) {
      Integer count = idToCount.get(r.getUserid());
      if (count == null) idToCount.put(r.getUserid(), 1);
      else idToCount.put(r.getUserid(), count + 1);

      Set<String> uniqueForUser = idToUniqueCount.get(r.getUserid());
      if (uniqueForUser == null) idToUniqueCount.put(r.getUserid(), uniqueForUser = new HashSet<String>());
      uniqueForUser.add(r.getExerciseID());
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
  }
}
