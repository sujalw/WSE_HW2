package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Vector;

/**
 * Evaluator for HW1.
 * 
 * @author fdiaz
 * @author congyu
 */
class Evaluator {

	static Vector<Vector<String>> data = new Vector<Vector<String>>();
	static String query;

	public static class DocumentRelevances {
		private static Map<Integer, Double> relevances = new HashMap<Integer, Double>();
		private static Map<Integer, Double> scoredRelevances = new HashMap<Integer, Double>();
		
		public static Map<Integer, Double> getRelevances() {
			return relevances;
		}

		public DocumentRelevances() {
		}
		
		public static Map<Integer, Double> sortByComparator() {

			List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(scoredRelevances.entrySet());

			// sort list based on comparator
			Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
				@Override
				public int compare(Map.Entry<Integer, Double> o1,
						Map.Entry<Integer, Double> o2) {
					return (o2.getValue()).compareTo(o1.getValue());
				}
			});

			Map<Integer, Double> result = new LinkedHashMap<Integer, Double>();
			for (Map.Entry<Integer, Double> entry : list) {
				result.put(entry.getKey(), entry.getValue());
			}
			return result;
		}

		public void addDocument(int docid, String grade) {
			relevances.put(docid, convertToBinaryRelevance(grade));
		}
		
		public void addScoredDocument(int docid, String grade) {
			scoredRelevances.put(docid, convertToScoredRelevance(grade));
		}

		public boolean hasRelevanceForDoc(int docid) {
			return relevances.containsKey(docid);
		}
		
		public double getScoredRelevanceForDoc(int docid) {
			if(scoredRelevances.containsKey(docid)) {
				return scoredRelevances.get(docid);
			} else {
				return 1.0;
			}
		}

		public double getBinaryRelevanceForDoc(int docid) {
			if(relevances.containsKey(docid)) {
				return relevances.get(docid);
			} else {
				return 0.0;
			}
		}

		private static double convertToBinaryRelevance(String grade) {
			if (grade.equalsIgnoreCase("Perfect")
					|| grade.equalsIgnoreCase("Excellent")
					|| grade.equalsIgnoreCase("Good")) {
				return 1.0;
			}
			return 0.0;
		}
		
		private static double convertToScoredRelevance(String grade) {
			double relScore = 1d;
			
			if (grade.equals("Perfect")) {
				relScore = 5.0;
			} else if (grade.equals("Excellent")) {
				relScore = 4.0;
			} else if (grade.equals("Good")) {
				relScore = 3.0;
			} else if (grade.equals("Fair")) {
				relScore = 2.0;
			} else if (grade.equals("Bad")) {
				relScore = 1.0;
			}
			
			return relScore;
		}
	}

	/**
	 * Usage: java -cp src edu.nyu.cs.cs2580.Evaluator [judge_file]
	 */
	public static void main(String[] args) throws IOException {
		HashMap<String, DocumentRelevances> judgements = new HashMap<String, DocumentRelevances>();
		
		SearchEngine.Check(args.length == 1, "Must provide judgements!");
		readRelevanceJudgments(args[0], judgements);
		evaluateStdin(judgements);
		
		DCG dcg = new DCG(data, judgements);

		String evaluatorOutput = "";

		evaluatorOutput += query + "\t";

		evaluatorOutput += evaluatePrecision(judgements, data, 1)
				+ "\t" + evaluatePrecision(judgements, data, 5) + "\t"
				+ evaluatePrecision(judgements, data, 10);

		evaluatorOutput += "\t" + evaluateRecall(judgements, data, 1)
				+ "\t" + evaluateRecall(judgements, data, 5) + "\t"
				+ evaluateRecall(judgements, data, 10);

		evaluatorOutput += "\t" + f1Score(judgements, data, 1) + "\t"
				+ f1Score(judgements, data, 5) + "\t"
				+ f1Score(judgements, data, 10);

		Evaluator.computeRecallPrecision(judgements, data);

		Vector<Double> recalVec = Evaluator.getRecallValues();
		Vector<Double> precisionVec = Evaluator.getPrecisionValues();

		double[] recallPoints = new double[] { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5,
				0.6, 0.7, 0.8, 0.9, 1.0 };
		for (int i = 0; i < recallPoints.length; i++) {
			evaluatorOutput += "\t"
					+ Utilities.getPrecisionAtRecall(recalVec, precisionVec,
							recallPoints[i]);
		}

		evaluatorOutput += "\t" + averagePrecision(judgements, data);

		evaluatorOutput += "\t" + dcg.computeNDCG(query, 1) + "\t"
				+ dcg.computeNDCG(query, 5) + "\t" + dcg.computeNDCG(query, 10);
		evaluatorOutput += "\t" + dcg.computeReciprocalRank(query);

		System.out.println(evaluatorOutput);
	}

	public static void readRelevanceJudgments(String judgeFile,
			Map<String, DocumentRelevances> judgements) throws IOException {
		
		String line = null;
		BufferedReader reader = new BufferedReader(new FileReader(judgeFile));
		while ((line = reader.readLine()) != null) {
			// Line format: query \t docid \t grade
			Scanner s = new Scanner(line).useDelimiter("\t");
			String query = s.next();
			DocumentRelevances relevances = judgements.get(query);
			if (relevances == null) {
				relevances = new DocumentRelevances();
				judgements.put(query, relevances);
			}
						
			int docid = Integer.parseInt(s.next());
			String grade = s.next();
			
			relevances.addDocument(docid, grade);
			relevances.addScoredDocument(docid, grade);
			
			s.close();
		}
		reader.close();
	}

	public static void evaluateStdin(Map<String, DocumentRelevances> judgments)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		double RR = 0.0;
		double N = 0.0;
		String line = null;
		while ((line = reader.readLine()) != null) {
			Vector<String> row = new Vector<String>();
			Scanner s = new Scanner(line).useDelimiter("\t");
			query = s.next();
			int docid = Integer.parseInt(s.next());

			row.add(query);
			row.add(String.valueOf(docid));
			data.add(row);

			DocumentRelevances relevances = judgments.get(query);
			if (relevances == null) {
				System.out.println("Query \'" + query + "\' not found!");
			} else {
				if (relevances.hasRelevanceForDoc(docid)) {
					RR += relevances.getBinaryRelevanceForDoc(docid);
				}
				++N;
			}
			s.close();
		}
		reader.close();
		//System.out.println("Accuracy: " + Double.toString(RR / N));
	}

	public static Vector<Vector<String>> getData() {
		return data;
	}
	
	/**
	 * This function calculates Precision value at a given point depending on
	 * the relevance.
	 * 
	 * @param relevance_judgments
	 * @return
	 */
	public static double evaluatePrecision(
			HashMap<String, DocumentRelevances> judgments,
			Vector<Vector<String>> data, int precisionPoint) {
		double RR = 0.0;
		double N = 0.0;
		try {
			int lineCount = 0;
			for (int i = 0; i < data.size(); i++) {
				lineCount++;
				if (lineCount > precisionPoint)
					break;
				Vector<String> row = data.get(i);
				String query = row.get(0);
				int did = Integer.parseInt(row.get(1));
				if (judgments.containsKey(query) == false) {
					throw new IOException("query not found");
				}
				DocumentRelevances qr = judgments.get(query);
				if (qr != null) {
					RR += qr.getBinaryRelevanceForDoc(did);
				}
				++N;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error:" + e.getMessage());
			
		}

		if (RR != 0.0 && N != 0.0)
			return (RR / N);
		else
			return 0.0;
	}

	/**
	 * This function calculates Recall value at a given point depending on the
	 * relevance.
	 * 
	 * @param relevance_judgments
	 * @param recallPoint
	 * @return
	 */
	public static double evaluateRecall(
			HashMap<String, DocumentRelevances> judgments,
			Vector<Vector<String>> data, int recallPoint) {
		double RR = 0.0;
		double N = 0.0;
		String query = "";
		try {
			int lineCount = 0;
			for (int i = 0; i < data.size(); i++) {
				lineCount++;
				if (lineCount > recallPoint)
					break;
				Vector<String> row = data.get(i);
				query = row.get(0);
				int did = Integer.parseInt(row.get(1));
				if (judgments.containsKey(query) == false) {
					throw new IOException("query not found");
				}
				DocumentRelevances qr = judgments.get(query);
				if (qr != null) {
					RR += qr.getBinaryRelevanceForDoc(did);
				}
			}
			if (judgments.containsKey(query) == false) {
				throw new IOException("query not found");
			}
			DocumentRelevances qr = judgments.get(query);
			Map<Integer, Double> relevances = qr.getRelevances();
			Iterator<Entry<Integer, Double>> it = relevances.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, Double> pairs = (Map.Entry<Integer, Double>) it
						.next();
				if (pairs.getValue() != 0d) {
					N += pairs.getValue();
				}
			}
			// System.out.println("Recall at : "+recallPoint);
			// System.out.println(Double.toString(RR/N));
		} catch (Exception e) {
			System.err.println("Error:" + e.getMessage());
		}
		if (RR != 0.0 && N != 0.0)
			return (RR / N);
		else
			return 0.0;
	}

	private static Vector<Double> recallValues;
	private static Vector<Double> precisionValues;

	public static Vector<Double> getRecallValues() {
		return recallValues;
	}

	public static Vector<Double> getPrecisionValues() {
		return precisionValues;
	}

	public static void computeRecallPrecision(
			HashMap<String, DocumentRelevances> judgments,
			Vector<Vector<String>> data) {

		recallValues = new Vector<Double>();
		precisionValues = new Vector<Double>();
		int averagePoint = data.size();
		for (int i = 1; i <= averagePoint; i++) {
			recallValues.add(evaluateRecall(judgments, data, i));
			precisionValues
					.add(evaluatePrecision(judgments, data, i));
		}
	}

	public static double averagePrecision(
			HashMap<String, DocumentRelevances> judgments,
			Vector<Vector<String>> data) {

		computeRecallPrecision(judgments, data);

		double precisionSum = precisionValues.get(0);
		double previous = recallValues.get(0);
		double count = 0;
		if (previous != 0d) {
			count = 1;
		}
		for (int i = 1; i < recallValues.size(); i++) {
			if (recallValues.get(i) != previous) {
				previous = recallValues.get(i);
				precisionSum += precisionValues.get(i);
				count++;
			}
		}
		
		if(count != 0.0d) {
			return (precisionSum / count);
		} else {
			return 0;
		}
		
	}

	public static double f1Score(
			HashMap<String, DocumentRelevances> judgments,
			Vector<Vector<String>> data, int f1ScorePoint) {
		double precision = evaluatePrecision(judgments, data,
				f1ScorePoint);
		double recall = evaluateRecall(judgments, data, f1ScorePoint);
		double num = 2 * precision * recall;
		double denom = precision + recall;

		if (denom != 0.0)
			return (num / denom);
		else
			return 0.0;
	}
}