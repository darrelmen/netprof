package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
* Results (answers), grades, and a breakdown of the results by spoken/written and en/fl->results
* User: GO22670
* Date: 8/16/12
* Time: 12:09 PM
* To change this template use File | Settings | File Templates.
*/
public class ResultsAndGrades implements IsSerializable {
  public Collection<Result> results;
  public Collection<Grade> grades;
  public Map<Boolean,Map<Boolean,List<Result>>> spokenToLangToResult;
  public ResultsAndGrades() {}

  /**
   * @see mitll.langtest.server.database.DatabaseImpl#getResultsForExercise(String)
   * @param results
   * @param grades
   */
  public ResultsAndGrades(Collection<Result> results, Collection<Grade> grades, Map<Boolean,Map<Boolean,List<Result>>> spokenToLangToResult) {
    this.results = results;
    this.grades  = grades;
    this.spokenToLangToResult = spokenToLangToResult;
  }
}
