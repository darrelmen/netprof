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
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.analysis.PhoneAndScore;
import mitll.langtest.shared.analysis.WordAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasePhoneDAO extends DAO {
  private static final Logger logger = LogManager.getLogger(BasePhoneDAO.class);

  protected static final String PHONE = "phone";
  protected static final String SEQ = "seq";
  protected static final String SCORE = "score";
  //private static final boolean DEBUG = false;
  protected static final String RID1 = "RID";
  protected final ParseResultJson parseResultJson;

  protected BasePhoneDAO(Database database) {
    super(database);
    parseResultJson = new ParseResultJson(database.getServerProps());
  }

  protected void addTranscript(Map<String, Map<NetPronImageType, List<TranscriptSegment>>> stringToMap, String scoreJson, WordAndScore wordAndScore) {
    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = stringToMap.get(scoreJson);
    if (netPronImageTypeListMap == null) {
      netPronImageTypeListMap = parseResultJson.parseJson(scoreJson);
      stringToMap.put(scoreJson, netPronImageTypeListMap);
    } else {
      // logger.debug("cache hit " + scoreJson.length());
    }

    setTranscript(wordAndScore, netPronImageTypeListMap);
  }

  WordAndScore getAndRememberWordAndScore(Map<Integer, String> idToRef,
                                          Map<String, List<PhoneAndScore>> phoneToScores,
                                          Map<String, List<WordAndScore>> phoneToWordAndScore,
                                          int exid,
                                          String audioAnswer,
                                          String scoreJson,
                                          long resultTime,
                                          int wseq, String word,
                                          long rid, String phone, int seq, float phoneScore) {
    PhoneAndScore phoneAndScore = getAndRememberPhoneAndScore(phoneToScores, phone, phoneScore, resultTime);

    List<WordAndScore> wordAndScores = phoneToWordAndScore.get(phone);
    if (wordAndScores == null) {
      phoneToWordAndScore.put(phone, wordAndScores = new ArrayList<>());
    }

    WordAndScore wordAndScore = new WordAndScore(exid, word, phoneScore, rid, wseq, seq, trimPathForWebPage(audioAnswer),
        idToRef.get(exid), scoreJson, resultTime);

    wordAndScores.add(wordAndScore);
    phoneAndScore.setWordAndScore(wordAndScore);
    return wordAndScore;
  }

  private PhoneAndScore getAndRememberPhoneAndScore(Map<String, List<PhoneAndScore>> phoneToScores,
                                                    String phone, float phoneScore, long resultTime) {
    List<PhoneAndScore> scores = phoneToScores.get(phone);
    if (scores == null) phoneToScores.put(phone, scores = new ArrayList<>());
    PhoneAndScore phoneAndScore = new PhoneAndScore(phoneScore, resultTime);
    scores.add(phoneAndScore);
    return phoneAndScore;
  }

  private void setTranscript(WordAndScore wordAndScore, Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    wordAndScore.setTranscript(netPronImageTypeListMap);
    wordAndScore.clearJSON();
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }
}
