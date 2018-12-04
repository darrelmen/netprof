package mitll.langtest.shared.dialog;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Dialog-specific meta data.
 */
public enum DialogMetadata implements IsSerializable {
  UNIT,
  CHAPTER,
  PAGE,
  PRESENTATION,
  FLPRESENTATION,
  SPEAKER,
  FLTITLE;

  public String getLC() {
    return toString().toLowerCase();
  }

  public String getCap() {
    return toString().substring(0, 1).toUpperCase() + toString().substring(1);
  }
}
