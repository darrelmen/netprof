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

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.word.IWordDAO;
import mitll.langtest.server.database.word.Word;
import mitll.langtest.server.scoring.ASR;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.langtest.shared.scoring.PretestScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
   *
   * @param projID
   * @param answer
   * @param answerID
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, AnswerInfo.RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   * @see DatabaseServices#recordWordAndPhoneInfo(int, AudioAnswer, long)
   */
  public void recordWordAndPhoneInfo(int projID, AudioAnswer answer, int answerID) {
    PretestScore pretestScore = answer.getPretestScore();

    if (DEBUG) {
      if (pretestScore == null) {
        logger.debug(" : recordWordAndPhoneInfo pretest score is null for " + answer + " and result id " + answerID);
      } else {
        logger.debug(" : recordWordAndPhoneInfo pretest score is " + pretestScore + " for " + answer + " and result id " + answerID);
      }
    }

    recordWordAndPhoneInfo(projID, answerID, pretestScore);
  }

  /**
   *
   * @param projID
   * @param answerID
   * @param pretestScore
   * @see DatabaseServices#rememberScore
   */
  public void recordWordAndPhoneInfo(int projID,
                                     int answerID,
                                     PretestScore pretestScore) {
    if (pretestScore != null) {
      recordWordAndPhoneInfo(projID, answerID, pretestScore.getTypeToSegments());
    }
  }

  /**
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
    List<TranscriptSegment> phones = netPronImageTypeListMap.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (words != null) {
      int windex = 0;
      int pindex = 0;

      List<Phone> toAdd = new ArrayList<>();

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
                Phone phone = new Phone(projID, answerID, wid, pevent, pindex++, pseg.getScore(), duration);
                toAdd.add(phone);
              }
            }
          }
        }
      }

      logger.info("recordWordAndPhoneInfo for " + answerID+ " adding " + windex + " words and " + toAdd.size() + " phones");
      phoneDAO.addBulkPhones(toAdd, projID);
    }
  }

  private boolean keepEvent(String event) {
    return !event.equals(ASR.UNKNOWN_MODEL) && !event.equals(SIL);
  }
}
