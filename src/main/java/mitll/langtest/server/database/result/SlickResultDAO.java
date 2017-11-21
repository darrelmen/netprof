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
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.result.MonitorResult;
import mitll.langtest.shared.scoring.NetPronImageType;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickPerfResult;
import mitll.npdata.dao.SlickResult;
import mitll.npdata.dao.result.ResultDAOWrapper;
import mitll.npdata.dao.result.SlickCorrectAndScore;
import mitll.npdata.dao.result.SlickExerciseScore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import scala.Tuple2;
import scala.collection.Seq;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class SlickResultDAO extends BaseResultDAO implements IResultDAO {
  private static final Logger logger = LogManager.getLogger(SlickResultDAO.class);

  private final ResultDAOWrapper dao;
  private SlickResult defaultResult;

  /**
   * @param database
   * @param dbConnection
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public SlickResultDAO(DatabaseImpl database, DBConnection dbConnection) {
    super(database);
    dao = new ResultDAOWrapper(dbConnection);
  }

  public int ensureDefault(int projid, int beforeLoginUser, int unknownExerciseID) {
    List<SlickResult> defResult = dao.getAllByProject(projid);

    if (defResult.isEmpty()) {
      Timestamp modified = new Timestamp(System.currentTimeMillis());

      dao.insert(new SlickResult(-1, beforeLoginUser, unknownExerciseID, modified,
          // 0,
          AudioType.UNSET.toString(),
          "",
          false, Validity.INVALID.name(),
          0,
          0,
          0,
          false,
          0,
          "unk",
          "unk",
          "", false, 0, "", -1,
          projid, ""
      ));
      defResult = dao.getAllByProject(projid);
    }

    if (!defResult.isEmpty()) {
      defaultResult = defResult.iterator().next();
      return defaultResult.id();
    } else {
      logger.info("nope - no default result ");
      return -1;
    }
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  /**
   * @param shared
   * @param projid
   * @param exToInt
   * @param transcript
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyResult
   */
  public SlickResult toSlick(Result shared,
                             int projid,
                             // Map<String, Integer> exToInt,
                             Integer realExID,
                             String transcript) {
//    Integer realExID = exToInt.get(shared.getOldExID());
//
//    if (realExID == null) {
//      return null;
//    }
//    else {
    String model = shared.getModel();
    if (model == null) model = "";
    return new SlickResult(-1,
        shared.getUserid(),
        realExID,
        new Timestamp(shared.getTimestamp()),
        //shared.getQid(),
        shared.getAudioType().toString(),
        shared.getAnswer(),
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
        //    getLanguage(),
        transcript,
        shared.getUniqueID(),
        projid,
        model
    );
//    }
  }

  private String checkNull(String deviceType) {
    return deviceType == null ? "" : deviceType;
  }

  private Result fromSlick(SlickResult slick) {
    String audiotype = slick.audiotype();
    audiotype = audiotype.replaceAll("=", "_");
    return new Result(slick.id(),
        slick.userid(),
        slick.exid(), 0,
        // slick.qid(),
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
        slick.validity(),
        slick.model());
  }

  private MonitorResult fromSlick2(SlickResult slick) {
    String audiotype = slick.audiotype();
    audiotype = audiotype.replaceAll("=", "_");
    String simpleDevice = slick.device();
    // String dtype = slick.devicetype();
    // String device = dtype == null ? "Unk" : dtype.equals("browser") ? simpleDevice : (dtype + "/" + simpleDevice);

    return new MonitorResult(slick.id(),
        slick.userid(),
        "",
        // slick.qid(),
        getRelativePath(slick),
        slick.valid(),
        slick.modified().getTime(),
        AudioType.valueOf(audiotype.toUpperCase()),
        slick.duration(),
        slick.correct(),
        slick.pronscore(),
        simpleDevice,
        //slick.devicetype(),
        slick.processdur(),
        slick.roundtripdur(),
        slick.withflash(),
        slick.dynamicrange(),
        slick.validity(),
        slick.devicetype(),
        simpleDevice,
        "",
        slick.transcript(),
        slick.exid());
  }

  private String getRelativePath(SlickResult slick) {
    return trimPathForWebPage2(slick.answer());
  }

  @Override
  public long getFirstTime(int projid) {
    return dao.getFirst(projid).modified().getTime();
  }

  public void addBulk(List<SlickResult> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public List<Result> getResults() {
    return getResults(getAll());
  }

  private List<Result> getResults(Collection<SlickResult> all) {
    List<Result> copy = new ArrayList<>();
    for (SlickResult result : all) copy.add(fromSlick(result));
    return copy;
  }

  private List<SlickResult> getAll() {
    return dao.getAll();
  }

  private List<SlickResult> getAllByProject(int projid) {
    return dao.getAllByProject(projid);
  }

  @Override
  public Collection<MonitorResult> getResultsDevices(int projid) {
    return getMonitorResults(dao.allDevices(projid));
  }

  @Override
  public Result getResultByID(int id) {
    return fromSlick(dao.byID(id));
  }

  @Override
  public List<MonitorResult> getMonitorResults(int projid) {
    return getMonitorResults(getAllByProject(projid));
  }

  private List<MonitorResult> getMonitorResults(Collection<SlickResult> all) {
    List<MonitorResult> copy = new ArrayList<>();
    for (SlickResult result : all) copy.add(fromSlick2(result));
    return copy;
  }

  @Override
  public List<MonitorResult> getMonitorResultsByID(int id) {
    return getMonitorResults(dao.byExID(id));
  }

  /**
   * @see BaseResultDAO#getUserToNumAnswers
   * @return
   */
  @Override
  public Collection<UserAndTime> getUserAndTimes() {
    return null;
/*
    List<SlickUserAndTime> tuple4s = dao.userAndTime();
    List<UserAndTime> userAndTimes = new ArrayList<>();
    for (SlickUserAndTime tuple : tuple4s) {
      userAndTimes.add(new MyUserAndTime(tuple.userid(), tuple.exerciseid(), tuple.modified()*/
/*, tuple.qid()*//*
));
    }
    return userAndTimes;
*/
  }

  /**
   * @param ids
   * @param matchAVP
   * @param userid
   * @param language
   * @return
   */
  @Override
  public List<CorrectAndScore> getResultsForExIDInForUser(Collection<Integer> ids, boolean matchAVP, int userid, String language) {
    if (matchAVP) {
      return getCorrectAndScores(dao.correctAndScoreMatchAVPUser(ids, matchAVP, userid), language);
    } else {
      return getResultsForExIDInForUser(ids, userid, "", language);
    }
  }

  /**
   * @param ids
   * @param userid
   * @param language
   * @return
   * @see BaseResultDAO#getScoreHistories
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getScoreHistories
   */
  public Map<Integer, List<CorrectAndScore>> getCorrectAndScoreMap(Collection<Integer> ids, int userid, String language) {
    return getResultsForExIDInForUser(ids, userid, "", language)
        .stream()
        .collect(Collectors.groupingBy(CorrectAndScore::getExid));
  }

  /**
   * @param ids
   * @param userid
   * @param ignoredSession
   * @param language
   * @return
   */
  @Override
  public List<CorrectAndScore> getResultsForExIDInForUser(Collection<Integer> ids, int userid, String ignoredSession, String language) {
    return getCorrectAndScores(dao.correctAndScoreWhere(userid, ids), language);
  }


  @Override
  List<CorrectAndScore> getResultsForExIDIn(Collection<Integer> ids, String language) {
    return getCorrectAndScores(dao.correctAndScoreMatchAVP(ids, true), language);
  }

  @Override
  public int getNumResults(int projid) {
    return dao.numRowsForProject(projid);
  }

  /**
   * Scores for indicated exercises for user.
   *
   * @param <T>
   * @param userid
   * @param exercises
   * @see mitll.langtest.server.services.ExerciseServiceImpl#makeExerciseListWrapper
   */
  @Override
  public <T extends HasID> Map<Integer, Float> getScores(int userid, Collection<T> exercises) {

    Set<Integer> idsToFind = exercises
        .stream()
        .map(HasID::getID)
        .collect(Collectors.toSet());

    Map<Integer, SlickExerciseScore> correctAndScoresForReal;
    if (idsToFind.size() < 200 || true) {
      long then = System.currentTimeMillis();
      correctAndScoresForReal = dao.exidAndScoreWhere(userid, idsToFind);
      long now = System.currentTimeMillis();
      logger.info("took " + (now - then) + " millis to ask for " + idsToFind.size());
    } else {
      long then = System.currentTimeMillis();
      correctAndScoresForReal = dao.exidAndScore(userid);
      long now = System.currentTimeMillis();
      logger.info("took " + (now - then) + " millis to ask for " + idsToFind.size() + " ids");

      logger.info("From " + correctAndScoresForReal.size());
      Map<Integer, SlickExerciseScore> filtered = new HashMap<>(idsToFind.size());

      correctAndScoresForReal.forEach((k, v) -> {
        if (idsToFind.contains(k)) {
          filtered.put(k, v);
        }
      });

      logger.info("down to " + filtered.size() + " for " + idsToFind.size());
      correctAndScoresForReal = filtered;
    }
    logger.info("getScores : for user " + userid + " checking " + exercises.size() + " exercises, found " + correctAndScoresForReal.size() + " scores");
     return getScores2(correctAndScoresForReal);
  }

  /**
   * Consider a cache here - this isn't going to change much
   * <p>
   * Set the score on the exercise - just a float.
   *
   * @param correctAndScoresForReal
   * @paramx exercises
   * @seex #addScoresForAll
   * @see #getScores
   */
/*  private <T extends CommonShell> void setScores(Collection<T> exercises,
                                                 Map<Integer, SlickExerciseScore> correctAndScoresForReal) {
    long then = System.currentTimeMillis();
    int c = 0;
    for (T ex : exercises) {
      SlickExerciseScore slickExerciseScore = correctAndScoresForReal.get(ex.getID());
      if (slickExerciseScore != null) {
        ex.getMutableShell().setScore(slickExerciseScore.pronscore());
        c++;
//        logger.info("Set "+ ex.getID() + " to "+ slickExerciseScore.pronscore());
      }
    }
    long now = System.currentTimeMillis();
    if (now - then > 10) {
      logger.info("setScores took " + (now - then) + " to" +
          "\n\tget " + c + " scores," +
          "\n\tand " + exercises.size() + " exercises, " +
          "\n\tcorrect & score " + correctAndScoresForReal.size());
    }
  }*/
  private <T extends HasID> Map<Integer, Float> getScores2(
      Map<Integer, SlickExerciseScore> correctAndScoresForReal) {
    Map<Integer, Float> idToScore = new HashMap<>();
    correctAndScoresForReal.forEach((k, v) -> idToScore.put(k, v.pronscore()));
    return idToScore;
  }


  private <T extends HasID> Map<Integer, Float> getScores(Collection<T> exercises,
                                                          Map<Integer, SlickExerciseScore> correctAndScoresForReal) {
    Map<Integer, Float> idToScore = new HashMap<>();

    long then = System.currentTimeMillis();
    int c = 0;
    for (HasID ex : exercises) {
      int id = ex.getID();

      SlickExerciseScore slickExerciseScore = correctAndScoresForReal.get(id);
      if (slickExerciseScore != null) {
        //ex.getMutableShell().setScore(slickExerciseScore.pronscore());
        idToScore.put(id, slickExerciseScore.pronscore());
        c++;
//        logger.info("Set "+ ex.getID() + " to "+ slickExerciseScore.pronscore());
      }
    }
    long now = System.currentTimeMillis();
    if (now - then > 10) {
      logger.info("getScores took " + (now - then) + " to" +
          "\n\tget " + c + " scores," +
          "\n\tand " + exercises.size() + " exercises, " +
          "\n\tcorrect & score " + correctAndScoresForReal.size());
    }

    return idToScore;
  }

/*
  Map<Integer, SlickExerciseScore> getCorrectAndScoresForReal(int userid, Collection<Integer> exids) {
    return dao.exidAndScoreWhere(userid, exids);
  }
*/

  private List<CorrectAndScore> getCorrectAndScores(Collection<SlickCorrectAndScore> slickCorrectAndScores, String language) {
    List<CorrectAndScore> cs = new ArrayList<>();
    String relPrefix = getRelPrefix(language);
    for (SlickCorrectAndScore scs : slickCorrectAndScores) cs.add(fromSlickCorrectAndScoreWithRelPath(scs, relPrefix));
    return cs;
  }

  private final ParseResultJson parser = new ParseResultJson(database.getServerProps());

  @NotNull
  private CorrectAndScore fromSlickCorrectAndScoreWithRelPath(SlickCorrectAndScore cs,
                                                              String relPrefix) {
    String path = cs.path();
    boolean isLegacy = path.startsWith("answers");
    String filePath = isLegacy ?
        relPrefix + path :
        trimPathForWebPage2(path);

    String json = cs.json();
    CorrectAndScore correctAndScore = new CorrectAndScore(cs.id(), cs.userid(), cs.exerciseid(), cs.correct(), cs.pronscore(), cs.modified(),
        trimPathForWebPage2(filePath), json);

    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap =
        parser.readFromJSON(json);

    // TODO : maybe turn back on later?
/*
    if (netPronImageTypeListMap.isEmpty()) {
      logger.warn("no word and phones for " + json + " for " + cs);
    }
    */

    correctAndScore.setScores(netPronImageTypeListMap);
//    logger.info("returning " + correctAndScore);
    return correctAndScore;
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

  /**
   * Fix the path -  on hydra it's at:
   * <p>
   * /opt/netprof/answers/english/answers/plan/1039/1/subject-130
   * <p>
   * rel path:
   * <p>
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

    return prefix.substring(netProfDurLength) + File.separator;
  }

  public Collection<SlickPerfResult> getPerf(int projid, float minScore) {
    return dao.perf(projid, minScore);
  }

  @Override
  public Map<String,Integer> getStudentAnswers(int projid) {
    List<Tuple2<String, Integer>> tuple2s = dao.studentAnswers(projid);
    Map<String,Integer> pathToUser = new HashMap<>();
    tuple2s.forEach(stringIntegerTuple2 -> pathToUser.put(stringIntegerTuple2._1, stringIntegerTuple2._2));
    return pathToUser;
  }

  /**
   * @param userid
   * @param projid
   * @return
   * @see mitll.langtest.server.database.analysis.SlickAnalysis#getBestForUser
   */
  public Collection<SlickPerfResult> getPerfForUser(int userid, int projid) {
    return dao.perfForUser(userid, projid);
  }

  public Collection<SlickPerfResult> getPerfForUserOnList(int userid, int listid) {
    Collection<SlickPerfResult> slickPerfResults = dao.perfForUserOnList(userid, listid);
    List<Integer> unique = new ArrayList<>();
    slickPerfResults.forEach(p -> unique.add(p.id()));

    List<Integer> uniqueex = new ArrayList<>();
    slickPerfResults.forEach(p -> uniqueex.add(p.exid()));

    logger.info("getPerfForUserOnList perf for" +
        "\n\tuser   " + userid +
        "\n\tlist   " + listid + " : got " + slickPerfResults.size() +
        "\n\tids    " + unique +
        "\n\tex ids " + uniqueex
    );

    return slickPerfResults;
  }

  public Collection<Integer> getPracticedByUser(int userid, int projid) {
    return dao.practicedByUser(userid, projid);
  }

  public int getDefaultResult() {
    return defaultResult.id();
  }
}
