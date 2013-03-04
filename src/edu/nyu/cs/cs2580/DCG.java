/**
 * 
 */
package edu.nyu.cs.cs2580;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import edu.nyu.cs.cs2580.Evaluator.DocumentRelevances;

/**
 * @author amey
 * 
 */

public class DCG {
	
	Map<String, DocumentRelevances> judgments = new HashMap<String, DocumentRelevances>();
	Vector<Vector<String>> data = new Vector<Vector<String>>();

	public DCG(Vector<Vector<String>> data,
			Map<String, DocumentRelevances> judgments) {

		this.judgments = judgments;
		this.data = data;
	}

	public double computeDCG(String query, int point) {
		double dcg = 1.0;
		// HashMap<Integer, Double> qr = judgments.get(query);

		// System.out.println("qr.size " + qr.size());
		// for (Map.Entry<Integer, Double> entry : qr.entrySet()){
		// System.out.println("Key = " + entry.getKey() + "," +
		// " Value = " + entry.getValue());
		// }

		if (point == 1) {
			// idealDcg = qrSorted.get(0);
			for (Vector<String> queryDid : data) {
				if (judgments.containsKey(queryDid.get(0))) {
					dcg = judgments.get(queryDid.get(0))
							.getScoredRelevanceForDoc(
									Integer.parseInt(queryDid.get(1)));
				}
				break;
			}

		} else {
			for (Vector<String> queryDid : data) {
				if (judgments.containsKey(queryDid.get(0))) {
					dcg = judgments.get(queryDid.get(0))
							.getScoredRelevanceForDoc(
									Integer.parseInt(queryDid.get(1)));
				}
				break;
			}

			int count = 0;
			for (Vector<String> queryDid : data) {
				count++;
				if (count == 1) {
					continue;
				} else if (count > point) {
					break;
				} else {
					if (judgments.containsKey(queryDid.get(0))) {
						dcg += judgments.get(queryDid.get(0))
								.getScoredRelevanceForDoc(
										Integer.parseInt(queryDid.get(1)))
								/ Math.log(count) / Math.log(2d);
					} else {
						dcg += (1.0d / Math.log(count)) / Math.log(2d);

					}
				}
			}
		}

		// System.out.println("dcg = " + dcg);
		return dcg;
	}

	public double computeIdealDCG(String query, int point) {
		double idealDcg = 0.0;
		// HashMap<Integer, Double> qr = scored_judgments.get(query);
		Map<Integer, Double> scoredRelevances = DocumentRelevances
				.sortByComparator();
		// for (Map.Entry<Integer, Double> entry : qrSorted.entrySet()){
		// System.out.println("Key = " + entry.getKey() + "," +
		// " Value = " + entry.getValue());
		// }
		if (point == 1) {
			for (Map.Entry<Integer, Double> entry : scoredRelevances.entrySet()) {
				idealDcg = entry.getValue();
				break;
			}
		} else {
			for (Map.Entry<Integer, Double> entry : scoredRelevances.entrySet()) {
				idealDcg = entry.getValue();
				break;
			}
			int count = 0;

			for (Map.Entry<Integer, Double> entry : scoredRelevances.entrySet()) {
				count++;
				if (count == 1) {
					continue;
				} else if (count > point) {
					break;
				} else {
					idealDcg += (entry.getValue() / Math.log(count))
							/ Math.log(2d);
				}
			}
		}

		// System.out.println("idealDcg = " + idealDcg);
		return idealDcg;
	}

	public double computeNDCG(String query, int point) {
		return computeDCG(query, point) / computeIdealDCG(query, point);
	}

	public double computeReciprocalRank(String query) {
		double reciprocalRank = 0.0;
		// HashMap<Integer, Double> qr = relevance_judgments.get(query);
		int count = 0;
		/*
		 * for (Map.Entry<Integer, Double> entry : qr.entrySet()){
		 * System.out.println("Key = " + entry.getKey() + "," + " Value = " +
		 * entry.getValue()); }
		 */
		for (Vector<String> queryDid : data) {
			count++;
			if (judgments.containsKey(queryDid.get(0))) {
				if (judgments.get(queryDid.get(0)).getScoredRelevanceForDoc(
						Integer.parseInt(queryDid.get(1))) > 0.0) {
					reciprocalRank = 1.0 / (double) count;
					break;
				}
			}
		}
		return reciprocalRank;
	}

}
