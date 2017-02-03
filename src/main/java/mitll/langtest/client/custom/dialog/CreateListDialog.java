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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ControlGroup;
import com.github.gwtbootstrap.client.ui.FluidRow;
import com.github.gwtbootstrap.client.ui.Heading;
import com.github.gwtbootstrap.client.ui.RadioButton;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.base.TextBoxBase;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SimpleHtmlSanitizer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.ListManager;
import mitll.langtest.client.dialog.KeyPressHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ListService;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.client.user.BasicDialog;
import mitll.langtest.client.user.FormField;
import mitll.langtest.client.user.UserManager;

import java.util.logging.Logger;

/**
 * Copyright &copy; 2011-2016 Massachusetts Institute of Technology, Lincoln Laboratory
 *
 * @author <a href="mailto:gordon.vidaver@ll.mit.edu">Gordon Vidaver</a>
 */
public class CreateListDialog extends BasicDialog {
  private final Logger logger = Logger.getLogger("CreateListDialog");
//  private static final String PRACTICE = "Practice";

  private static final String CLASS = "Course Info (optional)";
  private static final String CREATE_LIST = "Create List";
  private static final String TITLE = "Title";
  private static final String DESCRIPTION_OPTIONAL = "Description (optional)";
/*  private static final boolean REQUIRE_DESC = false;
  private static final boolean REQUIRE_CLASS = false;*/

  private final ListManager navigation;
  //  private final LangTestDatabaseAsync service;
  private final ListServiceAsync listService = GWT.create(ListService.class);

  private final UserManager userManager;
  private final ExerciseController controller;

  public CreateListDialog(ListManager navigation,
                          UserManager userManager,
                          ExerciseController controller) {
    this.navigation = navigation;
    //  this.service = service;
    this.userManager = userManager;
    this.controller = controller;
  }

  /**
   * @param thirdRow
   * @seex
   * @see mitll.langtest.client.custom.Navigation#getTabPanel
   */
  public void doCreate(Panel thirdRow) {
    thirdRow.clear();
    final KeyPressHelper enterKeyButtonHelper = new KeyPressHelper(true);
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
    final Heading header = new Heading(2, "Create a New List");
    row.add(header);

    row = new FluidRow();
    child.add(row);

    final FormField titleBox = addControlFormField(row, TITLE);
    final TextBoxBase box = titleBox.box;
    box.getElement().setId("CreateListDialog_Title");
    box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        controller.logEvent(box, "TextBox", "Create New List", "Title = " + box.getValue());
      }
    });

    row = new FluidRow();
    child.add(row);
    final TextArea area = new TextArea();
    final FormField description = getSimpleFormField(row, DESCRIPTION_OPTIONAL, area, 1);
    description.box.getElement().setId("CreateListDialog_Description");
    description.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        controller.logEvent(description.box, "TextBox", "Create New List", "Description = " + description.box.getValue());
      }
    });

    row = new FluidRow();
    child.add(row);

    final FormField classBox = addControlFormField(row, CLASS);
    classBox.box.getElement().setId("CreateListDialog_CourseInfo");
    classBox.box.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        controller.logEvent(classBox.box, "TextBox", "Create New List", "CourseInfo = " + classBox.box.getValue());
      }
    });


    row = new FluidRow();
    child.add(row);

    RadioButton radioButton = new RadioButton("Public_Private_Group", "Public");
    RadioButton radioButton2 = new RadioButton("Public_Private_Group", "Private");

    boolean isStudent = controller.getUserState().getCurrent().isStudent();

    radioButton.setValue(!isStudent);
    radioButton2.setValue(isStudent);

    Panel hp = new HorizontalPanel();
    hp.add(radioButton);
    radioButton2.addStyleName("leftFiveMargin");
    hp.add(radioButton2);

    ControlGroup widgets = addControlGroupEntry(row, "Keep List Public/Private?", hp, "");

    row.add(widgets);

    row = new FluidRow();
    child.add(row);

    Button submit = makeCreateButton(enterKeyButtonHelper, titleBox, area, classBox, radioButton);
    row.add(submit);

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        box.setFocus(true);
      }
    });
  }

  private Button makeCreateButton(final KeyPressHelper enterKeyButtonHelper,
                                  final FormField titleBox,
                                  final TextArea area,
                                  final FormField classBox,
                                  final RadioButton publicRadio) {
    Button submit = new Button(CREATE_LIST);
    submit.setType(ButtonType.PRIMARY);
    submit.getElement().setId("CreateList_Submit");
    controller.register(submit, "CreateList");

    submit.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

    submit.addStyleName("leftFiveMargin");
    submit.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // logger.info("creating list for " + titleBox + " " + area.getText() + " and " + classBox.getSafeText());
        enterKeyButtonHelper.removeKeyHandler();
        if (validateCreateList(titleBox)) {
          addUserList(titleBox, area, classBox, publicRadio.getValue());
        }
      }
    });
    enterKeyButtonHelper.addKeyHandler(submit);
    return submit;
  }

  private void addUserList(final FormField titleBox, TextArea area, FormField classBox, boolean isPublic) {
    int user = userManager.getUser();
    final String safeText = titleBox.getSafeText();

    listService.addUserList(
        safeText,
        sanitize(area.getText()),
        classBox.getSafeText(), isPublic, new AsyncCallback<Long>() {
          @Override
          public void onFailure(Throwable caught) {
          }

          @Override
          public void onSuccess(Long result) {
            if (result == -1) {
              markError(titleBox, "You already have a list named " + safeText);
            } else {
              navigation.clickOnYourLists(result);
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
      markError(titleBox, "Please fill in a title");
      return false;
    } else {
      return true;
    }
  }

  private String sanitize(String text) {
    return SimpleHtmlSanitizer.sanitizeHtml(text).asString();
  }
}