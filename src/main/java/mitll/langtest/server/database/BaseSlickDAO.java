package mitll.langtest.server.database;

import mitll.npdata.dao.BaseWrapper;

/**
 * Created by go22670 on 3/13/17.
 */
public abstract class BaseSlickDAO extends DAO {

  protected BaseSlickDAO(Database database) {
    super(database);
  }

  protected abstract BaseWrapper getWrapper();


  public void createTable() {  getWrapper().createTable();  }

  @Override
  public String getName() {   return getWrapper().getName();  }
}
