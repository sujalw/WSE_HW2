package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.jsoup.Jsoup;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * 
 * @author sujal
 * 
 */
public class IndexerInvertedDoconly extends Indexer {

	// Store inverted index doconly
	Map<String, TreeSet<Integer>> _invertedIndex = new LinkedHashMap<String, TreeSet<Integer>>();

	// Maximum no. of files to process in memory at a time
	int _maxFiles = 500;

	// Temporary index file
	String _indexNew = "new.idx";
	
	// Final index file
	String _indexFile = _options._indexPrefix + "/" + "corpus.idx";

	public IndexerInvertedDoconly(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	@Override
	public void constructIndex() throws IOException {
		String corpusDirPath = _options._corpusPrefix;
		System.out.println("Constructing index from: " + corpusDirPath);

		File corpusDir = new File(corpusDirPath);
		int docId = 1;
		for (File corpusFile : corpusDir.listFiles()) {

			String contents = Jsoup.parse(corpusFile, "UTF-8").text();
			String title = Jsoup.parse(corpusFile, "UTF-8").title();

			processDocument(contents, title, docId);

			if (docId % _maxFiles == 0) {
				// write index to intermediate file
				writeIndexToFile();

				// flush the in memory index
				_invertedIndex = new LinkedHashMap<String, TreeSet<Integer>>();
			}

			docId++;

			// if (docId == 95) {
			// break;
			// }
		}
	}

	private void writeIndexToFile() throws IOException {

		String indexFileNew = _options._indexPrefix + "/" + _indexNew;

		String tokenOld = "", tokenNew = "";
		StringBuffer docIds = new StringBuffer();

		// sort the keys
		SortedSet<String> keys = new TreeSet<String>(_invertedIndex.keySet());
		Iterator<String> keyIterator = keys.iterator();

		BufferedWriter bw = new BufferedWriter(new FileWriter(indexFileNew));

		// if old index exists, merge new data with it and create new index file
		if (new File(_indexFile).exists()) {
			BufferedReader br = new BufferedReader(new FileReader(_indexFile));

			String line = "";
			boolean readNextNewToken = true;

			while ((line = br.readLine()) != null) {

				if (line.trim().length() <= 0) {
					break;
				}
				// if new index has any new terms
				if (keyIterator.hasNext()) {
					Scanner scanner = new Scanner(line);
					scanner.useDelimiter("[:\n]");

					tokenOld = scanner.next();

					if (readNextNewToken) {
						tokenNew = keyIterator.next();
					}

					// if old and new tokens are same, then combine their
					// corresponding docids
					if (tokenNew.equals(tokenOld)) {
						docIds = new StringBuffer();
						docIds.append(tokenNew);
						docIds.append(":");

						// append old docid entries
						docIds.append(scanner.next()); 

						// append new docid entries
						for (Integer docIdNew : _invertedIndex.get(tokenNew)) {
							docIds.append(docIdNew);
							docIds.append(" ");
						}
						docIds.append("\n");

						// write to file
						bw.write(docIds.toString());

						readNextNewToken = true;

					} else if (tokenNew.compareTo(tokenOld) < 0) {
						// write info of new token to the file
						docIds = new StringBuffer();
						docIds.append(tokenNew);
						docIds.append(":");
						// append new entries
						for (Integer docIdNew : _invertedIndex.get(tokenNew)) {
							docIds.append(docIdNew);
							docIds.append(" ");
						}
						docIds.append("\n");

						// write to file
						bw.write(docIds.toString());

						readNextNewToken = true;
					} else {
						// write info of old token to the new file
						bw.write(line);
						bw.newLine();

						readNextNewToken = false;
					}

				} else {
					// write info of old token to the new file
					bw.write(line);
					bw.newLine();
				}
			}
			br.close();
		}

		// Write remaining info of new tokens, if any, to the new index file
		if (keyIterator.hasNext()) {
			docIds = new StringBuffer();

			while (keyIterator.hasNext()) {
				tokenNew = keyIterator.next();
				docIds.append(tokenNew);
				docIds.append(":");
				// append new entries
				for (Integer docIdNew : _invertedIndex.get(tokenNew)) {
					docIds.append(docIdNew);
					docIds.append(" ");
				}
				docIds.append("\n");
			}

			bw.write(docIds.toString());
			bw.newLine();
		}

		bw.close();

		// remove old index file
		if (new File(_indexFile).exists()) {
			if (!new File(_indexFile).delete()) {
				System.out.println("Old index file cannot be deleted");
			} else {
				// rename new file to the old one
				if (!new File(indexFileNew).renameTo(new File(_indexFile))) {
					System.out
							.println("New index file cannot be renamed to the old one");
				}
			}
		}		
	}

	/**
	 * Process the raw content (i.e., one line in corpus.tsv) corresponding to a
	 * document, and constructs the token vectors for both title and body.
	 * 
	 * @param content
	 */
	private void processDocument(String content, String title, int docId) {
		System.out.println("Processing : " + docId);
		Vector<String> contentsVector = getStemmed(content);
		for (String t : contentsVector) {
			t = t.trim();
			if (t.length() > 0) {
				if (!_invertedIndex.containsKey(t)) {
					_invertedIndex.put(t, new TreeSet<Integer>());
				}

				_invertedIndex.get(t).add(docId);
			}
		}
	}

	private Vector<String> getStemmed(String contents) {
		Vector<String> stemmedContents = new Vector<String>();

		Scanner s = new Scanner(contents.toLowerCase());
		s.useDelimiter("[^a-zA-Z0-9]");
		while (s.hasNext()) {
			String term = s.next();

			Stemmer stemmer = new Stemmer();
			stemmer.add(term.toCharArray(), term.length());
			stemmer.stem(); // code of stemmer is modified to compute just step1()

			stemmedContents.add(stemmer.toString());
		}
		s.close();

		return stemmedContents;
	}

	@Override
	public void loadIndex() {

		
		System.out.println("Loading index from : " + _indexFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(_indexFile));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.trim().length() != 0) {
					Scanner scanner = new Scanner(line);
					scanner.useDelimiter("[:\n]");

					String token = scanner.next();
					_invertedIndex.put(token, new TreeSet<Integer>());

					String[] docIds = scanner.next().split(" ");
					TreeSet<Integer> docIdList = _invertedIndex.get(token);
					for (String docId : docIds) {
						docIdList.add(Integer.parseInt(docId));
					}
				}
			}

			br.close();
			System.out.println("Loading completed");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Document getDoc(int docid) {
		SearchEngine.Check(false, "Do NOT change, not used for this Indexer!");
		return null;
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}
	 */
	@Override
	public Document nextDoc(Query query, int docid) {
		return null;
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

	public static void main(String[] args) {
		// String htmlText = "<head>hello</head>";
		// String text = Jsoup.parse(htmlText).text();
		// System.out.println("text = " + text);

		try {
			Options options = new Options("conf/engine.conf");
			IndexerInvertedDoconly iido = new IndexerInvertedDoconly(options);
			long start = System.currentTimeMillis();
			// iido.constructIndex();
			iido.loadIndex();
			long end = System.currentTimeMillis();
			System.out.println("time = " + (end - start));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
