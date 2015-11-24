/*
 * Copyright © 2011-2015 Massachusetts Institute of Technology, Lincoln Laboratory
 */

package mitll.langtest.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by go22670 on 9/25/14.
 */
public class ResultAndTotal implements IsSerializable {
  public List<MonitorResult> results;
  public int numTotal;
  public int req;
  public ResultAndTotal() {}

  public ResultAndTotal(ArrayList<MonitorResult> monitorResults, int n, int req) {
    this.results = monitorResults;
    this.numTotal = n;
    this.req = req;
  }

}
