package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

/**
 * Representation of a user query.
 * 
 * In HW1: instructors provide this simple implementation.
 * 
 * In HW2: students must implement {@link QueryPhrase} to handle phrases.
 * 
 * @author congyu
 * @auhtor fdiaz
 */
public class Query {
  public String _query = null;
  public Vector<String> _tokens = new Vector<String>();
  private boolean isProcessed = false;

  public Query(String query) {
    _query = query.trim();
  }

  public void processQuery() {
    if (_query == null) {
      return;
    }
    
    if(isProcessed) {
    	return;
    }
    
    Scanner s = new Scanner(_query);
    while (s.hasNext()) {
      _tokens.add(Utilities.getStemmed(s.next()).get(0));
    }
    s.close();
    
    isProcessed = true;
  }
}
