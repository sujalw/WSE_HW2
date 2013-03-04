package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class Output {
	Vector<ScoredDocument> _outputVector;
	Map<String, String> _query_map;
	String _clickLoggingData;

	public static enum outputType {
		TEXT, HTML
	};

	public static enum action {
		RENDER, CLICK
	};

	public Output() {

	}

	public Output(final Vector<ScoredDocument> outputVector,
			final Map<String, String> query_map) {
		_outputVector = outputVector;
		_query_map = query_map;
		_clickLoggingData = "";
	}

	/**
	 * @author sujal
	 * @param sessionId
	 * @return
	 */
	public String generateTextOutput(String sessionId) {
		String queryResponse = "";

		try {
			Iterator<ScoredDocument> itr = _outputVector.iterator();
			while (itr.hasNext()) {
				ScoredDocument sd = itr.next();
				if (queryResponse.length() > 0) {
					queryResponse = queryResponse + "\n";
				}
				queryResponse = queryResponse + _query_map.get("query") + "\t"
						+ sd.asTextResult();

				logAction(sessionId, _query_map.get("query"), sd.getDocId(),
						action.RENDER, new Date());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return queryResponse;
		// return _clickLogging;
	}

	public static void logAction(String sessionId, String query, int did,
			action logAction, Date date) {
		String logData = "";
		Properties prop = new Properties();
		// load a properties file
		try {
			prop.load(Output.class.getResourceAsStream("config.properties"));
			String resultsDir = prop.getProperty("results_dir");
			String click_logging_results = prop
					.getProperty("click_logging_results");

			logData = sessionId + "\t" + query + "\t" + did + "\t" + logAction
					+ "\t" + date + "\n";

			Utilities.writeToFile(resultsDir + click_logging_results, logData,
					true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String generateHtmlOutput(String sessionId, String host) {
		String queryResponse = "";

		queryResponse += "<html><head></head><body><br>";
		queryResponse += "<table border=\"1\">";
		queryResponse += "<tr><th>Query</th><th>Document Title</th><th>Doc Id</th><th>Score</th></tr>";

		try {
			Iterator<ScoredDocument> itr = _outputVector.iterator();
			while (itr.hasNext()) {
				ScoredDocument sd = itr.next();
				if (queryResponse.length() > 0) {
					queryResponse = queryResponse + "\n";
				}

				String loggingUrl = "http://" + host + "/logging?query="
						+ _query_map.get("query") + "&did=" + sd.getDocId();
				URL url = new URL(loggingUrl);
				URI uri = new URI(url.getProtocol(), url.getUserInfo(),
						url.getHost(), url.getPort(), url.getPath(),
						url.getQuery(), url.getRef());
				url = uri.toURL();
				loggingUrl = url.toString();

				queryResponse += "<tr>";
				queryResponse += "<td>" + _query_map.get("query") + "</td>";
				queryResponse += "<td>" + "<a href=\"" + loggingUrl + "\">"
						+ sd.getTitle() + "</a>" + "</td>";
				queryResponse += "<td>" + sd.getDocId() + "</td>";
				queryResponse += "<td>" + sd.get_score() + "</td>";
				queryResponse += "</tr>";

				_clickLoggingData = _clickLoggingData + sessionId + "\t"
						+ _query_map.get("query") + "\t" + sd.getDocId() + "\t"
						+ action.RENDER + "\t" + new Date() + "\n";
			}

			queryResponse += "</table>";
			queryResponse += "<br></body></html>";

			// write _clickLogging data to the file
			// Utilities.writeToFile(resultsDir + click_logging_results,
			// _clickLoggingData, true);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return queryResponse;
		// return _clickLogging;
	}
}
