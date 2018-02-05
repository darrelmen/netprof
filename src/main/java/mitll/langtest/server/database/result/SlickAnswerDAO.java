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

package mitll.langtest.server.database.result;

import mitll.langtest.server.audio.AudioCheck;
import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.Database;
import mitll.langtest.shared.answer.AudioAnswer;
import mitll.langtest.shared.scoring.AudioContext;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickResult;
import mitll.npdata.dao.result.ResultDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;

public class SlickAnswerDAO extends BaseAnswerDAO implements IAnswerDAO {
  private static final Logger logger = LogManager.getLogger(SlickAnswerDAO.class);

  private final ResultDAOWrapper dao;

  public SlickAnswerDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new ResultDAOWrapper(dbConnection);
  }

  /**
   * Cheesy thing where audio file path is actually in the audio file slot and not the answer.
   *
   * @param answerInfo
   * @param timestamp
   * @return
   * @see mitll.langtest.server.audio.AudioFileHelper#getAudioAnswerAlignment
   * @see mitll.langtest.server.audio.AudioFileHelper#recordInResults(AudioContext, AnswerInfo.RecordingInfo, AudioCheck.ValidityAndDur, AudioAnswer)
   */
  public int addAnswer(AnswerInfo answerInfo, long timestamp) {
    boolean isAudioAnswer = answerInfo.getAnswer() == null || answerInfo.getAnswer().length() == 0;
    String answerInserted = isAudioAnswer ? answerInfo.getAudioFile() : answerInfo.getAnswer();

    String model = answerInfo.getModel() == null ? "" : answerInfo.getModel();
    SlickResult res = new SlickResult(-1,
        answerInfo.getUserid(),
        answerInfo.getId(),
        new Timestamp(timestamp),
        answerInfo.getAudioType().toString(),
        answerInserted,
        answerInfo.isValid(),
        answerInfo.getValidity(),
        answerInfo.getDurationInMillis(),
        answerInfo.getProcessDur(),
        answerInfo.getRoundTripDur(),
        answerInfo.isCorrect(),
        answerInfo.getPronScore(),
        answerInfo.getDeviceType(),
        answerInfo.getDevice(),
        answerInfo.getScoreJson(),
        answerInfo.isWithFlash(),
        (float) answerInfo.getSnr(),
        answerInfo.getTranscript(),
        -1,
        answerInfo.getProjid(),
        model);

    int id = dao.insert(res).id();

    logger.info("addAnswer inserting answer" +
        "\n\tby     " + answerInfo.getUserid() +
        "\n\tto     " + answerInfo.getId() +
        "\n\tanswer " + answerInfo.getAnswer() +
        "\n\tslick  " + res +
        "\n\tid     " + id);

    return id;
  }

  @Override
  public void addRoundTrip(int resultID, int roundTrip) {
    dao.addRoundTrip(resultID, roundTrip);
  }

  @Override
  public void addUserScore(int id, float score) {
    dao.addUserScore(id, score);
  }

  /**
   * TODO : set the isCorrect field
   *
   * @param id
   * @param score
   * @param processDur
   * @param json
   * @param isCorrect
   */
  @Override
  public void changeAnswer(int id, float score, int processDur, String json, boolean isCorrect) {
    dao.changeAnswer(id, score, processDur, json);
  }

  @Override
  public void updateUser(int oldUser, int newUser) {
    dao.updateUser(oldUser, newUser);
  }
}
