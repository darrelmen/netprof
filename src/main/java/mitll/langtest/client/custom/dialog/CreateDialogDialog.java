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

import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.dialog.Dialog;
import mitll.langtest.shared.dialog.DialogType;
import mitll.langtest.shared.dialog.IDialog;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CreateDialogDialog<T extends IDialog> extends CreateDialog<T> {
  private final Logger logger = Logger.getLogger("CreateDialogDialog");

  private FormField entitleBox;

  public CreateDialogDialog(T current, Set<String> names, boolean isEdit, ExerciseController controller) {
    super(current, names, isEdit, controller);
  }

  CreateDialogDialog(Set<String> names, ExerciseController controller) {
    super(null, names, false, controller);
  }

  /**
   * TODO : add en title
   * TODO : add unit/chapter drop downs
   *
   * @param child
   */
  @Override
  protected void addFieldsBelowDescription(Panel child) {
//    FluidRow row = new FluidRow();
//    child.add(row);

    Grid grid = new Grid(2, 4);


    int col = 0;
    int row = 0;

    RowAndCol rowAndCol = new RowAndCol(grid, col, row).invoke();

    grid.addStyleName("leftFiveMargin");

    child.add(grid);

    ListBox listBox = getListBox(200);
    listBox.addStyleName("leftFiveMargin");
    listBox.addItem("-- Choose type of dialog --");
    listBox.addItem(DialogType.DIALOG.toString());
    listBox.addItem(DialogType.INTERPRETER.toString());
    //listBox.addChangeHandler(event -> gotChangeOn);

    child.add(listBox);

    child.add(getPrivacyChoices());
    moveFocusToTitleLater();
  }

  @Override
  protected void addDescription(Panel child) {
    FluidRow row = new FluidRow();
    child.add(row);
    addEnTitle(row);
    super.addDescription(child);
  }

  private void addEnTitle(FluidRow row) {
    entitleBox = addControlFormField(row, "", "");
    //   entitleBox.getWidget().getElement().getStyle().setMarginTop(10, Style.Unit.PX);

    final TextBoxBase box = entitleBox.box;
    entitleBox.setHint("English Title (optional)");
    if (isEditing()) box.setText(getCurrent().getName());
    box.getElement().setId("En_Title");
    box.addKeyUpHandler(this::checkEnter);
    box.addBlurHandler(event -> controller.logEvent(box, TEXT_BOX, CREATE_NEW_LIST, "English Title = " + box.getValue()));
  }


  /**
   * TODO figure out image upload...
   * TODO :add dialog type drop down
   */
  @Override
  protected void doCreate() {
    enterKeyButtonHelper.removeKeyHandler();

    List<String> typeOrder = controller.getProjectStartupInfo().getTypeOrder();

    Map<String, String> unitAndChapterSelections = getUnitAndChapterSelections();

    String unit = typeOrder.size() > 0 ? unitAndChapterSelections.get(typeOrder.get(0)) : "";
    String chapter = typeOrder.size() > 1 ? unitAndChapterSelections.get(typeOrder.get(1)) : "";

    int imageID = -1;

    String imageRef = "";

    DialogType dialogType = DialogType.DIALOG;

    IDialog newDialog = new Dialog(-1,
        controller.getUser(),
        controller.getProjectID(),
        imageID,
        -1,
        System.currentTimeMillis(),

        unit,
        chapter,
        sanitize(theDescription.getText()),
        imageRef,
        titleBox.getSafeText(),
        "",
        new ArrayList<>(),
        new ArrayList<>(),
        new ArrayList<>(),
        dialogType,
        "",
        publicChoice.getValue()
    );

    controller.getDialogService().addDialog(newDialog,
        new AsyncCallback<IDialog>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("making a new dialog", caught);
          }

          @Override
          public void onSuccess(IDialog result) {
            logger.info("Got " + result);
          }
        });
  }

  @Override
  protected void setDescription(T current) {
    if (isEditing()) theDescription.setText(getCurrent().getOrientation());
  }


//  @Override
//  protected void setDescription(T current) {
//    current.getMutable().setOrientation(theDescription.getText());
//  }

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

  }
}