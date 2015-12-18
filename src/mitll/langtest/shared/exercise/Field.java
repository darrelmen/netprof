/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared.exercise;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Collection;

/**
 * Created by go22670 on 12/16/15.
 */
public class Field implements IsSerializable {
  //private boolean isPrimitive;
  private String name;
  private FIELD_TYPE type;
  //private boolean isCollection;
  // private boolean isMap;
  private String value;
  private long objectRef;
  private long id;
  private FieldObject fieldObject;
//  private String exid;

  enum FIELD_TYPE {STRING, OBJECT, LIST, MAP}

  public Field() {
  }

  public Field(long id, String name,
               //       boolean isPrimitive,
               //     boolean isCollection, boolean isMap,
               FIELD_TYPE type, String value,
               long objectRef) {
    this.id = id;
    this.name = name;
    //  this.isPrimitive = isPrimitive;
    // this.isCollection = isCollection;
    // this.isMap = isMap;
    this.type = type;
    this.value = value;
    this.objectRef = objectRef;
//    this.exid = exid;
  }

  /**
   * exObject.addField("english", new Field(-1, "english", true, false, "String", ex.getEnglish(), -1, ex.getID()));
   */
  public Field(String name, String value) {
    this(-1, name,
        FIELD_TYPE.STRING, value, -1);
  }

  public Field(String name, String value, long objectRef) {
    this(-1, name,
        FIELD_TYPE.STRING, value, objectRef);
  }

  public Field(String name, BaseObject baseObject) {
    this(-1, name,
        FIELD_TYPE.OBJECT, "", baseObject.getObjectRef());
    this.fieldObject = baseObject;
  }

  public Field(String name, ListContainer baseObject) {
    this(-1, name,
        //true,
        //isCollection, isMap,
        FIELD_TYPE.LIST, "", baseObject.getObjectRef());
    this.fieldObject = baseObject;
  }

  public Field(String name, MapContainer baseObject) {
    this(-1, name,
        FIELD_TYPE.MAP, "", baseObject.getObjectRef());
    this.fieldObject = baseObject;
  }

  public Field(String name, FIELD_TYPE type) {
    this.name = name;
    this.type = type;
    switch (type) {
      case LIST:
        fieldObject = new ListContainer();
        break;
      case MAP:
        fieldObject = new MapContainer();
        break;
      default:
        throw new IllegalArgumentException("unknown field " +type);
    }
  }

  public boolean isPrimitive() {
    return type == FIELD_TYPE.STRING;
  }

  public FIELD_TYPE getType() {
    return type;
  }

  public String getValue() {
    return value;
  }

  public void add(FieldObject object) {
    ListContainer fieldObject = (ListContainer) this.fieldObject;
    fieldObject.add(object);
  }

  public Collection<FieldObject> getList() {
    ListContainer fieldObject = (ListContainer) this.fieldObject;
    return fieldObject.getContents();
  }

  public void put(String name, FieldObject object) {
    MapContainer fieldObject = (MapContainer) this.fieldObject;
    fieldObject.put(name, object);
  }

  public void put(String name, String value) {
    MapContainer fieldObject = (MapContainer) this.fieldObject;
    fieldObject.put(name, new StringObject(value));
  }

  public long getObjectRef() {
    return objectRef;
  }

  public long getId() {
    return id;
  }

/*
  public String getExid() {
    return exid;
  }
*/

  @Override
  public String toString() {
    return //"ex " + getObjectRef() + "/" + getName() + " : " +
        "" + (isPrimitive() ? value : fieldObject);
  }

  public String getName() {
    return name;
  }
}
