package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

public class NumViewsRanker extends Ranker {

	private static Indexer _indexer;

	public NumViewsRanker(Options options,
		      CgiArguments arguments, Indexer indexer) {
		super(options, arguments, indexer);
		_options = options;
		_indexer = indexer;
	    System.out.println("Using Ranker: " + this.getClass().getSimpleName());		
	}


	@Override
	public Vector<ScoredDocument> runQuery(Query query, int numResults) {
		// TODO : return only numResults no. of documents. Currently it returns all documents
		System.out.println("inside runQuery of numviews");
		return createNumViewsReverseSorted();
	}
	
	public static HashMap<Integer, Integer> createDiDViewMap() {
		int numberofDocs = _indexer._numDocs;
		System.out.println("numdocs = " + numberofDocs);
		HashMap<Integer, Integer> didView = new HashMap<Integer, Integer>();
		for (int i = 0; i < numberofDocs; i++) {
			didView.put(i, _indexer.getDoc(i).getNumViews());
		}
		return didView;
	}

	public Vector<ScoredDocument> createNumViewsReverseSorted() {
		HashMap<Integer, Integer> didView = createDiDViewMap();
		Utility u = new Utility();
		HashMap<Integer, Integer> sortedDidViewMap = u
				.sortByComparator(didView);

		Vector<ScoredDocument> vsd = new Vector<ScoredDocument>();

		Iterator<Map.Entry<Integer, Integer>> it = sortedDidViewMap.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Integer> pairs = (Map.Entry<Integer, Integer>) it
					.next();
			int did = pairs.getKey();
			Document doc = _indexer.getDoc(did);
			String titleStr = doc.getTitle();
			int score = pairs.getValue();

			vsd.add(new ScoredDocument(doc, score));
		}

		return vsd;
	}
	
	public double getNumViewsScore(Document d) {
		int did = d._docid;
		
		return createDiDViewMap().get(did);
	}

}
