package mitll.langtest.client.custom;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ButtonToolbar;
import com.github.gwtbootstrap.client.ui.NavList;
import com.github.gwtbootstrap.client.ui.base.IconAnchor;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.PopupPanel;

import java.util.Set;


/**
 * Created by GO22670 on 4/17/2014.
 */
public class MyDropdown {
  private Button addBtn;
  // avoid issues in bootstrap dropdown button sizing by creating our own
  // small popup.
  // TODO Refactor. Duplicates Domino Toolbar.
  private PopupPanel addPopup;
  private Button removeBtn;
  // private OrganizerMainPanel mainPanel;
  private UIHandler uiHandler;

  public MyDropdown(/*OrganizerMainPanel parent*/) {
    //   super(parent);
    //  this.mainPanel = parent;
    init();
  }
/*
    private ProjectDocListPanel getDocList() {
      return mainPanel.getAssembleView().getProjectDocListPanel();
    }*/

  private void init() {
    uiHandler = new UIHandler();
    ButtonToolbar toolbar = new ButtonToolbar();
    //   boolean toggleBtn = mainPanel.getAssembleView().getExam().getContent().getFormIds().size() > 1;
    //  addBtn = createBSButton(IconType.ARROW_DOWN, null, toggleBtn, uiHandler);
    addBtn.setCaret(true);
    // createAddTooltip(addBtn, "Add from Project to Assembly", Placement.RIGHT);
    //  removeBtn = createBSButton(IconType.ARROW_UP, null, false, uiHandler);
    //  createAddTooltip(removeBtn, "Remove from Assembly", Placement.RIGHT);
    toolbar.add(addBtn);
    toolbar.add(removeBtn);
    //  initWidget(toolbar);
  }

/*    private void doHandleRemove() {
      if (mainPanel.getOrganizerList().getSelected().size() <= 0) {
        getMsgHelper().makeInlineMessage("Please select an item from the Exam Included Items table to remove.",
          true, AlertType.INFO);
      } else {
        mainPanel.removeDocumentsFromAssembly(mainPanel.getOrganizerList().getSelected());
      }
    }*/

/*    private void doHandleAdd() {
      Set<ValidationFormID> fids = mainPanel.getAssembleView().getExam().getContent().getFormIds();
      if (fids.size() == 1) {
        addToAssembly(fids.iterator().next());
      } else {
        updateDropdownMenu(fids);
      }
    }*/

/*    private void addToAssembly(ValidationFormID fid) {
      if (getDocList().getSelected().size() == 0) {
        getMsgHelper().makeInlineMessage("Please select an item from the Exam Excluded Items table to add.",
          true, AlertType.INFO);
      } else {
        mainPanel.addDocumentsToAssembly(fid, getDocList().getSelected());
      }
    }*/

/*  private void updateDropdownMenu(Set<String> fids) {
    if (!addBtn.isActive()) {
      if (addPopup == null) {
        addPopup = new PopupPanel(true);
        addPopup.addStyleName("dropdown-popup");
        addPopup.addAutoHidePartner(addBtn.getElement());
        addPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
          @Override
          public void onClose(CloseEvent<PopupPanel> event) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
              @Override
              public void execute() {
                addBtn.setActive(false);
              }
            });
          }
        });
        NavList theList = new NavList();


     *//*     for (ValidationFormID fid : fids) {
            theList.add(createNavLink(fid.getDisplayName(), null, null, uiHandler));
          }*//*

        addPopup.add(theList);
      }
      addPopup.showRelativeTo(addBtn);
    } else if (addPopup != null) {
      addPopup.hide();
    }
  }*/

  private class UIHandler implements ClickHandler {

    @Override
    public void onClick(ClickEvent event) {
      Object src = event.getSource();
      if (src == removeBtn) {
        //doHandleRemove();
      } else if (src == addBtn) {
        //doHandleAdd();
      } else if (true) {//hasAncestor(addPopup, src)) {
        addPopup.hide();
        // handle split button and item selection
        if (src instanceof IconAnchor) {
          String formIdNm = ((IconAnchor) src).getText().trim();
          //addToAssembly(ValidationFormID.valueOfDN(formIdNm));
        }
      }
    }
  }
}

