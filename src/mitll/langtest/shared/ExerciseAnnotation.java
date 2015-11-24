/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

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
  private String status;
  private String comment;

  public ExerciseAnnotation(){}
  public ExerciseAnnotation(String status, String comment) { this.status = status; this.comment = comment; }
  public boolean isCorrect() { return getStatus().equals("correct"); }

  public String getStatus() {
    return status;
  }

  public String getComment() {
    return comment;
  }

  public String toString() { return "Anno status=" + getStatus() + " : '" + getComment() +"'"; }
}
