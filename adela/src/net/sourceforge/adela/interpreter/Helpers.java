package net.sourceforge.adela.interpreter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

class Helpers {
	static int getLongestLine(String multilineString) {
		String[] lines = multilineString.split("\n");
		int length = 0;
		for (int i = 0; i < lines.length; i++) {
			int len = lines[i].length();
			if (len > length) {
				length = len;
			}
		}
		return length;
	}

	static String addSpaces(int numSpaces) {
		StringBuilder sb = new StringBuilder(numSpaces);
		for (int i = 0; i < numSpaces; i++) {
			sb.append(' ');
		}
		return sb.toString();
	}

	static String makeBlock(String multilineString) {
		int ll = getLongestLine(multilineString);
		String[] lines = multilineString.split("\n");
		StringBuilder sb = new StringBuilder(ll * lines.length);
		for (int i = 0; i < lines.length; i++) {
			int len = lines[i].length();
			sb.append(lines[i]);
			if (len < ll) {
				sb.append(addSpaces(ll - len));
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	static String joinTwo(String left, String right) {
		String[] leftBlock = makeBlock(left).split("\n");
		String[] rightBlock = makeBlock(right).split("\n");
		int thelen = leftBlock.length;
		if (thelen < rightBlock.length) {
			thelen = rightBlock.length;
		}
		int ll;
		if (leftBlock.length > 0) {
			ll = leftBlock[0].length();
		} else {
			ll = 0;
		}
		int rl;
		if (rightBlock.length > 0) {
			rl = rightBlock[0].length();
		} else {
			rl = 0;
		}
		StringBuilder sb = new StringBuilder((thelen * ll) + (thelen * rl));
		for (int i = 0; i < thelen; i++) {
			if (leftBlock.length <= i) {
				sb.append(addSpaces(ll));
			} else {
				sb.append(leftBlock[i]);
			}
			sb.append(" ");
			if (rightBlock.length <= i) {
				sb.append(addSpaces(rl));
			} else {
				sb.append(rightBlock[i]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	static String makeTree(String left, String middle, String right) {
		if ((left == null) && (right == null)) {
			return middle;
		}
		if (left == null) {
			left = "";
		}
		if (right == null) {
			right = "";
		}
		String[] leftBlock = makeBlock("\n" + left).split("\n");
		String[] rightBlock = makeBlock("\n" + right).split("\n");
		String[] mBlock = makeBlock(middle).split("\n");
		int thelen = leftBlock.length;
		if (thelen < rightBlock.length) {
			thelen = rightBlock.length;
		}
		int ll;
		if (leftBlock.length > 0) {
			ll = leftBlock[0].length();
		} else {
			ll = 0;
		}
		int rl;
		if (rightBlock.length > 0) {
			rl = rightBlock[0].length();
		} else {
			rl = 0;
		}
		int mlen = mBlock[0].length();
		int mlines = mBlock.length;
		StringBuilder sb = new StringBuilder((thelen * ll) + (thelen * rl)
				+ (thelen * (3 + mlen)));
		for (int i = 0; i < mlines; i++) {
			sb.append(addSpaces(ll) + " ");
			sb.append(mBlock[i]);
			sb.append(addSpaces(rl) + "\n");
		}
		for (int i = 0; i < thelen; i++) {
			if (leftBlock.length <= i) {
				sb.append(addSpaces(ll));
			} else {
				sb.append(leftBlock[i]);
			}
			if (i == 0) {
				if (ll != 0) {
					sb.append("/");
				} else {
					sb.append(" ");
				}
				sb.append(addSpaces(mlen));
				if (rl != 0) {
					sb.append("\\");
				} else {
					sb.append(" ");
				}
			} else {
				sb.append(" ");
				sb.append(addSpaces(mlen));
				sb.append(" ");
			}
			if (rightBlock.length <= i) {
				sb.append(addSpaces(rl));
			} else {
				sb.append(rightBlock[i]);
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	static String opToString(char operator) {
		switch (operator) {
		case 'l':
			return "<=";
		case 'g':
			return ">=";
		case 'e':
			return "==";
		case 'x':
			return ">>";
		case 'y':
			return "<<";
		case 'a':
			return "&&";
		case 'o':
			return "||";

		default:
			return String.valueOf(operator);
		}
	}

	public static void main(String[] args) {
		System.out.println(makeTree("a\nbc\ndef", "was\nferz",
				"was eeh\nbleed\nzeig"));
	}

	public static void writeString(DataOutput out, String s) throws IOException {
		out.writeUTF(s);
	}

	public static String readString(DataInput in) throws IOException {
		return in.readUTF();
	}
}
