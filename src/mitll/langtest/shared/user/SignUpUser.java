/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies 
 * and their contractors; 2015. Other request for this document shall be referred 
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted 
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA). 
 * Transfer of this data by any means to a non-US person who is not eligible to 
 * obtain export-controlled data is prohibited. By accepting this data; the consignee 
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For 
 * unclassified; limited distribution documents; destroy by any method that will 
 * prevent disclosure of the contents or reconstruction of the document.
 *  
 * This material is based upon work supported under Air Force Contract No. 
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions; findings; conclusions 
 * or recommendations expressed in this material are those of the author(s) and 
 * do not necessarily reflect the views of the U.S. Air Force.
 *  
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights; as defined in DFARS 
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice; 
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or 
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically 
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.shared.user;

import com.google.gwt.user.client.rpc.IsSerializable;

public class SignUpUser implements IsSerializable {
  private String userID;
  private String freeTextPassword;

  /**
   * @deprecated
   */
  private String passwordH;

  private String emailH;
  private String email;
  private User.Kind kind;
  private boolean isMale;
  private int age;
  private String dialect;
  private String device;
  private String first;
  private String last;
  private String ip;
  private String url;

  public SignUpUser() {
  }

  /**
   * @see mitll.langtest.client.user.SignUpForm#gotSignUp
   * @param userID
   * @param freeTextPassword
   * @param passwordH
   * @param emailH
   * @param email
   * @param kind
   * @param isMale
   * @param age
   * @param dialect
   * @param device
   * @param ip
   * @param first
   * @param last
   * @param url
   */
  public SignUpUser(String userID,
                    String freeTextPassword,
                    String passwordH,
                    String emailH,
                    String email,
                    User.Kind kind,
                    boolean isMale,
                    int age,
                    String dialect,
                    String device,
                    String ip,
                    String first,
                    String last, String url) {
    this.userID = userID;
    this.passwordH = passwordH;
    this.freeTextPassword = freeTextPassword;
    this.emailH = emailH;
    this.email = email;
    this.kind = kind;
    this.isMale = isMale;
    this.age = age;
    this.dialect = dialect;
    this.device = device;
    this.ip = ip;
    this.first = first;
    this.last = last;
    this.url = url;
  }

  public String getUserID() {
    return userID;
  }

  public String getPasswordH() {
    return passwordH;
  }
  public String getFreeTextPassword() {
    return freeTextPassword;
  }

  public String getEmailH() {
    return emailH;
  }

  public String getEmail() {
    return email;
  }

  public User.Kind getKind() {
    return kind;
  }

  public boolean isMale() {
    return isMale;
  }

  public int getAge() {
    return age;
  }

  public String getDialect() {
    return dialect;
  }

  public String getDevice() {
    return device;
  }

  public String getFirst() {
    return first;
  }

  public String getLast() {
    return last;
  }

  public String getIp() {
    return ip;
  }

  public SignUpUser setIp(String ip) {
    this.ip = ip;
    return this;
  }

  public SignUpUser setKind(User.Kind kind) {
    this.kind = kind;
    return this;
  }

  public SignUpUser setMale(boolean male) {
    isMale = male;
    return this;
  }

  public String getUrl() {
    return url;
  }
}
