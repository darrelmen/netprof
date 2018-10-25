package mitll.langtest.server.database.audio;

public class NativeAudioResult {
  private String nativeAudioRef;
  private boolean isContext;

  public NativeAudioResult(String nativeAudioRef, boolean isContext) {
    this.nativeAudioRef = nativeAudioRef;
    this.isContext = isContext;
  }

  public String getNativeAudioRef() {
    return nativeAudioRef;
  }

  public boolean isContext() {
    return isContext;
  }
}
