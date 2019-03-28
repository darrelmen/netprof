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

package mitll.langtest.server.database.refaudio;

import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.DatabaseServices;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.result.ISlimResult;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.result.SlimResult;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.ExerciseToPhone;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.shared.answer.AudioType;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickRefResult;
import mitll.npdata.dao.SlickRefResultJson;
import mitll.npdata.dao.refaudio.RefResultDAOWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

public class SlickRefResultDAO extends BaseRefResultDAO implements IRefResultDAO {
  private static final Logger logger = LogManager.getLogger(SlickRefResultDAO.class);
  private static final int WARN_THRESHOLD = 50;
  private static final String WORDS = "{\"words\":[]}";

  private final RefResultDAOWrapper dao;

  public SlickRefResultDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new RefResultDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  public boolean updateProject(int old, int newprojid) {
    return dao.updateProject(old, newprojid) > 0;
  }

  /**
   * @param bulk
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyRefResult
   */
  public void addBulk(List<SlickRefResult> bulk) {
    dao.addBulk(bulk);
  }

  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }


  /**
   * @param userID
   * @param projid
   * @param exid
   * @param durationInMillis
   * @param correct
   * @param alignOutput
   * @param decodeOutput
   * @param alignOutputOld
   * @param decodeOutputOld
   * @param isMale
   * @param speed
   * @param model
   * @return
   * @paramz audioFile
   * @see DatabaseServices#addRefAnswer
   */
  @Override
  public long addAnswer(int userID, int projid, int exid, int audioid,
                        long durationInMillis, boolean correct,
                        DecodeAlignOutput alignOutput, DecodeAlignOutput decodeOutput,
                        DecodeAlignOutput alignOutputOld, DecodeAlignOutput decodeOutputOld,
                        boolean isMale, String speed, String model) {
    SlickRefResult insert = dao.insert(toSlick(userID, projid, exid,
        audioid,
        durationInMillis, correct,
        alignOutput, decodeOutput, isMale, speed, model));
    return insert.id();
  }

  private SlickRefResult toSlick(int userID, int projid, int exid,
                                 int audioid,
                                 long durationInMillis, boolean correct,
                                 DecodeAlignOutput alignOutput, DecodeAlignOutput decodeOutput, boolean isMale,
                                 String speed, String model) {
    if (model == null) model = "";
    return new SlickRefResult(-1,
        userID,
        projid,
        exid,
        audioid,
        new Timestamp(System.currentTimeMillis()),
        //audioFile,
        durationInMillis,
        correct,
        decodeOutput.getScore(), decodeOutput.getJson(), decodeOutput.getNumPhones(), decodeOutput.getProcessDurInMillis(),
        alignOutput.getScore(), alignOutput.getJson(), alignOutput.getNumPhones(), alignOutput.getProcessDurInMillis(),
        isMale, speed,
        model
    );
  }

  public SlickRefResult toSlick(int projid, Result result, int audioID) {
    DecodeAlignOutput alignOutput = result.getAlignOutput();
    DecodeAlignOutput decodeOutput = result.getDecodeOutput();
    String model = result.getModel();
    if (model == null) model = "";
    return new SlickRefResult(-1,
        result.getUserid(),
        projid,
        result.getExerciseID(),
        audioID,
        new Timestamp(result.getTimestamp()),
        result.getDurationInMillis(),
        result.isCorrect(),
        decodeOutput.getScore(), decodeOutput.getJson(), decodeOutput.getNumPhones(), decodeOutput.getProcessDurInMillis(),
        alignOutput.getScore(), alignOutput.getJson(), alignOutput.getNumPhones(), alignOutput.getProcessDurInMillis(),
        result.isMale(),
        result.getAudioType().toString(),
        model
    );
  }

  @Override
  public boolean removeByAudioID(int audioID) {
    return dao.deleteByAudioID(audioID) > 0;
  }

  /**
   * @return
   * @see mitll.langtest.server.database.copy.CopyToPostgres#copyRefResult
   */
  @Override
  public List<Result> getResults() {
    List<SlickRefResult> all = dao.getAll();
    List<Result> results = new ArrayList<>(all.size());
    all.forEach(slickRefResult -> results.add(fromSlick(slickRefResult)));
    return results;
  }

  public List<Integer> getAllAudioIDsForProject(int projid) {
    return dao.getAllAudioID(projid);
  }


  @Override
  public Map<Integer, ISlimResult> getAudioIDMap(int projid, Set<Integer> audioIDs) {
    return getAudioIDMap(this.getAllSlimForProjectIn(projid, audioIDs));
  }

  @Override
  @NotNull
  public Map<Integer, ISlimResult> getAudioIDMap(int id) {
    return getAudioIDMap(getAllSlimForProject(id));
  }

  @NotNull
  private Map<Integer, ISlimResult> getAudioIDMap(Collection<ISlimResult> jsonResultsForProject) {
    Map<Integer, ISlimResult> audioToResult = new HashMap<>(jsonResultsForProject.size());
    jsonResultsForProject.forEach(iSlimResult -> audioToResult.put(iSlimResult.getAudioID(), iSlimResult));
    return audioToResult;
  }

  /**
   * @param projid
   * @return
   */
  public Collection<ISlimResult> getAllSlimForProject(int projid) {
    return dao.getAllSlimForProject(projid).stream().map(this::fromSlickToSlim).collect(Collectors.toList());
  }

  public Collection<ISlimResult> getAllSlimForProjectIn(int projid, Set<Integer> audioIDs) {
    return dao.getAllSlimIn(projid, audioIDs).stream().map(this::fromSlickToSlim).collect(Collectors.toList());
  }

  /**
   * @param projid
   * @return
   * @see DBExerciseDAO#readExercises
   */
  public Map<Integer, ExercisePhoneInfo> getExerciseToPhoneForProject(int projid) {
    return new ExerciseToPhone().getExerciseToPhoneForProject(getJsonResultsForProject(projid));
  }

  public int getNumPhonesForEx(int exid) {
    return dao.numPhonesForEx(exid);
  }

  private Collection<SlickRefResultJson> getJsonResultsForProject(int projid) {
    return dao.getAllSlimForProject(projid);
  }

  @Override
  public ISlimResult getResult(int audioid) {
    long then = System.currentTimeMillis();
    Collection<SlickRefResultJson> slickRefResults = dao.getAllSlimByAudioID(audioid);
    long now = System.currentTimeMillis();
    if (now - then > WARN_THRESHOLD) logger.info("getResult took " + (now - then) + " to lookup " + audioid);

    if (slickRefResults.isEmpty()) {
      // logger.info("getResult : no results for " + audioid);
      return null;
    } else {
      return fromSlickToSlim(slickRefResults.iterator().next());
    }
  }

  private Result fromSlick(SlickRefResult slickRef) {
    float pronScore = slickRef.pronscore();
    String scoreJson = slickRef.scorejson();
    boolean validDecodeJSON = pronScore > 0 && !scoreJson.equals(WORDS);

    float alignScore = slickRef.alignscore();
    String alignJSON = slickRef.alignscorejson();

    boolean validAlignJSON = alignScore > 0 && !alignJSON.contains(WORDS);

    if (validAlignJSON || validDecodeJSON) {
      float pronScore1 = validDecodeJSON ? pronScore : alignScore;
      String scoreJson1 = validDecodeJSON ? scoreJson : alignJSON;
      Result result = new Result(
          slickRef.id(),
          slickRef.userid(), //id
          // "", // plan
          slickRef.exid(), // id
          0, // qid
          "",//trimPathForWebPage2(slickRef.answer()), // answer
          true, // valid
          slickRef.modified().getTime(),
          AudioType.UNSET,
          slickRef.duration(),
          slickRef.correct(), pronScore1,
          "browser", "",
          0, 0, false, 30, "",
          slickRef.model());
      result.setJsonScore(scoreJson1);
      return result;
    } else return null;
  }

  /**
   * @param slickRef
   * @return
   * @see #getAllSlimForProject
   * @see #getResult
   */
  private SlimResult fromSlickToSlim(SlickRefResultJson slickRef) {
    String scoreJson = slickRef.scorejson();
    float alignScore = slickRef.alignscore();

    boolean validAlignJSON = alignScore > 0 && !scoreJson.contains(WORDS);

    if (!validAlignJSON && scoreJson != null) {
//      logger.info("fromSlickToSlim : slickRef " + slickRef + " not valid " + alignScore + " score " + scoreJson);
    }

    return new SlimResult(slickRef.exid(), slickRef.audioid(), validAlignJSON, scoreJson, alignScore);
  }

  @Override
  public int getNumResults() {
    return dao.getNumRows();
  }

  /**
   * @param projid
   * @see RefResultDecoder#writeRefDecode
   */
  @Override
  public void deleteForProject(int projid) {
    dao.deleteForProject(projid);
  }
}
