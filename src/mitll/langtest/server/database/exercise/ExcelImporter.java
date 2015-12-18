/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.server.database.exercise;

import mitll.langtest.server.ServerProperties;
import mitll.langtest.shared.exercise.CommonExercise;

import java.util.List;

/**
 * Created by go22670 on 12/18/15.
 */
public class ExcelImporter {
  ServerProperties props;

  public ExcelImporter(ServerProperties props) {
    this.props = props;
  }

  public void importExcel(String toImport) {
    ExcelImport excelImport = new ExcelImport(toImport, props);
    List<CommonExercise> exercises = excelImport.readExercises();

  }
}
