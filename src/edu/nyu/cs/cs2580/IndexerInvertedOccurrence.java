package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer {

	HashMap<String, HashMap<Integer, Vector<Integer>>> _invertedIndexOccurrences = new HashMap<String, HashMap<Integer, Vector<Integer>>>();
	
	// Stores all Document (not body vectors) in memory.
	public Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
	public Map<String, Integer> _docIdUriMap = new HashMap<String, Integer>();

	public IndexerInvertedOccurrence(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	@Override
	public void constructIndex() throws IOException {
	}

	@Override
	public void loadIndex() throws IOException, ClassNotFoundException {
	}

	@Override
	public DocumentIndexed getDoc(int docid) {
		return _documents.get(docid);
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}.
	 */
	@Override
	public DocumentIndexed nextDoc(Query query, int docid) {
		if (query == null || query._query.trim().length() == 0) {
			return null;
		}

		int[] docIds = new int[query._tokens.size()];

		// perform conjunctive retrieval on each token which can be a phrase
		int qTokenNo = 0;
		for (String qTerm : query._tokens) {
			docIds[qTokenNo++] = nextDoc(qTerm, docid);
		}

		// loop until docids for all the terms are same and is not -1
		while (!IndexerUtils.isSame(docIds)
				&& IndexerUtils.continueSearch(docIds)) {
			int newDocId = Utilities.getMax(docIds) - 1;
			docIds = new int[query._tokens.size()];

			qTokenNo = 0;
			for (String qTerm : query._tokens) {
				docIds[qTokenNo++] = nextDoc(qTerm, newDocId);
			}
		}

		// check if no docid is found
		if (!IndexerUtils.continueSearch(docIds)
				|| !IndexerUtils.isSame(docIds)) {
			return null;
		}

		// Return docid. At this point, all the entries in the array are same.
		return new DocumentIndexed(docIds[0]);
	}

	/**
	 * Searches for next document containing the term (which can be a phrase also)
	 * @param term
	 * @param docid
	 * @return
	 */
	private int nextDoc(String term, int docid) {

		if (term == null || term.trim().length() == 0) {
			return -1;
		}

		int nextDocId = -1;
		Integer[] docIdList = null;

		// check whether the term is a phrase
		if (term.contains(" ")) {
			// search for phrase

			String[] phraseTerms = term.split(" ");
			boolean isPresent = false;

			// get document containing all the words in term
			Query query = new Query(term);
			query.processQuery();
			
			// search for a document that contains all the phrase terms
			DocumentIndexed dIndexed = nextDoc(query, docid);			
			if (dIndexed == null) {
				return -1;
			} else {
				// check for phrase
				
				int[] occurrences = new int[phraseTerms.length];

				// perform occurrence retrieval
				int qTermNo = 0;
				int currentOccurrence = -1;
				
				// get occurrence of each phrase term
				for (String qTerm : phraseTerms) {

					Set<Integer> occurrencesListSorted = new TreeSet<Integer>(
							_invertedIndexOccurrences.get(qTerm).get(
									dIndexed._docid));
					Integer[] occurrencesList = new Integer[occurrencesListSorted
							.size()];
					occurrencesListSorted.toArray(occurrencesList);

					occurrences[qTermNo++] = IndexerUtils.search(
							currentOccurrence, occurrencesList, false);
				}

				// loop for all the occurrences of phrase
				while (IndexerUtils.continueSearch(occurrences)) {

					// if a phrase is found, then add its info to the index
					if (IndexerUtils.isPhrase(occurrences)) {

						isPresent = true;

						// create the entry for the phrase, if not created
						// earlier
						if (!_invertedIndexOccurrences.containsKey(term)) {
							_invertedIndexOccurrences.put(term,
									new HashMap<Integer, Vector<Integer>>());
						}

						// add phrase occurrences info to the index
						HashMap<Integer, Vector<Integer>> docInfo = _invertedIndexOccurrences
								.get(term);
						if (!docInfo.containsKey(dIndexed._docid)) {
							docInfo.put(dIndexed._docid, new Vector<Integer>());
						}
						docInfo.get(dIndexed._docid).add(occurrences[0]);
					}

					// update search parameters for searching for next occurrences of phrase terms 
					int minOccurrence = Utilities.getMin(occurrences);

					if (minOccurrence == occurrences[0]) {
						currentOccurrence = occurrences[0] + 1;
					} else {
						currentOccurrence = occurrences[0] - 1;
					}

					// get next occurrences of phrase terms
					qTermNo = 0;
					for (String qTerm : phraseTerms) {

						Set<Integer> occurrencesListSorted = new TreeSet<Integer>(
								_invertedIndexOccurrences.get(qTerm).get(
										dIndexed._docid));
						Integer[] occurrencesList = new Integer[occurrencesListSorted
								.size()];
						occurrencesListSorted.toArray(occurrencesList);

						occurrences[qTermNo++] = IndexerUtils.search(
								currentOccurrence, occurrencesList, false);
					}
				}
				
				if (!isPresent) {
					// if phrase is not present in current document, return next document containing it
					return nextDoc(term, dIndexed._docid);
				} else {
					// return current document as it contains the phrase
					return dIndexed._docid;
				}
			}
		} else {
			// search for single term
			if (_invertedIndexOccurrences.containsKey(term)) {
				Map<Integer, Vector<Integer>> docList = _invertedIndexOccurrences
						.get(term);
				docIdList = new Integer[docList.size()];

				Set<Integer> docListSorted = new TreeSet<Integer>(
						docList.keySet());
				docListSorted.toArray(docIdList);

				// perform search for nextdocid on this array
				nextDocId = IndexerUtils.search(docid, docIdList, true);
			}
		}

		return nextDocId;
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		return 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		return 0;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		if(_invertedIndexOccurrences.containsKey(term)) {
			int docId = _docIdUriMap.get(url);
			return _invertedIndexOccurrences.get(term).get(docId).size();
		}
		
		return 0;
	}

	public IndexerInvertedOccurrence() {
		try {
			Options options = new Options("conf/engine.conf");
			String testFile = options._corpusPrefix + "/phrase_test";

			IndexerInvertedOccurrence iio = new IndexerInvertedOccurrence(
					options);

			// populate inverted index
			BufferedReader br = new BufferedReader(new FileReader(new File(
					testFile)));
			String line = "";
			int docId = 0;

			while ((line = br.readLine()) != null) {
				String[] terms = line.split(" ");
				for (int i = 0; i < terms.length; i++) {
					String termStemmed = Utilities.getStemmed(terms[i]).get(0);
					if (iio._invertedIndexOccurrences.containsKey(termStemmed)) {
						HashMap<Integer, Vector<Integer>> docInfo = iio._invertedIndexOccurrences
								.get(termStemmed);
						if (!docInfo.containsKey(docId)) {
							docInfo.put(docId, new Vector<Integer>());
						}

						docInfo.get(docId).add(i); // add occurrence
					} else {
						iio._invertedIndexOccurrences.put(termStemmed,
								new HashMap<Integer, Vector<Integer>>());
						iio._invertedIndexOccurrences.get(termStemmed).put(docId,
								new Vector<Integer>());
						iio._invertedIndexOccurrences.get(termStemmed).get(docId)
								.add(i);
					}
				}

				docId++;
			}
			br.close();

			// /////////////////////////////////////////////////////////////////

			/*
			 * Vector<String> tokens = qPhrase._tokens; for(String t : tokens) {
			 * System.out.println(t); }
			 */

			//System.out.println("\nafter loading = \n");
			//printIndex(iio);
			
			//testNextDoc(iio);
			testRankerFavorite(iio);

			//System.out.println("\n\nafter search\n");
			//printIndex(iio);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void printIndex(IndexerInvertedOccurrence iio) {
		// print the info
		for (String term : iio._invertedIndexOccurrences.keySet()) {
			System.out.print(term + ";");
			HashMap<Integer, Vector<Integer>> docInfo = iio._invertedIndexOccurrences
					.get(term);
			for (Integer doc_id : docInfo.keySet()) {
				System.out.print(doc_id + ":");
				for (Integer occ : docInfo.get(doc_id)) {
					System.out.print(occ + ",");
				}

				System.out.print(" ");
			}

			System.out.println();
		}
	}
	
	private void testRankerFavorite(IndexerInvertedOccurrence iio) {
		RankerFavorite rf = new RankerFavorite(iio._options, null, iio);		
		QueryPhrase q = new QueryPhrase("the \"new york\" city");
		
		// populate _documents
		DocumentIndexed dIndexed = new DocumentIndexed(0);
		dIndexed.setUrl("doc1");
		dIndexed.setTitle("My Studies");
		dIndexed.setTotalWords(15);
		iio._totalTermFrequency += 15;
		iio._documents.add(dIndexed);								
		iio._docIdUriMap.put("doc1", 0);
		
		dIndexed = new DocumentIndexed(1);
		dIndexed.setUrl("doc2");
		dIndexed.setTitle("NYC");
		dIndexed.setTotalWords(19);
		iio._totalTermFrequency += 19;
		iio._documents.add(dIndexed);								
		iio._docIdUriMap.put("doc2", 1);
		
		Vector<ScoredDocument> sd = rf.runQuery(q, 5);
		for(ScoredDocument d : sd) {
			System.out.println(d.getDocId() + " : " + d.get_score());
		}
	}

	private void testNextDoc(IndexerInvertedOccurrence iio) {
		// String query = "the \"new york city\"";
		String query = "the \"new york city\"";
		QueryPhrase qPhrase = new QueryPhrase(query);
		qPhrase.processQuery();

		DocumentIndexed di = iio.nextDoc(qPhrase, -1);
		if (di == null) {
			System.out.println("No documents found !!!");
		}
		while (di != null) {
			System.out.println(di._docid);
			di = iio.nextDoc(qPhrase, di._docid);
		}
	}

	public static void main(String[] args) {
		new IndexerInvertedOccurrence();
	}
}
