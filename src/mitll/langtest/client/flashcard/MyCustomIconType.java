/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.flashcard;

import com.github.gwtbootstrap.client.ui.constants.BaseIconType;
import com.google.gwt.core.client.GWT;

/** My custom base icon collection */
public enum MyCustomIconType implements BaseIconType {

  enter,// Our runtime access
  waiting, // Our runtime access
  record1, // Our runtime access
  record2, // Our runtime access
  correct, // Our runtime access
  record, // Our runtime access
  stop, // Our runtime access
  white, // Our runtime access
  gray, // Our runtime access
  incorrect; // Our runtime access

  /** Inject the icon's css once at first usage */
  static {
    IconResources icons = GWT.create(IconResources.class);
    icons.css().ensureInjected();
  }

  private static final String PREFIX = "myBaseIcon_";
  private final String className;

  MyCustomIconType() {
    this.className = this.name().toLowerCase();
  }
  @Override public String get() {
    return PREFIX + className;
  }
}
