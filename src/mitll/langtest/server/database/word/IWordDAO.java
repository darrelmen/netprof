package mitll.langtest.server.database.word;

import java.util.List;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IWordDAO {
  long addWord(Word word);
  List<Word> getAll();
}
