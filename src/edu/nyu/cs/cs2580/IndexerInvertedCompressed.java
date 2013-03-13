package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
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

public class IndexerInvertedCompressed extends Indexer {

  // The compressed index is build such that for every term, there exists a HashMap, with key as
  // docId(not converted) and value as List of byte-align converted, delta encoded 
  // position of word occurrences.
  // There is no need to maintain the no. of occurrences as that can be retrieved as size of the list (if required). 	
  Map<String, Map<Integer, Vector<Character>>> _compressedIndex = 
		  new LinkedHashMap<String, Map<Integer,Vector<Character>>>();

  //"data/title.idx";
  String _docInfoFile = "docinfo.inf";
  // Maximum no. of files to process in memory at a time
  int _maxFiles = 500;
  
  final String _termDoclistDelim = ";";
  final String _docCountDelim = ":";
  final String _doclistDelim = " ";
  final String _docInfoDelim = ";";
  
  StringBuffer docInfo = new StringBuffer();
  
  //Stores all Document (not body vectors) in memory.
  private Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
  private Map<String, Integer> _docIdUriMap = new HashMap<String, Integer>();
  
  public static void main(String[] args) {
	new IndexerInvertedCompressed();
  }
  
  public IndexerInvertedCompressed() {

		try {
			Options options = new Options("conf/engine.conf");
			IndexerInvertedCompressed iic = new IndexerInvertedCompressed(options);
			long start = System.currentTimeMillis();
			_docInfoFile = options._indexPrefix + "/" + _docInfoFile;
			//iic.constructIndex();
			iic.loadIndex(); 
			long end = System.currentTimeMillis();
			System.out.println("time = " + (end - start));
			System.out.println("total docs loaded = " + iic._documents.size());
			
			testNextDoc(iic);
			
		} catch (IOException e) { // TODO Auto-generated
			e.printStackTrace();
		}

		// testMerge();
  }
  
  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
	  // delete previously created index
	  Utilities.deleteFilesInDir(_options._indexPrefix);

	  String corpusDirPath = _options._corpusPrefix;
	  System.out.println("Constructing index from: " + corpusDirPath);
	  
	  StringBuffer ss = new StringBuffer();
	  File corpusDir = new File(corpusDirPath);
	  for (File corpusFile : corpusDir.listFiles()) {
		  if(corpusFile.getName().startsWith(".")) {
			  continue;
		  }
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
			  // flush the in memory index
			  _compressedIndex = new LinkedHashMap<String, Map<Integer,Vector<Character>>>();
			  docInfo = new StringBuffer();
		  }

