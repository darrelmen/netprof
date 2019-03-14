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

package mitll.langtest.server.database.phone;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.result.IResultDAO;
import mitll.langtest.server.database.word.IWordDAO;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.server.scoring.ASR;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.answer.SimpleAudioAnswer;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class RecordWordAndPhone {
  private static final Logger logger = LogManager.getLogger(RecordWordAndPhone.class);
  private static final boolean DEBUG = false;


  private static final String SIL = "sil";
  private final IWordDAO wordDAO;
  private final IPhoneDAO<Phone> phoneDAO;

  /**
   * @param wordDAO
   * @param phoneDAO
   * @see DatabaseImpl#initializeDAOs
   */
  public RecordWordAndPhone(IWordDAO wordDAO, IPhoneDAO<Phone> phoneDAO) {
    this.wordDAO = wordDAO;
    this.phoneDAO = phoneDAO;
  }

  /**
   * @param projID
   * @param answer
   * @param answerID
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, AnswerInfo.RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   * @see DatabaseImpl#recordWordAndPhoneInfo
   */
  public void recordWordAndPhoneInfo(int projID, SimpleAudioAnswer answer, int answerID) {
    long then = System.currentTimeMillis();
    PretestScore pretestScore = answer.getPretestScore();

    if (DEBUG) {
      if (pretestScore == null) {
        logger.warn(" : recordWordAndPhoneInfo pretest score is null for " + answer + " and result id " + answerID);
      } else {
        logger.info(" : recordWordAndPhoneInfo answer " + answer + " and result id " + answerID);
      }
    }

    recordWordAndPhoneInfo(projID, answerID, pretestScore);
    long now = System.currentTimeMillis();
    if (now - then > 20) {
      logger.info("recordWordAndPhoneInfo answer " + answer + " and result id " + answerID + " took " + (now - then));
    }
  }

  /**
   * @param projID
   * @param answerID
   * @param pretestScore
   * @see DatabaseImpl#rememberScore
   */
  public void recordWordAndPhoneInfo(int projID,
                                     int answerID,
                                     PretestScore pretestScore) {
    if (pretestScore != null) {
      recordWordAndPhoneInfo(projID, answerID, pretestScore.getTypeToSegments());
    }
  }

  /**
   * Blow away old phones and words, rewrite with remapped words and phones
   * Take awhile!
   *
   * @param projid
   * @param dao
   * @param serverProperties
   * @param lang
   */
  public void remapPhones(int projid, IResultDAO dao, ServerProperties serverProperties, Language lang) {
    int i = phoneDAO.deleteForProject(projid);
    logger.info("remapPhones " + projid + " removed " + i + " phones");

    int i1 = wordDAO.deleteForProject(projid);
    logger.info("remapPhones " + projid + " removed " + i1 + " words");

    //TranscriptSegmentGenerator generator = new TranscriptSegmentGenerator(serverProperties);
    Map<Integer, String> resultIDToJSON = dao.getResultIDToJSON(projid);
    logger.info("remapPhones " + projid + " found " + resultIDToJSON.size() + " to remap");

    ParseResultJson parseResultJson = new ParseResultJson(serverProperties, lang);

    AtomicInteger atomicInteger = new AtomicInteger();
    resultIDToJSON.forEach((k, v) -> {
      recordWordAndPhoneInfo(projid, k, parseResultJson.readFromJSON(v));
      int andIncrement = atomicInteger.getAndIncrement();
      if (andIncrement % 1000 == 0) logger.info("remapPhones did " + andIncrement);
    });
  }

  /**
   * TODO : bulk add of words...
   *
   * @param projID
   * @param answerID
   * @param netPronImageTypeListMap
   * @see #recordWordAndPhoneInfo(int, int, PretestScore)
   */
  private void recordWordAndPhoneInfo(int projID,
                                      int answerID,
                                      Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap) {
    List<TranscriptSegment> words = netPronImageTypeListMap.get(NetPronImageType.WORD_TRANSCRIPT);
    if (words != null) {
      int windex = 0;
      int pindex = 0;

      List<Phone> toAdd = new ArrayList<>();
      // logger.info("recordWordAndPhoneInfo : " + words.size() + " words for answer " + answerID + " in project " + projID);

      List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);

      for (TranscriptSegment segment : words) {
        String event = segment.getEvent();
        if (keepEvent(event)) {
          int wid = wordDAO.addWord(new Word(projID, answerID, event, windex++, segment.getScore()));

          for (TranscriptSegment pseg : phones) {
            if (pseg.getStart() >= segment.getStart() && pseg.getEnd() <= segment.getEnd()) {
              String pevent = pseg.getEvent();
              if (keepEvent(pevent)) {
                int duration = pseg.getDuration();
                if (duration == 0) {
                  logger.warn("zero duration for " + answerID + " wid " + wid + " event " + pevent);
                }
                //Phone phone = new Phone(projID, answerID, wid, pevent, pindex++, pseg.getScore(), duration);
                toAdd.add(new Phone(projID, answerID, wid, pevent, pindex++, pseg.getScore(), duration));
              }
            }
          }
        }
      }

//      logger.info("recordWordAndPhoneInfo for " + answerID+ " adding " + windex + " words and " + toAdd.size() + " phones");
      phoneDAO.addBulkPhones(toAdd, projID);
    } else {
      logger.warn("recordWordAndPhoneInfo : (?) no word info for answer " + answerID +
          " in project " + projID + " with map " + netPronImageTypeListMap);
    }
  }

  private boolean keepEvent(String event) {
    return !event.equals(ASR.UNKNOWN_MODEL) && !event.equals(SIL);
  }
}
