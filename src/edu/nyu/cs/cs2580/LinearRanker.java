package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * 
 * @author sujal
 * 
 */
public class LinearRanker extends Ranker {
	
	public LinearRanker(Options options,
		      CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		_options = options;
	    System.out.println("Using Ranker: " + this.getClass().getSimpleName());	
	}
	
	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		return runquery(query, 0.3, 0.3, 0.3, 0.1);
	}

	/**
	 * 
	 * @param query
	 * @param wCosine
	 *            Weight of cosine score
	 * @param wLMP
	 *            Weight of Language model probabilities (Jenilek) score
	 * @param wPhrase
	 *            Weight of phrase model score
	 * @param wNumViews
	 *            Weight of numviews score
	 * @return
	 */
	public Vector<ScoredDocument> runquery(Query query, double wCosine,
			double wLMP, double wPhrase, double wNumViews) {

		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		double noOfDocs = _indexer.numDocs();
		for (int i = 0; i < noOfDocs; ++i) {
			retrieval_results.add(scoreDocument(query, _indexer.getDoc(i), wCosine, wLMP, wPhrase,
					wNumViews));

		}

		//retrieval_results = Utilities.getSorted(retrieval_results);
		retrieval_results = Utilities.sortScoredDocumentAsPer(retrieval_results);
		return retrieval_results;
	}

	public ScoredDocument scoreDocument(Query query, Document d, double wCosine,
			double wLMP, double wPhrase, double wNumViews) {
		// Build query vector
		query.processQuery();
		Vector<String> qv = query._tokens;
		
		// Get the document vector.
		// Get the document vector.
				Vector<String> docTokensTitle = ((DocumentFull) d).getConvertedTitleTokens();
				Vector<String> docTokensBody = ((DocumentFull) d).getConvertedBodyTokens();
				
				docTokensBody.addAll(docTokensTitle); // title is also a part of the document
				
		double score = getLinearScore(query, d, wCosine, wLMP, wPhrase,
				wNumViews);
		return new ScoredDocument(d, score);
	}

	public double getLinearScore(Query query, Document d, double wCosine,
			double wLMP, double wPhrase, double wNumViews) {
		double cosineScore = new CosineRanker(_options, _arguments, _indexer).getCosineScore(query,
				d);
		double phraseScore = new PhraseRanker(_options, _arguments, _indexer).getPhraseScore(query,
				d, 2);
		double lmpScore = new QueryLikelihoodRankerwithJMSmoothing(_options, _arguments, _indexer).getLMPScore(query, d);
		double numViewsScore = new NumViewsRanker(_options, _arguments, _indexer).getNumViewsScore(d);

		double score = wCosine * cosineScore + wLMP * lmpScore + wPhrase
				* phraseScore + wNumViews * numViewsScore;

		return score;
	}

	public static void main(String[] args) {
		Vector<String> docvec = new Vector<String>();
		docvec.add("car");
		docvec.add("insurance");
		docvec.add("auto");
		docvec.add("car");
		docvec.add("insurance");

		docvec = Utilities.getNGram(docvec, 2);

		HashMap<String, Double> dv = Utilities.getTermFreq(docvec);
		System.out.print("Document vector : ");
		for (String k : dv.keySet()) {
			System.out.print(k + " : " + dv.get(k) + ", ");
		}
		System.out.println("");
		/*
		 * Vector<String> queryvec = new Vector<String>(); queryvec.add("auto");
		 * queryvec.add("insurance");
		 * 
		 * HashMap<String, Integer> qv = Utilities.getTermFreq(queryvec);
		 * System.out.print("Query vector : "); for (String k : qv.keySet()) {
		 * System.out.print(k + " : " + qv.get(k) + ", "); }
		 * System.out.println("");
		 * 
		 * HashMap<String, Double> duv = Utilities.getUnitVector(dv);
		 * System.out.print("Document unit vector : "); for (String k :
		 * duv.keySet()) { System.out.print(k + " : " + duv.get(k) + ", "); }
		 * System.out.println("");
		 * 
		 * HashMap<String, Double> quv = Utilities.getUnitVector(qv);
		 * System.out.print("Query unit vector : "); for (String k :
		 * quv.keySet()) { System.out.print(k + " : " + quv.get(k) + ", "); }
		 * System.out.println("");
		 * 
		 * double dp = Utilities.getDotProduct(duv, quv);
		 * System.out.println("dp = " + dp);
		 */

		/*
		 * Vector<String> ngramvec = Utilities.getNGram(docvec, 3); for(int i=0
		 * ; i<ngramvec.size() ; i++) { System.out.println(ngramvec.get(i)); }
		 */
	}

}
