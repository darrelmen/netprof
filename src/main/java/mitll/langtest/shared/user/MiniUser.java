/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.user.UserDAO;
import mitll.langtest.shared.exercise.HasID;

public class MiniUser implements HasID, Comparable<HasID> {
  protected String first = "";
  protected String last = "";
  private int id;
  private int age;

  private boolean isMale;
  private Gender realGender = Gender.Unspecified;

  private String userID;
  private boolean isAdmin;
  protected long timestamp;

  public enum Gender implements IsSerializable {
    Unspecified,
    Male,
    Female
  }

  public MiniUser() {
  } // for serialization

  /**
   * @param id
   * @param age
   * @param isMale
   * @param userID
   * @param isAdmin
   */
  public MiniUser(int id, int age, boolean isMale, String userID, boolean isAdmin) {
//    setFields(id, age, isMale, userID, isAdmin);
//  }
//
//  private void setFields(int id, int age, boolean isMale, String userID, boolean isAdmin) {
    this.id = id;
    this.age = age;
    this.isMale = isMale;

    this.userID = userID;
    this.isAdmin = isAdmin;
  }

  public boolean isDefault() {
    return id < 0;
  }

  public boolean isUnknownDefault() {
    return id == -1;
  }

  /**
   * It seems strange to copy the string here, but I think it will help the RPC code not try to serialize
   * the User this object is made from.
   *
   * @param user
   * @see UserDAO#getMiniUsers()
   */
  public MiniUser(User user) {
    this(user.getID(), user.getAge(), user.isMale(), new String(user.getUserID()), user.isAdmin());
  }

  @Override
  public int compareTo(HasID o) {
    return id < o.getID() ? -1 : id > o.getID() ? +1 : 0;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof MiniUser) && compareTo((MiniUser) obj) == 0;
  }

  @Override
  public int hashCode() {
    return new Long(id).hashCode();
  }

  public int getGender() {
    return isMale ? 0 : 1;
  }

  public int getID() {
    return id;
  }

  public int getAge() {
    return age;
  }

  public String getUserID() {
    return userID;
  }

  /**
   * @param userID
   * @see UserDAO#getUsers
   */
  public void setUserID(String userID) {
    this.userID = userID;
  }

  public long getTimestampMillis() {
    return timestamp;
  }

  public void setTimestampMillis(long startTime) {
    this.timestamp = startTime;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public String getFirst() {
    return first;
  }

  public String getLast() {
    return last;
  }

  public void setFirst(String first) {
    this.first = first;
  }

  public void setLast(String last) {
    this.last = last;
  }

  public String getOldID() {return "";}
  public boolean isMale() {  return isMale;  }

  public void setMale(boolean male) {
    isMale = male;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public Gender getRealGender() {   return realGender;  }

  public void setRealGender(Gender realGender) {
    this.realGender = realGender;
  }

  public String toString() {
    return "mini-user " + id + " : " + age + " yr old " +
        (isMale() ? "male" : "female") +
        (isAdmin() ? "ADMIN" : "");
  }
}