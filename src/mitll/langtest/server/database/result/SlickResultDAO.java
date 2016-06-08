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

import mitll.langtest.server.database.DAO;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.server.database.user.IUserDAO;
import mitll.langtest.server.sorter.ExerciseSorter;
import mitll.langtest.shared.AudioType;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.exercise.CommonExercise;
import mitll.langtest.shared.exercise.CommonShell;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.flashcard.ExerciseCorrectAndScore;
import mitll.npdata.dao.SlickResult;
import mitll.npdata.dao.event.DBConnection;
import mitll.npdata.dao.result.ResultDAOWrapper;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class SlickResultDAO extends DAO implements IResultDAO, ISchema<Result, SlickResult> {
  private static final Logger logger = Logger.getLogger(SlickResultDAO.class);

  private final ResultDAOWrapper dao;

  public SlickResultDAO(Database database, DBConnection dbConnection, IUserDAO userDAO) {
    super(database);
    dao = new ResultDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  public void dropTable() {
    dao.drop();
  }

  @Override
  public SlickResult toSlick(Result shared, String language) {
    return new SlickResult(-1, shared.getUserid(), shared.getExid(), shared.getQid(), shared.getAudioType().toString(),
        shared.getAnswer(),
        new Timestamp(shared.getTimestamp()),
        shared.isValid(),
        shared.getValidity(),
        shared.getDurationInMillis(),
        shared.getProcessDur(),
        shared.getRoundTrip(),
        shared.isCorrect(),
        shared.getPronScore(),
        shared.getDeviceType(),
        shared.getDevice(),
        shared.getJsonScore(),
        shared.isWithFlash(),
        shared.getDynamicRange(),
        getLanguage(),
        shared.getUniqueID());
  }

  @Override
  public Result fromSlick(SlickResult slick) {
    return new Result(slick.id(),
        slick.userid(),
        slick.exid(),
        slick.qid(),
        slick.answer(),
        slick.valid(),
        slick.modified().getTime(),
        AudioType.valueOf(slick.audiotype().toUpperCase()),
        slick.duration(),
        slick.correct(),
        slick.pronscore(),
        slick.device(),
        slick.devicetype(),
        slick.processdur(),
        slick.roundtripdur(),
        slick.withflash(),
        slick.dynamicrange(),
        slick.validity());
  }

  public void insert(SlickResult result) { dao.insert(result); }

  public void addBulk(List<SlickResult> bulk) {
    dao.addBulk(bulk);
  }

  public int getNumRows() {
    return dao.getNumRows();
  }

  @Override
  public List<Result> getResults() {
    List<SlickResult> all = getAll();
    return getResults(all);
  }

  List<Result> getResults(List<SlickResult> all) {
    List<Result> copy = new ArrayList<>();
    for (SlickResult result : all) copy.add(fromSlick(result));
    return copy;
  }

  List<SlickResult> getAll() { return dao.getAll();  }

  @Override
  public Collection<Result> getResultsDevices() {
    return getResults(dao.allDevices());
  }

  @Override
  public Collection<Result> getResultsToDecode() {
    return null;
  }

  @Override
  public Result getResultByID(long id) {
    return null;
  }

  @Override
  public List<MonitorResult> getMonitorResults() {
    return null;
  }

  @Override
  public List<MonitorResult> getMonitorResultsByID(String id) {
    return null;
  }

  @Override
  public Collection<UserAndTime> getUserAndTimes() {
    return null;
  }

  @Override
  public void addUnitAndChapterToResults(Collection<MonitorResult> monitorResults, Map<String, CommonExercise> join) {

  }

  @Override
  public SessionsAndScores getSessionsForUserIn2(Collection<String> ids, long latestResultID, long userid, Collection<String> allIds, Map<String, CollationKey> idToKey) {
    return null;
  }

  @Override
  public <T extends CommonShell> List<T> getExercisesSortedIncorrectFirst(Collection<T> exercises, long userid, Collator collator) {
    return null;
  }

  @Override
  public Collection<ExerciseCorrectAndScore> getExerciseCorrectAndScoresByPhones(long userid, List<String> allIds, Map<String, CommonExercise> idToEx, ExerciseSorter sorter) {
    return null;
  }

  @Override
  public SessionInfo getSessions() {
    return null;
  }

  @Override
  public void attachScoreHistory(int userID, CommonExercise firstExercise, boolean isFlashcardRequest) {

  }

  @Override
  public Collection<CorrectAndScore> getResultsForExIDInForUser(Collection<String> ids, long userid, String session) {
    return null;
  }

  @Override
  public void invalidateCachedResults() {

  }

  @Override
  public int getNumResults() {
    return 0;
  }
}
