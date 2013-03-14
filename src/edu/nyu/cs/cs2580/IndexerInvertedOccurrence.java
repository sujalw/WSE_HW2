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

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedOccurrence extends Indexer {

	// The occurred index is build such that for every term, there exists a HashMap, with key as
	// docId and value as List of position of word occurrences.
	// There is no need to maintain the no. of occurrences as that can be retrieved as size of the list (if required). 	
	Map<String, Map<Integer, Vector<Integer>>> _occuredIndex = 
			new LinkedHashMap<String, Map<Integer,Vector<Integer>>>();

	String _docInfoFile = "docinfo.inf";
	// Maximum no. of files to process in memory at a time
	int _maxFiles = 500;

	final String _termDoclistDelim = ";";
	final String _docCountDelim = ":";
	final String _doclistDelim = " ";
	final String _docInfoDelim = ";";
	final String _wordOccurDelim = ",";

	StringBuffer docInfo = new StringBuffer();

	// Stores all Document (not body vectors) in memory.
	public Vector<DocumentIndexed> _documents = new Vector<DocumentIndexed>();
	public Map<String, Integer> _docIdUriMap = new HashMap<String, Integer>();

	public static void main(String[] args) {
		new IndexerInvertedOccurrence();
	}
	
	public IndexerInvertedOccurrence() {
		try {
			
			Options options = new Options("conf/engine.conf");
			IndexerInvertedOccurrence iio = new IndexerInvertedOccurrence(options);
			
			
			long start = System.currentTimeMillis();
			iio._docInfoFile = options._indexPrefix + "/" + _docInfoFile;
			//iio.constructIndex();
			// Added for Sujal
			//iio.phraseAndConjuctiveRetrieval();
			
			iio.loadIndex(); 
			long end = System.currentTimeMillis();
			System.out.println("time = " + (end - start));
			System.out.println("total docs loaded = " + iio._documents.size());
			
			testNextDoc(iio);
			//testPhraseAndConjuctiveRetrieval(iio);			 
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public IndexerInvertedOccurrence(Options options) {
		super(options);
		System.out.println("Using Indexer: " + this.getClass().getSimpleName());
		
		_docInfoFile = _options._indexPrefix + "/" + _docInfoFile;
	}
	
	  @Override
	  public void constructIndex() throws IOException {
		  // delete previously created index
		  //Utilities.deleteFilesInDir(_options._indexPrefix);

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
				  System.out.println("writing to : " + _docInfoFile);
				  // flush the in memory index
				  _occuredIndex = new LinkedHashMap<String, Map<Integer,Vector<Integer>>>();
				  docInfo = new StringBuffer();
			  }

			  _numDocs++;
		  }

		  // write last batch of info
		  writeIndexToFile();
		  Utilities.writeToFile(_docInfoFile, docInfo.toString(), true);
		}

	  private void processDocument(String content, Document doc, int docId) {

		  if (content == null || doc == null || docId < 0) {
			  return;
		  }
		  int pos = 0;
		  Vector<String> terms = Utilities.getStemmed(content);
		  Vector<Integer> wordOccurenceList; 
		  for (String t : terms) {
			  t = t.trim();
			  if (t.length() > 0) {
				  if (!_occuredIndex.containsKey(t)) {
					  _occuredIndex.put(t, new LinkedHashMap<Integer, Vector<Integer>>());
				  }

			  	  if (_occuredIndex.get(t).containsKey(docId)) {
			  		  wordOccurenceList =  _occuredIndex.get(t).get(docId);
			  		  wordOccurenceList.add(pos);
			  		  _occuredIndex.get(t).put(docId, wordOccurenceList);
			  		  
				  } else {
					  wordOccurenceList = new Vector<Integer>();
					  wordOccurenceList.add(pos);
					  _occuredIndex.get(t).put(docId, wordOccurenceList);
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
		  
		  int wordsInDoc = terms.size();

		  docInfo.append(docId);
		  docInfo.append(_docInfoDelim);		
		  docInfo.append(uri);
		  docInfo.append(_docInfoDelim);
		  docInfo.append(title);
		  docInfo.append(_docInfoDelim);
		  docInfo.append(wordsInDoc); // total words in the document
		  docInfo.append("\n");
	  }

	  private void writeIndexToFile() throws IOException {

			if (_occuredIndex == null) {
				return;
			}
			// Maintain 26+10 index files corresponding to 1st character in term (26
			// alphabets + 10 digits)
			String term;
			String indexFileNameOrig, indexFileNameTmp;
			StringBuffer indexData;
			char firstChar;

			// sort the keys
			SortedSet<String> keys = new TreeSet<String>(_occuredIndex.keySet());
			Iterator<String> keyIterator = keys.iterator();

			// loop through all the terms in the index
			while (keyIterator.hasNext()) {
				indexData = new StringBuffer();

				term = keyIterator.next();
				indexData.append(term);
				indexData.append(_termDoclistDelim);
				// append docIds and term count
				Map<Integer, Vector<Integer>> docInfo = _occuredIndex.get(term);
				SortedSet<Integer> docIds = new TreeSet<Integer>(docInfo.keySet());
				for (Integer docId : docIds) {
					indexData.append(docId);
					indexData.append(_docCountDelim);
					// appending till second last element and appending last outside the loop
					// so as to avoid extra comma at the end of the list.
					for(int i = 0; i < docInfo.get(docId).size()-1; i++) {
						indexData.append(docInfo.get(docId).get(i));
						indexData.append(_wordOccurDelim);
					}
					indexData.append(docInfo.get(docId).get(docInfo.get(docId).size()-1));
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
					docInfo = _occuredIndex.get(term);
					docIds = new TreeSet<Integer>(docInfo.keySet());
					for (Integer docId : docIds) {
						indexData.append(docId);
						indexData.append(_docCountDelim);
						// appending till second last element and appending last outside the loop
						// so as to avoid extra comma at the end of the list.
						for(int i = 0; i < docInfo.get(docId).size()-1; i++) {
							indexData.append(docInfo.get(docId).get(i));
							indexData.append(_wordOccurDelim);
						}
						indexData.append(docInfo.get(docId).get(docInfo.get(docId).size()-1));
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
				long totalWordsInDoc = Long.parseLong(info[3]); ///
				dIndexed.setTotalWords(totalWordsInDoc);
				_docIdUriMap.put(info[1], dId);
				_documents.add(dIndexed);
				
				_totalTermFrequency += totalWordsInDoc;
			}
			_numDocs = _documents.size();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Loading document info done ...");
	}
	
	private void testNextDoc(IndexerInvertedOccurrence iio) {
		long start, end;

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		String queryStr;

		try {
			while (!(queryStr = br.readLine()).equals("quit")) {
				QueryPhrase q = new QueryPhrase(queryStr);
				q.processQuery();
				int totalResults = 0;
				start = System.currentTimeMillis();
				DocumentIndexed di = iio.nextDoc(q, -1);
				if (di == null) {
					System.out.println("No documents found !!!");
				}
				while (di != null) {
					totalResults++;
					System.out.println(di._docid + " - "
							+ iio._documents.get(di._docid).getTitle());
					di = iio.nextDoc(q, di._docid);
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
			
			// remove duplicate terms in query
			//Set<String> queryProcessed = new TreeSet<String>(
			//Utilities.getStemmed(query._query));
					
			if(docid == -1) {
				// It means this is first call to nextDoc for given query.
				
				System.out.println("Searching ... ");
				
				// load necessary indices
				loadIndex(query);
				System.out.println("doc cnt for 'new' = " + _occuredIndex.get("new").size());
			}			

			int[] docIds = new int[query._tokens.size()];

			// perform conjunctive retrieval on each token which can be a phrase
			int qTokenNo = 0;
			int docId = -1;
			for (String qTerm : query._tokens) {
				docId = nextDoc(qTerm, docid);
				docIds[qTokenNo++] = docId;
				
				if(docId == -1) {
					break;
				}
			}

			// loop until docids for all the terms are same and is not -1
			while (!IndexerUtils.isSame(docIds)
					&& IndexerUtils.continueSearch(docIds)) {
				int newDocId = Utilities.getMax(docIds) - 1;
				docIds = new int[query._tokens.size()];

				qTokenNo = 0;
				for (String qTerm : query._tokens) {
					docId = nextDoc(qTerm, newDocId);
					docIds[qTokenNo++] = docId;
					
					if(docId == -1) {
						break;
					}
				}
			}

			// check if no docid is found
			if (!IndexerUtils.continueSearch(docIds)
					|| !IndexerUtils.isSame(docIds)) {
				return null;
			}

			// Return docid. At this point, all the entries in the array are same.
			//return new DocumentIndexed(docIds[0]);
			return _documents.get(docIds[0]);
		}

		public void loadIndex(Query query) {
			
			query.processQuery();
			SortedSet<String> qTerms = new TreeSet<String>();
			qTerms.addAll(Utilities.getStemmed(query._query));
			
			for(String qTerm : qTerms) {
				if(qTerm.trim().length() > 0) {
					if(! _occuredIndex.containsKey(qTerm)) {
						loadIndex(qTerm);
					}
				}
			}
		}

		/**
		 * Loads only the index file which may contain the input term
		 * @param term
		 */
		public void loadIndex(String term) {
			
			if(_occuredIndex.containsKey(term)) {
				return;
			}
					
			String indexFile = _options._indexPrefix + "/" + term.charAt(0) + ".idx";
			System.out.println("loading from : " + indexFile);
			Vector<Integer> wordOccurenceList;
			  
			try {
				BufferedReader br = new BufferedReader(new FileReader(indexFile));
				String line = "";

				while ((line = br.readLine()) != null) {	
					if (line.trim().length() != 0) {
						Scanner scanner = new Scanner(line);
						scanner.useDelimiter("[" + _termDoclistDelim + "\n]");

						String token = scanner.next();
						
						if(token.equals(term)) {
							// create new map entry for current term
							Map<Integer, Vector<Integer>> docInfoMap = new HashMap<Integer, Vector<Integer>>();

							// build docInfoMap
							String[] docInfo = scanner.next().split(_doclistDelim);
							for (String doc : docInfo) {
								String[] doc_count = doc.split(_docCountDelim);
								
								if (docInfoMap.containsKey(Integer.valueOf(doc_count[0]))) {
									/* Do Nothing */
								} else {
									int docId = Integer.valueOf(doc_count[0]);
									String allPositions = doc_count[1];
									wordOccurenceList = new Vector<Integer>();
									if(allPositions != null && !allPositions.equals("")) {
										String[] word_count = allPositions.split(_wordOccurDelim);
										for(int i = 0; i < word_count.length; i++) {
											wordOccurenceList.add(Integer.parseInt(word_count[i]));
										}	
									}	
									
									docInfoMap.put(docId, wordOccurenceList);
								}
							}

							_occuredIndex.put(token, docInfoMap);
							
							break;
						}						
					}
				}

				System.out.println("Loading done...");

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
					int occNo = -1;

					// get occurrence of each phrase term
					for (String qTerm : phraseTerms) {

						Set<Integer> occurrencesListSorted = new TreeSet<Integer>(
								_occuredIndex.get(qTerm).get(dIndexed._docid));
						Integer[] occurrencesList = new Integer[occurrencesListSorted.size()];
						occurrencesListSorted.toArray(occurrencesList);

						occNo = IndexerUtils.search(currentOccurrence, occurrencesList, true);
						occurrences[qTermNo++] = occNo;
						
						if(occNo == -1) {
							break;
						}
					}

					// loop for all the occurrences of phrase
					while (IndexerUtils.continueSearch(occurrences)) {

						// if a phrase is found, then add its info to the index
						if (IndexerUtils.isPhrase(occurrences)) {

							isPresent = true;

							// create the entry for the phrase, if not created
							// earlier
							if (!_occuredIndex.containsKey(term)) {
								_occuredIndex.put(term,
										new HashMap<Integer, Vector<Integer>>());
							}

							// add phrase occurrences info to the index
							Map<Integer, Vector<Integer>> docInfo = _occuredIndex.get(term);
							if (!docInfo.containsKey(dIndexed._docid)) {
								docInfo.put(dIndexed._docid, new Vector<Integer>());
							}
							docInfo.get(dIndexed._docid).add(occurrences[0]);
						}

						// update search parameters for searching for next occurrences of phrase terms 
						int minOccurrence = Utilities.getMin(occurrences);

						if (minOccurrence == occurrences[0]) {
							currentOccurrence = occurrences[0];
						} else {
							currentOccurrence = occurrences[0] - 1;
						}

						// get next occurrences of phrase terms
						qTermNo = 0;
						occNo = -1;
						for (String qTerm : phraseTerms) {

							Set<Integer> occurrencesListSorted = new TreeSet<Integer>(
									_occuredIndex.get(qTerm).get(
											dIndexed._docid));
							Integer[] occurrencesList = new Integer[occurrencesListSorted
									.size()];
							occurrencesListSorted.toArray(occurrencesList);

							occNo = IndexerUtils.search(
									currentOccurrence, occurrencesList, true);
							occurrences[qTermNo++] = occNo;
							
							if(occNo == -1) {
								break;
							}
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
				if (_occuredIndex.containsKey(term)) {
					Map<Integer, Vector<Integer>> docList = _occuredIndex.get(term);
					docIdList = new Integer[docList.size()];

					Set<Integer> docListSorted = new TreeSet<Integer>(
							docList.keySet());
					docListSorted.toArray(docIdList);

					// perform search for nextdocid on this array
					nextDocId = IndexerUtils.search(docid, docIdList, true);
				}
			}

			return nextDocId;
		}

		@SuppressWarnings("unused")	
	private void testPhraseAndConjuctiveRetrieval(IndexerInvertedOccurrence iio) {
		// String query = "the \"new york city\"";
		String query = "the \"new york city\"";
		QueryPhrase qPhrase = new QueryPhrase(query);
		qPhrase.processQuery();

		DocumentIndexed di = iio.nextDoc(qPhrase, -1);
		if (di == null) {
			System.out.println("No documents found !!!");
		}
		while (di != null) {
			System.out.println(di._docid);
			di = iio.nextDoc(qPhrase, di._docid);
		}
	}

	/*
	public void phraseAndConjuctiveRetrieval() {
		try {
			Options options = new Options("conf/engine.conf");
			String testFile = options._corpusPrefix + "/phrase_test";

			IndexerInvertedOccurrence iio = new IndexerInvertedOccurrence(options);

			// populate inverted index
			BufferedReader br = new BufferedReader(new FileReader(new File(testFile)));
			String line = "";
			int docId = 0;

			while ((line = br.readLine()) != null) {
				String[] terms = line.split(" ");
				for (int i = 0; i < terms.length; i++) {
					String termStemmed = Utilities.getStemmed(terms[i]).get(0);
					if (iio._occuredIndex.containsKey(termStemmed)) {
						Map<Integer, Vector<Integer>> docInfo = iio._occuredIndex.get(termStemmed);
						if (!docInfo.containsKey(docId)) {
							docInfo.put(docId, new Vector<Integer>());
						}
						docInfo.get(docId).add(i); // add occurrence
					} else {
						iio._occuredIndex.put(termStemmed,new HashMap<Integer, Vector<Integer>>());
						iio._occuredIndex.get(termStemmed).put(docId,new Vector<Integer>());
						iio._occuredIndex.get(termStemmed).get(docId).add(i);
					}
				}
				docId++;
			}
			br.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	
	@SuppressWarnings("unused")
	private void printIndex(IndexerInvertedOccurrence iio) {
		// print the info
		for (String term : iio._occuredIndex.keySet()) {
			System.out.print(term + ";");
			Map<Integer, Vector<Integer>> docInfo = iio._occuredIndex.get(term);
			for (Integer doc_id : docInfo.keySet()) {
				System.out.print(doc_id + ":");
				for (Integer occ : docInfo.get(doc_id)) {
					System.out.print(occ + ",");
				}

				System.out.print(" ");
			}

			System.out.println();
		}
	}

	@SuppressWarnings("unused")
	private void testRankerFavorite(IndexerInvertedOccurrence iio) {
		RankerFavorite rf = new RankerFavorite(iio._options, null, iio);		
		QueryPhrase q = new QueryPhrase("the \"new york\" city");

		// populate _documents
		DocumentIndexed dIndexed = new DocumentIndexed(0);
		dIndexed.setUrl("doc1");
		dIndexed.setTitle("My Studies");
		dIndexed.setTotalWords(15);
		iio._totalTermFrequency += 15;
		iio._documents.add(dIndexed);								
		iio._docIdUriMap.put("doc1", 0);

		dIndexed = new DocumentIndexed(1);
		dIndexed.setUrl("doc2");
		dIndexed.setTitle("NYC");
		dIndexed.setTotalWords(19);
		iio._totalTermFrequency += 19;
		iio._documents.add(dIndexed);								
		iio._docIdUriMap.put("doc2", 1);

		Vector<ScoredDocument> sd = rf.runQuery(q, 5);
		for(ScoredDocument d : sd) {
			System.out.println(d.getDocId() + " : " + d.get_score());
		}
	}
	
	@Override
	public DocumentIndexed getDoc(int docid) {
		return _documents.get(docid);
	}

	@Override
	public int corpusDocFrequencyByTerm(String term) {
		return _occuredIndex.containsKey(term) ? _occuredIndex.get(term)
				.size() : 0;
	}

	@Override
	public int corpusTermFrequency(String term) {
		int corpusTermFreq = 0;
		if(_occuredIndex.containsKey(term)) {
			Map<Integer, Vector<Integer>> docInfo = _occuredIndex.get(term);
			for(Integer docId : docInfo.keySet()) {
				corpusTermFreq += docInfo.get(docId).size();
			}
		}
		
		return corpusTermFreq;
	}

	@Override
	public int documentTermFrequency(String term, String url) {
		if(_occuredIndex.containsKey(term)) {
			int docId = _docIdUriMap.get(url);
			return _occuredIndex.get(term).get(docId).size();
		}

		return 0;
	}

}