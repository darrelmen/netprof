package mitll.langtest.server.trie;

/**
 * Created with IntelliJ IDEA.
 * User: go22670
 * Date: 11/13/13
 * Time: 1:21 AM
 * To change this template use File | Settings | File Templates.
 */
public interface TextEntityValue<T> {
  String getNormalizedValue();
  T getValue();
}
