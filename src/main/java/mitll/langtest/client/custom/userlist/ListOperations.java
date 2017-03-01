package mitll.langtest.client.custom.userlist;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
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

  public static final int TEXT_WIDTH = 640;
  /**
   * @see #addDocContainer
   */
  private static final String COLLAPSIBLE_DOCUMENT_VIEWER = "collapsibleDocumentViewer";
  private static final String LIST_OPERATIONS = "listOperations";
  private static final String ADD_MEDIA = "Add Media";
  // private static final String IMPORT_ITEM = "importItem";

  private Button showHideMedia;
  private Collapse collapse;
  private DivWidget mediaContainer;
  private final ExerciseController controller;
  private final ListServiceAsync listService;
  UserList ul;

  public ListOperations(ExerciseController controller, ListServiceAsync listService, UserList ul) {
    this.controller = controller;
    this.listService = listService;
    mediaContainer = new DivWidget();
    this.ul = ul;
  }

  public DivWidget getMediaContainer() {
    return mediaContainer;
  }

  public Panel getOperations(String instanceName) {
    Panel r1 = getRowForButtons();
    r1.add(getDownloadLink(ul, instanceName));
    r1.add(getAddMedia(ul.getID(), ul.getContextURL()));

    showHideMedia = new Button("Show/Hide Media");
    configureNewListButton(showHideMedia);

    CollapseTrigger trigger = new CollapseTrigger("#" + COLLAPSIBLE_DOCUMENT_VIEWER);
    trigger.setWidget(showHideMedia);

    //showHideMedia.setVisible(!ul.getContextURL().isEmpty());
    r1.add(trigger);

    return r1;
  }

  @NotNull
  private Panel getRowForButtons() {
    Panel r1 = new FluidRow();
    r1.getElement().setId(LIST_OPERATIONS);
    Style style = r1.getElement().getStyle();
    style.setMarginBottom(5, Style.Unit.PX);
    style.setPaddingBottom(5, Style.Unit.PX);
    r1.addStyleName("userListDarkerBlueColor");
    return r1;
  }

  /**
   * @param id
   * @param current
   * @return
   * @see #getOperations
   */
  private Button getAddMedia(int id, String current) {
    logger.info("getAddMedia " + id + " current " + current);

    final PopupContainerFactory.HidePopupTextBox textBox = getTextBoxForNewList(id, 250);
    textBox.setText(current);
    final Button newListButton = new Button(ADD_MEDIA);
    configureNewListButton(newListButton);
    newListButton.addStyleName("floatLeftList");

    Tooltip tooltip = new TooltipHelper().addTooltip(newListButton, ADD_MEDIA);

    new PopupContainerFactory().makePopupAndButton(textBox, newListButton, tooltip, new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        attachMedia(textBox, id);
      }
    });

    return newListButton;
  }

/*
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
  }*/

  @NotNull
  private PopupContainerFactory.HidePopupTextBox getTextBoxForNewList(int id, int length) {
    final PopupContainerFactory.HidePopupTextBox textBox = new PopupContainerFactory.HidePopupTextBox() {
      @Override
      protected void onEnter() {
        attachMedia(this, id);
      }
    };

    logger.info("text box is " + textBox);

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

  /**
   * Embed media in doc container
   *
   * @param mediaURL
   */
  public void addDocContainer(String mediaURL) {
    //   logger.info("Add doc container with " + mediaURL);
    //   logger.info("Add doc container mediaContainer " + mediaContainer);
    DivWidget docContainer = new DivWidget();
//docContainer.getElement().setId("docContainer");
    docContainer.setWidth("100%");

    if (!mediaURL.isEmpty()) {
      if (mediaURL.startsWith("<iframe")) {
        HTML w = new HTML(mediaURL);
        docContainer.add(w);
        w.addStyleName("floatLeftList");
      } else {
        Frame frame = new Frame(mediaURL);
        frame.setHeight("315px");
//      frame.setWidth("100%");
        docContainer.add(frame);
      }
    }

    RichTextArea richTextArea = new RichTextArea();
    richTextArea.setHTML(ul.getRichText());
//    richTextArea.addStyleName("floatRight");
    ScrollPanel widgets = new ScrollPanel(richTextArea);
    richTextArea.setHeight("310px");
    richTextArea.setWidth(TEXT_WIDTH + "px");
    widgets.addStyleName("leftFiveMargin");
    docContainer.add(widgets);
    widgets.addStyleName("floatLeftList");


    richTextArea.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        listService.updateRichText(ul.getID(), richTextArea.getHTML(), new AsyncCallback<Void>() {
          @Override
          public void onFailure(Throwable caught) {

          }

          @Override
          public void onSuccess(Void result) {

          }
        });
      }
    });

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
    downloadLink.addStyleName("floatLeftList");
    return downloadLink;
  }

}
