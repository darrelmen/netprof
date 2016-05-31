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

import mitll.langtest.server.database.Database;
import mitll.langtest.shared.MiniUser;
import mitll.langtest.shared.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickMiniUser;
import mitll.npdata.dao.SlickUser;
import mitll.npdata.dao.user.UserDAOWrapper;
import org.apache.log4j.Logger;
import scala.collection.Seq;

import java.sql.Timestamp;
import java.util.*;

public class SlickUserDAOImpl extends BaseUserDAO implements IUserDAO {
  private static final Logger logger = Logger.getLogger(SlickUserDAOImpl.class);
  private final UserDAOWrapper dao;

  public SlickUserDAOImpl(Database database, DBConnection dbConnection) {
    super(database);
    dao = new UserDAOWrapper(dbConnection);
  }

  @Override
  public int addUser(int age, String gender, int experience, String userAgent, String trueIP,
                     String nativeLang,
                     String dialect, String userID, boolean enabled,
                     Collection<User.Permission> permissions, User.Kind kind,
                     String passwordH, String emailH, String device) {
    StringBuilder builder = new StringBuilder();
    for (User.Permission permission : permissions) builder.append(permission).append(",");
    return dao.add(new SlickUser(-1, userID,
        gender.equalsIgnoreCase("male"),
        userAgent,
        trueIP,
        dialect,
        new Timestamp(System.currentTimeMillis()),
        enabled, "", "", builder.toString(),
        kind.toString(), passwordH, emailH, device));
  }

  protected void updateUser(int id, User.Kind kind, String passwordH, String emailH) {
    dao.updateUser(id, kind.name(), passwordH, emailH,
        kind == User.Kind.CONTENT_DEVELOPER ? CD_PERMISSIONS.toString() : EMPTY_PERM.toString());
  }

  @Override
  public boolean enableUser(int id) {
    return dao.enableUser(id);
  }

  @Override
  public String isValidEmail(String emailH) {
    List<String> usersWithEmail = dao.isValidEmail(emailH);
    return usersWithEmail.isEmpty() ? null : usersWithEmail.get(0);
  }

  @Override
  public Integer getIDForUserAndEmail(String user, String emailH) {
    List<Integer> idForUserAndEmail = dao.getIDForUserAndEmail(user, emailH);
    return idForUserAndEmail.isEmpty() ? null : idForUserAndEmail.get(0);
  }

  @Override
  public int getIdForUserID(String id) {
    return dao.idForUser(id);
  }

  @Override
  public User getUser(String id, String passwordHash) {
    Seq<SlickUser> userByIDAndPass = dao.getUserByIDAndPassOrFallback(id, passwordHash);
    return convertOrNull(userByIDAndPass);
  }

  private User convertOrNull(Seq<SlickUser> userByIDAndPass) {
    return userByIDAndPass.isEmpty() ? null : toUser(userByIDAndPass.head());
  }

  @Override
  public User getUserWithPass(String id, String passwordHash) {
    Seq<SlickUser> userByIDAndPass = dao.getUserByIDAndPass(id, passwordHash);
    return convertOrNull(userByIDAndPass);
  }

  @Override
  public User getUserByID(String id) {
    Seq<SlickUser> byUserID = dao.getByUserID(id);
    return convertOrNull(byUserID);
  }

  @Override
  public List<User> getUsers() {
    List<SlickUser> all = dao.getAll();
    return toUsers(all);
  }

  private List<User> toUsers(List<SlickUser> all) {
    List<User> copy = new ArrayList<>();
    for (SlickUser s : all)
      copy.add(toUser(s));
    return copy;
  }

  private User toUser(SlickUser s) {
    return new User(
        s.id(),
        89,
        s.ismale() ? 0 : 1,
        0,
        s.ipaddr(),
        s.passhash(),
        "",
        s.dialect(),
        s.userid(),
        s.enabled(),
        isAdmin(s.userid()),
        getPerm(s.permissions()),
        User.Kind.valueOf(s.kind()),
        s.emailhash(),
        s.device(),
        s.resetpasswordkey(),
        s.enabledreqkey(),
        s.modified().getTime()
    );
  }

  /**
   * OK this is kind of a hack, should be a separate table.
   *
   * @param perms
   * @return
   */
  private Collection<User.Permission> getPerm(String perms) {
    Collection<User.Permission> permissions = new ArrayList<>();

    if (perms != null) {
      perms = perms.replaceAll("\\[", "").replaceAll("\\]", "");
      for (String perm : perms.split(",")) {
        perm = perm.trim();
        try {
          if (!perm.isEmpty()) {
            permissions.add(User.Permission.valueOf(perm));
          }
        } catch (IllegalArgumentException e) {
          logger.warn(language + " : huh, for user " +// userid +
              " perm '" + perm +
              "' is not a permission?");
        }
      }
    }
    return permissions;
  }

  @Override
  public List<User> getUsersDevices() {
    return toUsers(dao.getUsersFromDevices());
  }

  @Override
  public Map<Integer, MiniUser> getMiniUsers() {
    Map<Integer, MiniUser> idToUser = new HashMap<>();
    for (SlickMiniUser s : dao.getAllSlim()) idToUser.put(s.id(), getMini(s));
    return idToUser;
  }

  private MiniUser getMini(SlickMiniUser s) {
    return new MiniUser(s.id(), 0, s.ismale(), s.userid(), isAdmin(s.userid()));
  }

  @Override
  public MiniUser getMiniUser(int userid) {
    List<SlickMiniUser> match = dao.getSlimFor(userid);
    return match.isEmpty() ? null : getMini(match.iterator().next());
  }

  @Override
  public User getUserWhereResetKey(String resetKey) {
    return convertOrNull(dao.getByReset(resetKey));
  }

  @Override
  public User getUserWhereEnabledReq(String resetKey) {
    return convertOrNull(dao.getByEnabledReq(resetKey));
  }

  @Override
  public User getUserWhere(int userid) {
    return convertOrNull(dao.byID(userid));
  }

  @Override
  public Map<Integer, User> getUserMap(boolean getMale) {
    Map<Integer, SlickUser> byMale = dao.getByMaleMap(getMale);
    Map<Integer, User> idToUser = new HashMap<>();
    byMale.forEach((k, v) -> idToUser.put(k, toUser(v)));
    return idToUser;
  }

  @Override
  public Map<Integer, User> getUserMap() {
    Map<Integer, User> idToUser = new HashMap<>();
    dao.getIdToUser().forEach((k, v) -> idToUser.put(k, toUser(v)));
    return idToUser;
  }

  @Override
  public boolean changePassword(Integer remove, String passwordH) {
    return dao.setPassword(remove, passwordH);
  }

  @Override
  public boolean updateKey(Integer userid, boolean resetKey, String key) {
    return dao.updateKey(userid,resetKey,key);
  }

  @Override
  public boolean clearKey(Integer remove, boolean resetKey) {
    return dao.updateKey(remove,resetKey,"");
  }

  @Override
  public boolean changeEnabled(int userid, boolean enabled) {
    return dao.changeEnabled(userid,enabled);
  }
}
