/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

/**
 * Created by go22670 on 12/16/15.
 */
public class StringObject extends FieldObject {
  private  String value;

  public StringObject() {}
  public StringObject(String value) { this.value = value; }

  public String getValue() {
    return value;
  }

  public Field.FIELD_TYPE getType() { return Field.FIELD_TYPE.STRING; }

  @Override
  public String toString() { return value; }
}
