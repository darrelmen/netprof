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

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Created by go22670 on 11/15/16.
 */
public class DecoderOptions implements IsSerializable {
  private boolean isRefRecording = false;
  private boolean recordInResults = true;
  private boolean doDecode = false;
  private boolean doAlign = false;
  private boolean canUseCache = true;
  private boolean allowAlternates = false;
  private boolean usePhoneToDisplay = false;
  private boolean compressLater = false;

  public DecoderOptions() {
  }

  public boolean isRefRecording() {
    return isRefRecording;
  }

  public DecoderOptions setRefRecording(boolean val) {
    this.isRefRecording = val;
    return this;
  }

  public boolean shouldDoDecoding() {
    return doDecode;
  }

  public DecoderOptions setDoDecode(boolean val) {
    this.doDecode = val;
    return this;
  }

  public boolean shouldDoAlignment() {
    return doAlign;
  }

  /**
   * @see mitll.langtest.client.recorder.RecordButtonPanel#postAudioFile
   * @param val
   * @return
   */
  public DecoderOptions setDoAlignment(boolean val) {
    this.doAlign = val;
    return this;
  }

  public boolean isCanUseCache() {
    return canUseCache;
  }

  public DecoderOptions setCanUseCache(boolean val) {
    this.canUseCache = val;
    return this;
  }

  public boolean isAllowAlternates() {
    return allowAlternates;
  }

  public DecoderOptions setAllowAlternates(boolean val) {
    this.allowAlternates = val;
    return this;
  }

  public boolean isRecordInResults() {
    return recordInResults;
  }

  /**
   * @param val
   * @return
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile
   */
  public DecoderOptions setRecordInResults(boolean val) {
    this.recordInResults = val;
    return this;
  }

  public DecoderOptions setUsePhoneToDisplay(boolean usePhoneToDisplay) {
    this.usePhoneToDisplay = usePhoneToDisplay;
    return this;
  }

  public boolean isUsePhoneToDisplay() {
    return usePhoneToDisplay;
  }

/*  public DecoderOptions setCompressLater(boolean val) {
    this.compressLater = val;
    return this;
  }*/

  public boolean shouldCompressLater() {
    return compressLater;
  }



  public String toString() {
    return "Decoder options " +
            (isRefRecording() ? "add to audio table " : "") +
            (recordInResults ? "add to results table " : "") +
            (doDecode ? "decoding " : "alignment ") +
            (canUseCache ? "use score cache " : "") +
            (allowAlternates ? "allow alternates paths in decoding " : "")
            // +        (useOldSchool ? "use hydec " : "use hydra")
            ;
  }
}
