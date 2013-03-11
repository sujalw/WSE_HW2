package edu.nyu.cs.cs2580;

public class IndexerUtils {
	
	public static int search(int current, Integer[] list, boolean galloping) {

		if (list == null || list.length == 0
				|| list[list.length - 1] < current) {
			return -1;
		}

		if (list[0] > current) {
			return list[0];
		}

		int low = 0, high = 0;
		int jump = 1;

		if (galloping) {
			// Through galloping, find a slot for binary search
			while ((high < list.length) && list[high] <= current) {

				low = high;
				// increase step size
				jump = jump << 1;
				high += jump;
			}

			if (high > (list.length - 1)) {
				high = list.length - 1;
			}
		} else {
			high = list.length - 1;
		}

		return binarySearch(list, low, high, current);
	}
	
	/*
	 * Perform binary search over the given list to find a number > current.
	 * Returns -1 if no such number is found
	 */
	public static int binarySearch(Integer[] list, int begin, int end, int current) {

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
	
	/**
	 * 
	 * @param docIds
	 * @return true if all numbers in the given list are same. Else returns
	 *         false
	 */
	public static boolean isSame(int[] docIds) {

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
	public static boolean continueSearch(int[] docIds) {

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

	public static boolean isPhrase(int[] occurrences) {
		// if successive occurrences are in sequence, then they are the occurrences of a phrase
		boolean phrase = true;
		
		for(int i=1 ; i<occurrences.length ; i++) {
			if((occurrences[i] - occurrences[0]) != i) {
				phrase = false;
				break; 
			}
		}

		return phrase;
	}	
}
