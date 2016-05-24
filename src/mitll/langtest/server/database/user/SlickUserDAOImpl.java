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
import mitll.npdata.dao.user.SlickUser;
import mitll.npdata.dao.user.UserDAOWrapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SlickUserDAOImpl extends BaseUserDAO implements IUserDAO {
  UserDAOWrapper dao;

  public SlickUserDAOImpl(Database database) {
    super(database);
    dao = new UserDAOWrapper("localhost", 5432, "netprof");
  }

/*  @Override
  public User addUser(String userID, String passwordH, String emailH, User.Kind kind, String ipAddr, boolean isMale,
                      int age, String dialect, String device) {
    return null;
  }*/

  @Override
  public int addUser(int age, String gender, int experience, String userAgent, String trueIP,
                     String nativeLang,
                     String dialect, String userID, boolean enabled,
                     Collection<User.Permission> permissions, User.Kind kind,
                     String passwordH, String emailH, String device) {
    StringBuilder builder = new StringBuilder();
    for (User.Permission permission : permissions) builder.append(permission).append(",");
    int assignedID = dao.add(new SlickUser(-1, userID,
        gender.equalsIgnoreCase("male"),
        userAgent,
        trueIP,
        dialect,
        new Timestamp(System.currentTimeMillis()),
        enabled, "", "", builder.toString(),
        kind.toString(), passwordH, emailH, device));
    return assignedID;
  }

  protected void updateUser(int id, User.Kind kind, String passwordH, String emailH) {

  }

  @Override
  public boolean enableUser(int id) {
    return false;
  }

  @Override
  public User isValidEmail(String emailH) {
    return null;
  }

  @Override
  public User isValidUserAndEmail(String user, String emailH) {
    return null;
  }

  @Override
  public int userExists(String id) {
    return 0;
  }

  @Override
  public User getUser(String id, String passwordHash) {
    return null;
  }

  @Override
  public User getUserWithPass(String id, String passwordHash) {
    return null;
  }

  @Override
  public User getUserByID(String id) {
    return null;
  }

  @Override
  public List<User> getUsers() {
    List<SlickUser> all = dao.getAll();
    List<User> copy = new ArrayList<>();

    for (SlickUser s : all) copy.add(new User());

  }

  @Override
  public List<User> getUsersDevices() {
    return null;
  }

  @Override
  public Map<Integer, MiniUser> getMiniUsers() {
    return null;
  }

  @Override
  public MiniUser getMiniUser(int userid) {
    return null;
  }

  @Override
  public User getUserWhereResetKey(String resetKey) {
    return null;
  }

  @Override
  public User getUserWhereEnabledReq(String resetKey) {
    return null;
  }

  @Override
  public User getUserWhere(int userid) {
    return null;
  }

  @Override
  public Map<Integer, User> getUserMap(boolean getMale) {
    return null;
  }

  @Override
  public Map<Integer, User> getUserMap() {
    return null;
  }

  @Override
  public boolean changePassword(Integer remove, String passwordH) {
    return false;
  }

  @Override
  public boolean updateKey(Integer userid, boolean resetKey, String key) {
    return false;
  }

  @Override
  public boolean clearKey(Integer remove, boolean resetKey) {
    return false;
  }

  @Override
  public boolean changeEnabled(int userid, boolean enabled) {
    return false;
  }
}
