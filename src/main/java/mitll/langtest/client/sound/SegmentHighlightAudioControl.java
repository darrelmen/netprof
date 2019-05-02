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

package mitll.langtest.client.sound;

import mitll.langtest.shared.instrumentation.TranscriptSegment;
import mitll.langtest.shared.scoring.NetPronImageType;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * Created by go22670 on 4/24/17.
 */
public class SegmentHighlightAudioControl implements AudioControl {
//  private final Logger logger = Logger.getLogger("SegmentHighlightAudioControl");
  private final SegmentAudioControl wordSegments;
  private SegmentAudioControl phoneSegments = null;

  /**
   * @param typeToSegmentToWidget
   * @param exID
   * @see mitll.langtest.client.scoring.DialogExercisePanel#setPlayListener(int, long, Map, HeadlessPlayAudio)
   */
  public SegmentHighlightAudioControl(Map<NetPronImageType, TreeMap<TranscriptSegment, IHighlightSegment>> typeToSegmentToWidget, int exID) {
    wordSegments = new SegmentAudioControl(exID, typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT));

    TreeMap<TranscriptSegment, IHighlightSegment> phones = typeToSegmentToWidget.get(NetPronImageType.PHONE_TRANSCRIPT);
    if (phones != null && !phones.isEmpty()) {
      phoneSegments = new SegmentAudioControl(exID, phones);
//      logger.info("phoneSegments now has " + phones.size());
//      logger.info("wordSegments  now has " + typeToSegmentToWidget.get(NetPronImageType.WORD_TRANSCRIPT).size());
    } else {
     // logger.warning("phoneSegments now has " + phones + " : " + typeToSegmentToWidget.keySet());
    }
  }

  @Override
  public void reinitialize() {
    // logger.info("reinitialize ");
    songFinished();
  }

  @Override
  public void songFirstLoaded(double durationEstimate) {
  }

  @Override
  public void loadAndPlaySegment(float startInSeconds, float endInSeconds) {
  }

  @Override
  public void songLoaded(double duration) {
    wordSegments.songLoaded(duration);
    if (phoneSegments != null) phoneSegments.songLoaded(duration);
  }

  @Override
  public void update(double position) {
    wordSegments.update(position);
    if (phoneSegments != null) {
      phoneSegments.update(position);
    }
//    else {
//      logger.info("no phone segments? update " + position);
//    }
  }

  @Override
  public void songFinished() {
    wordSegments.songFinished();
    if (phoneSegments != null) {
      phoneSegments.songFinished();
    }
  }

  public String toString() {
    return "SegmentHighlightAudioControl";
  }

}
