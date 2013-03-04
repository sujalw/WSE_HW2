package edu.nyu.cs.cs2580;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Utilities {

	/**
	 * @author sujal
	 * @param v
	 * @return
	 */
	public static HashMap<String, Double> getTermFreq(Vector<String> v) {
		HashMap<String, Double> termFreq = new HashMap<String, Double>();

		for (String term : v) {
			Double freq = termFreq.get(term);
			if (freq == null) {
				termFreq.put(term, 1d);
			} else {
				termFreq.put(term, freq + 1d);
			}
		}

		return termFreq;
	}

	/**
	 * @author sujal
	 * @param vec1
	 * @param vec2
	 * @return dot product of given vectors. Returns -1 if either of the given
	 *         vectors is null
	 */
	public static double getDotProduct(HashMap<String, Double> vec1,
			HashMap<String, Double> vec2) {

		double dotProduct = 0;
		HashMap<String, Double> v1 = null, v2 = null;

		if (vec1 == null || vec2 == null) {
			System.out.println("Error: One of the vectors is null");
			return -1;
		}

		// v1 holds the smaller vector and v2 holds the larger one
		if (vec1.keySet().size() < vec2.keySet().size()) {
			v1 = vec1;
			v2 = vec2;
		} else {
			v1 = vec2;
			v2 = vec1;
		}

		for (String k : v1.keySet()) {
			Double tmp = v2.get(k);
			if (tmp == null) {
				tmp = 0.0;
			}

			dotProduct += v1.get(k) * tmp;
		}

		return dotProduct;
	}

	/**
	 * @author sujal
	 * @param vec1
	 * @param vec2
	 * @return dot product of given vectors. Returns -1 if either of the given
	 *         vectors is null
	 */
	public static double getDotProductInteger(HashMap<String, Integer> vec1,
			HashMap<String, Integer> vec2) {

		double dotProduct = 0;
		HashMap<String, Integer> v1 = null, v2 = null;

		if (vec1 == null || vec2 == null) {
			System.out.println("Error: One of the vectors is null");
			return -1;
		}

		// v1 holds the smaller vector and v2 holds the larger one
		if (vec1.keySet().size() < vec2.keySet().size()) {
			v1 = vec1;
			v2 = vec2;
		} else {
			v1 = vec2;
			v2 = vec1;
		}

		for (String k : v1.keySet()) {
			Integer tmp = v2.get(k);
			if (tmp == null) {
				tmp = 0;
			}

			dotProduct += v1.get(k) * tmp;
		}

		return dotProduct;
	}

	/**
	 * @author sujal
	 * @param vec
	 * @return null if given vector is null. Else returns the unit vector of a
	 *         given vector.
	 */
	public static HashMap<String, Double> getNormalizedVector(
			HashMap<String, Double> vec, double norm) {

		if (vec == null) {
			return null;
		}

		HashMap<String, Double> unitVec = new HashMap<String, Double>();
		double vecNorm = getVectorNorm(vec, norm); // get 2-norm of the vector

		for (String k : vec.keySet()) {
			unitVec.put(k, vec.get(k) / vecNorm);
		}

		return unitVec;
	}

	/**
	 * @author sujal
	 * @param vec
	 * @param p
	 *            required norm of a vector. p <> 0 (does not handle for p ==
	 *            infinity)
	 * @return p-norm of the given vector
	 */
	public static double getVectorNorm(HashMap<String, Double> vec, Double p) {
		double norm = 0;

		if (p == 0 || vec == null) {
			return -1;
		}

		for (String k : vec.keySet()) {
			norm += Math.pow(vec.get(k), p);
		}
		norm = Math.pow(norm, 1d / p);

		return norm;
	}
	
	/**
	 * @author sujal
	 * @param fileName
	 * @param contents
	 * @param append
	 */
	public static void writeToFile(String fileName, String contents,
			boolean append) {
		try {
			FileWriter fstream = new FileWriter(fileName, append);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(contents);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @author sujal
	 * @param sdv
	 *            scored document vector
	 * @param query_map
	 */
	public static String generateOutput(Vector<ScoredDocument> sdv,
			Map<String, String> query_map, String sessionId) {

		String queryResponse = "";
		Output output = new Output(sdv, query_map);
		queryResponse = output.generateTextOutput(sessionId);

		return queryResponse;
	}

	/**
	 * @author sujal
	 * @param vec
	 * @param n
	 * @return vector with n-gram terms from given vector vec
	 */
	public static Vector<String> getNGram(Vector<String> vec, int n) {
		Vector<String> ngramVector = new Vector<String>();
		int vecLen = vec.size();

		if (n <= 0) {
			return null;
		}

		if (n > vecLen) {
			String phrase = vec.get(0);
			for (int i = 1; i < vecLen; i++) {
				phrase += " " + vec.get(i);
			}
			ngramVector.add(phrase);
		} else {
			for (int i = 0; i < vec.size() - n + 1; i++) {
				String phrase = "";

				phrase = vec.get(i);
				for (int j = 1; j < n; j++) {
					phrase += " " + vec.get(i + j);
				}
				ngramVector.add(phrase);
			}
		}

		return ngramVector;
	}

	/**
	 * @author sujal
	 * @param recallVec
	 * @param precisionVec
	 * @param recall
	 * @return precision at given recall
	 */
	public static double getPrecisionAtRecall(Vector<Double> recallVec,
			Vector<Double> precisionVec, double recall) {
		double precisionAtRecall = 0d;

		if (recallVec == null || precisionVec == null || recall < 0
				|| recall > 1) {
			return 0;
		}

		if (recallVec.size() != precisionVec.size()) {
			return 0;
		}

		// also need to check for valid values in recallVec and precisionVec

		int vecSize = precisionVec.size();
		Vector<Double> steppedPrecision = new Vector<Double>();
		Vector<Double> tmpPrecision = new Vector<Double>();

		// pre-process the precision array
		int i = 0;
		while (i < vecSize) {

			double currPrecision = precisionVec.get(i);
			double currRecall = recallVec.get(i);

			while (i < vecSize && recallVec.get(i) == currRecall) {
				tmpPrecision.add(currPrecision);
				i++;
			}
		}

		// process precision vector and convert it into a step functioned vector
		double maxPrecision = 0;
		for (i = vecSize - 1; i >= 0; i--) {
			if (tmpPrecision.get(i) > maxPrecision) {
				maxPrecision = tmpPrecision.get(i);
			}

			steppedPrecision.add(0, maxPrecision);
		}

		// search for precision at given recall
		for (i = 0; i < vecSize; i++) {
			if (recall <= recallVec.get(i)) {
				precisionAtRecall = steppedPrecision.get(i);
				break;
			}
		}

		return precisionAtRecall;
	}

	/**
	 * @author Amey
	 * @param sds
	 * @return
	 */
	public static Vector<ScoredDocument> sortScoredDocumentAsPer(
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

	public static HashMap<String, Double> getTfIdf(
			HashMap<String, Double> vec1, HashMap<String, Double> vec2) {

		HashMap<String, Double> tfIdf = new HashMap<String, Double>();

		for (String term : vec1.keySet()) {
			tfIdf.put(term, vec1.get(term) * vec2.get(term));
		}

		return tfIdf;
	}
}
