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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.LabelType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hidden;
import mitll.langtest.client.domino.common.UploadViewBase;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

class ImageUpload extends UploadViewBase {
  private Logger logger = Logger.getLogger("ImageUpload");

  private int dialogID;

  private String imageRef;

  ImageUpload(int userID, int dialogID, String imageRef) {
    super(userID);
    this.dialogID = dialogID;
    this.imageRef = imageRef;
    logger.info("dialogID " + dialogID);
  }

  @NotNull
  @Override
  protected String getDialogTitle() {
    return "Upload image";
  }

  @NotNull
  @Override
  protected String getHint() {
    return "Choose an image";
  }

  @NotNull
  @Override
  protected String getAcceptValue() {
    return "image/*";
  }

  @NotNull
  @Override
  protected String getService() {
    return "audio-manager";
  }

  /**
   * @param projectID
   * @see #init(int)
   */
  @Override
  protected void addHidden(int projectID) {
    super.addHidden(projectID);

    Hidden w = new Hidden("dialog-id", "" + dialogID);
    if (logger == null) {
      logger = Logger.getLogger("ImageUpload");
    }
    logger.info("dialogID " + w.getValue());
    fields.add(w);
  }

  private UploadResult result;

  @Override
  protected void handleFormSubmitSuccess(UploadResult result) {
    super.handleFormSubmitSuccess(result);
    logger.info("Remember " + result.getFilePath() + " and " + result.getImageID());
    this.result = result;
  }

  @Override
  protected void addInfoLabels() {
    Label w = getLabel("Choose an image (ideally square).");
    add(w);
    if (imageRef != null) {
      add(getCurrentImage(imageRef));
    }
  }

  private DivWidget getCurrentImage(String imageRef) {
    DivWidget imageContainer = new DivWidget();
    imageContainer.addStyleName("floatRight");
    imageContainer.addStyleName("rightFiveMargin");
    imageContainer.add(new HTML("Current Image"));
    imageContainer.addStyleName("cardBorderShadow");
    imageContainer.add(getImage(imageRef));
    return imageContainer;
  }

  @NotNull
  private com.google.gwt.user.client.ui.Image getImage(String cc) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(cc);
    image.setHeight(CreateDialogDialog.HEIGHT);
    image.setWidth(CreateDialogDialog.HEIGHT);
    return image;
  }

  @NotNull
  private Label getLabel(String text) {
    Label w = new Label(LabelType.WARNING, text);
    w.getElement().getStyle().setFontSize(16, Style.Unit.PX);
    w.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
    return w;
  }

  public UploadResult getResult() {
    return result;
  }
}
