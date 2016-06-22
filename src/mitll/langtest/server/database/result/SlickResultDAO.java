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

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.shared.AudioType;
import mitll.langtest.shared.MonitorResult;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickPerfResult;
import mitll.npdata.dao.SlickResult;
import mitll.npdata.dao.result.ResultDAOWrapper;
import mitll.npdata.dao.result.SlickCorrectAndScore;
import mitll.npdata.dao.result.SlickUserAndTime;
import org.apache.log4j.Logger;

import java.sql.Timestamp;
import java.util.*;

public class SlickResultDAO extends BaseResultDAO implements IResultDAO, ISchema<Result, SlickResult> {
  private static final Logger logger = Logger.getLogger(SlickResultDAO.class);

  private final ResultDAOWrapper dao;

  public SlickResultDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new ResultDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public SlickResult toSlick(Result shared, String language) {
    return new SlickResult(-1,
        shared.getUserid(), shared.getExid(), shared.getQid(),
        shared.getAudioType().toString(),
        shared.getAnswer(),
        new Timestamp(shared.getTimestamp()),
        shared.isValid(),
        shared.getValidity(),
        shared.getDurationInMillis(),
        shared.getProcessDur(),
        shared.getRoundTrip(),
        shared.isCorrect(),
        shared.getPronScore(),
        checkNull(shared.getDeviceType()),
        checkNull(shared.getDevice()),
        checkNull(shared.getJsonScore()),
        shared.isWithFlash(),
        shared.getDynamicRange(),
        getLanguage(),
        shared.getUniqueID());
  }

  private String checkNull(String deviceType) {
    return deviceType == null ? "" : deviceType;
  }

  @Override
  public Result fromSlick(SlickResult slick) {

    String audiotype = slick.audiotype();
    audiotype = audiotype.replaceAll("=", "_");
    return new Result(slick.id(),
        slick.userid(),
        slick.exid(),
        slick.qid(),
        getRelativePath(slick),
        slick.valid(),
        slick.modified().getTime(),
        AudioType.valueOf(audiotype.toUpperCase()),
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

  private MonitorResult fromSlick2(SlickResult slick) {
    String audiotype = slick.audiotype();
    audiotype = audiotype.replaceAll("=", "_");
    return new MonitorResult(slick.id(),
        slick.userid(),
        slick.exid(),
        // slick.qid(),
        getRelativePath(slick),
        slick.valid(),
        slick.modified().getTime(),
        AudioType.valueOf(audiotype.toUpperCase()),
        slick.duration(),
        slick.correct(),
        slick.pronscore(),
        slick.device(),
        //slick.devicetype(),
        slick.processdur(),
        slick.roundtripdur(),
        slick.withflash(),
        slick.dynamicrange(),
        slick.validity());
  }

  private String getRelativePath(SlickResult slick) {
    return trimPathForWebPage2(slick.answer());
  }

  public void addBulk(List<SlickResult> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public List<Result> getResults() {
    List<SlickResult> all = getAll();
    return getResults(all);
  }

  private List<Result> getResults(Collection<SlickResult> all) {
    List<Result> copy = new ArrayList<>();
    for (SlickResult result : all) copy.add(fromSlick(result));
    return copy;
  }

  private List<SlickResult> getAll() {
    return dao.getAll();
  }

  @Override
  public Collection<Result> getResultsDevices() {
    return getResults(dao.allDevices());
  }

  @Override
  public Result getResultByID(int id) {
    return fromSlick(dao.byID(id));
  }

  @Override
  public List<MonitorResult> getMonitorResults() {
    return getMonitorResults(getAll());
  }

  private List<MonitorResult> getMonitorResults(Collection<SlickResult> all) {
    List<MonitorResult> copy = new ArrayList<>();
    for (SlickResult result : all) copy.add(fromSlick2(result));
    return copy;
  }

  @Override
  public List<MonitorResult> getMonitorResultsByID(String id) {
    return getMonitorResults(dao.byExID(id));
  }

  @Override
  public Collection<UserAndTime> getUserAndTimes() {
    List<SlickUserAndTime> tuple4s = dao.userAndTime();
    List<UserAndTime> userAndTimes = new ArrayList<>();
    for (SlickUserAndTime tuple : tuple4s) {
      userAndTimes.add(new MyUserAndTime(tuple.userid(), tuple.exerciseid(), tuple.modified(), tuple.qid()));
    }
    return userAndTimes;
  }

  @Override
  public Collection<CorrectAndScore> getResultsForExIDInForUser(Collection<String> ids, int userid, String session) {
    return getCorrectAndScores(dao.correctAndScoreWhere(userid, ids));
  }

  @Override
  public int getNumResults() {
    return dao.getNumRows();
  }

  @Override
  List<CorrectAndScore> getCorrectAndScoresForReal() {
    List<SlickCorrectAndScore> slickCorrectAndScores = dao.correctAndScore();
    return getCorrectAndScores(slickCorrectAndScores);
  }

  @Override
  List<CorrectAndScore> getResultsForExIDIn(Collection<String> ids, boolean matchAVP) {
    return getCorrectAndScores(dao.correctAndScoreMatchAVP(ids, matchAVP));
  }

  @Override
  List<CorrectAndScore> getResultsForExIDInForUser(Collection<String> ids, boolean matchAVP, int userid) {
    return getCorrectAndScores(dao.correctAndScoreMatchAVPUser(ids, matchAVP, userid));
  }

  private List<CorrectAndScore> getCorrectAndScores(Collection<SlickCorrectAndScore> slickCorrectAndScores) {
    List<CorrectAndScore> cs = new ArrayList<>();
    for (SlickCorrectAndScore scs : slickCorrectAndScores) cs.add(fromSlickCS(scs));
    return cs;
  }

  private CorrectAndScore fromSlickCS(SlickCorrectAndScore cs) {
    return new CorrectAndScore(cs.id(), cs.userid(), cs.exerciseid(), cs.correct(), cs.pronscore(), cs.modified(),
        trimPathForWebPage2(cs.path()), cs.json());
  }

  public Map<Integer, Integer> getOldToNew() {
    Map<Integer, Integer> oldToNew = new HashMap<>();
    for (SlickResult user : dao.getAll()) oldToNew.put(user.legacyid(), user.id());
    return oldToNew;
  }

  private String trimPathForWebPage2(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }

  public Collection<SlickPerfResult> getPerf() {
    return dao.perf();
  }

  public Collection<SlickPerfResult> getPerfForUser(int userid) {
    return dao.perfForUser(userid);
  }

  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }
}
