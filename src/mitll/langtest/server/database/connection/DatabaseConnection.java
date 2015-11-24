/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.connection;

import java.sql.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 12/31/12
 * Time: 5:47 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DatabaseConnection {
  void contextDestroyed();
  int connectionsOpen();

  Connection getConnection(String who);

  boolean isValid();

  boolean usingCP();
}
