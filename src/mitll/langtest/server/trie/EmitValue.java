package mitll.langtest.server.trie;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 11/13/13
 * Time: 1:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class EmitValue {
  int n;
  private TextEntityValue textEntityDescription;
  public EmitValue(TextEntityValue textEntityDescription, int n) {
    this.n=  n;
    this.textEntityDescription = textEntityDescription;
  }
  public String getValue() { return textEntityDescription.getNormalizedValue(); }

  public TextEntityValue getTextEntityDescription() {
    return textEntityDescription;
  }
  public String toString() { return textEntityDescription == null ? "huh?" : "'"+textEntityDescription.getNormalizedValue() + "'"
      //+
      //" : " +n
      ;}
}
