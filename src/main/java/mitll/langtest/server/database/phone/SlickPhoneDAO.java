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
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.analysis.PhoneReport;
import mitll.langtest.shared.analysis.UserInfo;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.ProjectType;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickPhone;
import mitll.npdata.dao.SlickPhoneReport;
import mitll.npdata.dao.phone.PhoneDAOWrapper;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class SlickPhoneDAO extends BasePhoneDAO implements IPhoneDAO<Phone> {
  private static final Logger logger = LogManager.getLogger(SlickPhoneDAO.class);

  private final PhoneDAOWrapper dao;

  private static final boolean DEBUG = false;

  public SlickPhoneDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new PhoneDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  public boolean updateProject(int old, int newprojid) {
    return dao.updateProject(old, newprojid) > 0;
  }

  @Override
  public String getName() {
    return this.dao.dao().name();
  }

  /**
   * @param shared
   * @param projID
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyPhone
   */
  public SlickPhone toSlick(Phone shared, int projID) {
    return new SlickPhone(-1,
        projID,
        shared.getRid(),
        shared.getWid(),
        shared.getPhone(),
        shared.getSeq(),
        shared.getScore(),
        shared.getDuration());
  }

  public Phone fromSlick(SlickPhone slick) {
    return new Phone(
        slick.projid(),
        slick.rid(),
        slick.wid(),
        slick.phone(),
        slick.seq(),
        slick.score(),
        slick.duration());
  }

  /**
   * @param bulk
   */
  public void addBulk(List<SlickPhone> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public void addBulkPhones(List<Phone> bulk, int projID) {
    List<SlickPhone> sbulk = new ArrayList<>(bulk.size());
    bulk.forEach(phone -> sbulk.add(toSlick(phone, projID)));
    dao.addBulk(sbulk);
  }

  /**
   * TODO : consider if we need to add transcript or not.
   *
   * @param userid
   * @param exids
   * @param language
   * @param project
   * @return
   * @see mitll.langtest.server.database.JsonSupport#getJsonPhoneReport
   */
  @Override
  public JSONObject getWorstPhonesJson(int userid,
                                       Collection<Integer> exids,
                                       String language,
                                       Project project) {
    Collection<SlickPhoneReport> phoneReportByExercises = dao.getPhoneReportByExercises(userid, exids);
    PhoneReport report = getPhoneReport(phoneReportByExercises, false, false,
        userid, project);
    logger.info("getWorstPhonesJson phone report for" +
        "\n\tuser          " + userid +
        "\n\texids         " + exids.size() +
        "\n\treport        " + report +
        "\n\tphone reports " + phoneReportByExercises.size()
    );
    return new PhoneJSON().getWorstPhonesJson(report);
  }

  /**
   * TODOx : don't use idToRef map
   *
   * @param userid
   * @param ids
   * @param project
   * @return
   * @throws SQLException
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReport
   */
  @Override
  public PhoneReport getWorstPhonesForResults(int userid,
                                              Collection<Integer> ids,
                                              Project project) {
    long then = System.currentTimeMillis();
    logger.info("getWorstPhonesForResults " + userid + " project " + project.getID() + " ids " + ids);
    Collection<SlickPhoneReport> phoneReportByResult = dao.getPhoneReportByResult(userid, ids);
    long now = System.currentTimeMillis();
    if (now - then > 0)
      logger.info("getWorstPhonesForResults took " + (now - then) + " to get " + phoneReportByResult.size());

//    then = System.currentTimeMillis();
//    Collection<SlickPhoneReport> phoneReportByResult2 = dao.getPhoneReportByResultForUser(userid, ids);
//    now = System.currentTimeMillis();
//    if (now - then > 1) {
//      logger.info("getWorstPhonesForResults getPhoneReportByResultForUser took " + (now - then) + " to get " + phoneReportByResult2.size());
//    }

    return getPhoneReport(phoneReportByResult, true, false, userid, project);
  }

  /**
   * @param userid
   * @param ids
   * @param project
   * @param phone
   * @param from
   * @param to
   * @return
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReportForPhone
   * @see mitll.langtest.server.database.analysis.Analysis#getPhoneReportForPhone
   */
  @Override
  public PhoneReport getWorstPhonesForResultsForPhone(int userid,
                                                      Collection<Integer> ids,
                                                      Project project,
                                                      String phone, long from, long to) {
    long then = System.currentTimeMillis();
    Collection<SlickPhoneReport> phoneReportByResult =
        dao.getPhoneReportByResultForPhone(userid, ids, phone, new Timestamp(from), new Timestamp(to));
    long now = System.currentTimeMillis();
    if (now - then > 200 || DEBUG)
      logger.info("getWorstPhonesForResultsForPhone took " + (now - then) + " to get " + phoneReportByResult.size());

    return getPhoneReport(phoneReportByResult, true, false, userid, project);
  }

  /**
   * TODO : huh? doesn't seem to add last item to total score or total items?
   * TODO : don't use idToRef map
   * <p>
   * Why get native audio here?
   * <p>
   * TODO : add transcript in smarter way!
   *
   * @param addTranscript       true if going to analysis tab
   * @param sortByLatestExample
   * @param userid
   * @param project
   * @return
   * @throws SQLException
   * @see #getWorstPhonesForResults
   * @see #getWorstPhonesForResultsForPhone(int, Collection, Project, String, long, long)
   */
  private PhoneReport getPhoneReport(Collection<SlickPhoneReport> phoneReportByResult,
                                     boolean addTranscript,
                                     boolean sortByLatestExample,
                                     int userid,
                                     Project project) {
    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<>();
    Map<String, List<WordAndScore>> phoneToWordAndScore = new HashMap<>();
    boolean useSessionGran = project.getKind() == ProjectType.POLYGLOT;
    String language = project.getLanguage();

    if (DEBUG) {
      logger.info("getPhoneReport" +
          "\n\tuser    " + userid +
          "\n\tlang    " + language +
          "\n\tproject " + project.getID() +
          "\n\tadd transcript      " + addTranscript +
          "\n\tsort by latest      " + sortByLatestExample +
          "\n\tphoneReportByResult " + phoneReportByResult.size());

/*      List<SlickPhoneReport> sample = new ArrayList<>(phoneReportByResult);
      int n = Math.min(sample.size(), 10);

      for (int i = 0; i < n; i++) logger.info("\te.g. " + sample.get(i));
*/
    }

    float totalScore = 0;

    Map<String, Map<NetPronImageType, List<TranscriptSegment>>> jsonToTranscript = new HashMap<>();
    int c = 0;

    Map<Integer, MiniUser.Gender> userToGender = new HashMap<>();

    Set<Integer> exids = new HashSet<>();
    Map<Integer, MiniUser> idToMini = new HashMap<>();

    //Map<String, Long> sessionToLong = new HashMap<>();

    int num = 0;
    for (SlickPhoneReport report : phoneReportByResult) {  // for every phone the user has uttered
      // int i = 1;
      c++;
     // logger.info("getPhoneReport #"+ c + " : " + report);
      // info from result table
      int exid = report.exid();
      float pronScore = report.pronScore();

      boolean add = exids.add(exid);
      if (add) { // only first score counts ???
        totalScore += pronScore;
      }
 /*     if (exid != currentExercise) {
        currentExercise = exid;
        //  logger.debug("#" +c+  " adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }*/

      String refAudioForExercise = database.getNativeAudio(userToGender, userid, exid, project, idToMini);
      String scoreJson = report.scorejson();
      if (scoreJson.isEmpty() || scoreJson.equalsIgnoreCase("{}")) {
        logger.warn("no score json for " + report.exid()+ " " + report.word());
      }
      WordAndScore wordAndScore = getAndRememberWordAndScore(refAudioForExercise,
          phoneToScores,
          phoneToWordAndScore,
          exid,
          report.answer(),
          scoreJson,
          report.modified(),
          report.device(),
          report.wseq(),
          report.word(),
          report.rid(),
          report.phone(),
          report.pseq(),
          report.pscore(),
          language);

     if (DEBUG) logger.info("getPhoneReport adding " +new Date(wordAndScore.getTimestamp()));

      if (addTranscript) {
        addTranscript(jsonToTranscript, scoreJson, wordAndScore, language);
        num++;
      } /*else {
        getAndRememberPhoneAndScore(phoneToScores, report.phone(), report.pscore(), report.modified(),
            getSessionTime(sessionToLong, report.device()));
      }*/
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }

    if (DEBUG || addTranscript) {
      logger.info("getPhoneReport added " + num + " transcripts" +
          "\n\tphoneToScores       " + phoneToScores.size() +
          "\n\tphoneToWordAndScore " + phoneToWordAndScore.size() +
          "\n\ttotalScore " + totalScore +
          "\n\texids      " + exids.size()
      );
    }

    return new MakePhoneReport()
        .getPhoneReport(phoneToScores, phoneToWordAndScore, totalScore, exids.size(), sortByLatestExample, useSessionGran);
  }

  /**
   * @param resultid
   * @see mitll.langtest.server.audio.AudioFileHelper#recalcOne
   */
  public void removeForResult(int resultid) {
    dao.removeForResult(resultid);
  }

  /**
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyPhone
   */
  public int getNumRows() {
    return dao.getNumRows();
  }

  @Override
  public void deleteForProject(int projID) {
    dao.deleteForProject(projID);
  }

  public boolean updateProjectForRID(int rid, int newprojid) {
    return dao.updateProjectForRID(rid, newprojid) > 0;
  }
}
