package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * 
 * @author sujal
 * 
 */
public class IndexerInvertedDoconly extends Indexer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Store inverted index doconly with wordcount
	public Map<String, Map<Integer, Integer>> _invertedIndex = new LinkedHashMap<String, Map<Integer, Integer>>();

	// Maximum no. of files to process in memory at a time
	int _maxFiles = 500;

	String _docInfoFile = "docinfo.inf";// "data/title.idx";

	int _intermediateIndexFiles = 0;

	Vector<String> _docTitles = new Vector<String>();
	
	StringBuffer docInfo = new StringBuffer();

	final String _termDoclistDelim = ";";
	final String _docCountDelim = ":";
	final String _doclistDelim = " ";
	final String _docInfoDelim = ";";

	// Stores all Document (not body vectors) in memory.
	private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();

	public IndexerInvertedDoconly(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
	}

	@Override
	public void constructIndex() throws IOException {
		String corpusDirPath = _options._corpusPrefix;
		_docInfoFile = _options._indexPrefix + "/" + _docInfoFile;
		
		// delete previously created index
		Utilities.deleteFilesInDir(_options._indexPrefix);

		System.out.println("Constructing index from: " + corpusDirPath);

		StringBuffer ss = new StringBuffer();
		File corpusDir = new File(corpusDirPath);		
		for (File corpusFile : corpusDir.listFiles()) {
			Document doc = Jsoup.parse(corpusFile, "UTF-8");
			
			String contents = doc.text();

			System.out.println("Processing : " + _numDocs + " : " + corpusFile.getName());
			
			
			ss.append("Processing : " + _numDocs + " : " + corpusFile.getName());
			ss.append("\n");
			
			processDocument(contents, doc, _numDocs);

			if ((_numDocs + 1) % _maxFiles == 0) {
				// write index to intermediate file
				writeIndexToFile();
				Utilities.writeToFile(_docInfoFile, docInfo.toString(), true);
				_intermediateIndexFiles++;

				// flush the in memory index and document info
				_invertedIndex = new LinkedHashMap<String, Map<Integer, Integer>>();
				docInfo = new StringBuffer();
			}

			_numDocs++;
		}
		
		Utilities.writeToFile(_options._indexPrefix + "/log.txt", ss.toString(), false);
		
		System.out.println("no of docs processed = " + _numDocs);

		// write last batch of info
		writeIndexToFile();
		Utilities.writeToFile(_docInfoFile, docInfo.toString(), true);
	}

	private void mergeIndexFiles(String file1, String file2) {

		if (file1 == null || file2 == null || file1.trim().length() == 0
				|| file2.trim().length() == 0) {
			return;
		}

		try {
			File f1 = new File(_options._indexPrefix + "/" + file1);
			File f2 = new File(_options._indexPrefix + "/" + file2);

			if (!f2.exists()) {
				return;
			} else if (f1.exists() && f2.exists()) {
				String f3Name = _options._indexPrefix + "/"
						+ System.currentTimeMillis() + ".idx";

				BufferedReader br1 = new BufferedReader(new FileReader(f1));
				BufferedReader br2 = new BufferedReader(new FileReader(f2));
				BufferedWriter bw = new BufferedWriter(new FileWriter(f3Name));

				String line1 = br1.readLine(), line2 = br2.readLine();
				String term1, term2;
				Scanner scanner1, scanner2;
				while ((line1 != null) && (line2 != null)) {

					if (line1.trim().length() == 0
							|| line2.trim().length() == 0) {
						break;
					}

					scanner1 = new Scanner(line1);
					scanner1.useDelimiter("["
							+ String.valueOf(_termDoclistDelim) + "\n]");
					scanner2 = new Scanner(line2);
					scanner2.useDelimiter("["
							+ String.valueOf(_termDoclistDelim) + "\n]");

					term1 = scanner1.next();
					term2 = scanner2.next();

					if (term1.compareTo(term2) < 0) {
						// add term1 info as it is to the file
						bw.write(line1);
						bw.newLine();

						line1 = br1.readLine();
					} else if (term1.compareTo(term2) > 0) {
						// add term2 info as it is to the file
						bw.write(line2);
						bw.newLine();

						line2 = br2.readLine();
					} else {
						// write any term as both are same
						bw.write(term1);
						bw.write(_termDoclistDelim);

						String tmp1 = scanner1.next();
						String tmp2 = scanner2.next();

						// write docIds in sorted manner
						int docid1 = Integer.parseInt(tmp1.split(String
								.valueOf(_doclistDelim))[0].split(String
								.valueOf(_docCountDelim))[0]);
						int docid2 = Integer.parseInt(tmp2.split(String
								.valueOf(_doclistDelim))[0].split(String
								.valueOf(_docCountDelim))[0]);

						if (docid1 < docid2) {
							bw.write(tmp1);
							// bw.write(_doclistDelim);
							bw.write(tmp2);
						} else {
							bw.write(tmp2);
							// bw.write(_doclistDelim);
							bw.write(tmp1);
						}

						bw.newLine();

						line1 = br1.readLine();
						line2 = br2.readLine();
					}
				}

				// copy the remaining info from non empty file
				if (line1 != null) {
					while (line1 != null) {
						if (line1.trim().length() == 0) {
							break;
						}

						bw.write(line1);
						bw.newLine();
						line1 = br1.readLine();
					}
				} else if (line2 != null) {
					while (line2 != null) {
						if (line2.trim().length() == 0) {
							break;
						}

						bw.write(line2);
						bw.newLine();
						line2 = br2.readLine();
					}
				}

				// close open streams
				br1.close();
				br2.close();
				bw.close();

				// delete old files
				f1.delete();
				f2.delete();

				new File(f3Name).renameTo(f1);
			} else if (!f1.exists()) {
				// here f2 should exist

				// just rename f2 to f1
				f2.renameTo(f1);
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void writeIndexToFile() throws IOException {

		if (_invertedIndex == null) {
			return;
		}

		// Maintain 26+10 index files corresponding to 1st character in term (26
		// alphabets + 10 digits)

		String term;
		String indexFileNameOrig, indexFileNameTmp;
		StringBuffer indexData;
		char firstChar;

		// sort the keys
		SortedSet<String> keys = new TreeSet<String>(_invertedIndex.keySet());
		Iterator<String> keyIterator = keys.iterator();

		// loop through all the terms in the index
		while (keyIterator.hasNext()) {
			indexData = new StringBuffer();

			term = keyIterator.next();
			indexData.append(term);
			indexData.append(_termDoclistDelim);
			// append docIds and term count
			Map<Integer, Integer> docInfo = _invertedIndex.get(term);
			SortedSet<Integer> docIds = new TreeSet<Integer>(docInfo.keySet());
			for (Integer docId : docIds) {
				indexData.append(docId);
				indexData.append(_docCountDelim);
				indexData.append(docInfo.get(docId));
				indexData.append(_doclistDelim);
			}
			indexData.append("\n");

			firstChar = term.charAt(0);
			indexFileNameOrig = firstChar + ".idx";
			indexFileNameTmp = firstChar + "_tmp.idx";

			// Loop through all subsequent keys where the first character of the
			// term is same as that
			// of the previous term
			while (keyIterator.hasNext()
					&& (term = keyIterator.next()).charAt(0) == firstChar) {
				indexData.append(term);
				indexData.append(_termDoclistDelim);
				// append docIds and term count
				docInfo = _invertedIndex.get(term);
				docIds = new TreeSet<Integer>(docInfo.keySet());
				for (Integer docId : docIds) {
					indexData.append(docId); // docid
					indexData.append(_docCountDelim);
					indexData.append(docInfo.get(docId)); // term count
					indexData.append(_doclistDelim);
				}
				indexData.append("\n");
			}

			// write info of all terms with the same first characters to the tmp
			// file
			Utilities.writeToFile(_options._indexPrefix + "/"
					+ indexFileNameTmp, indexData.toString(), false);

			// merge old and new files. e.g. merge a.idx and a_tmp.idx -> a.idx
			mergeIndexFiles(indexFileNameOrig, indexFileNameTmp);
		}
	}

	/**
	 * Process the raw content (i.e., one line in corpus.tsv) corresponding to a
	 * document, and constructs the token vectors for both title and body.
	 * 
	 * @param content
	 */
	private void processDocument(String content, Document doc, int docId) {

		if (content == null || doc == null || docId < 0) {
			return;
		}
		
		Vector<String> terms = Utilities.getStemmed(content);
		for (String t : terms) {
			t = t.trim();
			if (t.length() > 0) {
				if (!_invertedIndex.containsKey(t)) {
					_invertedIndex.put(t, new HashMap<Integer, Integer>());
				}

				if (_invertedIndex.get(t).containsKey(docId)) {
					_invertedIndex.get(t).put(docId,
							_invertedIndex.get(t).get(docId) + 1);
				} else {
					_invertedIndex.get(t).put(docId, 1);
				}

				++_totalTermFrequency;
			}
		}
		
		String uri = doc.baseUri();
		uri = uri==null ? "" : uri;
		uri = new File(uri).getName();
		
		String title = doc.title().trim();
		title = title.length()==0 ? uri : title;
		
		docInfo.append(docId);
		docInfo.append(_docInfoDelim);		
		docInfo.append(uri);
		docInfo.append(_docInfoDelim);
		docInfo.append(title);
		docInfo.append("\n");		
	}
	
	public void loadIndex(Query query) {
		Map<Character, Byte> chars = new HashMap<Character, Byte>();
		query.processQuery();
		Vector<String> tokens = query._tokens;
		for(String token : tokens) {
			if(token.trim().length() != 0) {
				chars.put(token.charAt(0), null);
			}				
		}
		
		_invertedIndex = new LinkedHashMap<String, Map<Integer, Integer>>();
		for(Character c : chars.keySet()) {
			loadIndex(String.valueOf(c).toLowerCase());
		}
	}

	/**
	 * Loads only the index file which may contain the input term
	 * @param term
	 */
	public void loadIndex(String term) {
		//_invertedIndex = new LinkedHashMap<String, Map<Integer, Integer>>();
				
		String indexFile = _options._indexPrefix + "/" + term.charAt(0)
				+ ".idx";

		//System.out.println("Loading index from : " + indexFile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(indexFile));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.trim().length() != 0) {
					Scanner scanner = new Scanner(line);
					scanner.useDelimiter("[" + _termDoclistDelim + "\n]");

					String token = scanner.next();
					
					// create new map entry for current term
					Map<Integer, Integer> docInfoMap = new HashMap<Integer, Integer>();
					_invertedIndex.put(token, docInfoMap);

					// build docInfoMap
					String[] docInfo = scanner.next().split(_doclistDelim);
					for (String doc : docInfo) {
						String[] doc_count = doc.split(_docCountDelim);

						if (docInfoMap.containsKey(Integer
								.valueOf(doc_count[0]))) {
							docInfoMap.put(Integer.valueOf(doc_count[0]),
									docInfoMap.get(Integer
											.valueOf(doc_count[1])) + 1);
						} else {
							docInfoMap.put(Integer.valueOf(doc_count[0]), 1);
						}
					}

					if (token.equals(term)) {
						break;
					}
				}
			}

			//System.out.println("Loading done...");

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void loadIndex() {
		BufferedReader br;
		String line;
		_docInfoFile = _options._indexPrefix + "/" + _docInfoFile;
		
		/*FilenameFilter indexFilesFilter = new FilenameFilter() {
			@Override
			public boolean accept(File arg0, String arg1) {
				return arg1.endsWith(".idx");
			}
		};

		File indexDir = new File(_options._indexPrefix);
		System.out.println("Loading indexes from : " + _options._indexPrefix);

		File[] indexFiles = indexDir.listFiles(indexFilesFilter);
		
		String[] docInfo;
		Map<Integer, Integer> docInfoMap;
		Scanner scanner;
		String token;
		String[] doc_count;

		for (File indexFile : indexFiles) {
			System.out.println("loading : " + indexFile.getName());
			try {
				br = new BufferedReader(new FileReader(indexFile));
				line = "";

				while ((line = br.readLine()) != null) {
					if (line.trim().length() != 0) {
						scanner = new Scanner(line);
						scanner.useDelimiter("[" + _termDoclistDelim + "\n]");

						token = scanner.next();

						// create new map entry for current term
						docInfoMap = new HashMap<Integer, Integer>();
						_invertedIndex.put(token, docInfoMap);

						// build docInfoMap
						docInfo = scanner.next().split(_doclistDelim);
						for (String doc : docInfo) {
							doc_count = doc.split(_docCountDelim);							
							docInfoMap.put(Integer.valueOf(doc_count[0]), Integer.valueOf(doc_count[1]));

							_totalTermFrequency += Integer.valueOf(doc_count[1]);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Loading index done ...");
		*/

		// load doc info file
		System.out.println("Loading documents info from : " + _docInfoFile);

		try {
			br = new BufferedReader(new FileReader(_docInfoFile));
			
			String[] info;
			DocumentIndexed dIndexed;
			
			while ((line = br.readLine()) != null) {
				info = line.split(_docInfoDelim);
					
				dIndexed = new DocumentIndexed(Integer.parseInt(info[0]));
				dIndexed.setUrl(info[1]);
				dIndexed.setTitle(info[2]);
				_documents.add(dIndexed);
			}
			_numDocs = _documents.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Loading document info done ...");

		/*
		 * _invertedIndex = new LinkedHashMap<String, TreeSet<Integer>>();
		 * 
		 * System.out.println("Loading index from : " + _indexFile); try {
		 * BufferedReader br = new BufferedReader(new FileReader(_indexFile));
		 * String line = "";
		 * 
		 * while ((line = br.readLine()) != null) { if (line.trim().length() !=
		 * 0) { Scanner scanner = new Scanner(line);
		 * scanner.useDelimiter("[:\n]");
		 * 
		 * String token = scanner.next(); _invertedIndex.put(token, new
		 * TreeSet<Integer>());
		 * 
		 * String[] docIds = scanner.next().split("[ \n]"); TreeSet<Integer>
		 * docIdList = _invertedIndex.get(token); for (String docId : docIds) {
		 * if (docId.trim().length() != 0) {
		 * docIdList.add(Integer.parseInt(docId)); } } } } br.close();
		 * 
		 * // load titles info br = new BufferedReader(new
		 * FileReader(_titleFile)); while ((line = br.readLine()) != null) {
		 * _docTitles.add(line); }
		 * 
		 * _numDocs = _docTitles.size();
		 * 
		 * System.out.println("Loading completed"); } catch
		 * (NumberFormatException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); } catch (FileNotFoundException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); } catch (IOException
		 * e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */
	}

	@Override
	public DocumentIndexed getDoc(int docid) {
		return _documents.get(docid);
	}

	/**
	 * In HW2, you should be using {@link DocumentIndexed}
	 */
	@Override
	public DocumentIndexed nextDoc(Query query, int docid) {

		if (query == null || query._query.trim().length() == 0) {
			return null;
		}
				
		// remove duplicate terms in query
		Set<String> queryProcessed = new TreeSet<String>(
		Utilities.getStemmed(query._query));
				
		if(docid == -1) {
			// It means this is first call to nextDoc for given query.
			
			System.out.println("Searching ... ");
			
			// load necessary indices			
			loadIndex(query);
		}		

		int[] docIds = new int[queryProcessed.size()];

		// perform conjunctive retrieval
		int qTermNo = 0;
		for (String qTerm : queryProcessed) {
			// load appropriate index
			// loadIndex(qTerm);

			docIds[qTermNo++] = nextDoc(qTerm, docid);
		}

		while (!isSame(docIds) && continueSearch(docIds)) {
			int newDocId = getMax(docIds) - 1;
			docIds = new int[queryProcessed.size()];

			qTermNo = 0;
			for (String qTerm : queryProcessed) {
				// load appropriate index
				// loadIndex(qTerm);
				docIds[qTermNo++] = nextDoc(qTerm, newDocId);
			}
		}

		if (!continueSearch(docIds) || !isSame(docIds)) {
			return null;
		}

		// At this point, all the entries in the array are same
		return new DocumentIndexed(docIds[0]);
	}

	/**
	 * 
	 * @param list
	 * @return maximum integer from the given list
	 */
	private int getMax(int[] list) {

		if (list == null || list.length == 0) {
			return -1;
		}

		int max = -1;

		if (list.length > 0) {
			max = list[0];
			for (int i : list) {
				max = Math.max(max, i);
			}
		}

		return max;
	}

	/**
	 * 
	 * @param docIds
	 * @return true if all numbers in the given list are same. Else returns
	 *         false
	 */
	private boolean isSame(int[] docIds) {

		if (docIds == null || docIds.length == 0) {
			return false;
		}

		if (docIds.length > 0) {
			int first = docIds[0];
			for (int i = 1; i < docIds.length; i++) {
				if (first != docIds[i]) {
					return false;
				}
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param docIds
	 * @return
	 */
	private boolean continueSearch(int[] docIds) {

		if (docIds == null || docIds.length <= 0) {
			return false;
		}

		for (int docId : docIds) {
			// if atleast one term is not found, search should not continue
			if (docId == -1) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 
	 * @param term
	 * @param docid
	 * @return Next docId containing the given term
	 */
	private int nextDoc(String term, int docid) {

		if (term == null || term.trim().length() == 0) {
			return -1;
		}

		int nextDocId = -1;
		Integer[] docIdList = null;

		if (_invertedIndex.containsKey(term)) {
			Map<Integer, Integer> docList = _invertedIndex.get(term);
			docIdList = new Integer[docList.size()];

			Set<Integer> docListSorted = new TreeSet<Integer>(docList.keySet());
			docListSorted.toArray(docIdList);
			// docList.toArray(docIdList);

			// perform search for nextdocid on this array nextDocId =
			nextDocId = search(docid, docIdList, true);
		}

		return nextDocId;

	}

	private int search(int currentDocId, Integer[] docIdList, boolean galloping) {

		if (docIdList == null || docIdList.length == 0
				|| docIdList[docIdList.length - 1] < currentDocId) {
			return -1;
		}

		if (docIdList[0] > currentDocId) {
			return docIdList[0];
		}

		int low = 0, high = 0;
		int jump = 1;

		if (galloping) {
			// Through galloping, find a slot for binary search
			while ((high < docIdList.length) && docIdList[high] <= currentDocId) {

				low = high;
				// increase step size
				jump = jump << 1;
				high += jump;
			}

			if (high > (docIdList.length - 1)) {
				high = docIdList.length - 1;
			}
		} else {
			high = docIdList.length - 1;
		}

		return binarySearch(docIdList, low, high, currentDocId);
	}

	/*
	 * Perform binary search over the given list to find a number > current.
	 * Returns -1 if no such number is found
	 */
	private int binarySearch(Integer[] list, int begin, int end, int current) {

		if (list == null || list.length == 0) {
			return -1;
		}

		if (begin < 0 || end < 0 || begin >= list.length || end >= list.length) {
			return -1;
		}

		// if last number is less than current then return -1
		if (list[end] <= current) {
			return -1;
		} else {
			int mid;
			while (begin <= end) {
				mid = (begin + end) / 2;

				if (list[mid] <= current) {
					// search in right half
					begin = mid + 1;
				} else {
					// search in left half
					end = mid - 1;
				}
			}

			if (list[begin] > current) {
				return list[begin];
			} else {
				return list[begin + 1];
			}
		}
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		return _invertedIndex.containsKey(term) ? _invertedIndex.get(term)
				.size() : 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		int corpusTermFreq = 0;
		
		if(_invertedIndex.containsKey(term)) {
			Map<Integer, Integer> docList = _invertedIndex.get(term);
			for(Integer docId : docList.keySet()) {
				corpusTermFreq += docList.get(docId);
			}
		}
		
		return corpusTermFreq;
	}
	
	@Override
	public int documentTermFrequency(String term, String url) {
		return 0;
	}

	/**
	 * 
	 * @param term
	 * @param docId
	 * @return count of given term in document with given docId
	 */
	public int documentTermFrequency(String term, int docId) {
		int documentTermFreq = 0;

		// check whether the given term is present in the index
		if(_invertedIndex.containsKey(term)) {
			Map<Integer, Integer> docList = _invertedIndex.get(term);
			
			// check whether the given term is present in the document with given docId
			if(docList.containsKey(docId)) {
				// return the count of term in the document
				documentTermFreq = docList.get(docId);
			}
		}
		
		return documentTermFreq;
	}

	private void testNextDoc(IndexerInvertedDoconly iido) {
		long start, end;

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String queryStr;

		try {
			while (!(queryStr = br.readLine()).equals("quit")) {
				Query q = new Query(queryStr);
				int totalResults = 0;
				start = System.currentTimeMillis();
				DocumentIndexed di = iido.nextDoc(q, -1);
				if (di == null) {
					System.out.println("No documents found !!!");
				}
				while (di != null) {
					totalResults++;
					System.out.println(di._docid + " - "
							+ iido._documents.get(di._docid).getTitle());
					di = iido.nextDoc(q, di._docid);
				}
				end = System.currentTimeMillis();
				System.out.println("Total results = " + totalResults);
				System.out.println("Search time = " + (end - start));
				System.out.println("#####################################");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public IndexerInvertedDoconly() {

		try {
			Options options = new Options("conf/engine.conf");
			IndexerInvertedDoconly iido = new IndexerInvertedDoconly(options);
			long start = System.currentTimeMillis();
			 //iido.constructIndex();
			iido.loadIndex();
			// iido.loadIndex("xz");
			//iido.testParse(iido);
			long end = System.currentTimeMillis();
			System.out.println("time = " + (end - start));
			
			System.out.println("total docs loaded = " + iido._documents.size());
			

			//int cnt = iido._invertedIndex.get("xypolia").size();
			//System.out.println("cnt = " + cnt);

			testNextDoc(iido);
			
		} catch (IOException e) { // TODO Auto-generated
			e.printStackTrace();
		}

		// testMerge();
	}
	
	private void testParse(IndexerInvertedDoconly iido) {
		File corpusFile = new File(iido._options._corpusPrefix + "/2011");
		try {
			Document doc = Jsoup.parse(corpusFile, "UTF-8");
			//System.out.println("uri = " + doc.);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void testMerge() {
		try {
			Options options = new Options("conf/engine.conf");
			IndexerInvertedDoconly iido = new IndexerInvertedDoconly(options);

			String f1 = "f1.idx";
			String f2 = "f2.idx";
			iido.mergeIndexFiles(f1, f2);
		} catch (Exception e) {

		}
	}

	public static void main(String[] args) {
		new IndexerInvertedDoconly();
		// testFileFilter();
		// testParsing();
	}
}
