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

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Much of the time the UI doesn't need to know a lot about a user so just send the little it needs.
 *
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 * @since 4/9/2014.
 */
public class MiniUser implements IsSerializable, Comparable<MiniUser> {
  private long id;
  private int age;
  private int gender;
  private String userID;
  private boolean isAdmin;

  public MiniUser() {
  } // for serialization

  /**
   * @param id
   * @param age
   * @param gender
   * @param userID
   * @param isAdmin
   */
  public MiniUser(long id, int age, int gender, String userID, boolean isAdmin) {
    this.id = id;
    this.age = age;
    this.gender = gender;
    this.userID = userID;
    this.isAdmin = isAdmin;
  }

  public boolean isDefault() { return id < 0; }
  public boolean isUnknownDefault() { return id == -1; }

  /**
   * It seems strange to copy the string here, but I think it will help the RPC code not try to serialize
   * the User this object is made from.
   *
   * @see mitll.langtest.server.database.UserDAO#getMiniUsers()
   * @param user
   */
  public MiniUser(User user) {
    this(user.getId(), user.getAge(), user.getGender(), new String(user.getUserID()), user.isAdmin());
  }

  public boolean isMale() {
    return gender == 0;
  }

  @Override
  public int compareTo(MiniUser o) {
    return id < o.id ? -1 : id > o.id ? +1 : 0;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof MiniUser) && compareTo((MiniUser)obj) == 0;
  }

  @Override
  public int hashCode() {
    return new Long(id).hashCode();
  }

  public int getGender() { return gender;  }

  public long getId() {
    return id;
  }

  public int getAge() {
    return age;
  }

  public String getUserID() {
    return userID;
  }

  /**
   * @see mitll.langtest.server.database.UserDAO#getUsers
   * @param userID
   */
  public void setUserID(String userID) {
    this.userID = userID;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public String toString() {
    return "mini-user " + id + " : " + age + " yr old " +
        (isMale() ? "male" : "female") +
        (isAdmin() ? "ADMIN" : "")
        ;
  }


}