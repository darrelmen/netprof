package mitll.langtest.server.trie;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 11/13/13
 * Time: 1:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class EmitValue<T> {
  private int numTokens;
  private TextEntityValue<T> textEntityDescription;
  public EmitValue(TextEntityValue<T> textEntityDescription, int n) {
    this.numTokens = n;
    this.textEntityDescription = textEntityDescription;
  }

  public TextEntityValue<T> getTextEntityDescription() { return textEntityDescription; }

  public String getNormalizedValue() { return textEntityDescription.getNormalizedValue(); }
  public T getValue() { return textEntityDescription.getValue(); }

  public String toString() {
    return textEntityDescription == null ? "huh?" : "'" + textEntityDescription.getNormalizedValue() + "'";
  }
}
