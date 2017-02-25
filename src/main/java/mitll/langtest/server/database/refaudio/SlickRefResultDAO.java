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

package mitll.langtest.server.database.refaudio;

import mitll.langtest.server.audio.DecodeAlignOutput;
import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.exercise.DBExerciseDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.userexercise.ExercisePhoneInfo;
import mitll.langtest.server.database.userexercise.ExerciseToPhone;
import mitll.langtest.server.decoder.RefResultDecoder;
import mitll.langtest.shared.answer.AudioType;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickRefResult;
import mitll.npdata.dao.SlickRefResultJson;
import mitll.npdata.dao.refaudio.RefResultDAOWrapper;
import net.sf.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scala.Tuple3;

import java.sql.Timestamp;
import java.util.*;

public class SlickRefResultDAO extends BaseRefResultDAO implements IRefResultDAO {
  private static final Logger logger = LogManager.getLogger(SlickRefResultDAO.class);

  private final RefResultDAOWrapper dao;

  public SlickRefResultDAO(Database database, DBConnection dbConnection, boolean dropTable) {
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

  public void insert(SlickRefResult word) {
    dao.insert(word);
  }

  public void addBulk(List<SlickRefResult> bulk) {
    dao.addBulk(bulk);
  }

  public boolean isEmpty() {
    return dao.getNumRows() == 0;
  }

  @Override
  public boolean removeForExercise(int exid) {
    return dao.deleteByExID(exid) > 0;
  }

  /**
   * @param userID
   * @param projid
   * @param exid
   * @param audioFile
   * @param durationInMillis
   * @param correct
   * @param alignOutput
   * @param decodeOutput
   * @param alignOutputOld
   * @param decodeOutputOld
   * @param isMale
   * @param speed
   * @param model            @return
   * @see mitll.langtest.server.database.DatabaseImpl#addRefAnswer
   */
  @Override
  public long addAnswer(int userID, int projid, int exid,
                        String audioFile, long durationInMillis, boolean correct,
                        DecodeAlignOutput alignOutput, DecodeAlignOutput decodeOutput,
                        DecodeAlignOutput alignOutputOld, DecodeAlignOutput decodeOutputOld,
                        boolean isMale, String speed, String model) {
    SlickRefResult insert = dao.insert(toSlick(userID, projid, exid, audioFile, durationInMillis, correct,
        alignOutput, decodeOutput, isMale, speed, model));
    return insert.id();
  }

  private SlickRefResult toSlick(int userID, int projid, int exid, String audioFile, long durationInMillis, boolean correct,
                         DecodeAlignOutput alignOutput, DecodeAlignOutput decodeOutput, boolean isMale,
                         String speed, String model) {
    if (model == null) model = "";
    return new SlickRefResult(-1,
        userID, projid,
        exid, new Timestamp(System.currentTimeMillis()), audioFile, durationInMillis,
        correct,
        decodeOutput.getScore(), decodeOutput.getJson(), decodeOutput.getNumPhones(), decodeOutput.getProcessDurInMillis(),
        alignOutput.getScore(), alignOutput.getJson(), alignOutput.getNumPhones(), alignOutput.getProcessDurInMillis(),
        isMale, speed,
        model
    );
  }

  public SlickRefResult toSlick(int projid, Result result) {
    DecodeAlignOutput alignOutput = result.getAlignOutput();
    DecodeAlignOutput decodeOutput = result.getDecodeOutput();
    String model = result.getModel();
    if (model == null) model = "";
    return new SlickRefResult(-1,
        result.getUserid(),
        projid,
        result.getExerciseID(),
        new Timestamp(result.getTimestamp()),
        result.getAnswer(), result.getDurationInMillis(),
        result.isCorrect(),
        decodeOutput.getScore(), decodeOutput.getJson(), decodeOutput.getNumPhones(), decodeOutput.getProcessDurInMillis(),
        alignOutput.getScore(), alignOutput.getJson(), alignOutput.getNumPhones(), alignOutput.getProcessDurInMillis(),
        result.isMale(),
        result.getAudioType().toString(),
        model
    );
  }


  @Override
  public boolean removeForAudioFile(String audioFile) {
    return dao.deleteByAudioFile(audioFile) > 0;
  }

  /**
   * @return
   * @see RefResultDecoder#getDecodedFiles
   */
  @Override
  public List<Result> getResults() {
    List<Result> results = new ArrayList<>();
    for (SlickRefResult refResult : dao.getAll()) results.add(fromSlick(refResult));
    return results;
  }

  public List<String> getAllFilesForProject(int projid) {
    return dao.getAllFiles(projid);
  }

  private List<SlickRefResultJson> getJsonResultsForProject(int projid) {
    return dao.getAllSlimForProject(projid);
  }

  /**
   * @param projid
   * @return
   * @see DBExerciseDAO#readExercises
   */
  public Map<Integer, ExercisePhoneInfo> getExerciseToPhoneForProject(int projid) {
    List<SlickRefResultJson> jsonResults = getJsonResultsForProject(projid);
    return new ExerciseToPhone().getExerciseToPhoneForProject(jsonResults);
  }

  @Override
  public Result getResult(int exid, String answer) {
    long then = System.currentTimeMillis();
    Collection<SlickRefResult> slickRefResults = dao.byExAndAnswer(exid, answer);
    long now = System.currentTimeMillis();
    if (now - then > 20) logger.info("took " + (now - then) + " to lookup " + exid);
    if (slickRefResults.isEmpty()) return null;
    else return fromSlick(slickRefResults.iterator().next());
  }

  /**
   * @param ids
   * @return
   * @see mitll.langtest.server.database.JsonSupport#getJsonRefResults(Map)
   */
  @Override
  public JSONObject getJSONScores(Collection<Integer> ids) {
    Collection<Tuple3<Integer, String, String>> tuple3s = dao.jsonByExIDs(ids);
    Map<Integer, List<String>> idToAnswers = new HashMap<>();
    Map<Integer, List<String>> idToJSONs = new HashMap<>();
    for (Tuple3<Integer, String, String> three : tuple3s) {
      Integer exid = three._1();
      String answer = three._2();
      String json = three._3();

      addToAnswers(idToAnswers, exid, answer);
      addToJSONs(idToJSONs, exid, json);
    }
    return getJsonObject(idToAnswers, idToJSONs);
  }

  private static final String WORDS = "{\"words\":[]}";

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
          trimPathForWebPage2(slickRef.answer()), // answer
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

  @Override
  public int getNumResults() {
    return dao.getNumRows();
  }

  @Override
  public void deleteForProject(int projid) {
    dao.deleteForProject(projid);
  }
}
