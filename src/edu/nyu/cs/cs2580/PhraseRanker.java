package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * 
 * @author sujal
 * 
 */
public class PhraseRanker extends Ranker {
	
	public PhraseRanker(Options options,
		      CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
	    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {		
		return runQuery(query, 2, numResults);
	}
	

	/**
	 * 
	 * @param n
	 *            length of phrase
	 * @param query
	 * @return
	 */
	public Vector<ScoredDocument> runQuery(Query query, int n, int numResults) {

		if (n <= 0) {
			return null;
		}

		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		double noOfDocs = _indexer.numDocs();
		for (int i = 0; i < noOfDocs; ++i) {
			retrieval_results.add(scoreDocument(query, i, n));

		}

		// retrieval_results = Utilities.getSorted(retrieval_results);
		return retrieval_results;
	}

	public ScoredDocument scoreDocument(Query query, int did, int n) {

		double score = getPhraseScore(query, did, n);
		Document d = _indexer.getDoc(did);
		return new ScoredDocument(d, score);
	}

	public double getPhraseScore(Query query, int did, int n) {
		// Build query vector
		
		// Build query vector
		query.processQuery();
		Vector<String> qv = query._tokens;
		
		n = Math.min(n, qv.size());

		// get n-gram terms for query vector
		qv = Utilities.getNGram(qv, n);

		// Get the document vector.
		Document d = _indexer.getDoc(did);

		// get n-gram terms
		Vector<String> docTokensTitle = ((DocumentFull) d).getConvertedTitleTokens();
		Vector<String> docTokensBody = ((DocumentFull) d).getConvertedBodyTokens();
		
		docTokensBody.addAll(docTokensTitle); // title is also a part of the document		
		
		Vector<String> dv = Utilities.getNGram(docTokensBody, n);
		
		// get n-gram term frequencies in the query
		HashMap<String, Double> termFreqQuery = Utilities.getTermFreq(qv);

		// get n-gram term frequencies in the document
		HashMap<String, Double> termFreqDoc = Utilities.getTermFreq(dv);

		// get unit vectors
		// HashMap<String, Double> termFreqQuery_UnitVec = Utilities
		// .getUnitVector(termFreqQuery);
		// HashMap<String, Double> termFreqDoc_UnitVec = Utilities
		// .getUnitVector(termFreqDoc);

		double score = Utilities.getDotProduct(termFreqQuery, termFreqDoc);

		return score;
	}

	public static void main(String[] args) {
		Vector<String> docvec = new Vector<String>();
		docvec.add("bing");
		docvec.add("web");
		docvec.add("search");
		docvec.add("live");
		docvec.add("search");

		docvec = Utilities.getNGram(docvec, 2);

		HashMap<String, Double> dv = Utilities.getTermFreq(docvec);
		System.out.print("Document vector : ");
		for (String k : dv.keySet()) {
			System.out.print(k + " : " + dv.get(k) + ", ");
		}
		System.out.println("");

		Vector<String> queryvec = new Vector<String>();
		queryvec.add("bing");
		// queryvec.add("insurance");

		HashMap<String, Double> qv = Utilities.getTermFreq(queryvec);
		System.out.print("Query vector : ");
		for (String k : qv.keySet()) {
			System.out.print(k + " : " + qv.get(k) + ", ");
		}
		System.out.println("");

		HashMap<String, Double> duv = Utilities.getNormalizedVector(dv, 2);
		System.out.print("Document unit vector : ");
		for (String k : duv.keySet()) {
			System.out.print(k + " : " + duv.get(k) + ", ");
		}
		System.out.println("");

		HashMap<String, Double> quv = Utilities.getNormalizedVector(qv, 2);
		System.out.print("Query unit vector : ");
		for (String k : quv.keySet()) {
			System.out.print(k + " : " + quv.get(k) + ", ");
		}
		System.out.println("");

		double dp = Utilities.getDotProduct(duv, quv);
		System.out.println("dp = " + dp);

		/*
		 * Vector<String> ngramvec = Utilities.getNGram(docvec, 3); for(int i=0
		 * ; i<ngramvec.size() ; i++) { System.out.println(ngramvec.get(i)); }
		 */
	}
}
