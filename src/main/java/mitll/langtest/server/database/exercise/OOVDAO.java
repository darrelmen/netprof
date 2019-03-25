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

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.shared.exercise.OOV;
import mitll.langtest.shared.project.Language;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickOOV;
import mitll.npdata.dao.userexercise.OOVDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @see DatabaseImpl#initializeDAOs
 */
public class OOVDAO implements IOOVDAO {
  private static final Logger logger = LogManager.getLogger(OOVDAO.class);

  private final OOVDAOWrapper dao;

  /**
   * @param dbConnection
   * @see DatabaseImpl#initializeDAOs
   */
  public OOVDAO(DBConnection dbConnection) {
    dao = new OOVDAOWrapper(dbConnection);
  }

  @Override
  public boolean updateProject(int oldID, int newprojid) {
    return false;
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public void insertBulk(Collection<OOV> oovs, int userid, Language language) {
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    List<SlickOOV> slickOOVS = new ArrayList<>(oovs.size());
    oovs.forEach(oov -> slickOOVS.add(new SlickOOV(-1, userid, language.getLanguage(), modified, oov.getOOV(), "")));
    dao.addBulk(slickOOVS);
  }

  @Override
  public boolean delete(int id) {
    return dao.delete(id) > 0;
  }

  @Override
  public boolean insert(int userid, String oov, Language language) {
    Timestamp modified = new Timestamp(System.currentTimeMillis());
    return dao.insert(new SlickOOV(-1,
        userid,
        language.getLanguage(),
        modified,
        oov,
        "")) > 0;
  }

  @Override
  public boolean update(int id, String equivalent, int byUser) {
    return dao.update(id, equivalent, byUser) > 0;
  }

  @Override
  public List<OOV> forLanguage(Language language) {
    List<SlickOOV> slickOOVS = dao.forLanguage(language.getLanguage());
    List<OOV> oovs = new ArrayList<>(slickOOVS.size());
    slickOOVS.forEach(slickOOV -> oovs.add(new OOV(slickOOV.id(), slickOOV.userid(), slickOOV.modified().getTime(), slickOOV.oov(), slickOOV.equivalent())));
    return oovs;
  }

  @Override
  public Map<String, List<OOV>> getOOVToEquivalents(Language language) {
    return forLanguage(language).stream().collect(Collectors.groupingBy(OOV::getOOV));
  }
}
