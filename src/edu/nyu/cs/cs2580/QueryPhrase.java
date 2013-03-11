package edu.nyu.cs.cs2580;

import java.util.Scanner;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 *          ["new york city"], the presence of the phrase "new york city" must
 *          be recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {
	
	private String _phraseIndicator = "\"";

	public QueryPhrase(String query) {
		super(query);
	}

	@Override
	public void processQuery() {
		if (_query == null) {
			return;
		}
		
		Scanner s = new Scanner(_query);
		s.useDelimiter(_phraseIndicator);
		boolean isPhrase = false;
		String token = "";
		
		while(s.hasNext()) {
			token = s.next().trim();
			
			if(! isPhrase) {
				Scanner s2 = new Scanner(token);
				while(s2.hasNext()) {
					_tokens.add(s2.next());
				}
			} else {
				_tokens.add(token);
			}
			
			isPhrase = !isPhrase;			
		}
	}
}
