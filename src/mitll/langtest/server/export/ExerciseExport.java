package mitll.langtest.server.export;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
* Created by go22670 on 1/30/15.
*/
public class ExerciseExport {
  /**
   * Exercise id
   */
  public final String id;
  /**
   * Answer key answers
   */
  public List<String> key = new ArrayList<String>();
  /**
   * List of response-grade pairs
   */
  public final List<ResponseAndGrade> rgs = new ArrayList<ResponseAndGrade>();

  /**
   * @see mitll.langtest.server.database.Export#populateIdToExportMap
   * @param id
   * @param key
   */
  public ExerciseExport(String id, String key) {
    this.id = id;
    List<String> c = Arrays.asList(key.split("\\|\\|"));
    for (String answer : c) {
      String trim = answer.trim();
      if (!trim.isEmpty()) {
        this.key.add(trim);
      }
    }
  }

  /**
   * @param response
   * @param grade
   * @param maxGrade
   * @see mitll.langtest.server.database.Export#addPredefinedAnswers
   * @see mitll.langtest.server.database.Export#getExports
   */
  public ResponseAndGrade addRG(String response, int grade, int maxGrade) {
    ResponseAndGrade e = new ResponseAndGrade(response, grade, maxGrade);
    rgs.add(e);
    return e;
  }

  public String toString() {
    return "id " + id + " " + key.size() + " keys : " + "" +
        new HashSet<String>(key) +
        " and " + rgs.size() + " responses "
        + new HashSet<ResponseAndGrade>(rgs);
  }
}
