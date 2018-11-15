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
  ARABIC(true, "ar", "al"),
  CHINESE(32, "CHINESE-MANDARIN", "cmn", "cn"),
  CROATIAN("SERBO-CROATIAN", "hr"),
  DARI(true, "PERSIAN-AFGHAN", "prs", "af"),
  EGYPTIAN(true, "ARABIC-EGYPTIAN", "arz", "eg"),
  ENGLISH("en", "us"),
  FARSI(true, "PERSIAN-IRANIAN", "fa", "ir"),
  FRENCH("fr", "fr"),
  GERMAN("de", "de"),
  HINDI("hi", "in"),
  IRAQI(true, "ARABIC-IRAQI", "ar-IQ", "iq"),
  JAPANESE("ja", "jp"),
  LEVANTINE(true, "ARABIC-LEVANTINE", "ar-SY", "sy"),
  KOREAN(32, "ko", "kr"),
  MSA(true, "ARABIC (MODERN STANDARD)", "ar", "al"),
  PASHTO(true, "PUSHTU-AFGHAN", "ps", "af"),
  PORTUGUESE("PORTUGUESE-BRAZILIAN", "pt", "br"),
  RUSSIAN("ru", "ru"),
  SERBIAN("SERBO-CROATIAN", "sr", "rs"),
  SORANI(true, "ku", "ku"),
  SPANISH("es", "es"),
  SUDANESE(true, "ARABIC-SUDANESE", "apd", "sd"),
  TAGALOG("tl", "ph"),
  TURKISH("tr", "tr"),
  URDU(true, "ur", "pk"),

  UNKNOWN("unk", "us");  // TROUBLE

  private final boolean isRTL;
  private int fontSize = 24;
  private String dominoName = "";
  private String locale = "";
  private String cc = "";

  Language(String locale, String cc) {
    this.isRTL = false;
    this.locale = locale;
    this.cc = cc;
  }

  Language(boolean isRTL, String locale, String cc) {
    this.isRTL = isRTL;
    this.locale = locale;
    this.cc = cc;
  }

  Language(boolean isRTL, String dominoName, String locale, String cc) {
    this(isRTL, locale, cc);
    this.dominoName = dominoName;
  }

  Language(String dominoName, String locale, String cc) {
    this(false, locale, cc);
    this.dominoName = dominoName;
  }

  Language(int fontSize, String locale, String cc) {
    this(false, locale, cc);
    this.fontSize = fontSize;
  }

  Language(int fontSize, String dominoName, String locale, String cc) {
    this(fontSize, locale, cc);
    this.dominoName = dominoName;
  }

  public String toDisplay() {
    return name().substring(0, 1) + name().substring(1).toLowerCase();
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
}