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
import mitll.langtest.server.database.analysis.Analysis;
import mitll.langtest.server.database.audio.NativeAudioResult;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.shared.analysis.*;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.user.MiniUser;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickPhone;
import mitll.npdata.dao.SlickPhoneReport;
import mitll.npdata.dao.phone.PhoneDAOWrapper;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class SlickPhoneDAO extends BasePhoneDAO implements IPhoneDAO<Phone> {
  private static final Logger logger = LogManager.getLogger(SlickPhoneDAO.class);

  static final String UNDERSCORE = "_";

  public static final String NJ_E = "nj-e";

  private final PhoneDAOWrapper dao;

  private static final boolean DEBUG = false;
  private static final boolean DEBUG_PHONE = false;

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

/*  public Phone fromSlick(SlickPhone slick) {
    return new Phone(
        slick.projid(),
        slick.rid(),
        slick.wid(),
        slick.phone(),
        slick.seq(),
        slick.score(),
        slick.duration());
  }*/

  /**
   * @param bulk
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyPhone
   */
  public void addBulk(List<SlickPhone> bulk) {
    dao.addBulk(bulk);
  }

  /**
   * @see RecordWordAndPhone#recordWordAndPhoneInfo(int, int, Map)
   * @param bulk
   * @param projID
   */
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
    PhoneReport report = getPhoneReport(phoneReportByExercises, false, userid, project);
    logger.info("getWorstPhonesJson phone report for" +
        "\n\tuser          " + userid +
        "\n\texids         " + exids.size() +
        "\n\treport        " + report +
        "\n\tphone reports " + phoneReportByExercises.size()
    );
    return new PhoneJSON().getWorstPhonesJson(report);
  }

  /**
   * @param userid
   * @param resultIDs
   * @return
   * @throws SQLException
   * @paramx project
   * @seex mitll.langtest.server.database.analysis.Analysis#getPhoneReport
   * @seex mitll.langtest.server.database.analysis.Analysis#getPhoneReportForPeriod(int, UserInfo, Project, long, long)
   */
 /*  @Override
  public PhoneReport getWorstPhonesForResults(int userid, Collection<Integer> resultIDs, Project project) {
    return getPhoneReport(getSlickPhoneReports(userid, resultIDs), true, userid, project);
  }*/
  @Override
  public PhoneSummary getPhoneSummary(int userid, Collection<Integer> resultIDs) {
    return getPhoneSummary(getSlickPhoneReports(userid, resultIDs));
  }

  @Override
  public PhoneBigrams getPhoneBigrams(int userid, Collection<Integer> resultIDs) {
    return getPhoneBigrams(getSlickPhoneReports(userid, resultIDs));
  }

  private Collection<SlickPhoneReport> getSlickPhoneReports(int userid, Collection<Integer> resultIDs) {
    long then = System.currentTimeMillis();
    if (DEBUG) logger.info("getPhoneReports " + userid + " project ids " + resultIDs);

    Collection<SlickPhoneReport> phoneReportByResult = dao.getPhoneReportByResult(userid, resultIDs);

    long now = System.currentTimeMillis();
    if (now - then > 0 && DEBUG)
      logger.info("getWorstPhonesForResults took " + (now - then) + " to get " + phoneReportByResult.size());

    return phoneReportByResult;
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
   */
  @Override
  public PhoneReport getWorstPhonesForResultsForPhone(int userid,
                                                      Collection<Integer> ids,
                                                      Project project,
                                                      String phone,
                                                      long from,
                                                      long to) {
    long then = System.currentTimeMillis();
    Collection<SlickPhoneReport> phoneReportByResult =
        dao.getPhoneReportByResultForPhoneAllWords(userid, ids, phone, new Timestamp(from), new Timestamp(to));

    if (phoneReportByResult.size() < 100 || DEBUG_PHONE) {
      phoneReportByResult.forEach(slickPhoneReport -> logger.info("getWorstPhonesForResultsForPhone : exercise " + slickPhoneReport.exid() + " " + slickPhoneReport.rid() +
          " " + slickPhoneReport.answer() + " " + slickPhoneReport.phone()));
    }
    long now = System.currentTimeMillis();
    if (now - then > 200 || DEBUG)
      logger.info("getWorstPhonesForResultsForPhone took " + (now - then) + " to get " + phoneReportByResult.size());

    return getPhoneReport(phoneReportByResult, true, userid, project);
  }

  /**
   * @param userid
   * @param ids
   * @param project
   * @param from
   * @param to
   * @return
   * @see Analysis#getPhoneReportForPhoneForBigrams(int, UserInfo, Project, long, long)
   */

  @Override
  public PhoneReport getWorstPhonesForResultsForTimeWindow(int userid,
                                                           Collection<Integer> ids,
                                                           Project project,
                                                           long from,
                                                           long to) {
    long then = System.currentTimeMillis();
//    Collection<SlickPhoneReport> phoneReportByResult =
//        dao.getPhoneReportByResultForTimeWindow(userid, ids, new Timestamp(from), new Timestamp(to));

    Collection<SlickPhoneReport> phoneReportByResult = dao.getPhoneReportByResult(userid, ids);

//    if (phoneReportByResult.size() < 100 || DEBUG_PHONE) {
//      phoneReportByResult.forEach(slickPhoneReport -> logger.info("getWorstPhonesForResultsForPhone : exercise " + slickPhoneReport.exid() + " " + slickPhoneReport.rid() +
//          " " + slickPhoneReport.answer() + " " + slickPhoneReport.phone()));
//    }
    long now = System.currentTimeMillis();
    if (now - then > 200 || DEBUG)
      logger.info("getWorstPhonesForResultsForPhone took " + (now - then) + " to get " + phoneReportByResult.size());

    return getPhoneReport(phoneReportByResult, true, userid, project);
  }


  private PhoneSummary getPhoneSummary(Collection<SlickPhoneReport> phoneReportByResult) {
    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<>();
    for (SlickPhoneReport report : phoneReportByResult) {  // for every phone the user has uttered
      getAndRememberPhoneAndScore(phoneToScores,
          report.phone(), report.pscore(), report.modified(),
          getSessionTime(sessionToLong, report.device()));
    }
    return new MakePhoneReport().getPhoneSummary(phoneToScores);
  }

/*  private PhoneBigrams getPhoneBigramsOLD(Collection<SlickPhoneReport> phoneReportByResult) {
    Map<String, Map<String, Bigram>> phoneToBigramToScore = new HashMap<>();

    String prevPhone = UNDERSCORE;
    float prevScore = 0F;
    int prevResult = -1;
    int prevWord = -1;


    for (SlickPhoneReport report : phoneReportByResult) {  // for every phone the user has uttered
      String phone = report.phone();
      String bigram = prevPhone + "-" + phone;

      int resultID = report.rid();
      int wseq = report.wseq();


//      logger.info("getPhoneReport prevResult " + prevResult +
//          " resultID " + resultID + " prevWord " + prevWord + " wseq " + wseq + " pseq " + report.pseq() + " phone " + report.phone() + " bigram " + bigram+ " in word " + report.word());


      boolean firstPhoneInWord = prevResult != resultID || prevWord != wseq;
      {

        if (firstPhoneInWord) {
          if (DEBUG_PHONE)
            logger.info("getPhoneReport prevResult " + prevResult +
                " resultID " + resultID + " prevWord " + prevWord + " wseq " + wseq + " phone " + report.phone());

          prevPhone = UNDERSCORE;
          prevResult = resultID;
          prevWord = wseq;
          prevScore = 0F;
        }
      }

      float phoneScore = report.pscore();

      {
        // _ a b c
        //   ^
        // _-a    - a -> _-a ->score of a
        //   a-b  - a -> a-b ->score of a

        // _ a b c
        //     ^
        //   a-b    - b -> a-b -> score of b
        //     b-c  - b -> b-c -> score of b

        {
          Bigram bigramScore2 = getBigramCounter(phoneToBigramToScore, prevPhone, bigram);
          bigramScore2.increment(prevScore);
        }

        {
          Bigram bigramScore = getBigramCounter(phoneToBigramToScore, phone, bigram);
          bigramScore.increment(phoneScore);
        }
      }

      prevPhone = phone;
      prevScore = phoneScore;
    }

    phoneToBigramToScore.values().forEach(pair -> pair.values().forEach(Bigram::setScore));

    logger.info("for " + phoneReportByResult.size() + " got " + phoneToBigramToScore.size() + " phones");

    return new MakePhoneReport().getPhoneBigrams(phoneToBigramToScore);
  }*/

  private PhoneBigrams getPhoneBigrams(Collection<SlickPhoneReport> phoneReportByResult) {
    Set<Integer> exids = new HashSet<>();

    // first by phone,
    // then by bigram, then examples per bigram
    String prevPhone = UNDERSCORE;

    Map<String, Map<String, Bigram>> phoneToBigramToScore = new HashMap<>();

    float prevScore = 0F;

    int prevResult = -1;
    int prevWord = -1;
    for (SlickPhoneReport report : phoneReportByResult) {  // for every phone the user has uttered
      int resultID = report.rid();
      int wseq = report.wseq();

      boolean firstPhoneInWord = prevResult != resultID || prevWord != wseq;
      {

        if (firstPhoneInWord) {
          if (DEBUG_PHONE)
            logger.info("getPhoneBigrams prevResult " + prevResult +
                " resultID " + resultID + " prevWord " + prevWord + " wseq " + wseq + " phone " + report.phone());

          prevPhone = UNDERSCORE;
          prevResult = resultID;
          prevWord = wseq;
          prevScore = 0F;
        }
      }

      String phone = report.phone();
      String bigram = prevPhone + "-" + phone;
      String rememPrev = prevPhone;
      prevPhone = phone;

/*
      logger.info("getPhoneBigrams prevResult " + prevResult +
          " resultID " + resultID + " prevWord " + prevWord + " wseq " + wseq + " pseq " + report.pseq() + " phone " + report.phone() + " bigram " + bigram + " in word " + report.word());
*/

      float phoneScore = report.pscore();

      {
        // _ a b c
        //   ^
        // _-a    - a -> _-a ->score of a
        //   a-b  - a -> a-b ->score of a

        // _ a b c
        //     ^
        //   a-b    - b -> a-b -> score of b
        //     b-c  - b -> b-c -> score of b

        {
          Bigram bigramScore2 = getBigramCounter(phoneToBigramToScore, rememPrev, bigram);
          bigramScore2.increment(prevScore);

//          if (phone.equalsIgnoreCase("nj") || rememPrev.equalsIgnoreCase("nj")) {
//            logger.info("1 bigram " + rememPrev + " bigram " + bigram + " " + bigramScore2);
//          }
        }

        {
          Bigram bigramScore = getBigramCounter(phoneToBigramToScore, phone, bigram);
          bigramScore.increment(phoneScore);
//          if (phone.equalsIgnoreCase("nj") || rememPrev.equalsIgnoreCase("nj")) {
//            logger.info("2 bigram " + phone + " " + bigram + " " + bigramScore);
//          }
        }
      }
      prevScore = phoneScore;
    }

    phoneToBigramToScore.values().forEach(pair -> pair.values().forEach(Bigram::setScore));

    if (DEBUG) {
      logger.info("getPhoneBigrams for " + phoneReportByResult.size() + " got " + phoneToBigramToScore.size() + " phones");
    }

    return new MakePhoneReport().getPhoneBigrams(phoneToBigramToScore);
  }

  /**
   * TODO : huh? doesn't seem to add last item to total score or total items?
   * TODO : don't use idToRef map
   * <p>
   * Why get native audio here?
   * <p>
   * TODO : add transcript in smarter way!
   *
   * @param addTranscript true if going to analysis tab
   * @param userid
   * @param project
   * @return
   * @throws SQLException
   * @see #getWorstPhonesForResultsForTimeWindow(int, Collection, Project, long, long)
   * @see #getWorstPhonesForResultsForPhone(int, Collection, Project, String, long, long)
   */
  private PhoneReport getPhoneReport(Collection<SlickPhoneReport> phoneReportByResult,
                                     boolean addTranscript,
                                     int userid,
                                     Project project) {
    Map<String, List<PhoneAndScore>> phoneToScores = new HashMap<>();
    String language = project.getLanguage();

    if (DEBUG || true) {
      logger.info("getPhoneReport" +
          "\n\tuser    " + userid +
          "\n\tlang    " + language +
          "\n\tproject " + project.getID() +
          "\n\tadd transcript      " + addTranscript +
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

    int num = 0;
    Map<Integer, String> exidToRef = new HashMap<>();
Set<Integer> isContextEx=new HashSet<>();
    // first by phone,
    // then by bigram, then examples per bigram
    Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS = new HashMap<>();
    String prevPhone = UNDERSCORE;

    Map<String, Map<String, Bigram>> phoneToBigramToScore = new HashMap<>();

    float prevScore = 0F;

    int prevResult = -1;
    int prevWord = -1;
    for (SlickPhoneReport report : phoneReportByResult) {  // for every phone the user has uttered
      // int i = 1;
      c++;

      if (DEBUG_PHONE) logger.info("getPhoneReport #" + c + " : " + report);

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

      String refAudioForExercise = exidToRef.get(exid);

      if (refAudioForExercise == null) {
        NativeAudioResult nativeAudio = database.getNativeAudio(userToGender, userid, exid, project, idToMini);
        refAudioForExercise = nativeAudio.getNativeAudioRef();
        if (nativeAudio.isContext()) {
          isContextEx.add(exid);
          logger.info("getPhoneReport add context for " + exid);
        }
        exidToRef.put(exid, refAudioForExercise);
      }

      String scoreJson = report.scorejson();
      if (scoreJson.isEmpty() || scoreJson.equalsIgnoreCase("{}")) {
        logger.warn("getPhoneReport no score json for exid " + report.exid() + " rid " + report.rid() + " word " + report.word());
      }
      int resultID = report.rid();
      int wseq = report.wseq();

      boolean firstPhoneInWord = prevResult != resultID || prevWord != wseq;
      {

        if (firstPhoneInWord) {
          if (DEBUG_PHONE)
            logger.info("getPhoneReport prevResult " + prevResult +
                " resultID " + resultID + " prevWord " + prevWord + " wseq " + wseq + " phone " + report.phone());

          prevPhone = UNDERSCORE;
          prevResult = resultID;
          prevWord = wseq;
          prevScore = 0F;
        }
      }

      String phone = report.phone();
      String bigram = prevPhone + "-" + phone;
      String rememPrev = prevPhone;
      prevPhone = phone;


/*
      logger.info("getPhoneReport prevResult " + prevResult +
          " resultID " + resultID + " prevWord " + prevWord + " wseq " + wseq + " pseq " + report.pseq() + " phone " + report.phone() + " bigram " + bigram + " in word " + report.word());
*/

      float phoneScore = report.pscore();

      {
        // _ a b c
        //   ^
        // _-a    - a -> _-a ->score of a
        //   a-b  - a -> a-b ->score of a

        // _ a b c
        //     ^
        //   a-b    - b -> a-b -> score of b
        //     b-c  - b -> b-c -> score of b

        {
          Bigram bigramScore2 = getBigramCounter(phoneToBigramToScore, rememPrev, bigram);
          bigramScore2.increment(prevScore);

//          if (phone.equalsIgnoreCase("nj") || rememPrev.equalsIgnoreCase("nj")) {
//            logger.info("1 bigram " + rememPrev + " bigram " + bigram + " " + bigramScore2);
//          }
        }

        {
          Bigram bigramScore = getBigramCounter(phoneToBigramToScore, phone, bigram);
          bigramScore.increment(phoneScore);
//          if (phone.equalsIgnoreCase("nj") || rememPrev.equalsIgnoreCase("nj")) {
//            logger.info("2 bigram " + phone + " " + bigram + " " + bigramScore);
//          }
        }
      }


      WordAndScore wordAndScore = getAndRememberWordAndScore(refAudioForExercise,
          phoneToScores,
          phoneToBigramToWS,
          exid,
          report.answer(),
          scoreJson,
          report.modified(),
          report.device(),
          wseq,
          report.word(),
          resultID,
          phone,
          bigram,
          report.pseq(),
          prevScore,
          phoneScore,
          language);
      wordAndScore.setIsContext(isContextEx.contains(exid));

//      if (wordAndScore.getIsContext()) {
//        logger.info("getPhoneReport exid " + exid + " is context");
//      }
      prevScore = phoneScore;

      // right thing to do?
      {
        Map<String, List<WordAndScore>> bigramToWords = phoneToBigramToWS.computeIfAbsent(rememPrev, k -> new HashMap<>());
        List<WordAndScore> wordAndScores1 = bigramToWords.computeIfAbsent(bigram, k -> new ArrayList<>());
        wordAndScores1.add(wordAndScore);

//        if (bigram.equalsIgnoreCase(NJ_E)) {
//          logger.info("getPhoneSummary bigram " + " " + NJ_E + " - " + wordAndScore.getWord());
//        }
      }

      if (DEBUG) {
        logger.info("getPhoneReport adding " +
            "\n\tanswer " + report.answer() +
            "\n\tdate   " + new Date(wordAndScore.getTimestamp()));
      }

      if (addTranscript) {
        /*Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap =*/
        addTranscript(jsonToTranscript, scoreJson, wordAndScore, project.getLanguageEnum());
//        logger.info("For " + wordAndScore+
//            "\n\tGot back " + netPronImageTypeListMap);
        num++;
      }
      //    } else {
     /*   logger.debug("------> current " + currentRID +
            " skipping " + exid + " " + rid + " word " + word + "<-------------- ");*/
      //  }
    }

    if (DEBUG || addTranscript) {
      logger.info("getPhoneReport added " + num + " transcripts" +
          "\n\tphoneToScores       " + phoneToScores.size() +
          "\n\tphoneToWordAndScore " + phoneToBigramToWS.size() +
          "\n\ttotalScore " + totalScore +
          "\n\texids      " + exids.size()
      );
    }

/*    List<String> sorted = new ArrayList<>(phoneToBigramToScore.values().keySet());
    sorted.sort(Comparator.naturalOrder());

    logger.info("getPhoneSummary : for user " + userid + " got " + sorted.size() + " bigrams");

    if (DEBUG_PHONE) {
      sorted.forEach(bigram ->
          logger.info(bigram +
              "\t" + bigramToCount.get(bigram) +
              "\t" + bigramToScore.get(bigram) / bigramToCount.get(bigram))
      );
    }*/

    phoneToBigramToScore.values().forEach(pair -> pair.values().forEach(Bigram::setScore));

//    phoneToBigramToScore.forEach((k, v) -> logger.info("getPhoneReport : " + k + " -> " + v.size() + " : " + v));

    return new MakePhoneReport()
        .getPhoneReport(
            phoneToScores,
            phoneToBigramToWS,
            phoneToBigramToScore,
            totalScore,
            exids.size());
  }

  @NotNull
  private Bigram getBigramCounter(Map<String, Map<String, Bigram>> phoneToBigramToScore,
                                  String phone,
                                  String bigram) {
    Map<String, Bigram> bigramToScore = phoneToBigramToScore.computeIfAbsent(phone, k -> new HashMap<>());

    Bigram bigramScore = bigramToScore.get(bigram);
    if (bigramScore == null) {
      bigramToScore.put(bigram, bigramScore = new Bigram(bigram));
    }
    return bigramScore;
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
  public int deleteForProject(int projID) {
   return dao.deleteForProject(projID);
  }

  public boolean updateProjectForRID(int rid, int newprojid) {
    return dao.updateProjectForRID(rid, newprojid) > 0;
  }
}
