package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import mitll.langtest.client.custom.TooltipHelper;
import mitll.langtest.client.custom.exercise.PopupContainerFactory;
import mitll.langtest.client.download.DownloadLink;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.services.ListServiceAsync;
import mitll.langtest.shared.custom.UserList;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 3/1/17.
 */
public class ListOperations {
  private final Logger logger = Logger.getLogger("ListOperations");

  private static final String COLLAPSIBLE_DOCUMENT_VIEWER = "collapsibleDocumentViewer";
  private static final String LIST_OPERATIONS = "listOperations";
  private static final String ADD_MEDIA = "Add Media";
  private static final String IMPORT_ITEM = "importItem";

  private Button showHideMedia;
  Collapse collapse;
  private DivWidget mediaContainer;
  private final ExerciseController controller;
  private final ListServiceAsync listService;

  public ListOperations(ExerciseController controller, ListServiceAsync listService) {
    this.controller = controller;
    this.listService = listService;
    mediaContainer = new DivWidget();
  }

  public DivWidget getMediaContainer() { return  mediaContainer; }

  public Panel getDownloadLinkRow(UserList ul, String instanceName) {
    Panel r1 = new FluidRow();
    r1.getElement().setId(LIST_OPERATIONS);
    Style style = r1.getElement().getStyle();
    style.setMarginBottom(5, Style.Unit.PX);
    style.setPaddingBottom(5, Style.Unit.PX);
    r1.addStyleName("userListDarkerBlueColor");
    Anchor downloadLink = getDownloadLink(ul, instanceName);
    downloadLink.addStyleName("floatLeftList");
    r1.add(downloadLink);

    Button child = getAddMedia(ul.getID(), ul.getContextURL());
    child.addStyleName("floatLeftList");

    r1.add(child);

    showHideMedia = new Button("Show/Hide Media");
    configureNewListButton(showHideMedia);

    CollapseTrigger trigger = new CollapseTrigger("#" + COLLAPSIBLE_DOCUMENT_VIEWER);
    trigger.setWidget(showHideMedia);

    showHideMedia.setVisible(!ul.getContextURL().isEmpty());
    r1.add(trigger);
    return r1;
  }
  /**
   * @param id
   * @param current
   * @return
   * @see #getDownloadLinkRow(UserList, String)
   */
  private Button getAddMedia(int id, String current) {
    final PopupContainerFactory.HidePopupTextBox textBox = getTextBoxForNewList(id, 250);
    textBox.setText(current);
    final Button newListButton = new Button(ADD_MEDIA);
    configureNewListButton(newListButton);

    Tooltip tooltip = new TooltipHelper().addTooltip(newListButton, ADD_MEDIA);

    new PopupContainerFactory().makePopupAndButton(textBox, newListButton, tooltip, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        attachMedia(textBox, id);
      }
    });

    return newListButton;
  }

  private Button getAddText(int id, String current) {
    final PopupContainerFactory.HidePopupTextBox textBox = getTextBoxForNewList(id, 250);
    textBox.setText(current);
    final Button newListButton = new Button(ADD_MEDIA);
    configureNewListButton(newListButton);

    Tooltip tooltip = new TooltipHelper().addTooltip(newListButton, ADD_MEDIA);

    new PopupContainerFactory().makePopupAndButton(textBox, newListButton, tooltip, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        attachMedia(textBox, id);
      }
    });

    return newListButton;
  }

  @NotNull
  private PopupContainerFactory.HidePopupTextBox getTextBoxForNewList(int id, int length) {
    final PopupContainerFactory.HidePopupTextBox textBox = new PopupContainerFactory.HidePopupTextBox() {
      @Override
      protected void onEnter() {
        attachMedia(this, id);
      }
    };

    textBox.getElement().setId("NewList");
    textBox.setVisibleLength(length);
    textBox.setWidth("300px");
    return textBox;
  }

  private void attachMedia(TextBox textEntry, int id) {
    String newListName = textEntry.getValue();
    if (!newListName.isEmpty()) {
      //  controller.logEvent(textEntry, "NewList_TextBox", exercise.getID(), "make new list called '" + newListName + "'");
      logger.info("got " + newListName);
      listService.updateContext(id, newListName, new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {

        }

        @Override
        public void onSuccess(Void result) {
          addDocContainer(newListName);
          collapse.setToggle(true);
          showHideMedia.setVisible(true);
        }
      });
    }
  }

  public void addDocContainer(String mediaURL) {
    logger.info("Add doc container with " + mediaURL);
    logger.info("Add doc container mediaContainer " + mediaContainer);

    // if (frame == null) {
    DivWidget docContainer = new DivWidget();

    docContainer.setWidth("100%");

    if (mediaURL.startsWith("<iframe")) {
      docContainer.add(new HTML(mediaURL));
    } else {
      Frame frame = new Frame(mediaURL);
      frame.setHeight("315px");
      frame.setWidth("100%");
      docContainer.add(frame);
    }

    //   String test = "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/dkcY6pv7ALk\" frameborder=\"0\" allowfullscreen></iframe>";

/*      Frame  frame = new Frame(mediaURL);
      frame.setHeight("315px");
      frame.setWidth("100%");

      docContainer.add(frame);*/

    // docContainer.add(new HTML(test));

    collapse = new Collapse();
    collapse.setId(COLLAPSIBLE_DOCUMENT_VIEWER);
    collapse.setDefaultOpen(false);
    collapse.setExistTrigger(true);
    collapse.setWidget(docContainer);

    mediaContainer.clear();
    mediaContainer.add(collapse);
//    } else {
    //     frame.setUrl(mediaURL);
    //   }
  }

  private void configureNewListButton(final Button popupButton) {
    popupButton.setIcon(IconType.FOLDER_OPEN);
    popupButton.setType(ButtonType.PRIMARY);
    popupButton.addStyleName("leftFiveMargin");
    // popupButton.getElement().setId("NPFExercise_popup");
    controller.register(popupButton, "N/A", "added document");
  }

  private Anchor getDownloadLink(UserList ul, String instanceName) {
    long listID = ul.getID();
    String linkid = instanceName + "_" + listID;
    Anchor downloadLink = new DownloadLink(controller).getDownloadLink(listID, linkid, ul.getName());
    Node child = downloadLink.getElement().getChild(0);
    AnchorElement.as(child).getStyle().setColor("#333333");
    return downloadLink;
  }

}
