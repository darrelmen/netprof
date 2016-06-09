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

import mitll.langtest.server.database.AnswerInfo;
import mitll.langtest.server.database.Database;
import mitll.npdata.dao.SlickResult;
import mitll.npdata.dao.event.DBConnection;
import mitll.npdata.dao.result.ResultDAOWrapper;
import org.apache.log4j.Logger;

import java.sql.Timestamp;

public class SlickAnswerDAO extends BaseAnswerDAO implements IAnswerDAO {
  private static final Logger logger = Logger.getLogger(SlickAnswerDAO.class);

  private final ResultDAOWrapper dao;

  public SlickAnswerDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new ResultDAOWrapper(dbConnection);
  }

  public int addAnswer(AnswerInfo answerInfo) {
    SlickResult res = new SlickResult(-1,
        answerInfo.getUserid(),
        answerInfo.getId(), answerInfo.getQuestionID(), answerInfo.getAudioType().toString(),
        answerInfo.getAnswer(),
        new Timestamp(System.currentTimeMillis()),
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
        getLanguage(),
        -1);

    return dao.insert(res).id();
  }

  @Override
  public void addRoundTrip(int resultID, int roundTrip) {
    dao.addRoundTrip(resultID, roundTrip);
  }

  @Override
  public void addUserScore(int id, float score) {
    dao.addUserScore(id, score);
  }

  @Override
  public void changeAnswer(int id, float score, int processDur, String json) {
    dao.changeAnswer(id, score, processDur, json);
  }
}
