package mitll.langtest.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by go22670 on 1/24/14.
 */
public class LZW {
  /**
   * Compress a string to a list of output symbols.
   */
  public static List<Integer> compress(String uncompressed) {
    // Build the dictionary.
    int length = uncompressed.length();
    System.out.println("got uncompressed size " + length);

    long then = System.currentTimeMillis();
    int dictSize = 256;
    Map<String, Integer> dictionary = new HashMap<String, Integer>();
    for (int i = 0; i < 256; i++)
      dictionary.put("" + (char) i, i);

    String w = "";
    List<Integer> result = new ArrayList<Integer>();
    for (char c : uncompressed.toCharArray()) {
      String wc = w + c;
      if (dictionary.containsKey(wc))
        w = wc;
      else {
        result.add(dictionary.get(w));
        // Add wc to the dictionary.
        dictionary.put(wc, dictSize++);
        w = "" + c;
      }
    }

    // Output the code for w.
    if (!w.equals(""))
      result.add(dictionary.get(w));
    long now = System.currentTimeMillis();
    if (now - then > 20) {
      System.out.println("took " + (now - then) + " millis to compress " + length);
    }
    return result;
  }

  /**
   * Decompress a list of output ks to a string.
   */
  public static String decompress(List<Integer> compressed) {
    long then = System.currentTimeMillis();

    int size = compressed.size();
    System.out.println("got compressed size " + size);
    // Build the dictionary.
    int dictSize = 256;
    Map<Integer, String> dictionary = new HashMap<Integer, String>();
    for (int i = 0; i < 256; i++)
      dictionary.put(i, "" + (char) i);

    String w = "" + (char) (int) compressed.remove(0);
    StringBuilder result = new StringBuilder(w);
    for (int k : compressed) {
      String entry;
      if (dictionary.containsKey(k))
        entry = dictionary.get(k);
      else if (k == dictSize)
        entry = w + w.charAt(0);
      else
        throw new IllegalArgumentException("Bad compressed k: " + k);

      result.append(entry);

      // Add w+entry[0] to the dictionary.
      dictionary.put(dictSize++, w + entry.charAt(0));

      w = entry;
    }
    long now = System.currentTimeMillis();

    if(now-then > 100) {
      System.out.println("took " + (now-then) + " millis to decompress " + size);
    }

    return result.toString();
  }

}
