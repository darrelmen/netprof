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

package mitll.langtest.server.database.phone;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.shared.analysis.PhoneAndScore;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.BaseOps;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickPhone;
import mitll.npdata.dao.SlickPhoneReport;
import mitll.npdata.dao.phone.PhoneDAOWrapper;
import mitll.npdata.dao.phone.PhoneTable;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlickPhoneDAO extends BasePhoneDAO implements IPhoneDAO<Phone>, ISchema<Phone, SlickPhone> {
  private static final Logger logger = Logger.getLogger(SlickPhoneDAO.class);

  private final PhoneDAOWrapper dao;

  public SlickPhoneDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new PhoneDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    BaseOps dao = this.dao.dao();
    return dao.name();
  }

  @Override
  public SlickPhone toSlick(Phone shared, String language) {
    return new SlickPhone(-1,
        (int) shared.getRid(),
        (int) shared.getWid(),
        shared.getPhone(),
        shared.getSeq(),
        shared.getScore());
  }

  @Override
  public Phone fromSlick(SlickPhone slick) {
    return new Phone(
//        (long)slick.id(),
        (long) slick.rid(),
        slick.wid(),
        slick.phone(),
        slick.seq(),
        slick.score());
  }

  public void insert(SlickPhone word) {
    dao.insert(word);
  }

  public void addBulk(List<SlickPhone> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public boolean addPhone(Phone word) {
    return dao.insert(toSlick(word, "")) > 0;
  }

  /**
   * TODO : fill in!
   *
   * @param userid
   * @param exids
   * @param idToRef
   * @return
   */
  @Override
  public JSONObject getWorstPhonesJson(long userid, Collection<String> exids, Map<String, String> idToRef) {
    Collection<SlickPhoneReport> phoneReportByResult = dao.getPhoneReportByExercises((int) userid, exids);
    PhoneReport report = getPhoneReport(phoneReportByResult, idToRef, false, true);

   // logger.info("getWorstPhonesJson phone report " + report);

    return new PhoneJSON().getWorstPhonesJson(report);
  }

  /**
   * TODO complete this!
   *
   * @param userid
   * @param ids
   * @param idToRef
   * @return
   * @throws SQLException
   */
  @Override
  public PhoneReport getWorstPhonesForResults(long userid, Collection<Integer> ids, Map<String, String> idToRef) {
    Collection<SlickPhoneReport> phoneReportByResult = dao.getPhoneReportByResult((int) userid, ids);
    return getPhoneReport(phoneReportByResult, idToRef, true, false);
  }

  /**
   * TODO : huh? doesn't seem to add last item to total score or total items?
   *
   * @param idToRef
   * @param addTranscript       true if going to analysis tab
   * @param sortByLatestExample
   * @return
   * @throws SQLException
   * @paramx sql
   * @seex #getPhoneReport(String, Map, boolean, boolean)
   * @see IPhoneDAO#getWorstPhonesForResults(long, Collection, Map)
   */
  protected PhoneReport getPhoneReport(Collection<SlickPhoneReport> phoneReportByResult,
                                       Map<String, String> idToRef,
                                       boolean addTranscript,
                                       boolean sortByLatestExample) {
    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<>();

    String currentExercise = "";
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<>();

    float totalScore = 0;
    float totalItems = 0;

    Map<String, Map<NetPronImageType, List<TranscriptSegment>>> stringToMap = new HashMap<>();
    int c = 0;
    for (SlickPhoneReport report : phoneReportByResult) {
      int i = 1;
      c++;

     // logger.info("#"+ c + " : " + report);
      // info from result table
      String exid = report.exid();
      String scoreJson = report.scorejson();
      float pronScore = report.pronScore();

      if (!exid.equals(currentExercise)) {
        currentExercise = exid;
      //  logger.debug("#" +c+  " adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

      WordAndScore wordAndScore = getAndRememberWordAndScore(idToRef, phoneToScores, phoneToWordAndScore,
          exid, report.answer(), scoreJson, report.modified(),
          report.wseq(), report.word(),
          report.rid(), report.phone(), report.pseq(), report.pscore());

      if (addTranscript) {
        addTranscript(stringToMap, scoreJson, wordAndScore);
      }
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }

    return new MakePhoneReport().getPhoneReport(phoneToScores, phoneToWordAndScore, totalScore, totalItems, sortByLatestExample);
  }

  public int getNumRows() { return dao.getNumRows(); }
  public boolean isEmpty() { return getNumRows() == 0; }
}
