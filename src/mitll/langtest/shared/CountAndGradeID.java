package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
* Created with IntelliJ IDEA.
* User: go22670
* Date: 8/24/12
* Time: 8:01 PM
* To change this template use File | Settings | File Templates.
*/
public class CountAndGradeID implements IsSerializable {
  public int count;
  public long gradeID;

  public CountAndGradeID() {}
  public CountAndGradeID(int c, long g) {
    this.count = c;
    this.gradeID = g;
  }
}
