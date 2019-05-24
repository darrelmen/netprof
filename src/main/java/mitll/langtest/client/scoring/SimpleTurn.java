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
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.user.client.ui.HTML;
import mitll.langtest.client.dialog.ITurnContainer;
import mitll.langtest.client.sound.HighlightSegment;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class SimpleTurn extends DivWidget implements ISimpleTurn, IObscurable {
  private final Logger logger = Logger.getLogger("SimpleTurn");

  protected ClientExercise exercise;
  private TurnPanelDelegate turnPanelDelegate;
  private Language language;
  private boolean isChinese;
  boolean deleting;

  public SimpleTurn(ClientExercise exercise, ITurnContainer.COLUMNS columns, boolean rightJustify, Language language) {
    this.exercise = exercise;
    turnPanelDelegate = new TurnPanelDelegate(exercise, this, columns, rightJustify);
    this.language = language;
    isChinese = language == Language.MANDARIN || language == Language.JAPANESE;
  }

  @Override
  public int getExID() {
    return exercise.getID();
  }

  @Override
  public void makeVisible() {
    turnPanelDelegate.makeVisible();
  }

  @Override
  public void grabFocus() {

  }

  @Override
  public boolean isDeleting() {
    return deleting;
  }

  @Override
  public void setDeleting(boolean deleting) {
    this.deleting = deleting;
  }

  @Override
  public boolean hasCurrentMark() {
    return turnPanelDelegate.hasCurrentMark();
  }

  @Override
  public void markCurrent() {
    turnPanelDelegate.markCurrent();
  }

  @Override
  public void removeMarkCurrent() {
    turnPanelDelegate.removeMarkCurrent();
  }

  @Override
  public void obscureTextAndPhones() {
    obscureText();
  }

  @Override
  public void obscureText() {
    segments.forEach(iHighlightSegment -> {
      if (iHighlightSegment.isObscurable()) {
        iHighlightSegment.showHighlight();
      }
    });

  }

  @Override
  public void restoreText() {
//    segments.forEach(IHighlightSegment::restoreText);

    segments.forEach(iHighlightSegment -> {
      // if (iHighlightSegment.isObscurable()) {
      iHighlightSegment.restoreText();
      iHighlightSegment.clearHighlight();
      // }
    });
  }

  @Override
  public void maybeSetObscure(List<ClientExercise> coreVocabs) {
    new ObscureHelper(exercise.getID(), exercise.getForeignLanguage(), segments).maybeSetObscure(coreVocabs);
  }

  @Override
  public void maybeObscure(Set<String> coreVocabs) {
    new ObscureHelper(exercise.getID(), exercise.getForeignLanguage(), segments).maybeObscure(coreVocabs);
  }

  private List<IHighlightSegment> segments = new ArrayList<>();

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
//    html.addStyleName("flfont");
//    html.getElement().getStyle().setPadding(10, Style.Unit.PX);
    //  logger.info("addWidgets : got '" + exercise.getForeignLanguage() + "' for " + exercise.getID());
    DivWidget widgets = new DivWidget();
    segments.clear();
//    widgets.addStyleName("topFiveMargin");
    widgets.getElement().getStyle().setPaddingTop(10, Style.Unit.PX);
    widgets.getElement().getStyle().setPaddingLeft(10, Style.Unit.PX);
    widgets.addStyleName("flfont");

    List<String> tokens = new SearchTokenizer().getTokens(exercise.getForeignLanguage(), isChinese, new ArrayList<>());
    HasDirection.Direction direction = language.isRTL() ? HasDirection.Direction.RTL : HasDirection.Direction.LTR;

    tokens.forEach(token -> {
      HighlightSegment blue = new HighlightSegment(
          widgets.getWidgetCount(),
          token,
          direction,
          false, false, IHighlightSegment.DEFAULT_HIGHLIGHT, true);
      widgets.add(blue);
      segments.add(blue);
    });

    // widgets.add(html);
    styleMe(widgets);
    add(widgets);
    return widgets;
  }

  protected void styleMe(DivWidget wrapper) {
    turnPanelDelegate.styleMe(wrapper);
  }

  public String getContent() {
    return exercise.getForeignLanguage();
  }
}
