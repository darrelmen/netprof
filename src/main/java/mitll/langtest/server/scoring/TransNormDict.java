package mitll.langtest.server.scoring;

public class TransNormDict {
  private final String transcript;
  private final String normTranscript;
  private final String dict;

  TransNormDict(String transcript, String normTranscript, String dict) {
    this.transcript = transcript;
    this.normTranscript = normTranscript;
    this.dict = dict;
  }

  public String getTranscript() {
    return transcript;
  }

  String getNormTranscript() {
    return normTranscript;
  }

  public String getDict() {
    return dict;
  }

  public String toString() {
    return
        "\ttranscript " + transcript +
            "\n\tnorm     " + normTranscript +
            "\n\tdict     " + dict;
  }
}
