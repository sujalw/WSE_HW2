/**
 * 
 */
package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @author Amey
 * 
 */
public class QueryLikelihoodRankerwithJMSmoothing extends Ranker{

	private static double lambda = 0.0;

	public QueryLikelihoodRankerwithJMSmoothing(Options options,
		      CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		_options = options;
	    System.out.println("Using Ranker: " + this.getClass().getSimpleName());	
	}

	public static void setLambda(double lambda) {
		QueryLikelihoodRankerwithJMSmoothing.lambda = lambda;
	}
	
	/**
	 * @author Amey
	 * @return returns the fquid - no. of occurences of query term i in document
	 *         d
	 */
	public double getFrequid(String term, int documentId) {
		double freq = 0;
		Document d = _indexer.getDoc(documentId);
		Vector<String> content = ((DocumentFull) d).getConvertedBodyTokens();
		content.addAll(((DocumentFull) d).getConvertedTitleTokens());
		for (int i = 0; i < content.size(); i++) {
			if (term.equalsIgnoreCase(content.get(i))) {
				// System.out.println("hi "+ documentId);
				freq++;
			}
		}
		return freq;
	}

	/**
	 * @author Amey returns the |D| - total no. of words in document D
	 * @return
	 */
	public double getTotalNumberOfWordsInaDocument(int documentId) {
		double wordCount = 0;
		Document d = _indexer.getDoc(documentId);
		Vector<String> content = ((DocumentFull) d).getConvertedBodyTokens();
		content.addAll(((DocumentFull) d).getConvertedTitleTokens());
		wordCount = content.size();
		return wordCount;
	}

	/**
	 * @author Amey
	 * @param sds
	 * @return
	 */
	public Vector<ScoredDocument> sortScoredDocumentAsPerScore(
			Vector<ScoredDocument> sds) {
		if (sds.size() > 0) {
			Collections.sort(sds, new Comparator<ScoredDocument>() {
				@Override
				public int compare(final ScoredDocument obj1,
						final ScoredDocument obj2) {
					return Double.compare(obj2.get_score(), obj1.get_score());
				}
			});
		}
		return sds;
	}

	/**
	 * @author Amey
	 * @param term
	 * @param did
	 * @return
	 */
	public double getQueryLikelihood(String term, int did) {
		return (getFrequid(term, did) / getTotalNumberOfWordsInaDocument(did));
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		for (int i = 0; i < _indexer.numDocs(); ++i) {
			retrieval_results.add(runquery(query.toString(), i));
		}
		return sortScoredDocumentAsPerScore(retrieval_results);
	}
		
	public ScoredDocument runquery(String query, int did) {

		// Build query vector
		Scanner s = new Scanner(query);
		Vector<String> qv = new Vector<String>();
		while (s.hasNext()) {
			String term = s.next();
			qv.add(term);
		}

		// Get the document vector. For hw1, you don't have to worry about the
		// details of how index works.
		Document d = _indexer.getDoc(did);
		// Vector <String> content = d.get_body_vector();
		// Vector < String > dv = d.get_title_vector();

		double score = 0.0;
		for (int i = 0; i < qv.size(); ++i) {

			score += Math
					.log((1 - lambda)
							* (getQueryLikelihood(qv.get(i), did))
							+ (lambda)
							* (_indexer.corpusTermFrequency(qv.get(i)) / _indexer
									.totalTermFrequency()));
		}

		// antilog
		score = Math.pow(Math.E, score);

		return new ScoredDocument(d, score);
	}

	public double getLMPScore(Query query, Document d) {
		return runquery(query.toString(), d._docid).get_score();
	}	

}
