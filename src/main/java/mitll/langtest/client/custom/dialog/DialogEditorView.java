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

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.UIObject;
import mitll.langtest.client.analysis.ButtonMemoryItemContainer;
import mitll.langtest.client.custom.INavigation;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.dialog.CoreVocabEditor;
import mitll.langtest.client.dialog.DialogEditor;
import mitll.langtest.client.dialog.DialogHelper;
import mitll.langtest.client.domino.common.UploadViewBase;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.exercise.SimplePagingContainer;
import mitll.langtest.client.scoring.IFocusable;
import mitll.langtest.client.scoring.UserListSupport;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ExerciseListRequest;
import mitll.langtest.shared.exercise.ExerciseListWrapper;
import mitll.langtest.shared.user.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static mitll.langtest.client.custom.INavigation.QC_PERMISSIONS;

/**
 * Created by go22670 on 7/3/17.
 */
public class DialogEditorView<T extends IDialog> extends ContentEditorView<T> {
  public static final String LISTEN = "Listen";
  public static final String CORE_VOCAB = "Core Vocab";
  public static final String UPLOAD_IMAGE = "Upload Image";

  private final Logger logger = Logger.getLogger("DialogEditorView");
  private static final String UPLOAD_A_DIALOG_IMAGE = "Upload a dialog image.";

  private static final String LIST = "dialog";
  private static final String DOUBLE_CLICK_TO_LEARN_THE_LIST = "Double click to view a " + LIST;

  /**
   *
   */
  private static final String YOUR_LISTS1 = "Your Dialogs";
  public static final String DIALOG = "Dialog";

  private static final int MY_LIST_HEIGHT = 450;//500;//530;//560;

  /**
   * @param controller
   * @see mitll.langtest.client.banner.NewContentChooser#NewContentChooser
   */
  public DialogEditorView(ExerciseController controller) {
    super(controller);
  }

  @Override
  protected String getItemName() {
    return "Dialog";
  }

  public void showContent(Panel listContent, INavigation.VIEWS instanceName) {
    super.showContent(listContent, instanceName);
    addYours(left);
  }

  @NotNull
  @Override
  protected DivWidget getButtons(ButtonMemoryItemContainer<T> container) {
    DivWidget buttons = getCommonButtonContainer(container);

    Button coreVocabButton = getCoreVocabButton(container);

    buttons.add(coreVocabButton);
    Button uploadImageButton = getUploadImageButton(container);
    buttons.add(uploadImageButton);
    uploadImageButton.addStyleName("rightTenMargin");

    buttons.add(share = getShare(container));

    buttons.add(getListenButton(container));
    return buttons;
  }

  protected Button getShare(ButtonMemoryItemContainer<T> container) {
    Button successButton = new Button(SHARE);
    successButton.setType(ButtonType.INFO);
    successButton.addStyleName("leftFiveMargin");

    successButton.setIcon(IconType.SHARE);
    addTooltip(successButton, SHARE_THE_LIST + getSuffix());
    container.addButton(successButton);
    return successButton;
  }

  @NotNull
  private Button getListenButton(ButtonMemoryItemContainer<T> container) {
    Button learn = getSuccessButton(container, LISTEN);
    learn.setType(ButtonType.INFO);
    learn.addClickHandler(event -> showLearnList(container));
    addTooltip(learn, "Listen to the dialog.");
    return learn;
  }

  @NotNull
  private Button getCoreVocabButton(ButtonMemoryItemContainer<T> container) {
    Button learn = getSuccessButton(container, CORE_VOCAB);
    learn.setIcon(IconType.PENCIL);
    learn.addClickHandler(event -> editCoreVocabList(getCurrentSelection()));
    addTooltip(learn, "Edit the core vocabulary for the dialog.");
    return learn;
  }

  @NotNull
  private Button getUploadImageButton(ButtonMemoryItemContainer<T> container) {
    Button learn = getSuccessButton(container, UPLOAD_IMAGE);
    learn.setIcon(IconType.PICTURE);

    learn.addClickHandler(event -> {
      int itemID = getItemID(container);
//      logger.info("dialog item ID " + itemID);
      ImageUpload widgets1 = new ImageUpload(controller.getUser(), itemID, getCurrentSelection().getImageRef()) {
        @Override
        protected void handleFormSubmitSuccess(UploadResult result) {
          super.handleFormSubmitSuccess(result);
          if (result.isSuccess()) {
            getCurrentSelection(container).getMutable().setImageRef(result.getFilePath());
            controller.getExerciseService().setImagePath(itemID, result.getFilePath(), new AsyncCallback<Boolean>() {
              @Override
              public void onFailure(Throwable caught) {


              }

              @Override
              public void onSuccess(Boolean result) {
                //  logger.info("Set image path " + result);
                if (!result) {
                  logger.warning("Set image path " + result);

                }
              }
            });
          }

        }
      };
      widgets1.init(controller.getProjectID());
      widgets1.showModal(new DialogHelper.CloseListener() {
        @Override
        public boolean gotYes() {
          // UploadViewBase.UploadResult result = widgets1.getResult();
 /*         if (result != null && result.getImageID() > 0) {
            logger.info("got back " + result.getImageID() + " " + result.getFilePath());
          } else {
            logger.info("2 got back " + result);
          }*/
          return false;
        }

        @Override
        public void gotNo() {
//          logger.info("gotNo ");
        }

        @Override
        public void gotHidden() {
//          logger.info("gotHidden ");
//          logger.info("2 got back " + widgets1.getResult());
        }
      });
    });
    addTooltip(learn, UPLOAD_A_DIALOG_IMAGE);
    return learn;
  }

