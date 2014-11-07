package mitll.langtest.server.database;

import com.google.gwt.dev.Link;
import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.PathHelper;
import mitll.langtest.shared.Result;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/9/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class PhoneDAO extends DAO {
  private static final int MAX_EXAMPLES = 30;
  private static final Logger logger = Logger.getLogger(PhoneDAO.class);

  private static final String PHONE = "phone";
  public static final String RID = "rid";
  public static final String WID = "wid";
  public static final String SEQ = "seq";
 // public static final String SEQ = SEQ1;
  public static final String SCORE = "score";
  private LogAndNotify logAndNotify;

  /**
   * @param database
   * @paramx
   * @see DatabaseImpl#initializeDAOs(mitll.langtest.server.PathHelper)
   */
  public PhoneDAO(Database database, LogAndNotify logAndNotify) {
    super(database);
    this.logAndNotify = logAndNotify;
    try {
      createTable(database);
      createIndex(database, RID, PHONE);
      createIndex(database, WID, PHONE);
      Connection connection = database.getConnection(this.getClass().toString());
      database.closeConnection(connection);
    } catch (SQLException e) {
      logger.error("got " + e, e);
    }
  }

  public static class Phone {
    long id;
    long wid;
    long rid;
    String phone;
    int seq;
    float score;

    public Phone(long id, long rid, long wid, String phone, int seq, float score) {
      this(rid, wid, phone, seq, score);
      this.id = id;
    }

    public Phone(long rid, long wid, String phone, int seq, float score) {

      this.rid = rid;
      this.wid = wid;
      this.phone = phone;
      this.seq = seq;
      this.score = score;
    }

    public String toString() {
      return "# " + id + " rid " + rid + " wid " + wid + " : " + phone + " at " + seq + " score " + score;
    }
  }

  /**
   * Word – result id – word seq – score – uid
   * Do we care about start and end offsets into audio???
   *
   * @param database
   * @throws java.sql.SQLException
   */
  void createTable(Database database) throws SQLException {
    Connection connection = database.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " +
        PHONE +
        " (" +
        "ID IDENTITY, " +
        RID + " BIGINT, " +
        WID + " BIGINT, " +
        PHONE + " VARCHAR, " +
        SEQ + " INT, " +
        SCORE + " FLOAT, " +

        "FOREIGN KEY(" +
        RID +
        ") REFERENCES " +
        ResultDAO.RESULTS +
        "(ID)," +

        "FOREIGN KEY(" +
        WID +
        ") REFERENCES " +
        WordDAO.WORD +
        "(ID)" +

        ")");

    finish(database, connection, statement);
  }


  /**
   * <p/>
   *
   * @param phone
   */
  public boolean addPhone(Phone phone) {
    Connection connection = getConnection();
    boolean val = true;
    try {
      // there are much better ways of doing this...

      PreparedStatement statement = connection.prepareStatement(
          "INSERT INTO " + PHONE +
              "(" +
              RID + "," +
              WID + "," +
              PHONE + "," +
              SEQ + "," +
              SCORE +
              //"," +
              ") " +
              "VALUES(?,?,?,?,?);");
      int i = 1;

      statement.setLong(i++, phone.rid);
      statement.setLong(i++, phone.wid);
      statement.setString(i++, phone.phone);
      statement.setInt(i++, phone.seq);
      statement.setFloat(i++, phone.score);

      int j = statement.executeUpdate();

      if (j != 1) {
        logger.error("huh? didn't insert row for ");// + grade + " grade for " + resultID + " and " + grader + " and " + gradeID + " and " + gradeType);
        val = false;
      }
      statement.close();

    } catch (SQLException ee) {
      logger.error("trying to add event " + phone + " got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
      val = false;
    } finally {
      database.closeConnection(connection);
    }
    return val;
  }

  public List<Phone> getAll() {
    try {
      return getPhones("SELECT * from " + PHONE);
    } catch (Exception ee) {
      logger.error("got " + ee, ee);
      logAndNotify.logAndNotifyServerException(ee);
    }
    return Collections.emptyList();
  }

  public JSONObject getWorstPhonesJson(long userid, List<String> exids, Map<String, String> idToRef) {
    JSONObject jsonObject = new JSONObject();
    try {
      PhoneReport worstPhonesAndScore = getWorstPhones(userid, exids, idToRef);

      Map<String, List<WordAndScore>> worstPhones = worstPhonesAndScore.phoneToWordAndScoreSorted;
      Map<Long,String> resToAnswer = new HashMap<Long, String>();
      Map<Long,String> resToRef = new HashMap<Long, String>();
      Map<Long,String> resToResult = new HashMap<Long, String>();

      JSONObject phones = new JSONObject();

      for (Map.Entry<String,List<WordAndScore>> pair : worstPhones.entrySet()) {
        List<WordAndScore> value = pair.getValue();
        JSONArray words = new JSONArray();

        int count = 0;
        for (WordAndScore wordAndScore : value) {
          JSONObject word = new JSONObject();
          word.put("wid",Integer.toString(wordAndScore.wseq));
          word.put("seq",Integer.toString(wordAndScore.seq));
          word.put("w",wordAndScore.word);
          word.put("s",Float.toString(round(wordAndScore.score)));
          word.put("result",Long.toString(wordAndScore.resultID));
          resToAnswer.put(wordAndScore.resultID,wordAndScore.answerAudio);
          resToRef.put(wordAndScore.resultID,wordAndScore.refAudio);
          resToResult.put(wordAndScore.resultID,wordAndScore.scoreJson);
          words.add(word);

          if (count++ >MAX_EXAMPLES) {
            break;
          }
        }
        phones.put(pair.getKey(), words);
      }

      jsonObject.put("phones",phones);
      JSONArray order = new JSONArray();
      for (String phone : worstPhones.keySet()) order.add(phone);
      jsonObject.put("order",order);

      JSONObject results = new JSONObject();
      for (Map.Entry<Long, String> pair : resToAnswer.entrySet()) {
        JSONObject result = new JSONObject();

        Long key = pair.getKey();
        result.put("answer",pair.getValue());
        result.put("ref",resToRef.get(key));
        result.put("result",resToResult.get(key));

        results.put(Long.toString(key), result);
      }
      jsonObject.put("results",results);
      jsonObject.put("phoneScore",Integer.toString(worstPhonesAndScore.overallPercent));
    } catch (SQLException e) {
      logger.error("Got " +e,e);
    }
    return jsonObject;
  }

  private static float round(float d) {
    return round(d,3);
  }
  private static float round(float d, int decimalPlace) {
    BigDecimal bd = new BigDecimal(Float.toString(d));
    bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
    return bd.floatValue();
  }

  /**
   *
   * @param userid
   * @param exids
   * @return
   * @throws SQLException
   */
  private PhoneReport getWorstPhones(long userid, List<String> exids, Map<String, String> idToRef) throws SQLException {
    String sql = "select " +
        "results.exid," +
        "results.answer," +
        "results." +ResultDAO.SCORE_JSON+ "," +
        "results." +ResultDAO.PRON_SCORE+ "," +
        "results.time,  " +
        "word.seq, " +
        "word.word, " +
        "word.score, " +
        "phone.* " +

        " from " +
        "results, phone, word " +
        "where results.id = phone.rid AND " +
      ResultDAO.RESULTS+"."+ResultDAO.USERID +"="+userid+ " AND " + ResultDAO.RESULTS +"."+ Database.EXID+ " in (" +getInList(exids)+
        ")"+" AND phone.wid = word.id "+
        " order by results.exid, results.time desc";

    logger.debug("getWorstPhones query is " + sql);

    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();

    long currentRID = -1;
    Map<String,List<Float>> phoneToScores = new HashMap<String, List<Float>>();

    String currentExercise = "";
    Map<String,List<WordAndScore>> phoneToWordAndScore= new HashMap<String, List<WordAndScore>>();
    float totalScore = 0;
    float totalItems = 0;
    while (rs.next()) {
      int i = 1;
      String exid = rs.getString(i++);
      String audioAnswer = rs.getString(i++);
      String scoreJson = rs.getString(i++);
      float pronScore = rs.getFloat(i++);
      i++;
      int wseq = rs.getInt(i++);
      String word = rs.getString(i++);
      float wscore = rs.getFloat(i++);

      long rid = rs.getLong("RID");
      if (!exid.equals(currentExercise)) {
        currentRID = rid;
        currentExercise = exid;
        //logger.debug("adding " + exid + " score " + pronScore);
        totalScore += pronScore;
        totalItems++;
      }

      if (currentRID == rid) {
        String phone = rs.getString(PHONE);
        int seq = rs.getInt(SEQ);
        List<Float> scores = phoneToScores.get(phone);
        if (scores == null) phoneToScores.put(phone, scores = new ArrayList<Float>());
        scores.add(rs.getFloat(SCORE));

        List<WordAndScore> wordAndScores = phoneToWordAndScore.get(phone);
        if (wordAndScores == null) {
          phoneToWordAndScore.put(phone,wordAndScores = new ArrayList<WordAndScore>());
        }
        wordAndScores.add(new WordAndScore(word,wscore,rid, wseq,seq, trimPathForWebPage(audioAnswer), idToRef.get(exid), scoreJson));
      }
      //else {
        //logger.debug("skipping " + exid + " " + rid + " phone " + phone);
     // }
    }
    finish(connection, statement, rs);

    float overallScore = totalScore/totalItems;
    int percentOverall = (int) (100f*round(overallScore,2));
    logger.debug("score " +overallScore + " items " +totalItems + " percent " +percentOverall);

    logger.debug("phoneToScores " + phoneToScores);

    final Map<String,Float> phoneToAvg = new HashMap<String, Float>();
    for (Map.Entry<String,List<Float>> pair : phoneToScores.entrySet()) {
      String phone = pair.getKey();
      float total = 0f;
      List<Float> scores = pair.getValue();
      for (Float f : scores) total+= f;
      total /= ((float) scores.size());

      phoneToAvg.put(phone,total);
    }

    //logger.debug("phoneToAvg " + phoneToAvg);

    List<String> sorted = new ArrayList<String>(phoneToAvg.keySet());
    Collections.sort(sorted,new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        Float first = phoneToAvg.get(o1);
        Float second = phoneToAvg.get(o2);
        return first < second ? -1 : second > first ? +1 : 0;
      }
    });

  //  logger.debug("sorted " + sorted);

    Map<String, Float> phoneToAvgSorted = new LinkedHashMap<String, Float>();
    for (String phone : sorted) phoneToAvgSorted.put(phone, phoneToAvg.get(phone));
    //  logger.debug("phoneToAvgSorted " + phoneToAvgSorted);

    for (List<WordAndScore> words : phoneToWordAndScore.values()) {
      Collections.sort(words);
    }

    Map<String, List<WordAndScore>> phoneToWordAndScoreSorted = new LinkedHashMap<String, List<WordAndScore>>();

    for (String phone : sorted) {
      List<WordAndScore> value = phoneToWordAndScore.get(phone);
      phoneToWordAndScoreSorted.put(phone, value);
    }

    //logger.debug("phone->words " + phoneToWordAndScore);

    return new PhoneReport(percentOverall,phoneToWordAndScoreSorted);
  }

  private static class PhoneReport {
    int overallPercent;
    Map<String, List<WordAndScore>> phoneToWordAndScoreSorted;
    public PhoneReport(int overallPercent, Map<String,List<WordAndScore>> phoneToWordAndScoreSorted) {
      this.overallPercent = overallPercent;
      this.phoneToWordAndScoreSorted = phoneToWordAndScoreSorted;
    }
  }

  private List<Phone> getPhones(String sql) throws SQLException {
    Connection connection = getConnection();
    PreparedStatement statement = connection.prepareStatement(sql);
    ResultSet rs = statement.executeQuery();
    List<Phone> lists = new ArrayList<Phone>();

    while (rs.next()) {
      lists.add(new Phone(
              rs.getLong("ID"),
              rs.getLong("RID"),
              rs.getLong("WID"),
              rs.getString(PHONE),
              rs.getInt(SEQ),
              rs.getFloat(SCORE)
          )
      );
    }

    finish(connection, statement, rs);
    return lists;
  }

  private String trimPathForWebPage(String path) {
    int answer = path.indexOf(PathHelper.ANSWERS);
    return (answer == -1) ? path : path.substring(answer);
  }


  public static class WordAndScore implements Comparable<WordAndScore> {
    int wseq;
    int seq;
    String word;
    float score;
    long resultID;
    String answerAudio;
    String refAudio;
    String scoreJson;

    /**
     * @see #getWorstPhones(long, java.util.List, java.util.Map)
     * @param word
     * @param score
     * @param resultID
     * @param wseq which word in phrase
     * @param seq which phoneme in phrase (not in word)
     * @param answerAudio
     * @param refAudio
     * @param scoreJson
     */
    public WordAndScore(String word, float score, long resultID, int wseq, int seq, String answerAudio, String refAudio, String scoreJson) {
      this.word = word;
      this.score = score;
      this.resultID = resultID;
      this.wseq = wseq;
      this.seq = seq;
      this.answerAudio = answerAudio;
      this.refAudio = refAudio;
      this.scoreJson = scoreJson;
    }

    @Override
    public int compareTo(WordAndScore o) {
      return score < o.score ? -1 : score > o.score ? +1 : 0;
    }

    public String toString() { return "#" + wseq +" : " + word +" s " + score + " res " + resultID + " answer " + answerAudio + " ref " +refAudio; }
  }
}