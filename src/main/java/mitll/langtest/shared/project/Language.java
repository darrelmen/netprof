package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 4/24/17.
 */
public enum Language implements IsSerializable {
  ARABIC(true),
  CROATIAN("SERBO-CROATIAN"),
  DARI(true, "PERSIAN-AFGHAN"),
  EGYPTIAN(true, "ARABIC-EGYPTIAN"),
  ENGLISH,
  FARSI(true, "PERSIAN-IRANIAN"),
  FRENCH,
  GERMAN,
  HINDI,
  IRAQI(true, "ARABIC-IRAQI"),
  JAPANESE,
  LEVANTINE(true, "ARABIC-LEVANTINE"),
  KOREAN(32),
  MANDARIN(32, "CHINESE-MANDARIN"),
  MSA(true, "ARABIC (MODERN STANDARD)"),
  PASHTO(true, "PUSHTU-AFGHAN"),
  PORTUGUESE("PORTUGUESE-BRAZILIAN"),
  RUSSIAN,
  SERBIAN("SERBO-CROATIAN"),
  SORANI(true),
  SPANISH,
  SUDANESE(true, "ARABIC-SUDANESE"),
  TAGALOG,
  TURKISH,
  URDU(true),
  UNKNOWN;  // TROUBLE

  private final boolean isRTL;
  private int fontSize = 24;
  private String dominoName = "";

  Language() {
    this.isRTL = false;
  }

  Language(boolean isRTL) {
    this.isRTL = isRTL;
  }

  Language(boolean isRTL, String dominoName) {
    this(isRTL);
    this.dominoName = dominoName;
  }

  Language(String dominoName) {
    this(false);
    this.dominoName = dominoName;
  }

  Language(int fontSize) {
    this(false);
    this.fontSize = fontSize;
  }

  Language(int fontSize, String dominoName) {
    this(fontSize);
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
}