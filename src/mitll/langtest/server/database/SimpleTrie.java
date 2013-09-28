package mitll.langtest.server.database;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 9/27/13
 * Time: 10:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleTrie {
  public class Trie {

    private Node root = new Node("");

    public Trie() {
    }

    public Trie(List<String> argInitialWords) {
      for (String word : argInitialWords) {
        addWord(word);
      }
    }

    public void addWord(String argWord) {
      Node currentNode = root;

      for (char argChar : argWord.toCharArray()) {
        if (!currentNode.containsChildValue(argChar)) {
          currentNode.addChild(argChar, new Node(currentNode.getValue() + argChar));
        }

        currentNode = currentNode.getChild(argChar);
      }

      currentNode.setIsWord(true);
    }

    public boolean containsPrefix(String argPrefix) {
      return contains(argPrefix, false);
    }

    public boolean containsWord(String argWord) {
      return contains(argWord, true);
    }

    public Node getWord(String argString) {
      Node node = getNode(argString);
      return node != null && node.isWord() ? node : null;
    }

    public Node getPrefix(String argString) {
      return getNode(argString);
    }


    private boolean contains(String argString, boolean argIsWord) {
      Node node = getNode(argString);
      return (node != null && node.isWord() && argIsWord) || (!argIsWord && node != null);
    }

    private Node getNode(String argString) {
      Node currentNode = root;
      char argChars[] = argString.toCharArray();
      for (int i = 0; i < argChars.length && currentNode != null; i++) {
        currentNode = currentNode.getChild(argChars[i]);

        if (currentNode == null) {
          return null;
        }
      }

      return currentNode;
    }
  }


  class Node {

    private final String value;
    private Map<Character, Node> children = new HashMap<Character, Node>();
    private boolean isValidWord;

    public Node(String argValue) {
      value = argValue;
    }

    public boolean addChild(char c, Node argChild) {
      children.put(c, argChild);
      return true;
    }

    public boolean containsChildValue(char c) {
      return children.containsKey(c);
    }

    public String getValue() {
      return value;
    }

    public Node getChild(char c) {
      return children.get(c);
    }

    public boolean isWord() {
      return isValidWord;
    }

    public void setIsWord(boolean argIsWord) {
      isValidWord = argIsWord;

    }

    public String toString() {
      return value;
    }

  }

  public static void main(String [] arg) {

  }
}
