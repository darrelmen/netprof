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
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.DatabaseImpl;
import mitll.langtest.server.scoring.ParseResultJson;
import mitll.langtest.shared.UserAndTime;
import mitll.langtest.shared.answer.AudioType;
import mitll.langtest.shared.answer.Validity;
import mitll.langtest.shared.exercise.HasID;
import mitll.langtest.shared.flashcard.CorrectAndScore;
import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.project.Language;
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

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class SlickResultDAO extends BaseResultDAO implements IResultDAO {
  private static final Logger logger = LogManager.getLogger(SlickResultDAO.class);

  private final ResultDAOWrapper dao;
  private SlickResult defaultResult;
  private final ServerProperties serverProps;

  /**
   * @param database
   * @param dbConnection
   * @paramx xlanguage
   * @see mitll.langtest.server.database.DatabaseImpl#initializeDAOs
   */
  public SlickResultDAO(DatabaseImpl database, DBConnection dbConnection) {
    super(database);
    serverProps = database.getServerProps();
    dao = new ResultDAOWrapper(dbConnection);
  }

  public boolean updateProject(int old, int newprojid) {
    return dao.updateProject(old, newprojid) > 0;
  }

  /**
   * @param rid
   * @param newprojid
   * @param newEXID
   * @return
   * @see DatabaseImpl#updateRecordings
   */
  public boolean updateProjectAndEx(int rid, int newprojid, int newEXID) {
    return dao.updateProjectAndEx(rid, newprojid, newEXID) > 0;
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
   * NOTE : WE can't add any more columns to this table.
   *
   * @param toImport
   * @param projid
   * @param transcript
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyResult
   */
  public SlickResult toSlick(Result toImport,
                             int projid,
                             Integer realExID,
                             String transcript) {

    String model = toImport.getModel();
    if (model == null) model = "";
    return new SlickResult(-1,
        toImport.getUserid(),
        realExID,
        new Timestamp(toImport.getTimestamp()),

        toImport.getAudioType().toString(),
        toImport.getAnswer(),
        toImport.isValid(),
        toImport.getValidity(),
        toImport.getDurationInMillis(),
        toImport.getProcessDur(),
        toImport.getRoundTrip(),
        toImport.isCorrect(),
        toImport.getPronScore(),
        checkNull(toImport.getDeviceType()),
        checkNull(toImport.getDevice()),
        checkNull(toImport.getJsonScore()),
        toImport.isWithFlash(),
        toImport.getDynamicRange(),
        transcript,
        toImport.getUniqueID(),
        projid,
        model
    );

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
        "");
  }

  private MonitorResult fromSlickToMonitorResult(SlickResult slick) {
    String audiotype = slick.audiotype();
    audiotype = audiotype.replaceAll("=", "_");
    String simpleDevice = slick.device();
    // String dtype = slick.devicetype();
    // String device = dtype == null ? "Unk" : dtype.equals("browser") ? simpleDevice : (dtype + "/" + simpleDevice);

    return new MonitorResult(slick.id(),
        slick.userid(),
        getRelativePath(slick),
        slick.valid(),
        slick.modified().getTime(),
        AudioType.valueOf(audiotype.toUpperCase()),
        slick.duration(),
        slick.correct(),
        slick.pronscore(),
        simpleDevice,
        slick.processdur(),
        slick.roundtripdur(),
        slick.withflash(),
        slick.dynamicrange(),
        slick.validity(),
        slick.devicetype(),
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
    return Collections.emptyList();
  }

  @Override
  public Collection<MonitorResult> getResultsDevices(int projid) {
    return getMonitorResults(dao.allDevices(projid));
  }

  @Override
  public Result getResultByID(int id) {
    return fromSlick(dao.byID(id));
  }

  /**
   * Don't return results where the exercises are not known.
   *
   * @param projid
   * @return
   */
  @Override
  public List<MonitorResult> getMonitorResults(int projid) {
    return getMonitorResults(dao.getAllByProject(projid));
  }

  @Override
  public List<MonitorResult> getMonitorResultsKnownExercises(int projid) {
    return getMonitorResults(dao.getAllByProjectKnownExercises(projid));
  }

  private List<MonitorResult> getMonitorResults(Collection<SlickResult> all) {
    List<MonitorResult> copy = new ArrayList<>(all.size());
    for (SlickResult result : all) copy.add(fromSlickToMonitorResult(result));
    return copy;
  }

  @Override
  public List<MonitorResult> getMonitorResultsByExerciseID(int id) {
    return getMonitorResults(dao.byExID(id));
  }

  @Override
  public MonitorResult getMonitorResultByID(int id) {
    SlickResult slick = dao.byID(id);
    return slick == null ? null : fromSlickToMonitorResult(slick);
  }

  /**
   * @return
   * @see BaseResultDAO#getUserToNumAnswers
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
   * @param userid
   * @param language
   * @return
   * @see BaseResultDAO#getScoreHistories
   * @see mitll.langtest.server.services.ExerciseServiceImpl#getScoreHistories
   */
 /* public Map<Integer, List<CorrectAndScore>> getCorrectAndScoreMap(Collection<Integer> ids, int userid, String language) {
    Map<Integer, List<CorrectAndScore>> collect = getResultsForExIDInForUser(ids, userid, "", language)
        .stream()
        .collect(Collectors.groupingBy(CorrectAndScore::getExid));
    return collect;
  }*/

  /**
   * Highest scoring attempt a student has ever recorded.
   *
   * @param userid
   * @param ids
   * @param language
   * @return
   */
  public Map<Integer, CorrectAndScore> getScoreHistories(int userid, Collection<Integer> ids, Language language) {
    Map<Integer, CorrectAndScore> exidToMaxScoreEver = new HashMap<>(ids.size());

    getResultsForExIDInForUserEasy(ids, userid, language)
        .stream()
        .collect(Collectors.groupingBy(CorrectAndScore::getExid))
        .forEach((k, v) -> exidToMaxScoreEver.put(k,
            v
                .stream()
                .max((o1, o2) -> {
                  int compare = Float.compare(o1.getScore(), o2.getScore());
                  if (compare == 0) compare = Long.compare(o1.getTimestamp(), o2.getTimestamp());
                  return compare;
                })
                .get())
        );

    return exidToMaxScoreEver;
  }

  /**
   * @param ids
   * @param userid
   * @param language
   * @return
   */
  @Override
  public List<CorrectAndScore> getResultsForExIDInForUser(Collection<Integer> ids, int userid, Language language) {
    return getResultsForExIDInForUserEasy(ids, userid, language);
  }

  /**
   * @param ids
   * @param userid
   * @param language
   * @return
   * @paramx ignoredSession
   */
  @Override
  public List<CorrectAndScore> getResultsForExIDInForUserEasy(Collection<Integer> ids, int userid, Language language) {
    return getCorrectAndScores(dao.correctAndScoreWhere(userid, ids), language);
  }

  @Override
  public int getNumResults(int projid) {
    return dao.numRowsForProject(projid);
  }

  /**
   * Scores for indicated exercises for user.
   * <p>
   * So we can pass in the exids to the query as inSet query, but that's wasteful when we just want all the scores for
   * a person.
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

    if (idsToFind.size() < 2000) {
      long then = System.currentTimeMillis();
      Map<Integer, Float> integerFloatMap = dao.exidAndScoreWhere(userid, idsToFind);
      long now = System.currentTimeMillis();

      long diff = now - then;
      if (diff > 40) {
        logger.info("getScores took " + diff + " millis to ask for scores for " + idsToFind.size() + " exercises for user " + userid + " enumerated...");
      }

      return new HashMap<>(integerFloatMap);
    } else {
      long then = System.currentTimeMillis();
      Map<Integer, Float> integerFloatMap = dao.exidAndScore(userid);
      long now = System.currentTimeMillis();
      logger.info("getScores took " + (now - then) + " millis to ask for " + idsToFind.size() + " ids - " + integerFloatMap.size());
      return new HashMap<>(integerFloatMap);
    }

    /*   else {

      long then = System.currentTimeMillis();
      correctAndScoresForReal = dao.exidAndScore(userid);
      long now = System.currentTimeMillis();
      logger.info("getScores took " + (now - then) + " millis to ask for " + idsToFind.size() + " ids");

      logger.info("getScores From " + correctAndScoresForReal.size());
      Map<Integer, SlickExerciseScore> filtered = new HashMap<>(idsToFind.size());

      correctAndScoresForReal.forEach((k, v) -> {
        if (idsToFind.contains(k)) {
          filtered.put(k, v);
        }
      });

      logger.info("getScores down to " + filtered.size() + " for " + idsToFind.size());
      correctAndScoresForReal = filtered;
    }*/

//    logger.info("getScores : for user " + userid + " checking " + exercises.size() + " exercises, found " + correctAndScoresForReal.size() + " scores");
//    return getScores2(correctAndScoresForReal);
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

/*  private <T extends HasID> Map<Integer, Float> getScores2(Map<Integer, SlickExerciseScore> correctAndScoresForReal) {
    Map<Integer, Float> idToScore = new HashMap<>();
    correctAndScoresForReal.forEach((k, v) -> idToScore.put(k, v.pronscore()));
    return idToScore;
  }*/
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

  /**
   * @param slickCorrectAndScores
   * @param language              so we can fix the file path
   * @return
   */
  private List<CorrectAndScore> getCorrectAndScores(Collection<SlickCorrectAndScore> slickCorrectAndScores, Language language) {
    List<CorrectAndScore> cs = new ArrayList<>(slickCorrectAndScores.size());
    String relPrefix = database.getRelPrefix(language.getLanguage());
    slickCorrectAndScores
        .forEach(slickCorrectAndScore -> cs.add(fromSlickCorrectAndScoreWithRelPath(slickCorrectAndScore, relPrefix, language)));
    return cs;
  }


  @NotNull
  private CorrectAndScore fromSlickCorrectAndScoreWithRelPath(SlickCorrectAndScore cs,
                                                              String relPrefix,
                                                              Language language) {
    String path = cs.path();

//    boolean isLegacy = path.startsWith("answers");
//    String filePath = isLegacy ?
//        relPrefix + path :
//        trimPathForWebPage2(path);

    String filePath = database.getWebPageAudioRefWithPrefix(relPrefix, path);


    String json = cs.json();
    CorrectAndScore correctAndScore = new CorrectAndScore(cs.exerciseid(), cs.correct(), cs.pronscore(), cs.modified(),
        trimPathForWebPage2(filePath), json);

    Map<NetPronImageType, List<TranscriptSegment>> netPronImageTypeListMap = new ParseResultJson(serverProps, language).readFromJSON(json);

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

  public Map<Integer, Integer> getOldToNew(int projID) {
    List<SlickResult> allByProject = dao.getAllByProject(projID);
    Map<Integer, Integer> oldToNew = new HashMap<>(allByProject.size());
    allByProject.forEach(slickResult -> oldToNew.put(slickResult.legacyid(), slickResult.id()));
    logger.info("getOldToNew for " + projID + " -> found " + oldToNew.size());
    return oldToNew;
  }

  public Map<Integer, Integer> getOldToNewSince(int projID, long since) {
    List<SlickResult> allByProject = dao.getAllByProjectSince(projID, since);
    Map<Integer, Integer> oldToNew = new HashMap<>(allByProject.size());
    allByProject.forEach(slickResult -> oldToNew.put(slickResult.legacyid(), slickResult.id()));
    logger.info("getOldToNewSince for " + projID + " -> found " + oldToNew.size());
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
   * @return
   * @paramx language
   */
/*  private String getRelPrefix(String language) {
    String installPath = database.getServerProps().getAnswerDir();

    String s = language.toLowerCase();
    String prefix = installPath + File.separator + s;
    int netProfDurLength = database.getServerProps().getAudioBaseDir().length();

    return prefix.substring(netProfDurLength) + File.separator;
  }*/
  public Collection<SlickPerfResult> getPerf(int projid, float minScore) {
    return dao.perf(projid, minScore);
  }

  @Override
  public Map<String, Integer> getStudentAnswers(int projid) {
    List<Tuple2<String, Integer>> tuple2s = dao.studentAnswers(projid);
    Map<String, Integer> pathToUser = new HashMap<>();
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

  public Collection<SlickPerfResult> getPerfForUserInDialog(int userid, Collection<Integer> exids) {
    return dao.perfByUserInDialog(userid, exids);
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
