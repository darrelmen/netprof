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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.user.User;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickInvite;
import mitll.npdata.dao.user.InviteDAOWrapper;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SlickInviteDAOImpl extends DAO implements IInviteDAO {
  private static final Logger logger = Logger.getLogger(SlickInviteDAOImpl.class);
  private final InviteDAOWrapper dao;

  /**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs(PathHelper)
   */
  public SlickInviteDAOImpl(Database database, DBConnection dbConnection) {
    super(database);
    dao = new InviteDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  public Map<String, Integer> getInvitationCounts(User.Kind requestRole) {
    Collection<SlickInvite> all = dao.all();

    Map<String,Integer> typeToCount = new HashMap<>();
    for (SlickInvite invite:all) {
      User.Kind kind = getKind(invite.kind());
      if ((kind.compareTo(requestRole)<0)){
        String state = invite.state();
        Integer integer = typeToCount.get(state);
        typeToCount.put(state,integer==null?0:integer+1);
      }
    }

    return typeToCount;
  }

  private User.Kind getKind(String key) {
    return User.Kind.valueOf(key);
  }

  /**
   * @param invite
   * @return
   */
  @Override
  public int add(SlickInvite invite) {
    return dao.add(invite);
  }

  public void update(int id, String key) { dao.update(id,key);}

  @Override
  public Collection<SlickInvite> getPending() {
    return dao.pending();
  }

  public int getNumRows() {
    return dao.numRows();
  }
}