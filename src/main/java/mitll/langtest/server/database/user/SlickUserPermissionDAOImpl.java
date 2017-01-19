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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickUserPermission;
import mitll.npdata.dao.permission.UserPermissionDAOWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SlickUserPermissionDAOImpl extends DAO {//} implements IUserPermissionDAO {
  private final UserPermissionDAOWrapper dao;

  public SlickUserPermissionDAOImpl(Database database, DBConnection dbConnection) {
    super(database);
    this.dao = new UserPermissionDAOWrapper(dbConnection);
  }

 /* @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public void createTable() {
    dao.createTable();
  }

  @Override
  public Collection<SlickUserPermission> getUserPermissions() {
    return dao.all();
  }

  @Override
  public Collection<SlickUserPermission> getPendingPermissions() {
    return dao.pending();
  }

  @Override
  public void grant(int id, int changedby) {
    dao.grant(id, changedby);
  }

  @Override
  public void deny(int id, int changedby) {
    dao.deny(id, changedby);
  }

  @Override
  public void insert(SlickUserPermission e) {  dao.insert(e);  }

  public Map<Integer, Collection<String>> granted() {
    return dao.granted();
  }

  @Override
  public Collection<SlickUserPermission> grantedForUser(int userid) {
    return dao.grantedPermsForUser(userid);
  }

//  @Override
//  public Collection<SlickUserPermission> forUser(int userid) {
//    return dao.forUser(userid);
//  }

  public Collection<User.Permission> getGrantedForUser(int id) {
    Collection<String> strings = dao.grantedForUser(id);
    List<User.Permission> perms = new ArrayList<>();
    for (String perm : strings) {
      perms.add(User.Permission.valueOf(perm));
    }
    return perms;
  }*/
}
