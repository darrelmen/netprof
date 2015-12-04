/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.client.analysis;

import com.github.gwtbootstrap.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;

/**
 * Created by go22670 on 12/4/15.
 */
public class TimeWidgets {
  final Button prevButton;
  final Button nextButton;
  final Button all;
  final Button week;
  final Button month;
  final HTML display;

  public TimeWidgets(Button prevButton, Button nextButton, HTML display, Button all, Button week, Button month) {
    this.prevButton = prevButton;
    this.nextButton = nextButton;
    this.display = display;
    this.all = all;
    this.week = week;
    this.month = month;
  }

  public void reset() {
    all.setActive(true);
    week.setActive(false);
    month.setActive(false);
  }
}
