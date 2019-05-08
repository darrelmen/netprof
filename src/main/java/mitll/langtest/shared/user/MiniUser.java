/*
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2019. Other request for this document shall be referred
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
 * © 2015-2019 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 */

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.server.database.user.UserDAO;
import org.jetbrains.annotations.Nullable;

public class MiniUser extends FirstLastUser {
  private int age;

  private boolean isMale;
  private Gender realGender = Gender.Unspecified;

  private boolean isAdmin;

  public enum Gender implements IsSerializable {
    Unspecified,
    Male,
    Female
  }

  public MiniUser() {
  } // for serialization

  /**
   * @see mitll.langtest.server.database.user.BaseUserDAO#DEFAULT_USER
   * @param id
   * @param age
   * @param isMale
   * @param userID
   */
  public MiniUser(int id, int age, boolean isMale, String userID) {
    super(id);
    this.age = age;
    this.isMale = isMale;
    this.realGender = isMale ? Gender.Male : Gender.Female;

    this.userID  = userID;
    this.isAdmin = false;
  }

  /**
   * @param id
   * @param age
   * @param isMale
   * @param userID
   * @param isAdmin
   */
  public MiniUser(int id, int age, boolean isMale, Gender realGender, String userID, boolean isAdmin) {
    super(id);
    this.age = age;
    this.isMale = isMale;
    this.realGender = realGender;

    this.userID = userID;
    this.isAdmin = isAdmin;
  }

  public boolean isDefault() {
    return getID() < 0;
  }

  public boolean isUnknownDefault() {
    return getID() == -1;
  }

  /**
   * It seems strange to copy the string here, but I think it will help the RPC code not try to serialize
   * the User this object is made from.
   *
   * @param user
   * @see UserDAO#getMiniUsers()
   */
  public MiniUser(User user) {
    this(user.getID(), user.getAge(), user.isMale(), user.getRealGender(), user.getUserID(), user.isAdmin());
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof MiniUser) && compareTo((MiniUser) obj) == 0;
  }

  @Override
  public int hashCode() {
    return new Integer(getID()).hashCode();
  }

  public int getAge() {
    return age;
  }

  /**
   * @param userID
   * @see UserDAO#getUsers
   */
  public void setUserID(String userID) {
    this.userID = userID;
  }

  /**
   * @paramx startTime
   * @see mitll.langtest.server.database.user.DominoUserDAOImpl#getMini
   */
  public void setTimestampMillis(long startTime) {
    setLastChecked(startTime);
  }

  public String getFullName() {
    return first != null && !first.isEmpty() || last != null && !last.isEmpty() ? getName() : getUserID();
  }

  @Nullable
  public String getFirstInitialName() {
    String f = first == null ? "" :
        (first.length() > 0 ?
            first.substring(0, 1) + ". " : "");
    String l = last == null ? "" : last;
    String both = f + l;
    // logger.info("getFirstInitialName Got " +userid + " " + firstLastUser + " : " + s);

    if (both.isEmpty() || both.equalsIgnoreCase("F. Last")) {
      both = getUserID();
    }
    // logger.info("now Got " +userid + " " + firstLastUser + " : " + s);

    return both;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public String getOldID() {
    return "";
  }

  /**
   * Can't depend on this anymore!
   * @return
   */
  @Deprecated
  public boolean isMale() {
    return isMale;
  }

  public void setMale(boolean male) {
    isMale = male;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public Gender getRealGender() {
    return realGender;
  }

  public int getRealGenderInt() {
    int gender;
    switch (realGender) {
      case Male:
        gender = 0;
        break;
      case Female:
        gender = 1;
        break;
      default:
        gender = 2;
    }
    return gender;
  }

  public void setRealGender(Gender realGender) {
    this.realGender = realGender;
  }

  public String toString() {
    return "mini-user " + getID() + " : " + age + " yr old " +
        (isMale() ? "male" : "female") +
        (isAdmin() ? "ADMIN" : "");
  }
}