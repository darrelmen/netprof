package mitll.langtest.server.database;

import mitll.langtest.server.PathHelper;
import mitll.langtest.server.ServerProperties;
import mitll.langtest.server.database.connection.H2Connection;
import mitll.langtest.shared.CommonExercise;
import mitll.langtest.shared.Result;
import mitll.langtest.shared.analysis.*;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Created by GO22670 on 1/30/14.
 */
public class DumpRefResultTest {
  private static final Logger logger = Logger.getLogger(DumpRefResultTest.class);

  @BeforeClass
  public static void setup() {
    logger.debug("setup called");
  }

  @Test
  public void dump() {
    String s = "iraqi.h2.db\n" +
        "mandarin.h2.db\n" +
        "mandarin.h2.db.h2.db\n" +
        "mandarin.h2.db.trace.db\n" +
        "msaClassroom.h2.db\n" +
        "npfClassroomEgyptian.h2.db\n" +
        "npfDari.h2.db\n" +
        "npfEnglish.h2.db\n" +
        "npfFarsi.h2.db\n" +
        "npfKorean.h2.db\n" +
        "npfLevantine.h2.db\n" +
        "npfRussian.h2.db\n" +
        "npfSpanish.h2.db\n" +
        "npfTagalog.h2.db\n" +
        "npfUrdu.h2.db\n" +
        "pashto2.h2.db\n" +
        "pashto3.h2.db\n" +
        "pashtoCE.h2.db\n" +
        "sudaneseToday.h2.db";
    String[] split = s.split("\n");
    List<String> strings = Arrays.asList(split);
    logger.debug("Got " + strings);

    for (String db : strings) {
      //String path = "/Users/go22670/Development/asr/performance-reports/dbs/" + db;
      String path = "../Development/asr/performance-reports/dbs/" + db.replaceAll(".h2.db","");
      logger.debug("got " + path);
      H2Connection connection = new H2Connection(".", path, true);

      try {
        BufferedWriter writer = getWriter(db + ".csv");
        writer.write("id,exid,hydraDecode,hydraAlign,hydecDecode,hydecAlign,speed,path\n");
        getResults(connection, writer);
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public List<Result> getResults(H2Connection h2, BufferedWriter writer) {
    try {
      String sql = "SELECT * FROM " + "REFRESULT order by " +
          ResultDAO.PRON_SCORE+ ","+
          RefResultDAO.ALIGNSCORE+ ","+
          RefResultDAO.SPEED;

      List<Result> resultsForQuery = getResultsSQL(sql, h2, writer);
      return resultsForQuery;
    } catch (Exception ee) {
      logger.error("ee " + ee);
    }
    return new ArrayList<Result>();
  }

  private List<Result> getResultsSQL(String sql, H2Connection h2, BufferedWriter writer) throws SQLException, IOException {
    Connection connection = h2.getConnection(this.getClass().toString());
    PreparedStatement statement = connection.prepareStatement(sql);

      logger.debug("running " + sql );
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
      float alignScore = rs.getFloat(RefResultDAO.ALIGNSCORE);

      float hpronScore = rs.getFloat(RefResultDAO.HYDEC_DECODE_PRON_SCORE);
      float halignScore = rs.getFloat(RefResultDAO.HYDEC_ALIGN_PRON_SCORE);
      String path = rs.getString(ResultDAO.ANSWER);
      String speed = rs.getString(RefResultDAO.SPEED);

      writer.write(uniqueID + "," + exID+","+pronScore + "," + alignScore + "," + hpronScore + "," + halignScore+
          ","+speed+
          ","+path+
          "\n");
      i++;
    }
    finish(connection, statement, rs);
    logger.debug("wrote " + i);
    return results;
  }

  protected void finish(Connection connection, Statement statement, ResultSet rs) throws SQLException {
    rs.close();
    statement.close();
  }


  private BufferedWriter getWriter(String prefix) throws IOException {
    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("MM_dd_yy_HH_mm_ss");
    String today = simpleDateFormat2.format(new Date());
    File file = getReportFile(new PathHelper("war"), today, prefix);
    logger.info("writing to " + file.getAbsolutePath());
    return new BufferedWriter(new FileWriter(file));
  }

  private File getReportFile(PathHelper pathHelper, String today, String prefix) {
    File reports = pathHelper.getAbsoluteFile("reports");
    if (!reports.exists()) {
      logger.debug("making dir " + reports.getAbsolutePath());
      reports.mkdirs();
    } else {
      logger.debug("reports dir exists at " + reports.getAbsolutePath());
    }
    String fileName = prefix + "_report_" + today + ".csv";
    return new File(reports, fileName);
  }
}
