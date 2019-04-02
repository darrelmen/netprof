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

package mitll.langtest.client.list;

import com.github.gwtbootstrap.client.ui.Dropdown;
import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import mitll.langtest.client.LangTest;
import mitll.langtest.client.custom.KeyStorage;
import mitll.langtest.client.download.DownloadEvent;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.scoring.PhonesChoices;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Created by go22670 on 5/18/17.
 *
 * @see FacetExerciseList#getPagerAndSort(ExerciseController)
 */
public class DisplayMenu {
  private final Logger logger = Logger.getLogger("DisplayMenu");

  private static final int OPTIONS_WIDTH = 161;

  private static final String SHOW_CM_SIMPLIFIED = "Show CM Simplified";
  private static final String SHOW_CM_TRADITIONAL = "Show CM Traditional";

  private static final String SHOW1 = "Show and Download";
  private static final String DOWNLOAD = "Download Content";
  private static final String SHOW_ALTERNATE_TEXT = "Show Alternate text";
  private static final String SHOW_PRIMARY_TEXT = "Show Primary text";

  /**
   * @see #getShowSounds
   */
  private static final String SHOW_SOUNDS = "Show Sounds";
  private static final String SHOW_PINYIN = "Show Pinyin";

  /**
   * @see DisplayMenu#storePhoneChoices
   */
  public static final String SHOW_PHONES = "showPhones";

  private static final IconType CHECK = IconType.CHECK;

  public static final String SHOWFL = "showStorageFL";
  public static final String SHOWALT = "showStorageALTFL";
  private final boolean isMandarin;

  private final KeyStorage storage;
  private final ShowEventListener showEventListener;
  private final boolean shouldSwap;

  DisplayMenu(KeyStorage storage, ShowEventListener showEventListener, boolean isMandarin, boolean shouldSwap) {
    this.storage = storage;
    this.showEventListener = showEventListener;
    this.isMandarin = isMandarin;
    this.shouldSwap = shouldSwap;
  }

  /**
   * Only show primary vs alt choices for Mandarin, which is only one we have for now.?
   *
   * @return
   * @see FacetExerciseList#getPagerAndSort
   */
  @NotNull
  Dropdown getRealViewMenu() {
    Dropdown view = new Dropdown(SHOW1);
    view.setIcon(IconType.COG);
    view.getTriggerWidget().getElement().getStyle().setColor("black");
    view.addStyleName("rightFiveMargin");
    view.getElement().getStyle().setListStyleType(Style.ListStyleType.NONE);
    view.setWidth(OPTIONS_WIDTH + "px");

    view.add(getShowSounds());
    String toUse = isMandarin ? getPrimaryMandarinChoice() : SHOW_PRIMARY_TEXT;

    if (isMandarin) {
      NavLink primary = new NavLink(toUse);
      view.add(primary);
      view.add(flTextChoices(primary));
    }
    view.add(getDownload());

    return view;
  }

  @NotNull
  private NavLink getDownload() {
    NavLink download = new NavLink(DOWNLOAD);
    download.setIcon(IconType.DOWNLOAD_ALT);
    download.addClickHandler(event -> LangTest.EVENT_BUS.fireEvent(new DownloadEvent()));
    return download;
  }

  private NavLink getShowSounds() {
    NavLink phoneChoice = new NavLink(isMandarin ? SHOW_PINYIN : SHOW_SOUNDS);
    if (getPhonesDisplay() == PhonesChoices.SHOW) {
      phoneChoice.setIcon(CHECK);
    }

    phoneChoice.addClickHandler(event -> {
      if (getPhonesDisplay() == PhonesChoices.SHOW) {
        phoneChoice.setIcon(null);
        storePhoneChoices(PhonesChoices.HIDE.toString());
      } else {
        phoneChoice.setIcon(CHECK);
        storePhoneChoices(PhonesChoices.SHOW.toString());
      }
      fireShowEvent();
    });
    return phoneChoice;
  }

  private NavLink flTextChoices(NavLink primary) {
    NavLink altflChoice = new NavLink(isMandarin ? getAltMandarinChoice() : SHOW_ALTERNATE_TEXT);
    boolean choicesFL1 = getChoicesFL();
    primary.setIcon(choicesFL1 ? IconType.CHECK : null);
    boolean choicesALT1 = getChoicesALT();
    altflChoice.setIcon(choicesALT1 ? IconType.CHECK : null);

    if (!choicesFL1 && !choicesALT1) {
      storeShowChoicesFL(true);
      primary.setIcon(IconType.CHECK);
    }

    altflChoice.addClickHandler(event -> {
      boolean choicesFL = getChoicesFL();
      boolean choicesALT = getChoicesALT();

      // if alt selected, then unselect
      storeShowChoicesALT(!choicesALT);
      if (choicesALT && !choicesFL) {
        storeShowChoicesFL(true);
        primary.setIcon(IconType.CHECK);
      }
      fireShowEvent();

      altflChoice.setIcon(choicesALT ? null : CHECK);
    });

    primary.addClickHandler(event -> {
      boolean choicesFL = getChoicesFL();
      boolean choicesALT = getChoicesALT();

      // if alt selected, then unselect
      storeShowChoicesFL(!choicesFL);
      if (choicesFL && !choicesALT) {
        storeShowChoicesALT(true);
        altflChoice.setIcon(IconType.CHECK);
      }
      fireShowEvent();

      primary.setIcon(choicesFL ? null : CHECK);
    });

    return altflChoice;
  }

  @NotNull
  private String getPrimaryMandarinChoice() {
    return shouldSwap ? SHOW_CM_TRADITIONAL : SHOW_CM_SIMPLIFIED;
  }

  @NotNull
  private String getAltMandarinChoice() {
    return shouldSwap ? SHOW_CM_SIMPLIFIED : SHOW_CM_TRADITIONAL;
  }

  @NotNull
  private boolean getChoicesFL() {
    //logger.info("Stored fl value " + stored);
    return getStored(storage.getValue(SHOWFL), true);
  }

  private void storeShowChoicesFL(boolean toStore) {
    storage.storeValue(SHOWFL, Boolean.toString(toStore));
  }

  @NotNull
  private boolean getChoicesALT() {
    return getStored(storage.getValue(SHOWALT), false);
  }

  private void storeShowChoicesALT(boolean toStore) {
    storage.storeValue(SHOWALT, Boolean.toString(toStore));
  }

  private boolean getStored(String show, boolean defaultVal) {
    boolean showFL = defaultVal;
    if (show != null) {
      showFL = Boolean.parseBoolean(show);
    }
    return showFL;
  }


  private void storePhoneChoices(String toStore) {
    storage.storeValue(SHOW_PHONES, toStore);
  }

  private void fireShowEvent() {
    showEventListener.gotShow();
  }

  /**
   * @return
   * @see #getShowSounds
   */
  @NotNull
  private PhonesChoices getPhonesDisplay() {
    PhonesChoices choices = PhonesChoices.HIDE;
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
