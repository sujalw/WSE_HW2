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
public class CosineRanker extends Ranker {

	private DocumentFull _docFull;
	private Options _options;
	
	public CosineRanker(Options options,
		      CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		_options = options;
	    System.out.println("Using Ranker: " + this.getClass().getSimpleName());	
	}
	
	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		
		// TODO : return only numResults no. of documents. Currently it returns all documents
		
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		double noOfDocs = _indexer.numDocs();
		for (int i = 0; i < noOfDocs; ++i) {
			retrieval_results.add(scoreDocument(query, _indexer.getDoc(i)));
		}

		// retrieval_results = Utilities.getSorted(retrieval_results);
		return retrieval_results;
	}

	public ScoredDocument scoreDocument(Query query, Document doc) {
		double score = getCosineScore(query, doc);
		return new ScoredDocument(doc, score);
	}

	public double getCosineScore(Query query, Document d) {

		// Build query vector
		query.processQuery();
		Vector<String> qv = query._tokens;
		
		
		// Get the document vector.
		// get unigram terms
		Vector<String> docTokensTitle = ((DocumentFull) d).getConvertedTitleTokens();
		Vector<String> docTokensBody = ((DocumentFull) d).getConvertedBodyTokens();
		
		docTokensBody.addAll(docTokensTitle); // title is also a part of the document
		
		Vector<String> dv = Utilities.getNGram(docTokensBody, 1);
		
		// get term frequencies in the document
		HashMap<String, Double> termFreqDoc = Utilities.getTermFreq(dv);
		HashMap<String, Double> tfDoc = Utilities.getNormalizedVector(
				termFreqDoc, 1d);
		IndexerFullScan ifs = new IndexerFullScan(_options);
		_docFull = new DocumentFull(d._docid, ifs);
		NormalizedTFIDF normTfIdf = new NormalizedTFIDF(_docFull, _indexer);
		HashMap<String, Double> idfDoc = normTfIdf.invDocFreqVector(tfDoc);
		HashMap<String, Double> tfIdfDoc = Utilities.getTfIdf(tfDoc, idfDoc);
		HashMap<String, Double> tfIdfDocNormalised = Utilities
				.getNormalizedVector(tfIdfDoc, 2);

		// System.out.println("proc doc");

		HashMap<String, Double> termFreqQuery = Utilities.getTermFreq(qv);
		HashMap<String, Double> tfQuery = Utilities.getNormalizedVector(
				termFreqQuery, 1d);
		HashMap<String, Double> idfQuery = normTfIdf.invDocFreqVector(tfQuery);
		HashMap<String, Double> tfIdfQuery = Utilities.getTfIdf(tfQuery,
				idfQuery);
		HashMap<String, Double> tfIdfQueryNormalised = Utilities
				.getNormalizedVector(tfIdfQuery, 2);

		double score = Utilities.getDotProduct(tfIdfDocNormalised,
				tfIdfQueryNormalised);

		return score;
	}

	public static void main(String[] args) {
		Vector<String> docvec = new Vector<String>();
		docvec.add("car");
		docvec.add("insurance");
		docvec.add("auto");
		docvec.add("insurance");

		/*
		 * HashMap<String, Integer> dv = Utilities.getTermFreq(docvec);
		 * System.out.print("Document vector : "); for (String k : dv.keySet())
		 * { System.out.print(k + " : " + dv.get(k) + ", "); }
		 * System.out.println("");
		 * 
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

		Vector<String> ngramvec = Utilities.getNGram(docvec, 3);
		for (int i = 0; i < ngramvec.size(); i++) {
			System.out.println(ngramvec.get(i));
		}
	}
}
