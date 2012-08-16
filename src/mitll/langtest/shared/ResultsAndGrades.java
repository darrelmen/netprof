package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
* Created with IntelliJ IDEA.
* User: GO22670
* Date: 8/16/12
* Time: 12:09 PM
* To change this template use File | Settings | File Templates.
*/
public class ResultsAndGrades implements IsSerializable {
  public Collection<Result> results;
  public Collection<Grade> grades;
  public ResultsAndGrades() {}
  public ResultsAndGrades(Collection<Result> results, Collection<Grade> grades) {
    this.results = results;
    this.grades  = grades;
  }
}
