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

import com.github.gwtbootstrap.client.ui.Icon;
import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import mitll.langtest.client.dialog.IListenView;
import mitll.langtest.client.dialog.ITurnContainer;
import mitll.langtest.client.dialog.ListenViewHelper;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.AllHighlight;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.AudioAttribute;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.project.ProjectStartupInfo;
import mitll.langtest.shared.scoring.AlignmentOutput;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Does left, right, or middle justify
 *
 * @param <T>
 * @see ListenViewHelper#reallyGetTurnPanel
 */
public class TurnPanel extends DialogExercisePanel<ClientExercise> implements ITurnPanel {
  private final Logger logger = Logger.getLogger("TurnPanel");

  private TurnPanelDelegate turnPanelDelegate;

  /**
   * @param clientExercise
   * @param controller
   * @param listContainer
   * @param alignments
   * @param listenView
   * @param columns
   * @param prevColumn
   * @param rightJustify
   * @see ListenViewHelper#reallyGetTurnPanel
   */
  public TurnPanel(final ClientExercise clientExercise,
                   final ExerciseController<ClientExercise> controller,
                   final ListInterface<?, ?> listContainer,
                   Map<Integer, AlignmentOutput> alignments,
                   IListenView listenView,
                   ITurnContainer.COLUMNS columns,
                   ITurnContainer.COLUMNS prevColumn,
                   boolean rightJustify) {
    super(clientExercise, controller, listContainer, alignments, listenView, false);

    if (columns == ITurnContainer.COLUMNS.MIDDLE) {
      if (prevColumn == ITurnContainer.COLUMNS.RIGHT) {
        addStyleName("floatRight");
      } else {
        addStyleName("floatLeft");
      }
    }
    turnPanelDelegate = new TurnPanelDelegate(clientExercise, this, columns, rightJustify);
  }

  void makeClickableWords(ProjectStartupInfo projectStartupInfo, ListInterface listContainer) {
    super.makeClickableWords(projectStartupInfo, listContainer);
    clickableWords.setAreSegmentsClickable(false);
  }

  protected void rememberAudio() {
    AudioAttribute latestAudio = getLatestAudio();
    // logger.info("rememberAudio : turn " + getText() + " audio is " + latestAudio);
    rememberAudio(latestAudio);
  }

  private AudioAttribute getLatestAudio() {
    AudioAttribute latest = null;
    for (AudioAttribute audio : exercise.getAudioAttributes()) {
      if (latest == null || audio.getTimestamp() > latest.getTimestamp()) {
        latest = audio;
      }
    }
    return latest;
  }

  @Override
  public DivWidget addWidgets(boolean showFL, boolean showALTFL, PhonesChoices phonesChoices, EnglishDisplayChoices englishDisplayChoices) {
    DivWidget widgets = super.addWidgets(showFL, showALTFL, phonesChoices, englishDisplayChoices);

    if (exercise.getAudioAttributes().isEmpty()) {
      flClickableRow.add(getHasAudioIndicator());
    }
    return widgets;
  }

  private Widget getHasAudioIndicator() {
    Icon audioIndicator = new Icon(IconType.VOLUME_UP);

    audioIndicator.getElement().getStyle().setColor("red");
    Panel simple = new SimplePanel();
    simple.add(audioIndicator);
    simple.addStyleName("floatRight");
    return simple;
  }

  @Override
  public String getText() {
    return exercise.getForeignLanguage();
  }

  @Override
  boolean shouldShowPhones() {
//    List<ExerciseAttribute> speaker = exercise.getAttributes().stream().filter(exerciseAttribute -> exerciseAttribute.getProperty().equals("SPEAKER")).collect(Collectors.toList());
    boolean hasEnglishAttr = exercise.hasEnglishAttr();
    boolean b = !hasEnglishAttr && false;//isInterpreterTurn(speaker);
//    if (b)
//      logger.info("ex " + exercise.getID() + " " + exercise.getEnglish() + " " + exercise.getForeignLanguage() + " Got show phones " + b);
    return b;
  }

  /**
   * @param wrapper
   * @see mitll.langtest.shared.dialog.Dialog#addWidgets(boolean, boolean, PhonesChoices, EnglishDisplayChoices)
   */
  public void styleMe(DivWidget wrapper) {
    super.styleMe(wrapper);
    turnPanelDelegate.styleMe(wrapper);
    flClickableRow.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
  }

  @Override
  public void addFloatLeft(Widget w) {
    turnPanelDelegate.addFloatLeft(w);
  }

  @Override
  public boolean shouldAddFloatLeft() {
    return turnPanelDelegate.shouldAddFloatLeft();
  }

  @NotNull
  protected AllHighlight getAllHighlight(Collection<IHighlightSegment> flclickables) {
    return new AllHighlight(flclickables, !turnPanelDelegate.isMiddle());
  }

  @Override
  public boolean isMiddle() {
    return turnPanelDelegate.isMiddle();
  }

  @Override
  public boolean isLeft() {
    return turnPanelDelegate.isLeft();
  }

  @Override
  public boolean isRight() {
    return turnPanelDelegate.isRight();
  }

  /**
   * @see ListenViewHelper#removeMarkCurrent
   */
  public void removeMarkCurrent() {
    turnPanelDelegate.removeMarkCurrent();
  }

  /**
   * @see ListenViewHelper#markCurrent
   */
  public void markCurrent() {
    turnPanelDelegate.markCurrent();
  }

  public boolean hasCurrentMark() {
    return turnPanelDelegate.hasCurrentMark();
  }

  @Override
  public void grabFocus() {
    logger.info("grabFocus : OK " + getId() + " got the focus...");
  }

  public void makeVisible() {
    turnPanelDelegate.makeVisible();
  }

  @Override
  public void addClickHandler(ClickHandler clickHandler) {
    turnPanelDelegate.addClickHandler(clickHandler);
  }

  @Override
  public boolean isDeleting() {
    return false;
  }

  @Override
  public void setDeleting(boolean deleting) {
    logger.warning("don't call me");
  }

  @Override
  public String getContent() {
    return exercise.getForeignLanguage();
  }
}
