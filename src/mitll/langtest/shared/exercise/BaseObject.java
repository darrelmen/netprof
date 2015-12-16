/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 12/16/15.
 */
public class BaseObject extends FieldObject {
  private String exid;
  Map<String, Field> nameToField = new HashMap<>();

  public BaseObject() {
  }

  public BaseObject(long id, String exid) {
    super(id);
    this.exid = exid;
  }

  public void addField(String name, Field field) {
    nameToField.put(name, field);
  }

  public void addField(String name, String value) {
    addField(name, new Field(name, value, getObjectRef()));
  }

  @Override
  public String toString() {
    return "ex " + getObjectRef() +"/"+getID() + " : " + nameToField.toString();
  }

  public String getID() {
    return exid;
  }

  public Field.FIELD_TYPE getType() { return Field.FIELD_TYPE.OBJECT; }
/*
  public long getObjectRef() {
    return id;
  }
*/

/*  private static class FieldRef {
    boolean isPrimitive;
    boolean isCollection;
    boolean isMap;
    Map<String,String>

    public FieldRef(boolean isPrimitive,boolean isCollection, boolean isMap, Field field) {
      this.isPrimitive = isPrimitive;
      this.isCollection = isCollection;
      this.isMap = isMap;

      if (isMap) {

      }
    }
  }*/
}
