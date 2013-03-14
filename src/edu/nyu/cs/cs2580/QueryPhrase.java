package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;

/**
 * @CS2580: implement this class for HW2 to handle phrase. If the raw query is
 *          ["new york city"], the presence of the phrase "new york city" must
 *          be recorded here and be used in indexing and ranking.
 */
public class QueryPhrase extends Query {

	private String _phraseIndicator = "\"";
	private boolean isProcessed = false;
	public Vector<String> _phrases = new Vector<String>();

	public QueryPhrase(String query) {
		super(query);
	}

	@Override
	public void processQuery() {
		if (_query == null) {
			return;
		}

		if (isProcessed) {
			return;
		}

		// check if even number of quotes are present
		String quotes = _query.replaceAll("[^\"]", "");
		int noOfQoutes = quotes.length();

		if (noOfQoutes % 2 != 0) {
			// remove last occurrence of quote

			int lastIndexOfQuote = _query.lastIndexOf('"');
			_query = _query.substring(0, lastIndexOfQuote)
					+ _query.substring(lastIndexOfQuote + 1);
		}

		boolean isPhrase = false;
		if (_query.startsWith(_phraseIndicator)) {
			isPhrase = true;
		}

		Scanner s = new Scanner(_query);
		s.useDelimiter(_phraseIndicator);

		String token = "";

		while (s.hasNext()) {
			token = s.next().trim();

			if (!isPhrase) {
				Scanner s2 = new Scanner(token);
				while (s2.hasNext()) {
					String str = Utilities.getStemmed(s2.next()).get(0);
					_tokens.add(str);
				}
			} else {
				Vector<String> stemmedPhrase = Utilities.getStemmed(token);
				StringBuffer sb = new StringBuffer();
				for (String term : stemmedPhrase) {
					sb.append(term);
					sb.append(" ");
				}
				_tokens.add(sb.toString().trim());
				//_phrases.add(sb.toString().trim());
			}

			isPhrase = !isPhrase;
		}

		isProcessed = true;
	}
}
