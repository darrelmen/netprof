package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.DropdownSubmenu;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.download.DownloadEvent;
import mitll.langtest.client.download.ShowEvent;
import mitll.langtest.client.scoring.PhonesChoices;
import mitll.langtest.client.scoring.ShowChoices;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 5/18/17.
 */
public class DisplayMenu {
  public static final String SHOW_SOUNDS = "Show Sounds";
  private final Logger logger = Logger.getLogger("DisplayMenu");

  private static final String SHOW_PHONES = "showPhones";
  private static final IconType CHECK = IconType.CHECK;
  private static final String SHOW = "showStorage";

  private NavLink phoneChoice;
  private KeyStorage storage;
  
  public DisplayMenu(KeyStorage storage) {
    this.storage =storage;
  }

  @NotNull
  public Dropdown getRealViewMenu() {
    Dropdown view = new Dropdown("Show");
    view.getTriggerWidget().getElement().getStyle().setColor("black");
    view.addStyleName("rightFiveMargin");
    view.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);
    view.setWidth("60px");
   // view.getTriggerWidget().setCaret(false);

   // view.addClickHandler(event -> phoneChoice.setText(SHOW_SOUNDS));
  //  view.setIcon(IconType.REORDER);
    NavLink download = new NavLink("Download");
    download.setIcon(IconType.DOWNLOAD_ALT);
    view.add(download);

    download.addClickHandler(event -> LangTest.EVENT_BUS.fireEvent(new DownloadEvent()));

    DropdownSubmenu showChoices = getViewMenu();
    view.add(showChoices);
    return view;
  }

  @NotNull
  private DropdownSubmenu getViewMenu() {
    DropdownSubmenu showChoices = new DropdownSubmenu("Show");
    flTextChoices(showChoices);

    phoneChoice = new NavLink(SHOW_SOUNDS);
    if (getPhonesDisplay() == PhonesChoices.SHOW) {
      phoneChoice.setIcon(CHECK);
    }

    showChoices.add(phoneChoice);
    phoneChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        if (getPhonesDisplay() == PhonesChoices.SHOW) {
          // phoneChoice.setText("Hide Sounds");
          storePhoneChoices(PhonesChoices.HIDE.toString());
          phoneChoice.setIcon(null);
        } else {
          // phoneChoice.setText("Show Sounds");
          phoneChoice.setIcon(CHECK);
          storePhoneChoices(PhonesChoices.SHOW.toString());
        }
        fireShowEvent();
      }
    });

    return showChoices;
  }

//  @NotNull
//  private String getPhoneMenuTitle() {
//    return SHOW_SOUNDS;
//  }

  private void flTextChoices(DropdownSubmenu showChoices) {
    NavLink altflChoice = new NavLink("Alternate text");
    NavLink primary = new NavLink("Primary text");
    NavLink both = new NavLink("Both Primary and Alternate");

    ShowChoices choices = getChoices();
    switch (choices) {
      case BOTH:
        both.setIcon(CHECK);
        break;
      case FL:
        primary.setIcon(CHECK);
        break;
      case ALTFL:
        altflChoice.setIcon(CHECK);
        break;
    }
    altflChoice.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storeShowChoices(ShowChoices.ALTFL.toString());
        fireShowEvent();
        altflChoice.setIcon(CHECK);
        both.setIcon(null);
        primary.setIcon(null);
      }
    });
    showChoices.add(primary);
    showChoices.add(altflChoice);
    primary.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storeShowChoices(ShowChoices.FL.toString());
        fireShowEvent();
        altflChoice.setIcon(null);
        both.setIcon(null);
        primary.setIcon(CHECK);
      }
    });
    showChoices.add(both);
    both.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        storeShowChoices(ShowChoices.BOTH.toString());
        fireShowEvent();
        altflChoice.setIcon(null);
        both.setIcon(CHECK);
        primary.setIcon(null);
      }
    });
  }

  @NotNull
  private ShowChoices getChoices() {
    ShowChoices choices = ShowChoices.FL;
    String show = storage.getValue(SHOW);
    if (show != null) {
      try {
        choices = ShowChoices.valueOf(show);
      } catch (IllegalArgumentException ee) {
      }
    }
    return choices;
  }


  private void storeShowChoices(String toStore) {
    storage.storeValue(SHOW, toStore);
  }

  private void storePhoneChoices(String toStore) {
    storage.storeValue(SHOW_PHONES, toStore);
  }

  private void fireShowEvent() {
    LangTest.EVENT_BUS.fireEvent(new ShowEvent());
  }

  @NotNull
  private PhonesChoices getPhonesDisplay() {
    PhonesChoices choices = PhonesChoices.SHOW;
    String show = storage.getValue(SHOW_PHONES);
    if (show != null && !show.isEmpty()) {
      try {
        choices = PhonesChoices.valueOf(show);
        //   logger.info("getPhonesDisplay got " + choices);
      } catch (IllegalArgumentException ee) {
        logger.warning("getPhonesDisplay got " + ee);
      }
    }
    return choices;
  }
}
