package mitll.langtest.server.database;

import mitll.langtest.shared.Exercise;
import mitll.langtest.shared.Grade;
import mitll.langtest.shared.Result;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 11/29/12
 * Time: 6:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class Export implements Database {
  private ExerciseDAO exerciseDAO = null;
  private final ResultDAO resultDAO = new ResultDAO(this);
  private final GradeDAO gradeDAO = new GradeDAO(this);

  public Export(String dburl) {
    this.h2DbName = dburl;
    this.url = "jdbc:h2:" + h2DbName + ";IFEXISTS=TRUE;QUERY_CACHE_SIZE=0;";
    try {
      getConnection();
      resultDAO.createResultTable(getConnection());
    } catch (Exception e) {
      logger.error("got " + e, e);  //To change body of catch statement use File | Settings | File Templates.
    }

    exerciseDAO = new SQLExerciseDAO(this, "");
  }

  public List<ExerciseExport> getExport(boolean useFLQ,boolean useSpoken) {
    List<ExerciseExport> names = new ArrayList<ExerciseExport>();
//    int n = 20;

    Map<Integer, List<Grade>> idToGrade = getIdToGrade(gradeDAO.getGrades());
    Map<String,List<Result>> exerciseToResult = new HashMap<String, List<Result>>();
    List<Result> results = resultDAO.getResults();

    for (Result r : results) {
      List<Result> res = exerciseToResult.get(r.id);
      if (res == null) exerciseToResult.put(r.id, res = new ArrayList<Result>());
      res.add(r);
    }

    for (Exercise e : getExercises()) {
//      System.out.println("on " + e);
      List<Result> results1 = exerciseToResult.get(e.getID());
      List<ExerciseExport> resultsForExercise = getExports(idToGrade, results1, e, useFLQ, useSpoken);
      names.addAll(resultsForExercise);
      // if (n-- == 0) break;
    }
    return names;
  }

  public static class ExerciseExport {
    public String id;
    public List<String> key = new ArrayList<String>();
    public List<ResponseAndGrade> rgs = new ArrayList<ResponseAndGrade>();
    public ExerciseExport(String id, String key) {
      this.id  = id;
      this.key = Arrays.asList(key.split("\\|\\|"));
    }
    public void addRG(String response, int grade) { rgs.add(new ResponseAndGrade(response,grade)); }

    public String toString() {
      return "id " + id + " " + key.size() + " keys : '" + "" +/*new HashSet<String>(key) +   */
          "' and " + rgs.size() + " responses " + new HashSet<ResponseAndGrade>(rgs);
    }
  }


  public static class ResponseAndGrade {
    public String response;
    public float grade;
    public ResponseAndGrade(String response, int grade) {
      this.response = response;
      this.grade = ((float) grade)/5;
    }
    @Override
    public String toString() { return "grd " + grade + " for '" + "" /*response*/ +"'"; }
  }


  /**
   * @see #getExport(boolean, boolean)
   * @return
   */
  private List<Exercise> getExercises() {
    return exerciseDAO.getRawExercises();
  }

  /**
   * Complicated.  To figure out spoken/written, flq/english we have to go back and join against the schedule.
   * Unless the result column is set.
   * @param exercise
   * @param useFLQ
   * @param useSpoken
   * @return
   */
  public List<ExerciseExport> getExports(Map<Integer, List<Grade>> idToGrade, List<Result> resultsForExercise,Exercise exercise, boolean useFLQ, boolean useSpoken) {
    boolean debug = false;

    List<ExerciseExport> ret = new ArrayList<ExerciseExport>();

    Map<Integer,ExerciseExport> qidToExport = new HashMap<Integer, ExerciseExport>();
    int qid = 0;
    for (Exercise.QAPair q : exercise.getQuestions()) {
      ExerciseExport e1 = new ExerciseExport(exercise.getID()+"_"+ ++qid, q.getAnswer());
      qidToExport.put(qid,e1);
    }
    Set<ExerciseExport> valid = new HashSet<ExerciseExport>();

    // find results, after join with schedule, add join with the grade
    for (Result r : resultsForExercise) {
        if (r.flq == useFLQ && r.spoken == useSpoken) {
          ExerciseExport exerciseExport = qidToExport.get(r.qid);
          List<Grade> gradesForResult = idToGrade.get(r.uniqueID);
          if (gradesForResult == null) {
            //System.err.println("no grades for result " + r);
          }
          else {
            for (Grade g : gradesForResult) {
              if (g.grade > 0) {
              exerciseExport.addRG(r.answer, g.grade);
              if (!valid.contains(exerciseExport)) {
                ret.add(exerciseExport);
                valid.add(exerciseExport);
              }
            }
            }
          }
        }
        else {
          if (debug) System.out.println("\tSkipping result " + r + " since not match to " +useFLQ + " and " + useSpoken);
        }
    }
    return ret;
  }

  private Map<Integer, List<Grade>> getIdToGrade(Collection<Grade> grades) {
    Map<Integer,List<Grade>> idToGrade = new HashMap<Integer, List<Grade>>();
    for (Grade g : grades) {
      List<Grade> gradesForResult = idToGrade.get(g.resultID);
      if (gradesForResult == null) {
        idToGrade.put(g.resultID, gradesForResult = new ArrayList<Grade>());
      }
      gradesForResult.add(g);
     /* if (gradesForResult.size() > 1)
        System.out.println("r  " +g.resultID+ " grades " + gradesForResult.size());*/

    }
    // System.out.println("r->g " +idToGrade.size() + " keys " + idToGrade.keySet());
    return idToGrade;
  }

  //private class DatabaseImpl2 implements Database {
    private Logger logger = Logger.getLogger(Export.class);
    private String url = "jdbc:h2:" + H2_DB_NAME + ";IFEXISTS=TRUE;QUERY_CACHE_SIZE=0;",
        dbOptions = "",//"?characterEncoding=utf8&zeroDateTimeBehavior=convertToNull",
        driver = "org.h2.Driver";

    private static final boolean TESTING = false;

    private static final String H2_DB_NAME = TESTING ? "vlr-parle" : "/services/apache-tomcat-7.0.27/webapps/langTest/vlr-parle";

    private String h2DbName = H2_DB_NAME;

    public Connection getConnection() throws Exception {
      //  if (c != null) return c;
      Connection c;
      //  logger.info("install path is " +servlet.getInitParameter());
      try {
     //   if (servlet == null) {
          c = this.dbLogin();
      //  } else {
      //    ServletContext servletContext = servlet.getServletContext();
      //    c = (Connection) servletContext.getAttribute("connection");
     //   }
      } catch (Exception e) {  // for standalone testing
        logger.warn("The context DBStarter is not working : " + e.getMessage(), e);
        c = this.dbLogin();
      }
      if (c == null) {
        return c;
      }
      c.setAutoCommit(true);
      if (c.isClosed()) {
        logger.warn("getConnection : conn " + c + " is closed!");
      }
      return c;
    }

    /**
     * Just for dbLogin
     */
    private Connection localConnection;
    /**
     * Not necessary if we use the h2 DBStarter service -- see web.xml reference
     *
     * @return
     * @throws Exception
     */
    private Connection dbLogin() throws Exception {
      if (localConnection != null) return localConnection;
      try {
        Class.forName(driver).newInstance();
        try {
        //  url = servlet.getServletContext().getInitParameter("db.url"); // from web.xml
        } catch (Exception e) {
          logger.warn("no servlet context?");
        }
        logger.info("connecting to " + url);

    //    GWT.log("connecting to " + url);
        File f = new java.io.File(h2DbName + ".h2.db");
        if (!f.exists()) {
          String s = "huh? no file at " + f.getAbsolutePath();
          logger.warn(s);

      //    GWT.log(s);
        }
        Connection connection = DriverManager.getConnection(url + dbOptions);
        connection.setAutoCommit(false);
        boolean closed = connection.isClosed();
        if (closed) {
          logger.warn("connection is closed to : " + url);
        }
        this.localConnection = connection;
        return connection;
      } catch (Exception ex) {
        ex.printStackTrace();
        throw ex;
      }
    }


  public void closeConnection(Connection connection) throws SQLException {
    //  System.err.println("Closing " + connection);
    //connection.close();
    // System.err.println("Closing " + connection + " " + connection.isClosed());
  }

  public static void main(String[] arg) {
    Export langTestDatabase = new Export("C:\\Users\\go22670\\mt_repo\\jdewitt\\pilot\\vlr-parle");
    //List<Exercise> exercises = langTestDatabase.getExercises();
    if (true) {
      List<ExerciseExport> exerciseNames = langTestDatabase.getExport(true, false);

      System.out.println("names " + exerciseNames.size() + " e.g. " + exerciseNames.get(0));
      for (ExerciseExport ee : exerciseNames) {
        System.out.println("ee " + ee);
      }
    } else {
    //  List<Exercise> exercises = langTestDatabase.getRandomBalancedList();

    }
  }
}
