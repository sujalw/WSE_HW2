package edu.nyu.cs.cs2580;

/**
 * @author ravi
 */
public class CompressionUtility {

	// For Testing purpose
	public static void main(String args[]) {
		CompressionUtility cu = new CompressionUtility();
		System.out.println(cu.encodeByteAlign(3172));
	}
	
	public String encodeByteAlign(int number) {
		CompressionUtility cu = new CompressionUtility();
		return cu.binaryToHex(cu.appendAdditionalZeroOrOne(cu.appendZeros(cu.convertToBinary(number))));
	}
	
	public String convertToBinary(int num) {
		return Integer.toBinaryString(num);
	}
	
	/**
	 * Assumption : Input to this function is a binary String consisting of 1's and 0's
	 * @param binaryNum
	 * @return appended String containing length as a multiple of 7
	 */
	public String appendZeros(String binaryNum) {
		int len = binaryNum.length();
		if(len % 7 == 0) {
			return binaryNum;
		}
		else {
			int remainingDiff = (len % 7);
			int diff = 7 - remainingDiff;
			StringBuffer sb = new StringBuffer();
			for(int i = 0; i < diff; i++) {
				sb.append(0);
			}
			sb.append(binaryNum);
			return sb.toString();
		}
	}
	
	/**
	 * Assumption : Input to this function is a binary String as a multiple of 7
	 * @param binaryNum
	 * @return Binary String having length of size as a multiple of 8
	 */
	public String appendAdditionalZeroOrOne(String binaryNum) {
		StringBuffer sb = new StringBuffer();
		int current = 0;
		while(current < binaryNum.length()) {
			if((binaryNum.length() - current) > 7) {
				sb.append(0);
			}
			else {
				sb.append(1);
			}
			for(int i = current; i < (current+7); i++) {
				sb.append(binaryNum.charAt(i));
			}
			current += 7;
		}
		return sb.toString();
	}
	
	public String binaryToHex(String binaryNum) {
		StringBuffer result = new StringBuffer();
		StringBuffer temp;
		int current = 0;
		while(current < binaryNum.length()) {
			temp = new StringBuffer();
			for(int i = current; i < (current+4); i++) {
				temp.append(binaryNum.charAt(i));
			}
			result.append(binaryBlockOf4ToHex(temp.toString()));
			current += 4;
		}
		return result.toString();
	}
	
	public static char binaryBlockOf4ToHex(String input) {
		int sum = 0;
		int first = (input.charAt(0) - 48) * 8;
		int second = (input.charAt(1) - 48) * 4;
		int third = (input.charAt(2) - 48) * 2;
		int fourth = (input.charAt(3) - 48);
		sum = first + second + third + fourth;
		if(sum == 10)
			return 'A';
		else if(sum == 11)
			return 'B';
		else if(sum == 12)
			return 'C';
		else if(sum == 13)
			return 'D';
		else if(sum == 14)
			return 'E';
		else if(sum == 15)
			return 'F';
		else
			return (char) (sum+48);
	}
}
