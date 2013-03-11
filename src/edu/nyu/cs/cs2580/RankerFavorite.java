package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 *          Ranker (except RankerPhrase) from HW1. The new Ranker should no
 *          longer rely on the instructors' {@link IndexerFullScan}, instead it
 *          should use one of your more efficient implementations.
 *          
 *          This class implements QL Ranker
 */
public class RankerFavorite extends Ranker {
	
	private double lambda = 0.0;

	public RankerFavorite(Options options, CgiArguments arguments,
			Indexer indexer) {
		super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
	}
	
	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		Vector<ScoredDocument> retrieval_results = new Vector<ScoredDocument>();
		
		DocumentIndexed di = (DocumentIndexed)_indexer.nextDoc(query, -1);
		while (di != null) {
			System.out.println("docid = " + di._docid);
			retrieval_results.add(scoreDocument(query, di));
			di = (DocumentIndexed)_indexer.nextDoc(query, di._docid);
		}
		
		// sort results
		// TODO: sort only top numResults elements
		retrieval_results = sortScoredDocument(retrieval_results);		
		//sort(retrieval_results, numResults, true);

		Vector<ScoredDocument> sortedResults = new Vector<ScoredDocument>();
		
		System.out.println("numResults = " + numResults);
		System.out.println("vec size = " + retrieval_results.size());
		
		if(numResults < retrieval_results.size()) {
			sortedResults.addAll(retrieval_results.subList(0, numResults-1));
		} else {
			sortedResults.addAll(retrieval_results);
		}
		
		return sortedResults;
	}
			
	public Vector<ScoredDocument> sortScoredDocument(
			Vector<ScoredDocument> sds) {
		if (sds.size() > 0) {
			Collections.sort(sds, new Comparator<ScoredDocument>() {
				@Override
				public int compare(final ScoredDocument obj1,
						final ScoredDocument obj2) {
					return obj2.compareTo(obj1);
				}
			});
		}
		return sds;
	}
	
	/**
	 * Sorts top numResults elements in given vector
	 * @param retrieval_results
	 * @param numResults
	 */
	private static void sort(Vector<ScoredDocument> vec, int numResults, boolean desc) {
		ScoredDocument[] array = new ScoredDocument[vec.size()];
		for(int i=0 ; i<vec.size() ; i++) {
			array[i] = vec.get(i);
		}
		quickSort(array, numResults, desc);
		
		//vec = new Vector<ScoredDocument>();
		for(int i=0 ; i<array.length ; i++) {
			vec.add(i, array[i]);
			vec.remove(i+1);
		}
	}

	private static void quickSort(ScoredDocument[] array, int numResults, boolean desc) {
		quickSort(array, 0, array.length-1, numResults, desc);
	}

	private static void quickSort(ScoredDocument[] A, int p, int r,
			int numResults, boolean desc) {
		if(p >= r) {
			return;
		} else {
			int q = partition(A, p, r, desc);
			
			quickSort(A, p, q-1, numResults, desc);
			
			// sort right half only when required
			//if(q < numResults) {
				quickSort(A, q+1, r, numResults, desc);				
			//}
		}
	}

	private static int partition(ScoredDocument[] A, int p, int r, boolean desc) {
		int i = 0, j = 0;
		
		if(p == r) {
			return p;
		} else {
			double pivot = A[0].get_score();
			i = p + 1;
			j = r;
			
			while(i <= j) {
				if(! desc) {
					// ascending order
					while((i <= j) && A[i].get_score() <= pivot) i++;
					while((i <= j) && A[j].get_score() > pivot) j--;
				} else {
					// descending order
					while((i <= j) && A[i].get_score() > pivot) i++;
					while((i <= j) && A[j].get_score() <= pivot) j--;
					
					// swap
					if(i < j) {
						swap(A, i, j);
					}
				}
			}
			
			swap(A, p, j);
		}
		
		return j;
	}

	private static void swap(ScoredDocument[] a, int i, int j) {
		ScoredDocument tmp = a[i];
		a[i] = a[j];
		a[j] = tmp;
	}

	public ScoredDocument scoreDocument(Query query, DocumentIndexed doc) {
		double score = getLMPScore(query, doc);
		return new ScoredDocument(doc, score);
	}
	
	public double getLMPScore(Query query, Document d) {
		return runquery(query, d._docid).get_score();
	}
	
	public double getQueryLikelihood(String term, int docid) {
		
		DocumentIndexed dIndexed;
		int termFreqInDoc = 0;
		long totalWordsInDoc = 0;
		
		dIndexed = (DocumentIndexed)_indexer.getDoc(docid);
		termFreqInDoc = _indexer.documentTermFrequency(term, dIndexed.getUrl());
		totalWordsInDoc = dIndexed.getTotalWords();
		
		double ql = 0d;
		
		if(totalWordsInDoc > 0) {
			ql = termFreqInDoc * 1.0d / totalWordsInDoc;
		}
		//return (getFrequid(term, did) / getTotalNumberOfWordsInaDocument(did));
		
		return ql;
	}
	
	public ScoredDocument runquery(Query query, int docid) {

		// Build query vector
		Vector<String> qv = Utilities.getStemmed(query._query);
		
		DocumentIndexed dIndexed = null;
		
		dIndexed = (DocumentIndexed)_indexer.getDoc(docid);
		
		double score = 0.0;
		for (int i = 0; i < qv.size(); ++i) {

			score += Math
					.log((1 - lambda)
							* (getQueryLikelihood(qv.get(i), docid))
							+ (lambda)
							* (_indexer.corpusTermFrequency(qv.get(i)) / _indexer
									.totalTermFrequency()));
		}

		// antilog
		//score = Math.pow(Math.E, score);

		return new ScoredDocument(dIndexed, score);
	}
		
	public static void main(String[] args) {
		Vector<ScoredDocument> v = new Vector<ScoredDocument>();
		
		for(int i=0 ; i<10 ; i++) {
			v.add(new ScoredDocument(null, i));
		}
		
		System.out.println("before = ");
		for(ScoredDocument sd : v) {
			System.out.println(sd.get_score());
		}
		
		sort(v, 4, true);
		
		System.out.println("\n\nafter = ");
		for(ScoredDocument sd : v) {
			System.out.println(sd.get_score());
		}
	}
}
