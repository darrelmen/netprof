package mitll.langtest.server.database.word;

import mitll.langtest.server.database.IDAO;

/**
 * Created by go22670 on 3/29/16.
 */
public interface IWordDAO  extends IDAO  {
  int addWord(Word word);
  void removeForResult(int resultid);
  int deleteForProject(int projID);
  boolean updateProjectForRID(int rid, int newprojid);
}
