package mitll.langtest.server.audio;

/**
 * Created by go22670 on 11/15/16.
 */
public class DecoderOptions {
  private boolean isRefRecording = false;
  private boolean recordInResults = true;
  private boolean doDecode = false;
  private boolean canUseCache = true;
  private boolean allowAlternates = false;
 // private boolean useOldSchool = false;
  private boolean usePhoneToDisplay = false;

  public DecoderOptions() {}

  /**
   *
   * @param isRefRecording
   * @param recordInResults
   * @param doDecode
   * @param canUseCache
   * @param allowAlternates
   * @param usePhoneToDisplay
   */
  public DecoderOptions(boolean isRefRecording,
                        boolean recordInResults,
                        boolean doDecode,
                        boolean canUseCache,
                        boolean allowAlternates,
                        boolean usePhoneToDisplay) {
    this.isRefRecording = isRefRecording;
    this.recordInResults = recordInResults;
    this.doDecode = doDecode;
    this.canUseCache = canUseCache;
    this.allowAlternates = allowAlternates;
 //   this.useOldSchool = useOldSchool;
    this.usePhoneToDisplay = usePhoneToDisplay;
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
/*
  public boolean isUseOldSchool() {
    return useOldSchool;
  }

  public DecoderOptions setUseOldSchool(boolean val) {
    this.useOldSchool = val;
    return this;
  }
*/

  public boolean isRecordInResults() {
    return recordInResults;
  }

  /**
   * @see mitll.langtest.server.services.AudioServiceImpl#writeAudioFile
   * @param val
   * @return
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
