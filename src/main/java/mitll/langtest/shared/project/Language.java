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

package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 4/24/17.
 *
 *
 * Consistent with BCP 47.
 * <p>
 * These are based on lookups in <a href='http://www.iana.org/assignments/language-subtag-registry/language-subtag-registry'>subtag-registry</a>
 * <p>
 * Also see <a href='https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl#Locale_identification_and_negotiation'>Locale identification and negotiation</a>
 */
public enum Language implements IsSerializable {
  ARABIC(true, "ar", "al", " العربية"),
  MANDARIN(32, "CHINESE-MANDARIN", "cmn", "cn", "Chinese", "中文"),
  CROATIAN("SERBO-CROATIAN", "hr", "hr", "Hrvatski"),
  DARI(true, "PERSIAN-AFGHAN", "prs", "af", "دری"),
  EGYPTIAN(true, "ARABIC-EGYPTIAN", "arz", "eg", "اللغة المصرية العامية"),
  ENGLISH("en", "us", "English"),
  FARSI(true, "PERSIAN-IRANIAN", "fa", "ir", "فارسی"),
  FRENCH("fr", "Français"),
  GERMAN("de", "Deutsch"),
  HINDI("hi", "in", "हिन्दी"),
  IRAQI(true, "ARABIC-IRAQI", "ar-IQ", "iq", "اللهجة العراقية"),
  JAPANESE("ja", "jp", "日本語"),
  LEVANTINE(true, "ARABIC-LEVANTINE", "ar-SY", "sy", "اللَّهْجَةُ الشَّامِيَّة"),
  KOREAN(32, "ko", "kr", "한국어"),
  MSA(true, "ARABIC (MODERN STANDARD)", "ar", "al", "MSA", "العربية الفصحى, عربي فصيح"),
  PASHTO(true, "PUSHTU-AFGHAN", "ps", "af", "پښتو"),
  PORTUGUESE("PORTUGUESE-BRAZILIAN", "pt", "br", "Português"),
  RUSSIAN("ru", "Русский"),
  SERBIAN("SERBO-CROATIAN", "sr", "rs", "српски"),
  SORANI(true, "ku", "ku", "سۆرانی"),
  SPANISH("es", "Español"),
  SUDANESE(true, "ARABIC-SUDANESE", "apd", "sd", "سوداني"),
  TAGALOG("tl", "ph", "Tagalog"),
  TURKISH("tr", "tr", "Türkçe"),
  URDU(true, "ur", "pk", "اُردُو"),

  UNKNOWN("unk", "us", "Unknown");  // TROUBLE

  private final boolean isRTL;
  private int fontSize = 24;
  private String dominoName = "";
  private String locale = "";
  private String cc = "";
  private String displayName = "";
  private String actualName = "";

  Language(String locale, String cc, String actualName) {
    this.isRTL = false;
    this.locale = locale;
    this.cc = cc;
    this.actualName = actualName;
  }

  Language(String localeAndCC, String actualName) {
    this.isRTL = false;
    this.locale = localeAndCC;
    this.cc = localeAndCC;
    this.actualName = actualName;
  }

  Language(boolean isRTL, String locale, String cc, String actualName) {
    this.isRTL = isRTL;
    this.locale = locale;
    this.cc = cc;
    this.actualName = actualName;
  }

  Language(boolean isRTL, String dominoName, String locale, String cc, String actualName) {
    this(isRTL, locale, cc, actualName);
    this.dominoName = dominoName;
  }

  Language(boolean isRTL, String dominoName, String locale, String cc, String displayName, String actualName) {
    this(isRTL, locale, cc, actualName);
    this.dominoName = dominoName;
    this.displayName = displayName;
  }

  Language(String dominoName, String locale, String cc, String actualName) {
    this(false, locale, cc, actualName);
    this.dominoName = dominoName;
  }

  Language(int fontSize, String locale, String cc, String actualName) {
    this(false, locale, cc, actualName);
    this.fontSize = fontSize;
  }

  /**
   * So we can support language renaming - Mandarin->Chinese
   *
   * @param fontSize
   * @param dominoName
   * @param locale
   * @param cc
   * @param displayName
   */
  Language(int fontSize, String dominoName, String locale, String cc, String displayName, String actualName) {
    this(fontSize, locale, cc, actualName);
    this.dominoName = dominoName;
    this.displayName = displayName;
    this.actualName = actualName;
  }

  public String toDisplay() {
    return displayName.isEmpty() ? name().substring(0, 1) + name().substring(1).toLowerCase() : displayName;
  }

  public boolean isRTL() {
    return isRTL;
  }

  public int getFontSize() {
    return fontSize;
  }

  public String getDominoName() {
    return dominoName;
  }

  public String getLanguage() {
    return toString().toLowerCase();
  }

  public String getLocale() {
    return locale;
  }

  public String getCC() {
    return cc;
  }

  public String getActualName() {
    return actualName;
  }
}