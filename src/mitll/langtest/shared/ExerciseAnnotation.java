package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/12/13
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExerciseAnnotation implements IsSerializable {
  public String status;
  public String comment;
  public ExerciseAnnotation(){}
  public ExerciseAnnotation(String status, String comment) { this.status = status; this.comment = comment; }
  public String toString() { return "Anno " +status + " : " + comment; }
}
