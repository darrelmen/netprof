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