  public interface ResultListener {
    void got(UploadViewBase.UploadResult result);
  }

  private void showLearnList(ButtonMemoryItemContainer<T> container) {
    controller.getNavigation().showDialogIn(getItemID(container), INavigation.VIEWS.LISTEN);
  }

  /**
   * Unless you're an admin you can only see dialogs you made.
   *
   * @param left
   */
  private void addYours(DivWidget left) {
    showYours(Collections.emptyList(), left);

    int user = controller.getUser();
    //  logger.info("addYours project " + controller.getProjectID());
    ExerciseListRequest request = new ExerciseListRequest(0, user, controller.getProjectID()).setSortByDate(true);

    final boolean fcanSeeAll = isCanSeeAll();

    controller.getDialogService().getDialogs(request,
        new AsyncCallback<ExerciseListWrapper<T>>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(ExerciseListWrapper<T> result) {
            {
              List<T> exercises = result.getExercises();
              if (!fcanSeeAll) {
                exercises = exercises.stream().filter(d -> d.getUserid() == user).collect(Collectors.toList());
              }
              ButtonMemoryItemContainer<T> myLists = getMyLists();
              myLists.populateTable(exercises);

              if (exercises.isEmpty()) myLists.disableAll();
            }
            populateUniqueListNames(result.getExercises());
            setShareHREFLater();


          }
        });
  }

  private boolean isCanSeeAll() {
    boolean canSeeAll = controller.getUserManager().isAdmin();

    if (!canSeeAll) {
      for (Permission perm : controller.getUserManager().getPermissions()) {
        if (QC_PERMISSIONS.contains(perm)) {
          canSeeAll = true;
          break;
        }
      }
    }
    return canSeeAll;
  }

  /**
   * @param result
   * @param left
   * @see #addYours
   */
  private void showYours(Collection<T> result, DivWidget left) {
    DialogContainer<T> myLists = new MyDialogContainer();
    setMyLists(myLists);
    Panel tableWithPager = myLists.getTableWithPager(result);

    new TooltipHelper().createAddTooltip(tableWithPager, DOUBLE_CLICK_TO_LEARN_THE_LIST, Placement.BOTTOM);
    addPagerAndHeader(tableWithPager, YOUR_LISTS1, left);
    tableWithPager.setHeight(MY_LIST_HEIGHT + "px");
    tableWithPager.getElement().getStyle().setMarginBottom(10, Style.Unit.PX);

    left.add(getButtons(this.getMyLists()));
  }

  @Override
  protected void populateUniqueListNames(Collection<T> result) {
    result.forEach(list -> names.add(list.getName()));
  }

  /**
   * @return
   * @see ContentEditorView#doAdd
   */
  @NotNull
  @Override
  protected CreateDialog<T> getCreateDialog() {
    return new CreateDialogDialog<T>(names, controller, this);
  }

  /**
   * TODO : fill in with fancy editor
   *
   * @param selectedItem
   */
  @Override
  protected void editList(T selectedItem) {
    DivWidget listContent = new DivWidget();

    DialogEditor editorTurnDialogEditor = new DialogEditor(controller, INavigation.VIEWS.DIALOG_EDITOR, selectedItem);
    editorTurnDialogEditor.showContent(listContent, INavigation.VIEWS.DIALOG_EDITOR);

    new DialogHelper(true).show(
        "Add/Edit Turns" + " : " + getListName(),
        Collections.emptyList(),
        listContent,
        "Done",
        null,
        new MyShownCloseListener(editorTurnDialogEditor, selectedItem.getID()), 720, -1, true);
  }

  //  @Override
  private void editCoreVocabList(T selectedItem) {
    DivWidget listContent = new DivWidget();

    CoreVocabEditor editorTurnDialogEditor = new CoreVocabEditor(controller, INavigation.VIEWS.CORE_EDITOR, selectedItem);
    editorTurnDialogEditor.showContent(listContent, INavigation.VIEWS.CORE_EDITOR);

    //  logger.info("list content " + listContent);

    new DialogHelper(true).show(
        "Add/Edit Core Vocab" + " : " + getListName(),
        Collections.emptyList(),
        listContent,
        "Done",
        null,
        new MyShownCloseListener(editorTurnDialogEditor, selectedItem.getID()), 740, -1, true);
  }

  private class MyShownCloseListener implements DialogHelper.ShownCloseListener {
    IFocusable editItem;
    int dialogID;

    MyShownCloseListener(IFocusable editItem, int dialogID) {
      this.editItem = editItem;
      this.dialogID = dialogID;
    }

    @Override
    public boolean gotYes() {
//            int numItems = currentSelectionFromMyLists.getNumItems();
      //   logger.info("editList : on " + currentSelectionFromMyLists.getName() + " now " + numItems);
      getMyLists().flush();
      getMyLists().redraw();
      return true;
    }

    /**
     * CRITICAL TO REMOVE LISTENER!
     */
    @Override
    public void gotHidden() {
      // logger.info("Got hidden ");
      // editItem.removeHistoryListener();
      // History.newItem("");

      int projectID = controller.getProjectID();
      if (projectID != -1) {
        controller.getAudioService().reloadDialog(dialogID, new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {
            controller.handleNonFatalError("reloading dialog on hydra/score1", caught);

          }

          @Override
          public void onSuccess(Void result) {
            logger.info("did reload on other server.");
            controller.getExerciseService().reloadDialog(projectID, dialogID, new AsyncCallback<Void>() {
              @Override
              public void onFailure(Throwable caught) {
                controller.handleNonFatalError("reloading dialog on netprof.", caught);
              }

              @Override
              public void onSuccess(Void result) {
                logger.info("did reload on netprof.");

              }
            });
          }
        });
      }
    }

    @Override
    public void gotNo() {
    }

    @Override
    public void gotShown() {
      // logger.info("editList : edit view shown!");
      //editItem.reload();
      editItem.grabFocus();
    }
  }

  @NotNull
  private String getListName() {
    T originalList = getCurrentSelectionFromMyLists();
    boolean hasDescrip = !originalList.getEnglish().isEmpty();
    int maxLengthId = 30;
    return truncate(originalList.getName(), 35) +
        (hasDescrip ? " (" + truncate(originalList.getEnglish(), maxLengthId) + ")" : "");
  }


  @NotNull
  protected String truncate(String columnText, int maxLengthId) {
    if (columnText.length() > maxLengthId) columnText = columnText.substring(0, maxLengthId - 3) + "...";
    return columnText;
  }

  @Override
  protected void doDelete(UIObject delete, T currentSelection) {
    final int uniqueID = currentSelection.getID();

    controller.getDialogService().delete(controller.getProjectID(), uniqueID, new AsyncCallback<Boolean>() {
      @Override
      public void onFailure(Throwable caught) {
        controller.handleNonFatalError("deleting a dialog", caught);
      }

      @Override
      public void onSuccess(Boolean result) {
        gotDeleteResponse(result, currentSelection, uniqueID);
      }
    });
  }

  @NotNull
  @Override
  protected String getMailTo() {
    IDialog currentSelection = getCurrentSelection();
    return new UserListSupport(controller)
        .getMailToDialog(currentSelection.getID(), currentSelection.getForeignLanguage());
  }

  /**
   * @see #showYours(Collection, DivWidget)
   */
  private class MyDialogContainer extends DialogContainer<T> {
    MyDialogContainer() {
      super(DialogEditorView.this.controller);
    }

    @Override
    public void gotClickOnItem(final T user) {
      super.gotClickOnItem(user);
      setShareHREF(user);
    }

    @Override
    protected boolean hasDoubleClick() {
      return true;
    }

    /**
     * @param selected
     * @see SimplePagingContainer#addDoubleClick
     */
    @Override
    protected void gotDoubleClickOn(T selected) {
      //  logger.info("gotDoubleClickOn got double click on " + selected);
      editList(getCurrentSelection());
    }
  }

  @Override
  protected void addImportButton(DivWidget buttons, ButtonMemoryItemContainer<T> container) {
  }


  @NotNull
  @Override
  protected CreateDialog<T> getEditDialog() {
    return new CreateDialogDialog<T>(getCurrentSelectionFromMyLists(), names, true, controller, this);
  }

  @Override
  protected String getName() {
    return "Dialog";
  }

  @NotNull
  protected String getEditItemTooltipPrefix() {
    return "Edit the turns in the ";
  }

  @NotNull
  protected String getEditItemButtonTitle() {
    return "Turns";
  }
}
