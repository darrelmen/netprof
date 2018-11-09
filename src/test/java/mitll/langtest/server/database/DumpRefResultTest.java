package mitll.langtest.server.database;

import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.server.database.refaudio.RefResultDAO;
import mitll.langtest.server.database.result.Result;
import mitll.langtest.server.database.result.ResultDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 1/30/14.
 */
public class DumpRefResultTest extends BaseTest {
  private static final Logger logger = LogManager.getLogger(DumpRefResultTest.class);

  @Test
  public void dump() {
    List<String> strings = getDBs();
    logger.debug("Got " + strings);

    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      H2Connection connection = getH2(db);

      try {
        BufferedWriter writer = getWriter(db + ".csv");
        writer.write("id,exid," +
            "hydraDecode," +
            "hydraDecodeDur," +
            "hydraAlign," +
            "hydraAlignDur," +
            "hydecDecode," +
            "hydecDecodeDur," +
            "hydecAlign," +
            "hydecAlignDur," +
            "speed,path\n");
        getResults(connection, writer);
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private H2Connection getH2(String db) {
    String path = "../Development/asr/performance-reports/dbs/" + db.replaceAll(".h2.db", "");
    logger.debug("got " + path);
    return getH2Connection(path);
  }

  private List<String> getDBs() {
    String s =
        "npfDari.h2.db\n" +
            "npfClassroomEgyptian.h2.db\n" +
            "npfEnglish.h2.db\n" +
            "npfFarsi.h2.db\n" +
            "iraqi.h2.db\n" +
            "npfLevantine.h2.db\n" +
            "npfKorean.h2.db\n" +
            "mandarin.h2.db\n" +
            "msaClassroom.h2.db\n" +
            "pashtoCE.h2.db\n" +
            "pashto2.h2.db\n" +
            "pashto3.h2.db\n" +
            "npfRussian.h2.db\n"
            +
            "npfSpanish.h2.db\n" +

            "sudaneseToday.h2.db\n" +
            "npfTagalog.h2.db\n" +
            "npfUrdu.h2.db\n";
    String[] split = s.split("\n");
    List<String> strings = Arrays.asList(split);
    //  strings = Collections.singletonList("npfSpanish.h2.db");
    return strings;
  }

  private void getResults(H2Connection h2, BufferedWriter writer) {
    try {
      String sql = "SELECT * FROM " + "REFRESULT order by " +
          ResultDAO.PRON_SCORE + "," +
          RefResultDAO.ALIGNSCORE + "," +
          RefResultDAO.SPEED;

      List<Result> resultsForQuery = getResultsSQL(sql, h2, writer);
      // return resultsForQuery;
    } catch (Exception ee) {
      logger.error("ee " + ee);
    }
    //return new ArrayList<Result>();
  }

  private List<Result> getResultsSQL(String sql, H2Connection h2, BufferedWriter writer) throws SQLException, IOException {
    Connection connection = h2.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

    logger.debug("running " + sql);
    return getResultsForQuery(connection, statement, writer);
  }

  private List<Result> getResultsForQuery(Connection connection, PreparedStatement statement, BufferedWriter writer)
      throws SQLException, IOException {
    ResultSet rs = statement.executeQuery();
    List<Result> results = new ArrayList<Result>();
    int i = 0;
    while (rs.next()) {
      int uniqueID = rs.getInt(ResultDAO.ID);
      String exID = rs.getString(Database.EXID);

      float pronScore = rs.getFloat(ResultDAO.PRON_SCORE);
      long hydraDecodeDur = rs.getLong(RefResultDAO.DECODE_PROCESS_DUR);
      float alignScore = rs.getFloat(RefResultDAO.ALIGNSCORE);
      long hydraAlignDur = rs.getLong(RefResultDAO.ALIGN_PROCESS_DUR);

      float hpronScore = rs.getFloat(RefResultDAO.HYDEC_DECODE_PRON_SCORE);
      long hydecDecodeDur = rs.getLong(RefResultDAO.HYDEC_DECODE_PROCESS_DUR);

      float halignScore = rs.getFloat(RefResultDAO.HYDEC_ALIGN_PRON_SCORE);
      long hydecAlignDur = rs.getLong(RefResultDAO.HYDEC_ALIGN_PROCESS_DUR);

      String path = rs.getString(ResultDAO.ANSWER);
      String speed = rs.getString(RefResultDAO.SPEED);

      writer.write(uniqueID + "," + exID + "," +
          pronScore + "," +
          hydraDecodeDur + "," +
          alignScore + "," +
          hydraAlignDur + "," +

          hpronScore + "," +
          hydecDecodeDur + "," +

          halignScore + "," +
          hydecAlignDur + "," +
          speed +
          "," + path +
          "\n");
      i++;
    }
    finish(statement, rs);
    logger.debug("wrote " + i);
    return results;
  }

}
