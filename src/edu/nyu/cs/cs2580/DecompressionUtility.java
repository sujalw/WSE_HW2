package edu.nyu.cs.cs2580;

import java.util.ArrayList;
import java.util.Vector;

/**
 * @author ravi
 */
public class DecompressionUtility {

	public static void main(String args[]) {
		DecompressionUtility du = new DecompressionUtility();
		//System.out.println(du.hexToBinaryBlockOf4('E'));
		Vector<Character> l = new Vector<Character>();
		l.add('2');
		l.add('7');
		l.add('8');
		l.add('6');
		l.add('0');
		l.add('2');
		l.add('D');
		l.add('0');
		l.add('2');
		l.add('B');
		l.add('F');
		l.add('3');
		printUtility(du.decodeByteAlign(l));
	}
	
	public static void printUtility(Vector<Integer> input) {
		for(int i = 0; i < input.size(); i++) {
			System.out.println(input.get(i));
		}
	}
	
	public Vector<Integer> decodeByteAlign(Vector<Character> encodedList) {
		
		//System.out.println("in decodebytealign .....");
		
		if(encodedList == null || encodedList.size() == 0)
			return null;

		Vector<Integer> listOfDecimalNos = new Vector<Integer>();
		StringBuffer sb;
		int i = 0;
		int pos = 0;
		String binaryBlock1 = "";
		String binaryBlock2 = "";
		boolean endingBlock;
		while((i+1) < (encodedList.size())) {
			endingBlock = false;
			sb = new StringBuffer();
			pos = i;
			while(!endingBlock && (pos+1) < (encodedList.size())) {
				binaryBlock1 = hexToBinaryBlockOf4(encodedList.get(pos));
				binaryBlock2 = hexToBinaryBlockOf4(encodedList.get(pos+1));
				if(binaryBlock1.charAt(0) == '1') {
					endingBlock = true;
				}
				sb.append(binaryBlock1.substring(1, 4));
				sb.append(binaryBlock2);
				pos += 2;
			}
			int decimalNo = Integer.parseInt(sb.toString(),2);
			listOfDecimalNos.add(decimalNo);
			i = pos;
		}
		
		return listOfDecimalNos;
	}
	
	public String hexToBinaryBlockOf4(char hexChar) {
		if(hexChar == '0')
			return "0000";
		else if(hexChar == '1')
			return "0001";
		else if(hexChar == '2')
			return "0010";
		else if(hexChar == '3')
			return "0011";
		else if(hexChar == '4')
			return "0100";
		else if(hexChar == '5')
			return "0101";
		else if(hexChar == '6')
			return "0110";
		else if(hexChar == '7')
			return "0111";
		else if(hexChar == '8')
			return "1000";
		else if(hexChar == '9')
			return "1001";
		else if(hexChar == 'A' || hexChar == 'a')
			return "1010";
		else if(hexChar == 'B' || hexChar == 'b')
			return "1011";
		else if(hexChar == 'C' || hexChar == 'c')
			return "1100";
		else if(hexChar == 'D' || hexChar == 'd')
			return "1101";
		else if(hexChar == 'E' || hexChar == 'e')
			return "1110";
		else if(hexChar == 'F' || hexChar == 'f')
			return "1111";
		else
			return "";
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
