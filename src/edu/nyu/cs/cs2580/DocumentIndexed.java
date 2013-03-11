package edu.nyu.cs.cs2580;

import java.io.Serializable;

/**
 * @CS2580: implement this class for HW2 to incorporate any additional
 *          information needed for your favorite ranker.
 */
public class DocumentIndexed extends Document implements Serializable {
	private static final long serialVersionUID = 9184892508124423115L;

	private long _totalWords = 0;
	
	

	public DocumentIndexed(int docid) {
		super(docid);
	}

	public long getTotalWords() {
		return _totalWords;
	}

	public void setTotalWords(long totalWords) {
		this._totalWords = totalWords;
	}
}
