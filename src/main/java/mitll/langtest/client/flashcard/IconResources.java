/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
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
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.client.flashcard;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

/**
 * The resources we use
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public interface IconResources extends ClientBundle {

  /**
   * Get access to the css resource during gwt compilation
   */
  @CssResource.NotStrict
  @Source("resources/css/baseIcons.css")
  CssResource css();

  /**
   * Our sample image icon. Makes the image resource for the gwt-compiler's css composer accessible
   */
  @Source("resources/blueSpaceBar.png")
  com.google.gwt.resources.client.ImageResource enter();

  @Source("resources/animated_progress48.gif")
  com.google.gwt.resources.client.ImageResource waiting();

  @Source("resources/media-record-3.png")
  com.google.gwt.resources.client.ImageResource record1();


  @Source("resources/media-record-3_32x32.png")
  com.google.gwt.resources.client.ImageResource record1Small();

  @Source("resources/media-record-4.png")
  com.google.gwt.resources.client.ImageResource record2();

  @Source("resources/media-record-4_32x32.png")
  com.google.gwt.resources.client.ImageResource record2Small();

  @Source("resources/checkmark48.png")
  com.google.gwt.resources.client.ImageResource correct();


  @Source("resources/redx48.png")
  com.google.gwt.resources.client.ImageResource incorrect();

  @Source("resources/record.png")
  com.google.gwt.resources.client.ImageResource record();

  @Source("resources/stop.png")
  com.google.gwt.resources.client.ImageResource stop();

  @Source("resources/white_48x48.png")
  com.google.gwt.resources.client.ImageResource white();

  @Source("resources/gray_48x48.png")
  com.google.gwt.resources.client.ImageResource gray();

  // @Source( "resources/turtle_16_selected.png")
  @Source("resources/rabbit_32x21.png")
  com.google.gwt.resources.client.ImageResource rabbit();

  @Source("resources/turtle_32x21.png")
  com.google.gwt.resources.client.ImageResource turtle();
}
