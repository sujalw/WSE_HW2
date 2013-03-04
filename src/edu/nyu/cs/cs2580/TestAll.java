package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

/**
 * 
 * @author sujal
 * 
 */
public class TestAll {

	public TestAll() {
		// read all queries from queries.tsv and create a shell script that
		// tests all models on it.

		String[] rankerModels = new String[] { "cosine", "QL", "phrase",
				"numviews", "linear" };

		HashMap<String, Integer> rankerModelsMap = new HashMap<String, Integer>();
		rankerModelsMap.put("cosine", 0);
		rankerModelsMap.put("QL", 1);
		rankerModelsMap.put("phrase", 2);
		rankerModelsMap.put("numviews", 3);
		rankerModelsMap.put("linear", 4);

		String outputFormat = "text";
		String host = "localhost";
		int port = 25803;

		String script = "testall.sh";

		Properties prop = new Properties();
		// load a properties file
		try {
			prop.load(this.getClass().getResourceAsStream("config.properties"));
			String dataDir = prop.getProperty("data_dir");
			String queries = prop.getProperty("queries");
			String qrels = prop.getProperty("qrels");
			String results_dir = prop.getProperty("results_dir");

			String vsm_evaluator_results = prop
					.getProperty("vsm_evaluator_results");
			String ql_evaluator_results = prop
					.getProperty("ql_evaluator_results");
			String phrase_evaluator_results = prop
					.getProperty("phrase_evaluator_results");
			String numviews_evaluator_results = prop
					.getProperty("numviews_evaluator_results");
			String linear_evaluator_results = prop
					.getProperty("linear_evaluator_results");

			String[] outputResults = new String[] { vsm_evaluator_results,
					ql_evaluator_results, phrase_evaluator_results,
					numviews_evaluator_results, linear_evaluator_results };

			FileReader fr = new FileReader(dataDir + queries);
			BufferedReader br = new BufferedReader(fr);
			String query = "";

			String testUrl = "";
			while ((query = br.readLine()) != null) {
				for (String rankerModel : rankerModels) {
					testUrl = "http://" + host + ":" + port + "/search?query="
							+ query + "&ranker=" + rankerModel + "&format="
							+ outputFormat;
					URL url = new URL(testUrl);
					URI uri = new URI(url.getProtocol(), url.getUserInfo(),
							url.getHost(), url.getPort(), url.getPath(),
							url.getQuery(), url.getRef());
					url = uri.toURL();
					testUrl = "curl " + "\"" + url.toString() + "\"";
					testUrl += " | java edu.nyu.cs.cs2580.Evaluator " + "\""
							+ dataDir + qrels + "\"";

					testUrl += " >> " + "\"" + results_dir
							+ outputResults[rankerModelsMap.get(rankerModel)]
							+ "\"";

					testUrl += "\n";

					Utilities.writeToFile(results_dir + script, testUrl, true);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new TestAll();
	}
}
