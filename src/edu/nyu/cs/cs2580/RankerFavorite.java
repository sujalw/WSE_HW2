package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 *          Ranker (except RankerPhrase) from HW1. The new Ranker should no
 *          longer rely on the instructors' {@link IndexerFullScan}, instead it
 *          should use one of your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

	public RankerFavorite(Options options, CgiArguments arguments,
			Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		Vector<DocumentIndexed> all = new Vector<DocumentIndexed>();
		Vector<ScoredDocument> scoredDocs = new Vector<ScoredDocument>();

		DocumentIndexed di = _indexer.nextDoc(query, -1);
		while (di != null) {
			all.add(di);
			di = _indexer.nextDoc(query, di._docid);
		}
		
		

		return null;
	}
	
	public ScoredDocument scoreDocument(Query query, DocumentIndexed doc) {
		double score = getCosineScore(query, doc);
		return new ScoredDocument(doc, score);
	}
	
	public double getCosineScore(Query query, DocumentIndexed d) {
		
		double score = 0;

		// Build query vector
		query.processQuery();
		Vector<String> qv = Utilities.getStemmed(query._query);
		
		// get query term frequencies in the document
		HashMap<String, Double> termFreqDoc = new HashMap<String, Double>();
		for(String term : qv) {
			termFreqDoc.put(term, new Double(_indexer.documentTermFrequency(term, null)));
		}
		HashMap<String, Double> tfDoc = Utilities.getNormalizedVector(
				termFreqDoc, 1d);
		
		
		/*
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
				tfIdfQueryNormalised);*/

		return score;
	}
}
