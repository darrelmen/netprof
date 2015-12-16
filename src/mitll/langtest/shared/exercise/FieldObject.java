/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 12/16/15.
 */
public abstract class FieldObject implements IsSerializable {
  private long id;

  public FieldObject() {
  }

  public FieldObject(long id) {
    this.id = id;
  }

  public long getObjectRef() {
    return id;
  }
  abstract Field.FIELD_TYPE getType();
}
