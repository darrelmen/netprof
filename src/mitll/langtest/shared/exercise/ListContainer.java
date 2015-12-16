/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by go22670 on 12/16/15.
 */
public class ListContainer extends FieldObject {
  Collection<FieldObject> contents = new ArrayList<>();

  public ListContainer() {
  }

  public ListContainer(long id) {
    super(id);
  }

  public void add(FieldObject object) {
    contents.add(object);
  }
  public Collection<FieldObject> getContents() { return contents; }

  public Field.FIELD_TYPE getType() { return Field.FIELD_TYPE.LIST; }

  @Override
  public String toString() { return contents.toString(); }

}
