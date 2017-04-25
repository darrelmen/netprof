package mitll.langtest.shared.project;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 4/24/17.
 */
public enum Language implements IsSerializable {
  ARABIC,
  CROATIAN,
  DARI,
  EGYPTIAN,
  ENGLISH,
  FARSI,
  FRENCH,
  GERMAN,
  HINDI,
  IRAQI,
  JAPANESE,
  LEVANTINE,
  KOREAN,
  MANDARIN,
  MSA,
  PASHTO,
  PORTUGUESE,
  RUSSIAN,
  SERBIAN,
  SORANI,
  SPANISH,
  SUDANESE,
  TAGALOG,
  TURKISH,
  URDU;

  public String toDisplay() {
    return name().substring(0,1)+name().substring(1).toLowerCase();
  }
}