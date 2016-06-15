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
import mitll.langtest.server.database.userexercise.BaseUserExerciseDAO;
import mitll.langtest.shared.analysis.PhoneAndScore;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickPhone;
import mitll.npdata.dao.phone.PhoneDAOWrapper;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlickPhoneDAO
    extends BasePhoneDAO implements IPhoneDAO<Phone>, ISchema<Phone, SlickPhone> {
  private static final Logger logger = Logger.getLogger(SlickPhoneDAO.class);

  private final PhoneDAOWrapper dao;

  public SlickPhoneDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new PhoneDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

/*  public void dropTable() {
    dao.drop();
  }*/

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
   * @param userid
   * @param exids
   * @param idToRef
   * @return
   */
  @Override
  public JSONObject getWorstPhonesJson(long userid, List<String> exids, Map<String, String> idToRef) {
    PhoneReport report = new PhoneReport();
    return new PhoneJSON().getWorstPhonesJson(report);
  }

  /**
   * TODO complete this!
   * @param userid
   * @param ids
   * @param idToRef
   * @return
   * @throws SQLException
   */
  @Override
  public PhoneReport getWorstPhonesForResults(long userid, List<Integer> ids, Map<String, String> idToRef) throws SQLException {
    return null;
  }

  /**
   * TODO : huh? doesn't seem to add last item to total score or total items?
   *
   * @paramx sql
   * @param idToRef
   * @param addTranscript       true if going to analysis tab
   * @param sortByLatestExample
   * @return
   * @throws SQLException
   * @see #getWorstPhonesForResults(long, List, Map)
   * @seex #getPhoneReport(String, Map, boolean, boolean)
   */
  protected PhoneReport getPhoneReport(
                                       Map<String, String> idToRef,
                                       boolean addTranscript,
                                       boolean sortByLatestExample) throws SQLException {
    // logger.debug("getPhoneReport query is " + sql);
    Connection connection = getConnection();
    PreparedStatement statement = null;//connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();

    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<>();

    String currentExercise = "";
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<>();

    float totalScore = 0;
    float totalItems = 0;

    Map<String, Map<NetPronImageType, List<TranscriptSegment>>> stringToMap = new HashMap<>();
    while (rs.next()) {
      int i = 1;

      // info from result table
      String exid = rs.getString(i++);
      String audioAnswer = rs.getString(i++);
      String scoreJson = rs.getString(i++);
      float pronScore = rs.getFloat(i++);

      long resultTime = -1;
      Timestamp timestamp = rs.getTimestamp(i++);
      if (timestamp != null) resultTime = timestamp.getTime();

      // info from word table
      int wseq = rs.getInt(i++);
      String word = rs.getString(i++);
      /*float wscore =*/ //rs.getFloat(i++);

      // info from phone table
      long rid = rs.getLong(RID1);
//      logger.info("Got " + exid + " rid " + rid + " word " + word);
      String phone = rs.getString(PHONE);
      int seq = rs.getInt(SEQ);
      float phoneScore = rs.getFloat(SCORE);

      if (!exid.equals(currentExercise)) {
        currentExercise = exid;
        //logger.debug("adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

      WordAndScore wordAndScore = getAndRememberWordAndScore(idToRef, phoneToScores, phoneToWordAndScore,
          exid, audioAnswer, scoreJson, resultTime,
          wseq, word,
          rid, phone, seq, phoneScore);

      if (addTranscript) {
        addTranscript(stringToMap, scoreJson, wordAndScore);
      }
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }
    finish(connection, statement, rs);

    return new MakePhoneReport().getPhoneReport(phoneToScores, phoneToWordAndScore, totalScore, totalItems, sortByLatestExample);
  }

}
