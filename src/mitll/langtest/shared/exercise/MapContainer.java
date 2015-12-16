/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by go22670 on 12/16/15.
 */
public class MapContainer extends FieldObject {
  Map<String, FieldObject> contents = new HashMap<>();

  public MapContainer() {
  }

  public MapContainer(long id) {
    super(id);
  }

  public void put(String field, FieldObject value) {
    contents.put(field, value);
  }
  public void put(String field, String value) {
    contents.put(field, new StringObject(value));
  }

  public Map<String, FieldObject> getContents() {
    return contents;
  }

  public Field.FIELD_TYPE getType() { return Field.FIELD_TYPE.MAP; }

  public String toString() { return contents.toString(); }

}
