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

import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.dialog.IMutableDialog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CreateDialogDialog<T extends IDialog> extends CreateDialog<T> {
  public static final String PLEASE_CHOOSE = "Please choose.";
  public static final String CHOOSE_TYPE_OF_DIALOG = "-- Choose type of dialog --";
  private final Logger logger = Logger.getLogger("CreateDialogDialog");
  private static final String HEIGHT = 100 + "px";

  private FormField entitleBox;
  private ListBox dialogType;
  private ControlGroup dialogTypeContainer;

  /**
   * @param current
   * @param names
   * @param isEdit
   * @param controller
   * @param listView
   * @see DialogEditorView#getEditDialog()
   */
  CreateDialogDialog(T current,
                     Set<String> names,
                     boolean isEdit,
                     ExerciseController controller, CreateComplete<T> listView) {
    super(current, names, isEdit, controller, listView);
  }

  /**
   * @param names
   * @param controller
   * @see DialogEditorView#getCreateDialog()
   */
  CreateDialogDialog(Set<String> names, ExerciseController controller, CreateComplete<T> listView) {
    super(null, names, false, controller, listView);
  }

  @Override
  protected FormField addTitle(FluidRow row) {
    FormField formField = super.addTitle(row);
    // formField.getWidget().setWidth("300px");
    // ((TextBox)formField.box).setVisibleLength(40);
    return formField;
  }

//  private String filePath = "";
//  private int imageID;
//
//  @NotNull
//  private com.github.gwtbootstrap.client.ui.Button getUploadButton(int projid) {
//    com.github.gwtbootstrap.client.ui.Button w = new com.github.gwtbootstrap.client.ui.Button("Upload image");
//
//    w.setIcon(IconType.UPLOAD);
//    w.setType(ButtonType.INFO);
//
//    addTooltip(w, "Upload image for dialog.");
//
//    w.addClickHandler(event -> new ImageUpload(projid).showModal());
//
//    return w;
//  }

  @Override
  protected int getMaxTitleLength() {
    return 35;
  }

  protected int getTitleWidth() {
    return 325;
  }

  /**
   * add en title
   * shows unit/chapter drop downs
   *
   * @param child
   */
  @Override
  protected void addFieldsBelowDescription(Panel child) {
    Grid grid = new Grid(2, 4);

    int col = 0;
    int row = 0;

    String unit = isEdit ? getCurrent().getUnit() : null;
    String chapter = isEdit ? getCurrent().getChapter() : null;

    new RowAndCol(grid, col, row).invoke(false, unit, chapter);

    grid.addStyleName("leftFiveMargin");

    child.add(grid);

    addDialogType(child);

    if (isEdit) {
      DivWidget imageContainer = new DivWidget();
      imageContainer.addStyleName("floatRight");
      imageContainer.addStyleName("rightFiveMargin");
      String imageRef = getCurrent().getImageRef();
    //  logger.info("Show " + imageRef);
      imageContainer.add(getImage(imageRef));
      child.add(imageContainer);
    }
   /* {
      ListBox listBox = getListBox(200);
      listBox.addStyleName("leftFiveMargin");
      listBox.addItem("-- Choose first speaker --");
      listBox.addItem("English Speaker");
      listBox.addItem(controller.getProjectStartupInfo().getLanguageInfo().toDisplay() + " Speaker");
      child.add(listBox);
    }*/
    //listBox.addChangeHandler(event -> gotChangeOn);

    Widget privacyChoices = getPrivacyChoices();
    privacyChoices.getElement().getStyle().setClear(Style.Clear.LEFT);
    child.add(privacyChoices);
    moveFocusToTitleLater();
  }

  @NotNull
  private com.google.gwt.user.client.ui.Image getImage(String cc) {
    com.google.gwt.user.client.ui.Image image = new com.google.gwt.user.client.ui.Image(cc);
    image.setHeight(HEIGHT);
    image.setWidth(HEIGHT);
    return image;
  }

  private void addDialogType(Panel child) {
    ListBox listBox = getListBox(200);
    listBox.addStyleName("leftFiveMargin");
    listBox.addItem(CHOOSE_TYPE_OF_DIALOG);
    listBox.addItem(DialogType.DIALOG.toString());
    listBox.addItem(DialogType.INTERPRETER.toString());
    dialogTypeContainer = new ControlGroup();

    dialogTypeContainer.setWidth("50%");
    dialogTypeContainer.addStyleName("floatLeft");
    dialogTypeContainer.add(listBox);
    dialogType = listBox;
    child.add(dialogTypeContainer);

    if (isEdit) {
      if (getCurrent().getKind() == DialogType.DIALOG) {
        listBox.setSelectedValue(DialogType.DIALOG.toString());
      } else if (getCurrent().getKind() == DialogType.INTERPRETER) {
        listBox.setSelectedValue(DialogType.INTERPRETER.toString());
      }
      listBox.setEnabled(false);
    }
  }

  @Override
  protected void addDescription(Panel child) {
    FluidRow row = new FluidRow();
    child.add(row);
    entitleBox = addEnTitle(row);
    super.addDescription(child);
  }

  private FormField addEnTitle(FluidRow row) {
    FormField entitleBox = addControlFormField(row, "", "", getMaxTitleLength(), getTitleWidth());
    //   entitleBox.getWidget().getElement().getStyle().setMarginTop(10, Style.Unit.PX);

    final TextBoxBase box = entitleBox.box;
    entitleBox.setHint("English Title (optional)");
    if (isEditing()) {
      box.setText(getCurrent().getEnglish());
    }
    box.getElement().setId("En_Title");
    box.addKeyUpHandler(this::checkEnter);
    box.addBlurHandler(event -> controller.logEvent(box, TEXT_BOX, CREATE_NEW_LIST, "English Title = " + box.getValue()));

    return entitleBox;
  }

  @Override
  protected boolean isOKToCreate(Set<String> names) {
    boolean okToCreate = super.isOKToCreate(names);
    if (okToCreate) {
      List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();

      boolean validUnit = true;
      for (int i = 0; i < allUnitChapter.size(); i++) {
        ListBox listBox = allUnitChapter.get(i);
        if (listBox.getSelectedValue().startsWith("--")) {
          markError(allUnitChapterGroup.get(i), listBox, listBox, PLEASE_CHOOSE, "Please choose a " + typeOrder.get(i));
          validUnit = false;
          break;
        }
      }

      if (validUnit) {
        boolean valid = false;
        try {
          DialogType.valueOf(this.dialogType.getSelectedValue());
          valid = true;
        } catch (IllegalArgumentException e) {
          // e.printStackTrace();
        }

        if (!valid) {
          markError(dialogTypeContainer, dialogType, dialogType, PLEASE_CHOOSE, "Please choose a dialog type");
        }

        logger.info("isValid " + valid);
        return valid;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * TODO figure out image upload...
   *
   * @see #isOKToCreate()
   */
  @Override
  protected void doCreate() {
    enterKeyButtonHelper.removeKeyHandler();

    List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();

    Map<String, String> unitAndChapterSelections = getUnitAndChapterSelections();

    String unit = typeOrder.size() > 0 ? unitAndChapterSelections.get(typeOrder.get(0)) : "";
    String chapter = typeOrder.size() > 1 ? unitAndChapterSelections.get(typeOrder.get(1)) : "";

    String imageRef = "";

    boolean isPrivate = !publicChoice.getValue();

    IDialog newDialog = new Dialog(-1,
        controller.getUser(),
        controller.getProjectID(),
        -1,
        System.currentTimeMillis(),

        unit,
        chapter,
        sanitize(theDescription.getText()),
        imageRef,
        titleBox.getSafeText(),
        entitleBox.getSafeText(),
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        DialogType.valueOf(this.dialogType.getSelectedValue()),
        "",
        isPrivate
    );
    //  newDialog.setFilePath(filePath);

    logger.info("doCreate new " + newDialog.getID() + " " + newDialog.isPrivate());// + " image: " + filePath);

    controller.getDialogService().addDialog(newDialog,
        new AsyncCallback<IDialog>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("making a new dialog", caught);
          }

          @Override
          public void onSuccess(IDialog result) {
            // logger.info("Got " + result);
            listView.madeIt((T) result);
          }
        });
  }

  @Override
  protected void setDescription(T current) {
    if (isEditing()) theDescription.setText(getCurrent().getOrientation());
  }

  @NotNull
  @Override
  protected String getDescriptionOptional() {
    return "Orientation";
  }

  @Override
  protected void gotClickOnPublic() {
    getCurrent().getMutable().setIsPrivate(false);
  }

  @Override
  protected void gotClickOnPrivate() {
    getCurrent().getMutable().setIsPrivate(true);
  }

  @Override
  public void doEdit(T currentSelection, ButtonMemoryItemContainer<T> container) {
    IMutableDialog mutable = currentSelection.getMutable();
    mutable.setForeignLanguage(titleBox.getSafeText());
    mutable.setEnglish(entitleBox.getSafeText());
    mutable.setOrientation(theDescription.getText());
    mutable.setDialogType(DialogType.valueOf(dialogType.getSelectedValue()));
//    logger.info("doEdit : using image " + imageID);
    //  mutable.setImageID(imageID);

    controller.getDialogService().update(currentSelection, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("changing a dialog", caught);
      }

      @Override
      public void onSuccess(Void result) {
        container.redraw();
      }
    });
  }

}