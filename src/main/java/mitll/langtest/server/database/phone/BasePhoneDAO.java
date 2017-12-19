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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.exercise.Project;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BasePhoneDAO extends DAO {
  //private static final Logger logger = LogManager.getLogger(BasePhoneDAO.class);

  static final String PHONE = "phone";
  static final String SEQ = "seq";
  static final String SCORE = "score";
  static final String DURATION = "duration";
  //private static final boolean DEBUG = false;
  static final String RID1 = "RID";
//  private final ParseResultJson parseResultJson;

  BasePhoneDAO(Database database) {
    super(database);
  //  parseResultJson = new ParseResultJson(database.getServerProps(), language);
  }

  /**
   * @see SlickPhoneDAO#getPhoneReport(Collection, boolean, boolean, String, int, Project)
   * @param jsonToTranscript
   * @param scoreJson
   * @param wordAndScore
   * @param language
   */
   void addTranscript(Map<String, Map<NetPronImageType, List<TranscriptSegment>>> jsonToTranscript,
                      String scoreJson,
                      WordAndScore wordAndScore, String language) {
     ParseResultJson parseResultJson = new ParseResultJson(database.getServerProps(), language);
     Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap =
         jsonToTranscript.computeIfAbsent(scoreJson, k -> parseResultJson.readFromJSON(scoreJson));
    setTranscript(wordAndScore, netPronImageTypeListMap);
  }

  /**
   * TODO : don't do this with the idToRef map...
   *  look it up in a better way
   *
   * @param phoneToScores
   * @param phoneToWordAndScore
   * @param exid
   * @param audioAnswer
   * @param scoreJson
   * @param resultTime
   * @param wseq
   * @param word
   * @param rid
   * @param phone
   * @param seq
   * @param phoneScore
   * @param language
   * @return
   * @see SlickPhoneDAO#getPhoneReport
   */
  WordAndScore getAndRememberWordAndScore(
                                          String refAudioForExercise,
                                          Map<String, List<PhoneAndScore>> phoneToScores,
                                          Map<String, List<WordAndScore>> phoneToWordAndScore,
                                          int exid,
                                          String audioAnswer,
                                          String scoreJson,
                                          long resultTime,
                                          int wseq,
                                          String word,
                                          long rid,
                                          String phone,
                                          int seq,
                                          float phoneScore,
                                          String language) {
    PhoneAndScore phoneAndScore = getAndRememberPhoneAndScore(phoneToScores, phone, phoneScore, resultTime);

    List<WordAndScore> wordAndScores = phoneToWordAndScore.computeIfAbsent(phone, k -> new ArrayList<>());

    String filePath = getFilePath(audioAnswer, language);

    WordAndScore wordAndScore = new WordAndScore(exid,
        word,
        phoneScore,
        (int)rid,
        wseq,
        seq,
        filePath,
        refAudioForExercise,
        scoreJson,
        resultTime);

    wordAndScores.add(wordAndScore);
    phoneAndScore.setWordAndScore(wordAndScore);
    return wordAndScore;
  }

  private String getFilePath(String audioAnswer, String language) {
    boolean isLegacy = audioAnswer.startsWith("answers");
    return isLegacy ?
        getRelPrefix(language) + audioAnswer:
        trimPathForWebPage(audioAnswer);
  }

  /**
   *
   * @param phoneToScores
   * @param phone
   * @param phoneScore
   * @param resultTime
   * @return
   * @see #getAndRememberWordAndScore(String, Map, Map, int, String, String, long, int, String, long, String, int, float, String)
   */
  private PhoneAndScore getAndRememberPhoneAndScore(Map<String, List<PhoneAndScore>> phoneToScores,
                                                    String phone,
                                                    float phoneScore,
                                                    long resultTime) {
    List<PhoneAndScore> scores = phoneToScores.computeIfAbsent(phone, k -> new ArrayList<>());
    PhoneAndScore phoneAndScore = new PhoneAndScore(phoneScore, resultTime);
    scores.add(phoneAndScore);
    return phoneAndScore;
  }

  /**
   * @see #addTranscript(Map, String, WordAndScore, String)
   * @param wordAndScore
   * @param netPronImageTypeListMap
   */
  private void setTranscript(WordAndScore wordAndScore,
                             Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    wordAndScore.setFullTranscript(netPronImageTypeListMap);
    wordAndScore.clearJSON();
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }


  /**
   * Fix the path -  on hydra it's at:
   *
   * /opt/netprof/answers/english/answers/plan/1039/1/subject-130
   *
   * rel path:
   *
   * answers/english/answers/plan/1039/1/subject-130
   *
   * @param language
   * @return
   */
  private String getRelPrefix(String language) {
    String installPath = database.getServerProps().getAnswerDir();

    String s = language.toLowerCase();
    String prefix = installPath + File.separator + s;
    int netProfDurLength = database.getServerProps().getAudioBaseDir().length();

    String relPrefix = prefix.substring(netProfDurLength) + File.separator ;
    return relPrefix;
  }
}
