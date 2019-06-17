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

package mitll.langtest.shared.scoring;

import com.github.gwtbootstrap.client.ui.base.DivWidget;
import com.google.gwt.user.client.rpc.IsSerializable;
import mitll.langtest.client.scoring.ScoreFeedbackHelper;
import mitll.langtest.shared.instrumentation.TranscriptSegment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 4/13/17.
 */
public class AlignmentOutput implements IsSerializable {
  private long modified;
  private Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes = new HashMap<NetPronImageType, List<TranscriptSegment>>();

  private boolean showPhoneScores;

  /**
   * Required by RPC.
   */
  public AlignmentOutput() {
  }

  /**
   * @param sTypeToEndTimes
   * @see mitll.langtest.server.scoring.AlignmentHelper#getCachedAudioRef
   * @see mitll.langtest.server.services.ScoringServiceImpl#getCachedAudioRef
   */
  public AlignmentOutput(Map<NetPronImageType, List<TranscriptSegment>> sTypeToEndTimes, long modified) {
    this.sTypeToEndTimes = sTypeToEndTimes;
    this.modified = modified;
  }

  /**
   * @return
   * @see ScoreFeedbackHelper#showScoreFeedback(AlignmentAndScore, boolean, DivWidget, float)
   */
  public Map<NetPronImageType, List<TranscriptSegment>> getTypeToSegments() {
    return sTypeToEndTimes;
  }

  public boolean isShowPhoneScores() {
    return showPhoneScores;
  }

  public void setShowPhoneScores(boolean showPhoneScores) {
    this.showPhoneScores = showPhoneScores;
  }

  public boolean isStale(long modified) {
    return modified > this.modified;
  }

  public String toString() {
    List<TranscriptSegment> transcriptSegments = sTypeToEndTimes.get(NetPronImageType.WORD_TRANSCRIPT);
    return transcriptSegments == null ? "NO_SEGMENTS?" : transcriptSegments.isEmpty() ? " EMPTY " : transcriptSegments.toString();
  }

  public long getModified() {
    return modified;
  }
}
