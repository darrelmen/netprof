package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 4/24/17.
 */
public enum Language implements IsSerializable {
  ARABIC(true),
  CROATIAN,
  DARI(true),
  EGYPTIAN(true),
  ENGLISH,
  FARSI(true),
  FRENCH,
  GERMAN,
  HINDI,
  IRAQI(true),
  JAPANESE,
  LEVANTINE(true),
  KOREAN,
  MANDARIN,
  MSA(true),
  PASHTO(true),
  PORTUGUESE,
  RUSSIAN,
  SERBIAN,
  SORANI(true),
  SPANISH,
  SUDANESE(true),
  TAGALOG,
  TURKISH,
  URDU(true),
  UNKNOWN;  // TROUBLE

  private final boolean isRTL;

  Language() {
    this.isRTL = false;
  }

  Language(boolean isRTL) {
    this.isRTL = isRTL;
  }

  public String toDisplay() {
    return name().substring(0, 1) + name().substring(1).toLowerCase();
  }

  public boolean isRTL() {
    return isRTL;
  }
}