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

package mitll.langtest.client.custom.dialog;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.userlist.ListContainer;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.shared.custom.UserList;
import mitll.langtest.shared.exercise.CommonShell;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class CreateListDialog extends BasicDialog {
  public static final String PLEASE_FILL_IN_A_TITLE = "Please fill in a title";
  private final Logger logger = Logger.getLogger("CreateListDialog");

  private static final String CLASS = "Course Info (optional)";
  private static final String CREATE_LIST = "Create List";
  private static final String TITLE = "Title";
  private static final String DESCRIPTION_OPTIONAL = "Description (optional)";
  private final CreateListComplete listView;
  private FormField titleBox;
  private final ExerciseController controller;
  private KeyPressHelper enterKeyButtonHelper;
  private TextArea theDescription;
  private FormField classBox;
  private RadioButton radioButton;
  private UserList current = null;

  public CreateListDialog(CreateListComplete listView, ExerciseController controller, UserList current) {
    this(listView, controller);
    this.current = current;
  }

  public CreateListDialog(CreateListComplete listView, ExerciseController controller) {
    this.listView = listView;
    this.controller = controller;
  }

  /**
   * @param thirdRow
   * @seex
   * @seex mitll.langtest.client.custom.Navigation#getNavigation
   */
  public void doCreate(Panel thirdRow) {
    thirdRow.clear();
    enterKeyButtonHelper = new KeyPressHelper(true);
    Panel child = new DivWidget() {
      @Override
      protected void onUnload() {
        super.onUnload();
        enterKeyButtonHelper.removeKeyHandler();
      }
    };
    thirdRow.add(child);
    zeroPadding(child);
    child.addStyleName("userListContainer");

    FluidRow row = new FluidRow();
    child.add(row);

    {
      titleBox = addControlFormField(row, TITLE);
      final TextBoxBase box = titleBox.box;
      if (current != null) box.setText(current.getName());
      box.getElement().setId("CreateListDialog_Title");
      box.addBlurHandler(event -> controller.logEvent(box, "TextBox", "Create New List", "Title = " + box.getValue()));
    }

    {
      row = new FluidRow();
      child.add(row);

      theDescription = new TextArea();
      if (current != null) theDescription.setText(current.getDescription());
      final FormField description = getSimpleFormField(row, DESCRIPTION_OPTIONAL, theDescription, 1);
      description.box.getElement().setId("CreateListDialog_Description");
      description.box.addBlurHandler(event -> controller.logEvent(description.box, "TextBox", "Create New List", "Description = " + description.box.getValue()));
    }

    {
      row = new FluidRow();
      child.add(row);

      classBox = addControlFormField(row, CLASS);
      if (current != null) classBox.setText(current.getClassMarker());
      classBox.box.getElement().setId("CreateListDialog_CourseInfo");
      classBox.box.addBlurHandler(event -> controller.logEvent(classBox.box, "TextBox", "Create New List", "CourseInfo = " + classBox.box.getValue()));
    }

    {
      row = new FluidRow();
      child.add(row);

      radioButton = new RadioButton("Public_Private_Group", "Public");
      RadioButton radioButton2 = new RadioButton("Public_Private_Group", "Private");

      // students by default have private lists - ?
      boolean isPrivate = controller.getUserState().getCurrent().isStudent();

      if (current != null) isPrivate = current.isPrivate();

      radioButton.setValue(!isPrivate);
      radioButton2.setValue(isPrivate);


      Panel hp = new HorizontalPanel();
      hp.add(radioButton);
      radioButton2.addStyleName("leftFiveMargin");
      hp.add(radioButton2);

      ControlGroup widgets = addControlGroupEntry(row, "Keep List Public/Private?", hp, "");

      row.add(widgets);
    }

    //row = new FluidRow();
    // child.add(row);

    /*submit =*/
    makeCreateButton(enterKeyButtonHelper, theDescription, classBox, radioButton);
    //row.add(submit);

    Scheduler.get().scheduleDeferred(() -> titleBox.box.setFocus(true));
  }

  //private Button submit;

  private void makeCreateButton(final KeyPressHelper enterKeyButtonHelper,
                                  final TextArea area,
                                  final FormField classBox,
                                  final RadioButton publicRadio) {
    Button submit = new Button(CREATE_LIST);
    submit.setType(ButtonType.PRIMARY);
    submit.getElement().setId("CreateList_Submit");
    controller.register(submit, "CreateList");

    submit.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

    submit.addStyleName("leftFiveMargin");
    submit.addClickHandler(event -> {
      logger.info("creating list for " + titleBox + " " + area.getText() + " and " + classBox.getSafeText());
      if (current == null) {
        gotCreate(enterKeyButtonHelper, area, classBox, publicRadio);
      } else {
        listView.gotEdit();
      }
    });
    enterKeyButtonHelper.addKeyHandler(submit);
//    return submit;
  }

  public void doCreate() {
    gotCreate(enterKeyButtonHelper, theDescription, classBox, radioButton);
  }

  private void gotCreate(KeyPressHelper enterKeyButtonHelper,

                         TextArea area,
                         FormField classBox,
                         RadioButton publicRadio) {
    enterKeyButtonHelper.removeKeyHandler();
    addUserList(titleBox, area, classBox, publicRadio.getValue());
  }

  public boolean isOKToCreate(Set<String> names) {
    return isValidName() && !names.contains(titleBox.getSafeText());
  }

  public boolean isValidName() {
    return validateCreateList(titleBox);
  }

  /**
   * @param titleBox
   * @param area
   * @param classBox
   * @param isPublic
   * @see #gotCreate
   */
  private void addUserList(final FormField titleBox, TextArea area, FormField classBox, boolean isPublic) {
    final String safeText = titleBox.getSafeText();
    logger.info("addUserList " + safeText);
    controller.getListService().addUserList(
        safeText,
        sanitize(area.getText()),
        classBox.getSafeText(), isPublic, new AsyncCallback<UserList>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("making a new list", caught);
          }

          @Override
          public void onSuccess(UserList result) {
            if (result == null) {
              markError(titleBox, "You already have a list named " + safeText);
//              Window.alert("You already have a list with that name.");
              //logger.info("NOIT SUCCESS " + result);
              //listView.madeIt(result);
            } else {
              //logger.info("Success " + result);
              listView.madeIt(result);
            }
          }
        });
  }

  private void zeroPadding(Panel createContent) {
    createContent.getElement().getStyle().setPaddingLeft(0, Style.Unit.PX);
    createContent.getElement().getStyle().setPaddingRight(0, Style.Unit.PX);
  }

  private boolean validateCreateList(FormField titleBox) {
    if (titleBox.getSafeText().isEmpty()) {
      markError(titleBox, PLEASE_FILL_IN_A_TITLE);
      return false;
    } else {
      return true;
    }
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }

  public void doEdit(UserList<CommonShell> currentSelection, ListContainer container) {
    currentSelection.setName(titleBox.getSafeText());
    currentSelection.setDescription(sanitize(theDescription.getText()));
    currentSelection.setClassMarker(sanitize(classBox.getSafeText()));
    currentSelection.setPrivate(!radioButton.getValue());

    controller.getListService().update(currentSelection, new AsyncCallback<Void>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("changing a list", caught);
      }

      @Override
      public void onSuccess(Void result) {
        container.redraw();
      }
    });
  }
}