		  _numDocs++;
	  }

	  // write last batch of info
	  writeIndexToFile();		
	}

  private void processDocument(String content, Document doc, int docId) {

	  if (content == null || doc == null || docId < 0) {
		  return;
	  }

	  CompressionUtility cu = new CompressionUtility();
	  DecompressionUtility du = new DecompressionUtility();
	  Vector<String> terms = Utilities.getStemmed(content);
	  int pos = 0;
	  int previousOccurence = 0;
	  int newOccurence = 0;
	  String newOccurenceCoded = "";
	  Vector<Character> positionsEncoded;
	  for (String t : terms) {
		  t = t.trim();
		  if (t.length() > 0) {
			  if (!_compressedIndex.containsKey(t)) {
				  _compressedIndex.put(t, new LinkedHashMap<Integer, Vector<Character>>());
			  }

		  	  if (_compressedIndex.get(t).containsKey(docId)) {
		  		  
		  		  Vector<Integer> positionsDecoded = du.decodeByteAlign((_compressedIndex.get(t).get(docId)));
		  		  previousOccurence = positionsDecoded.get(positionsDecoded.size() - 1);
		  		  if((pos-previousOccurence) > 0) {
		  			  newOccurence = pos - previousOccurence;
		  			  newOccurenceCoded = cu.encodeByteAlign(newOccurence);
		  		  }
		  		positionsEncoded = new Vector<Character>();
		  		for(int i = 0; i < _compressedIndex.get(t).get(docId).size(); i++) {
		  			positionsEncoded.add(_compressedIndex.get(t).get(docId).get(i));
		  		}
		  		
		  		for(int i = 0; i < newOccurenceCoded.length(); i++) {
		  			positionsEncoded.add(newOccurenceCoded.charAt(i));
		  		}
		  		
				_compressedIndex.get(t).put(docId,positionsEncoded);
				
			  } else {
				  positionsEncoded = new Vector<Character>();
				  newOccurenceCoded = cu.encodeByteAlign(pos);
				  for(int i = 0; i < newOccurenceCoded.length(); i++) {
			  			positionsEncoded.add(newOccurenceCoded.charAt(i));
			  	  }
				  _compressedIndex.get(t).put(docId, positionsEncoded);
			  }

			  ++_totalTermFrequency;
		  }
		  pos++;
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
  
	private void writeIndexToFile() throws IOException {

		if (_compressedIndex == null) {
			return;
		}
		// Maintain 26+10 index files corresponding to 1st character in term (26
		// alphabets + 10 digits)
		String term;
		String indexFileNameOrig, indexFileNameTmp;
		StringBuffer indexData;
		char firstChar;

		// sort the keys
		SortedSet<String> keys = new TreeSet<String>(_compressedIndex.keySet());
		Iterator<String> keyIterator = keys.iterator();

		// loop through all the terms in the index
		while (keyIterator.hasNext()) {
			indexData = new StringBuffer();

			term = keyIterator.next();
			indexData.append(term);
			indexData.append(_termDoclistDelim);
			// append docIds and term count
			Map<Integer, Vector<Character>> docInfo = _compressedIndex.get(term);
			SortedSet<Integer> docIds = new TreeSet<Integer>(docInfo.keySet());
			for (Integer docId : docIds) {
				indexData.append(docId);
				indexData.append(_docCountDelim);
				for(int i = 0; i < docInfo.get(docId).size(); i++) {
					indexData.append(docInfo.get(docId).get(i));
				}
				indexData.append(_doclistDelim);
			}
			indexData.append("\n");

			firstChar = term.charAt(0);
			indexFileNameOrig = firstChar + ".idx";
			indexFileNameTmp = firstChar + "_tmp.idx";

			// Loop through all subsequent keys where the first character of the
			// term is same as that of the previous term
			while (keyIterator.hasNext()
					&& (term = keyIterator.next()).charAt(0) == firstChar) {
				indexData.append(term);
				indexData.append(_termDoclistDelim);
				// append docIds and term count
				docInfo = _compressedIndex.get(term);
				docIds = new TreeSet<Integer>(docInfo.keySet());
				for (Integer docId : docIds) {
					indexData.append(docId); // docid
					indexData.append(_docCountDelim);
					for(int i = 0; i < docInfo.get(docId).size(); i++) {
						indexData.append(docInfo.get(docId).get(i));
					}
					indexData.append(_doclistDelim);
				}
				indexData.append("\n");
			}

			// write info of all terms with the same first characters to the tmp file
			Utilities.writeToFile(_options._indexPrefix + "/"
					+ indexFileNameTmp, indexData.toString(), false);
			// merge old and new files. e.g. merge a.idx and a_tmp.idx -> a.idx
			mergeIndexFiles(indexFileNameOrig, indexFileNameTmp);
		}
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
  
	@Override
	public void loadIndex() {
		BufferedReader br;
		String line;
		
		// load doc info file
		System.out.println("Loading documents info from : " + _docInfoFile);

		try {
			br = new BufferedReader(new FileReader(_docInfoFile));
			
			String[] info;
			DocumentIndexed dIndexed;
			
			while ((line = br.readLine()) != null) {
				info = line.split(_docInfoDelim);
				
				int dId = Integer.parseInt(info[0]);
				dIndexed = new DocumentIndexed(dId);
				dIndexed.setUrl(info[1]);
				dIndexed.setTitle(info[2]);
				long totalWordsInDoc = Long.parseLong(info[3]);
				dIndexed.setTotalWords(totalWordsInDoc);
				_documents.add(dIndexed);
				_docIdUriMap.put(info[1], dId);
				_totalTermFrequency += totalWordsInDoc;
			}
			_numDocs = _documents.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Loading document info done ...");
	}
	
	private void testNextDoc(IndexerInvertedCompressed iic) {
		long start, end;

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String queryStr;

		try {
			while (!(queryStr = br.readLine()).equals("quit")) {
				QueryPhrase q = new QueryPhrase(queryStr);
				int totalResults = 0;
				start = System.currentTimeMillis();
				DocumentIndexed di = iic.nextDoc(q, -1);
				if (di == null) {
					System.out.println("No documents found !!!");
				}
				while (di != null) {
					totalResults++;
					System.out.println(di._docid + " - "
							+ iic._documents.get(di._docid).getTitle());
					di = iic.nextDoc(q, di._docid);
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

	public void loadIndex(Query query) {
		Set<Character> chars = new HashSet<Character>();
		query.processQuery();
		Vector<String> tokens = query._tokens;
		for(String token : tokens) {
			if(token.trim().length() != 0) {
				chars.add(token.charAt(0));
			}				
		}
		
		_compressedIndex = new LinkedHashMap<String, Map<Integer,Vector<Character>>>();
		for(Character c : chars) {
			loadIndex(String.valueOf(c).toLowerCase());
		}
	}

	/**
	 * Loads only the index file which may contain the input term
	 * @param term
	 */
	public void loadIndex(String term) {
				
		String indexFile = _options._indexPrefix + "/" + term.charAt(0) + ".idx";
		Vector<Character> positionsEncoded;
		  
		try {
			BufferedReader br = new BufferedReader(new FileReader(indexFile));
			String line = "";

			while ((line = br.readLine()) != null) {
				if (line.trim().length() != 0) {
					Scanner scanner = new Scanner(line);
					scanner.useDelimiter("[" + _termDoclistDelim + "\n]");

					String token = scanner.next();
					
					// create new map entry for current term
					Map<Integer, Vector<Character>> docInfoMap = new HashMap<Integer, Vector<Character>>();

					// build docInfoMap
					String[] docInfo = scanner.next().split(_doclistDelim);
					for (String doc : docInfo) {
						String[] doc_count = doc.split(_docCountDelim);
						
						if (docInfoMap.containsKey(Integer.valueOf(doc_count[0]))) {
							/* Do Nothing */
						} else {
							int docId = Integer.valueOf(doc_count[0]);
							String allPositions = doc_count[1];
							positionsEncoded = new Vector<Character>();
							if(allPositions != null && !allPositions.equals("")) {
								for(int i = 0; i < allPositions.length(); i++) {
									positionsEncoded.add(allPositions.charAt(i));
								}	
							}	
							docInfoMap.put(docId, positionsEncoded);
						}
					}

					_compressedIndex.put(token, docInfoMap);
					
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

	/**
	 * Searches for next document containing the term (which can be a phrase also)
	 * @param term
	 * @param docid
	 * @return
	 */
	private int nextDoc(String term, int docid) {
		
		CompressionUtility cu = new CompressionUtility();
		DecompressionUtility du = new DecompressionUtility();
		int previousOccurence = 0;
		int newOccurence = 0;
		String newOccurenceCoded = "";
		Vector<Character> positionsEncoded;
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

					Vector<Integer> decodedOccurrences = du.decodeByteAlign(_compressedIndex.get(qTerm).get(dIndexed._docid));
					int prev = 0;
					int current = 0;
					Vector<Integer> occurrencesList = new Vector<Integer>();
					for(int i = 0; i < decodedOccurrences.size(); i++) {
						current = decodedOccurrences.get(i) + prev;
						occurrencesList.add(current);
						prev = current;
					}
					
					occurrences[qTermNo++] = IndexerUtils.search(currentOccurrence, (Integer[]) occurrencesList.toArray(), false);
				}

				// loop for all the occurrences of phrase
				while (IndexerUtils.continueSearch(occurrences)) {

					// if a phrase is found, then add its info to the index
					if (IndexerUtils.isPhrase(occurrences)) {

						isPresent = true;

						// create the entry for the phrase, if not created
						// earlier
						if (!_compressedIndex.containsKey(term)) {
							_compressedIndex.put(term,
									new HashMap<Integer, Vector<Character>>());
						}

						// add phrase occurrences info to the index
						Map<Integer, Vector<Character>> docInfo = _compressedIndex.get(term);
						
					  	  if (docInfo.containsKey(dIndexed._docid)) {
					  		  
					  		  Vector<Integer> positionsDecoded = du.decodeByteAlign(docInfo.get(dIndexed._docid));
					  		  previousOccurence = positionsDecoded.get(positionsDecoded.size() - 1);
					  		  if((occurrences[0]-previousOccurence) > 0) {
					  			  newOccurence = occurrences[0] - previousOccurence;
					  			  newOccurenceCoded = cu.encodeByteAlign(newOccurence);
					  		  }
					  		positionsEncoded = new Vector<Character>();
					  		for(int i = 0; i < docInfo.get(dIndexed._docid).size(); i++) {
					  			positionsEncoded.add(docInfo.get(dIndexed._docid).get(i));
					  		}
					  		
					  		for(int i = 0; i < newOccurenceCoded.length(); i++) {
					  			positionsEncoded.add(newOccurenceCoded.charAt(i));
					  		}
					  		
					  		docInfo.put(dIndexed._docid,positionsEncoded);
							
						  } else {
							  positionsEncoded = new Vector<Character>();
							  newOccurenceCoded = cu.encodeByteAlign(occurrences[0]);
							  for(int i = 0; i < newOccurenceCoded.length(); i++) {
						  			positionsEncoded.add(newOccurenceCoded.charAt(i));
						  	  }
							  docInfo.put(dIndexed._docid,positionsEncoded);
						  }
					}

					// update search parameters for searching for next occurrences of phrase terms 
					int minOccurrence = Utilities.getMin(occurrences);

					if (minOccurrence == occurrences[0]) {
						currentOccurrence = occurrences[0];
					} else {
						currentOccurrence = occurrences[0] - 1;
					}
					
					// get occurrence of each phrase term
					qTermNo = 0;
					for (String qTerm : phraseTerms) {

						Vector<Integer> decodedOccurrences = du.decodeByteAlign(_compressedIndex.get(qTerm).get(dIndexed._docid));
						int prev = 0;
						int current = 0;
						Vector<Integer> occurrencesList = new Vector<Integer>();
						for(int i = 0; i < decodedOccurrences.size(); i++) {
							current = decodedOccurrences.get(i) + prev;
							occurrencesList.add(current);
							prev = current;
						}
						
						occurrences[qTermNo++] = IndexerUtils.search(currentOccurrence, (Integer[]) occurrencesList.toArray(), false);
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
			if (_compressedIndex.containsKey(term)) {
				Map<Integer, Vector<Character>> docInfo = _compressedIndex.get(term);
				docIdList = new Integer[docInfo.size()];

				Set<Integer> docListSorted = new TreeSet<Integer>(
						docInfo.keySet());
				docListSorted.toArray(docIdList);

				// perform search for nextdocid on this array
				nextDocId = IndexerUtils.search(docid, docIdList, true);
			}
		}

		return nextDocId;
	}
	
	@Override
	public DocumentIndexed getDoc(int docid) {
		return _documents.get(docid);
	}	
	
  @Override
  public int corpusDocFrequencyByTerm(String term) {
	  return _compressedIndex.containsKey(term) ? _compressedIndex.get(term).size() : 0;
  }

  @Override
	public int corpusTermFrequency(String term) {
		int corpusTermFreq = 0;
		DecompressionUtility du = new DecompressionUtility();
		Vector<Integer> positionsDecoded;
		if(_compressedIndex.containsKey(term)) {
			Map<Integer, Vector<Character>> docInfo = _compressedIndex.get(term);
			for(Integer docId : docInfo.keySet()) {
				positionsDecoded = du.decodeByteAlign(docInfo.get(docId));
				corpusTermFreq += positionsDecoded.size();
			}
		}
		
		return corpusTermFreq;
	}
  /**
   * @CS2580: Implement this for bonus points.
   */
	@Override
	public int documentTermFrequency(String term, String url) {
		DecompressionUtility du = new DecompressionUtility();
		Vector<Integer> positionsDecoded;
		if(_compressedIndex.containsKey(term)) {
			int docId = _docIdUriMap.get(url);
			positionsDecoded = du.decodeByteAlign(_compressedIndex.get(term).get(docId));
			return positionsDecoded.size();
		}
		return 0;
	}

}