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
		SearchEngine.Check(false, "Do NOT change, not used for this Indexer!");
		return null;
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}.
	 */
	@Override
	public DocumentIndexed nextDoc(Query query, int docid) {
		if (query == null || query._query.trim().length() == 0) {
			return null;
		}
		
		query.processQuery();
		
		int[] docIds = new int[query._tokens.size()];
		
		// perform conjunctive retrieval on each token which can be a phrase
		int qTokenNo = 0;
		for (String qTerm : query._tokens) {
			docIds[qTokenNo++] = nextDoc(qTerm, docid);
		}
		
		while (! IndexerUtils.isSame(docIds) && IndexerUtils.continueSearch(docIds)) {
			int newDocId = Utilities.getMax(docIds) - 1;
			docIds = new int[query._tokens.size()];

			qTokenNo = 0;
			for (String qTerm : query._tokens) {
				docIds[qTokenNo++] = nextDoc(qTerm, newDocId);
			}
		}
		
		if (! IndexerUtils.continueSearch(docIds) || ! IndexerUtils.isSame(docIds)) {
			return null;
		}

		// At this point, all the entries in the array are same
		//return new DocumentIndexed(docIds[0]);
		//return _documents.get(docIds[0]);
		return new DocumentIndexed(docIds[0]);
	}
	
	private int nextDoc(String term, int docid) {

		if (term == null || term.trim().length() == 0) {
			return -1;
		}

		int nextDocId = -1;
		Integer[] docIdList = null;

		if(term.contains(" ")) {
			// search for phrase
			
			// get document containing all the words in term
			DocumentIndexed dIndexed = nextDoc(new Query(term), docid);
			if(dIndexed == null) {
				return -1;
			} else {
				// check for presence of phrase
				
				String[] phraseTerms = term.split(" ");
				int[] occurrences = new int[phraseTerms.length];
				
				// perform occurrence retrieval
				int qTermNo = 0;
				int currentOccurrence = -1;
				for (String qTerm : phraseTerms) {
					
					Set<Integer> occurrencesListSorted = new TreeSet<Integer>(_invertedIndexOccurrences.get(qTerm).get(dIndexed._docid));
					Integer[] occurrencesList = new Integer[occurrencesListSorted.size()];
					occurrencesListSorted.toArray(occurrencesList);
					
					occurrences[qTermNo++] = IndexerUtils.search(currentOccurrence, occurrencesList, false);
				}
				
				while (! IndexerUtils.isPhrase(occurrences) && IndexerUtils.continueSearch(occurrences)) {
					int minOccurrence = Utilities.getMin(occurrences);
					
					if(minOccurrence == occurrences[0]) {
						currentOccurrence = occurrences[0] + 1;
					} else {
						currentOccurrence = occurrences[0] - 1;
					}
					
					qTermNo = 0;
					for (String qTerm : phraseTerms) {
						
						Set<Integer> occurrencesListSorted = new TreeSet<Integer>(_invertedIndexOccurrences.get(qTerm).get(dIndexed._docid));
						Integer[] occurrencesList = new Integer[occurrencesListSorted.size()];
						occurrencesListSorted.toArray(occurrencesList);
						
						occurrences[qTermNo++] = IndexerUtils.search(currentOccurrence, occurrencesList, false);
					}					
				}
				
				if (! IndexerUtils.continueSearch(occurrences) || ! IndexerUtils.isPhrase(occurrences)) {
					return nextDoc(term, dIndexed._docid);
				} else {
					return dIndexed._docid;
				}				
			}
		} else {
			// search for single term
			if (_invertedIndexOccurrences.containsKey(term)) {
				Map<Integer, Vector<Integer>> docList = _invertedIndexOccurrences.get(term);
				docIdList = new Integer[docList.size()];
				
				Set<Integer> docListSorted = new TreeSet<Integer>(docList.keySet());
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
		SearchEngine.Check(false, "Not implemented!");
		return 0;
	}

	public IndexerInvertedOccurrence() {
		try {
			Options options = new Options("conf/engine.conf");
			String testFile = options._corpusPrefix + "/phrase_test";
			
			IndexerInvertedOccurrence iio = new IndexerInvertedOccurrence(options);

			// populate inverted index
			BufferedReader br = new BufferedReader(new FileReader(new File(
					testFile)));
			String line = "";
			int docId = 0;

			while ((line = br.readLine()) != null) {
				String[] terms = line.split(" ");
				for (int i = 0; i < terms.length; i++) {
					if (iio._invertedIndexOccurrences.containsKey(terms[i])) {
						HashMap<Integer, Vector<Integer>> docInfo = iio._invertedIndexOccurrences
								.get(terms[i]);
						if (!docInfo.containsKey(docId)) {
							docInfo.put(docId, new Vector<Integer>());
						}

						docInfo.get(docId).add(i); // add occurrence
					} else {
						iio._invertedIndexOccurrences.put(terms[i],
								new HashMap<Integer, Vector<Integer>>());
						iio._invertedIndexOccurrences.get(terms[i]).put(docId,
								new Vector<Integer>());
						iio._invertedIndexOccurrences.get(terms[i]).get(docId)
								.add(i);
					}
				}

				docId++;
			}
			br.close();

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

			// /////////////////////////////////////////////////////////////////

			/*
			 * Vector<String> tokens = qPhrase._tokens; for(String t : tokens) {
			 * System.out.println(t); }
			 */
			
			testNextDoc(iio);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void testNextDoc(IndexerInvertedOccurrence iio) {
		//String query = "the \"new york city\"";
		String query = "the \"it is in\"";
		QueryPhrase qPhrase = new QueryPhrase(query);
		//qPhrase.processQuery();

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
