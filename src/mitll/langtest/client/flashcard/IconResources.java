/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.flashcard;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/** The resources we use */
public interface IconResources extends ClientBundle {

  /** Get access to the css resource during gwt compilation */
  @CssResource.NotStrict
  @Source("resources/css/baseIcons.css")
  CssResource css();

  /** Our sample image icon. Makes the image resource for the gwt-compiler's css composer accessible */
  @Source("resources/blueSpaceBar.png")
  com.google.gwt.resources.client.ImageResource enter();

  @Source("resources/animated_progress48.gif")
  com.google.gwt.resources.client.ImageResource waiting();

  @Source("resources/media-record-3.png")
  com.google.gwt.resources.client.ImageResource record1();


  @Source("resources/media-record-3_32x32.png")
  com.google.gwt.resources.client.ImageResource record1Small();

  @Source( "resources/media-record-4.png")
  com.google.gwt.resources.client.ImageResource record2();

  @Source( "resources/media-record-4_32x32.png")
  com.google.gwt.resources.client.ImageResource record2Small();

  @Source( "resources/checkmark48.png")
  com.google.gwt.resources.client.ImageResource correct();


  @Source( "resources/redx48.png")
  com.google.gwt.resources.client.ImageResource incorrect();

  @Source( "resources/record.png")
  com.google.gwt.resources.client.ImageResource record();

  @Source( "resources/stop.png")
  com.google.gwt.resources.client.ImageResource stop();

  @Source( "resources/white_48x48.png")
  com.google.gwt.resources.client.ImageResource white();

  @Source( "resources/gray_48x48.png")
  com.google.gwt.resources.client.ImageResource gray();
}
