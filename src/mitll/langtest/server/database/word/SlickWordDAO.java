/*
 *
 * DISTRIBUTION STATEMENT C. Distribution authorized to U.S. Government Agencies
 * and their contractors; 2015. Other request for this document shall be referred
 * to DLIFLC.
 *
 * WARNING: This document may contain technical data whose export is restricted
 * by the Arms Export Control Act (AECA) or the Export Administration Act (EAA).
 * Transfer of this data by any means to a non-US person who is not eligible to
 * obtain export-controlled data is prohibited. By accepting this data, the consignee
 * agrees to honor the requirements of the AECA and EAA. DESTRUCTION NOTICE: For
 * unclassified, limited distribution documents, destroy by any method that will
 * prevent disclosure of the contents or reconstruction of the document.
 *
 * This material is based upon work supported under Air Force Contract No.
 * FA8721-05-C-0002 and/or FA8702-15-D-0001. Any opinions, findings, conclusions
 * or recommendations expressed in this material are those of the author(s) and
 * do not necessarily reflect the views of the U.S. Air Force.
 *
 * © 2015 Massachusetts Institute of Technology.
 *
 * The software/firmware is provided to you on an As-Is basis
 *
 * Delivered to the US Government with Unlimited Rights, as defined in DFARS
 * Part 252.227-7013 or 7014 (Feb 2014). Notwithstanding any copyright notice,
 * U.S. Government rights in this work are defined by DFARS 252.227-7013 or
 * DFARS 252.227-7014 as detailed above. Use of this work other than as specifically
 * authorized by the U.S. Government may violate any copyrights that exist in this work.
 *
 *
 */

package mitll.langtest.server.database.word;

import mitll.langtest.server.database.Database;
import mitll.langtest.server.database.ISchema;
import mitll.langtest.server.database.userexercise.BaseUserExerciseDAO;
import mitll.npdata.dao.DBConnection;
import mitll.npdata.dao.SlickWord;
import mitll.npdata.dao.word.WordDAOWrapper;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlickWordDAO extends BaseUserExerciseDAO implements IWordDAO, ISchema<Word, SlickWord> {
  private static final Logger logger = Logger.getLogger(SlickWordDAO.class);

  private final WordDAOWrapper dao;

  public SlickWordDAO(Database database, DBConnection dbConnection) {
    super(database);
    dao = new WordDAOWrapper(dbConnection);
  }

  public void createTable() {
    dao.createTable();
  }

  @Override
  public String getName() {
    return dao.dao().name();
  }

  @Override
  public SlickWord toSlick(Word shared, String language) {
    return new SlickWord(-1,
        (int) shared.getRid(),
        shared.getWord(),
        shared.getSeq(),
        shared.getScore(),
        (int) shared.getId());
  }

  @Override
  public Word fromSlick(SlickWord slick) {
    return new Word(
        slick.id(),
        slick.rid(),
        slick.word(),
        slick.seq(),
        slick.score());
  }

  public void insert(SlickWord word) {
    dao.insert(word);
  }

  public void addBulk(List<SlickWord> bulk) {
    dao.addBulk(bulk);
  }

  @Override
  public long addWord(Word word) {
    return dao.insert(toSlick(word, ""));
  }

  public Map<Integer, Integer> getOldToNew() {
    Map<Integer, Integer> oldToNew = new HashMap<>();
    for (SlickWord word : dao.getAll()) oldToNew.put(word.legacyid(), word.id());
    return oldToNew;
  }

  public boolean isEmpty() { return dao.getNumRows() == 0; }
}
