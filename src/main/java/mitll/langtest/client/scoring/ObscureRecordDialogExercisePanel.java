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

import mitll.langtest.client.banner.SessionManager;
import mitll.langtest.client.dialog.IRehearseView;
import mitll.langtest.client.dialog.ITurnContainer;
import mitll.langtest.client.exercise.ExerciseController;
import mitll.langtest.client.list.ListInterface;
import mitll.langtest.client.sound.IHighlightSegment;
import mitll.langtest.shared.exercise.ClientExercise;
import mitll.langtest.shared.scoring.AlignmentOutput;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class ObscureRecordDialogExercisePanel extends RecordDialogExercisePanel implements IObscurable {
  private final Logger logger = Logger.getLogger("ObscureRecordDialogExercisePanel");

  public ObscureRecordDialogExercisePanel(ClientExercise commonExercise,
                                          ExerciseController controller,
                                          ListInterface<?, ?> listContainer,
                                          Map<Integer, AlignmentOutput> alignments,
                                          IRehearseView listenView, SessionManager sessionManager,
                                          ITurnContainer.COLUMNS columns,
                                          ITurnContainer.COLUMNS prevColumn) {
    super(commonExercise, controller, listContainer, alignments, listenView, sessionManager, columns, prevColumn);
  }

  @Override
  public void obscureTextAndPhones() {
    //  logger.info("obscureTextAndPhones For " + exercise.getID() + " obscure " + flclickables.size() + " clickables");
    flclickables.forEach(iHighlightSegment -> {
      iHighlightSegment.setObscurable();
      boolean b = iHighlightSegment.obscureText();
      if (!b) logger.info("huh? didn't obscure");
    });
    flClickableRowPhones.setVisible(false);
  }

  @Override
  public void obscureText() {
//    logger.info("obscureText For " + exercise.getID() + " obscure " + flclickables.size() + " clickables");
    flclickables.forEach(IHighlightSegment::obscureText);
  }

  @Override
  public void restoreText() {
    flclickables.forEach(IHighlightSegment::restoreText);
    flClickableRowPhones.setVisible(true);
  }

  /**
   * Rules:
   * <p>
   * 1) don't obscure everything
   * 2) obscure something
   * 3) Don't obscure more than one or two or three? words?
   * 4) if choosing only two out of all of them, choose the longest ones?
   * 3) if you have a choice, don't obscure first token? ?
   *
   * @param coreVocab
   * @see mitll.langtest.client.dialog.PerformViewHelper#getTurnPanel
   * Or should we use exact match?
   */
  public void maybeSetObscure(List<ClientExercise> coreVocabs) {
    new ObscureHelper(exercise.getID(), exercise.getForeignLanguage(), flclickables).maybeSetObscure(coreVocabs);
  }

  @Override
  public void maybeObscure(Set<String> coreVocabs) {
    new ObscureHelper(exercise.getID(), exercise.getForeignLanguage(), flclickables).maybeObscure(coreVocabs);
  }
}
