/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database;

import mitll.langtest.server.LogAndNotify;
import mitll.langtest.server.ServerProperties;

import java.sql.Connection;

/**
 * Created with IntelliJ IDEA.
 * User: GO22670
 * Date: 6/26/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Database {
  String TIME = "time";
  String EXID = "exid";

  Connection getConnection(String who);

  void closeConnection(Connection connection);

  void logEvent(String exid, String context, long userid, String device);

  ServerProperties getServerProps();

  String getLanguage();

  LogAndNotify getLogAndNotify();
}
