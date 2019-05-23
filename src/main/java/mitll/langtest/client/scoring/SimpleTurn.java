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

package mitll.langtest.client.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.dialog.ITurnContainer;
import mitll.langtest.shared.dialog.IDialog;
import mitll.langtest.shared.exercise.ClientExercise;

import java.util.logging.Logger;

public class SimpleTurn extends DivWidget implements ISimpleTurn {
  private final Logger logger = Logger.getLogger("SimpleTurn");

  protected ClientExercise exercise;
  private TurnPanelDelegate turnPanelDelegate;
  //private ITurnContainer.COLUMNS columns;

  public SimpleTurn(ClientExercise exercise, ITurnContainer.COLUMNS columns, boolean rightJustify) {
    this.exercise = exercise;
    // this.columns = columns;
    turnPanelDelegate = new TurnPanelDelegate(exercise, this, columns, rightJustify);
    ;
//    if (columns == MIDDLE) {
//      if (exercise.hasEnglishAttr()) {
//        addStyleName("floatRight");
//      } else {
//        addStyleName("floatRight");
//        getElement().getStyle().setClear(Style.Clear.BOTH);
//      }
//    }
  }

  @Override
  public int getExID() {
    return exercise.getID();
  }

  @Override
  public void makeVisible() {

  }

  /**
   * @param showFL
   * @param showALTFL
   * @param phonesChoices
   * @param englishDisplayChoices
   * @return
   * @see mitll.langtest.client.dialog.TurnViewHelper#getTurnPanel(ClientExercise, ITurnContainer.COLUMNS, ITurnContainer.COLUMNS, int)
   */
  @Override
  public DivWidget addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices, EnglishDisplayChoices englishDisplayChoices) {
    HTML html = new HTML(exercise.getForeignLanguage());
    html.addStyleName("flfont");
    html.getElement().getStyle().setPadding(10, Style.Unit.PX);
    logger.info("addWidgets : got '" + exercise.getForeignLanguage() + "' for " + exercise.getID());
    DivWidget widgets = new DivWidget();
    widgets.add(html);
    styleMe(widgets);
    add(widgets);
    return widgets;
  }

  protected void styleMe(DivWidget wrapper) {
    turnPanelDelegate.styleMe(wrapper);
//    if (columns == MIDDLE) {
//      wrapper.getElement().getStyle().setMarginRight(0, Style.Unit.PX);
//    }

  }
}
