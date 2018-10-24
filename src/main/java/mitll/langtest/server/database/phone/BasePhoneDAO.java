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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class BasePhoneDAO extends DAO {
  //private static final Logger logger = LogManager.getLogger(BasePhoneDAO.class);

  static final String PHONE = "phone";
  static final String SEQ = "seq";
  static final String SCORE = "score";
  static final String DURATION = "duration";
  static final String RID1 = "RID";

  final Map<String, Long> sessionToLong = new HashMap<>();

  BasePhoneDAO(Database database) {
    super(database);
  }

  /**
   * @param jsonToTranscript
   * @param scoreJson
   * @param wordAndScore
   * @param languageEnum
   * @see SlickPhoneDAO#getPhoneReport
   */
  Map<NetPronImageType, List<TranscriptSegment>> addTranscript(Map<String, Map<NetPronImageType, List<TranscriptSegment>>> jsonToTranscript,
                     String scoreJson,
                     WordAndScore wordAndScore,
                     Language languageEnum) {
    ParseResultJson parseResultJson = new ParseResultJson(database.getServerProps(), languageEnum);
    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap =
        jsonToTranscript.computeIfAbsent(scoreJson, k -> parseResultJson.readFromJSON(scoreJson));
    setTranscript(wordAndScore, netPronImageTypeListMap);

    return netPronImageTypeListMap;
  }

  /**
   * look it up in a better way
   *
   * @param phoneToScores
   * @param phoneToBigramToWS
   * @param exid
   * @param audioAnswer
   * @param scoreJson
   * @param resultTime
   * @param device
   * @param wseq
   * @param word
   * @param rid
   * @param phone
   * @param seq
   * @param prevScore
   * @param phoneScore
   * @param language
   * @return
   * @see SlickPhoneDAO#getPhoneReport
   */
  WordAndScore getAndRememberWordAndScore(String refAudioForExercise,
                                          Map<String, List<PhoneAndScore>> phoneToScores,
                                          Map<String, Map<String, List<WordAndScore>>> phoneToBigramToWS,
                                          int exid,
                                          String audioAnswer,
                                          String scoreJson,
                                          long resultTime,
                                          String device,
                                          int wseq,
                                          String word,
                                          int rid,
                                          String phone,
                                          String bigram,
                                          int seq,
                                          float prevScore, float phoneScore,
                                          String language) {
    PhoneAndScore phoneAndScore = getAndRememberPhoneAndScore(phoneToScores, phone, phoneScore, resultTime, getSessionTime(sessionToLong, device));

    Map<String, List<WordAndScore>> bigramToWords = phoneToBigramToWS.computeIfAbsent(phone, k -> new HashMap<>());
    List<WordAndScore> wordAndScores1 = bigramToWords.computeIfAbsent(bigram, k -> new ArrayList<>());

    String webPageAudioRef = database.getWebPageAudioRef(language, audioAnswer);

/*
    logger.info("getAndRememberWordAndScore : " +
        "\n\tfrom " + audioAnswer +
        "\n\tto   " + webPageAudioRef +
        "\n\tfor  " + language);
*/

    WordAndScore wordAndScore = new WordAndScore(exid,
        word,
        prevScore, phoneScore,
        rid,
        wseq,
        seq,
        webPageAudioRef,
        refAudioForExercise,
        scoreJson,
        resultTime);

    wordAndScores1.add(wordAndScore);
    phoneAndScore.setWordAndScore(wordAndScore);

    return wordAndScore;
  }

  /**
   * @param sessionToLong
   * @param device
   * @return
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getSessionTime
   */
  Long getSessionTime(Map<String, Long> sessionToLong, String device) {
    Long parsedTime = sessionToLong.get(device);

    if (parsedTime == null) {
      try {
        parsedTime = Long.parseLong(device);
//        logger.info("getSessionTime " + parsedTime);
      } catch (NumberFormatException e) {
        //      logger.info("can't parse " + device);
        parsedTime = -1L;
      }
      sessionToLong.put(device, parsedTime);
    }
    return parsedTime;
  }

  /**
   * @param phoneToScores
   * @param phone
   * @param phoneScore
   * @param resultTime
   * @return
   * @see #getAndRememberWordAndScore
   */
  PhoneAndScore getAndRememberPhoneAndScore(Map<String, List<PhoneAndScore>> phoneToScores,
                                            String phone,
                                            float phoneScore,
                                            long resultTime,
                                            long sessionStart) {
    PhoneAndScore phoneAndScore = new PhoneAndScore(phoneScore, resultTime, sessionStart);
    phoneToScores.computeIfAbsent(phone, k -> new ArrayList<>()).add(phoneAndScore);
    return phoneAndScore;
  }

  /**
   * @param wordAndScore
   * @param netPronImageTypeListMap
   * @see #addTranscript(Map, String, WordAndScore, Language)
   */
  private void setTranscript(WordAndScore wordAndScore,
                             Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    wordAndScore.setFullTranscript(netPronImageTypeListMap);
    wordAndScore.clearJSON();
  }
}
