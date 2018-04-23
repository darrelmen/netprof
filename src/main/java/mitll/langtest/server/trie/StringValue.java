package mitll.langtest.server.trie;

public class StringValue implements TextEntityValue<String> {
  private final String s;

  public StringValue(String s) {
    this.s = s;
  }

  @Override
  public String getNormalizedValue() {
    return s;
  }

  @Override
  public String getValue() {
    return s;
  }
}
