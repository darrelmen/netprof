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

package mitll.langtest.server.database.dliclass;

import mitll.langtest.shared.dliclass.DLIClass;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickDLIClass;
import mitll.npdata.dao.dliclass.DLIClassWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DLIClassDAO implements IDLIClassDAO {
  private static final Logger logger = LogManager.getLogger(DLIClassDAO.class);
  private static final String DEFAULT_PROJECT = "DEFAULT_PROJECT";

  private final DLIClassWrapper dao;

  /**
   * @param dbConnection
   * @paramx database
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public DLIClassDAO(DBConnection dbConnection) {
    dao = new DLIClassWrapper(dbConnection);
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  public List<DLIClass> getAll() {
    Collection<SlickDLIClass> all = dao.all();
    List<DLIClass> ret = new ArrayList<>();
    for (SlickDLIClass dliClass : all) ret.add(fromSlick(dliClass));
    return ret;
  }

  public List<DLIClass> getAllBy(int teacherid) {
    Collection<SlickDLIClass> all = dao.allBy(teacherid);
    List<DLIClass> ret = new ArrayList<>();
    for (SlickDLIClass dliClass : all) ret.add(fromSlick(dliClass));
    return ret;
  }

  private DLIClass fromSlick(SlickDLIClass dliClass) {
    return new DLIClass(
        dliClass.id(),
        dliClass.projid(),
        dliClass.ownerid(),
        dliClass.modified(),
        dliClass.classend(),
        dliClass.name(),
        dliClass.room(),
        dliClass.timeofday());
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }
}
