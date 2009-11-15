/**
 * adela
 * An embeddable, secure scripting language
 * for java applications
 *
 * (C) 2007, Hans-Christian Esperer
 * hc at hcespererorg
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 * * Neither the name of the H. Ch. Esperer nor the names of his
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * POSSIBILITY OF SUCH DAMAGE
 **************************************************************************/

package net.sourceforge.adela.interpreter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import net.sourceforge.adela.enums.EValueType;
import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.exceptions.EvaluatorIsNullException;
import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.govm.GOVMWriter;
import net.sourceforge.adela.govm.Opcode;
import net.sourceforge.adela.govm.StatementResolver;
import net.sourceforge.adela.interfaces.IEvaluator;

/**
 * A parse tree containing the parsing instructions for one statement
 * 
 * @author hc
 */
public class ADELAParseTree implements Externalizable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3172093318940768196L;

	/**
	 * Left part
	 */
	private ADELAParseTree leftTree;

	/**
	 * Right part
	 */
	private ADELAParseTree rightTree;

	/**
	 * The operator to use
	 */
	private char operator;

	/**
	 * Statement to evaluate
	 */
	private String statement;

	/**
	 * Cached int
	 */
	private int intValue;

	/**
	 * Cached long
	 */
	private long longValue;

	/**
	 * cached double
	 */
	private double doubleValue;

	/**
	 * cached string
	 */
	private String stringValue;

	/**
	 * Function to call on evaluation
	 */
	private String functionToCall;

	/**
	 * Parameters to call function with
	 */
	private ADELAParseTree[] functionParameters;

	/**
	 * Type of value we're storing
	 */
	private EValueType valueType;

	/**
	 * True if the result of this parse tree should be returned if used in a
	 * function block
	 */
	private boolean returnStatement;

	/**
	 * A PRNG for random NOP insertion to prevent cheating.
	 */
	private Random prng = new Random();

	/**
	 * Instanciiate a leaf with an int value
	 * 
	 * @param value
	 *            int value
	 */
	protected ADELAParseTree(int value) {
		this.valueType = EValueType.intValue;
		this.intValue = value;
	}

	/**
	 * Instanciiate a leaf with a double value
	 * 
	 * @param value
	 *            double value
	 */
	protected ADELAParseTree(double value) {
		this.valueType = EValueType.floatValue;
		this.doubleValue = value;
	}

	/**
	 * Instanciiate a leaf with a long value
	 * 
	 * @param value
	 *            long value
	 */
	protected ADELAParseTree(long value) {
		this.valueType = EValueType.longValue;
		this.longValue = value;
	}

	/**
	 * Instanciiate a leaf that calls a function
	 * 
	 * @param functionToCall
	 *            name of function to call
	 * @param arguments
	 *            parse trees of arguments to pass to that function
	 */
	protected ADELAParseTree(String functionToCall, ADELAParseTree[] arguments) {
		this.functionToCall = functionToCall;
		this.functionParameters = arguments;
		this.valueType = EValueType.functioncall;
	}

	/**
	 * Instanciiate a leaf with a string
	 * 
	 * @param value
	 *            string to use
	 * @param evaluate
	 *            evaluate string?
	 */
	protected ADELAParseTree(String value, boolean evaluate) {
		if (evaluate) {
			this.statement = value;
			this.valueType = EValueType.statement;
		} else {
			this.valueType = EValueType.stringValue;
			this.stringValue = value;
		}
	}

	/**
	 * Instanciiate a node that performs an operation
	 * 
	 * @param leftTree
	 *            left sub-tree to evaluate
	 * @param rightTree
	 *            right sub-tree to evaluate
	 * @param operator
	 *            operator to use for operation
	 */
	protected ADELAParseTree(ADELAParseTree leftTree, ADELAParseTree rightTree,
			char operator) {
		this.leftTree = leftTree;
		this.rightTree = rightTree;
		this.operator = operator;
		this.valueType = EValueType.operation;
	}

	/**
	 * Instanciiate a node that calls a method
	 * 
	 * @param leftTree
	 *            tree that gives us the class to call into
	 * @param method
	 *            method to call
	 * @throws ADELASyntaxException
	 */
	private ADELAParseTree(ADELAParseTree leftTree, String method)
			throws ADELASyntaxException {
		this.leftTree = leftTree;
		this.operator = '.';
		this.handleFunction(method);
		this.valueType = EValueType.operation;
	}

	public ADELAParseTree() {
	}

	/**
	 * Evaluate this subtree
	 * 
	 * @param locals
	 *            map of local variables to use for evaluation
	 * @param evaluator
	 *            evaluator to use
	 * @return evaluation result
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 * @throws ADELAException
	 */
	public Object doEvaluate(Map<String, Object> locals, IEvaluator evaluator)
			throws WrongParameterCountException, FunctionNotFoundException,
			ADELAException {
		if (evaluator == null) {
			throw new EvaluatorIsNullException();
		}
		return evaluate(locals, evaluator);
	}

	/**
	 * @param locals
	 * @param evaluator
	 * @return
	 * @throws WrongParameterCountException
	 * @throws FunctionNotFoundException
	 * @throws ADELAException
	 */
	@SuppressWarnings("unchecked")
	private Object evaluate(Map<String, Object> locals, IEvaluator evaluator)
			throws WrongParameterCountException, FunctionNotFoundException,
			ADELAException {
		switch (valueType) {
		case floatValue:
			return Double.valueOf(doubleValue);
		case intValue:
			return Integer.valueOf(intValue);
		case longValue:
			return Long.valueOf(longValue);
		case stringValue:
		case variable:
			return stringValue;
		case statement:
			return evaluator.evaluate(locals, statement);
		case functioncall:
			return evaluator.callFunction(null, this.functionToCall, locals,
					evaluateParameters(this.functionParameters, locals,
							evaluator));
		default:
			;
		}

		Object leftValue = leftTree.evaluate(locals, evaluator);
		Object rightValue;
		if (rightTree != null) {
			if (operator != '.') {
				if (operator == 'o') {
					if (((Boolean) leftValue) == false) {
						rightValue = rightTree.evaluate(locals, evaluator);
					} else {
						rightValue = null;
					}
				} else if (operator == 'a') {
					if (((Boolean) leftValue) == true) {
						rightValue = rightTree.evaluate(locals, evaluator);
					} else {
						rightValue = null;
					}
				} else {
					rightValue = rightTree.evaluate(locals, evaluator);
				}
			} else {
				rightValue = null;
			}
		} else {
			rightValue = null;
		}

		Comparable cLeft, cRight;
		switch (operator) {
		case '+': {
			if (leftValue.getClass() == Integer.class) {
				return (Integer) leftValue + (Integer) rightValue;
			} else if (leftValue.getClass() == Double.class) {
				return (Double) leftValue + (Double) rightValue;
			} else if (leftValue.getClass() == String.class) {
				return (String) leftValue + (String) rightValue;
			} else if (leftValue.getClass() == Long.class) {
				return (Long) leftValue + (Long) rightValue;
			}
		}
			// substraction
		case '-':
			if (leftValue.getClass() == Integer.class) {
				return (Integer) leftValue - (Integer) rightValue;
			} else if (leftValue.getClass() == Double.class) {
				return (Double) leftValue - (Double) rightValue;
			} else if (leftValue.getClass() == Long.class) {
				return (Long) leftValue - (Long) rightValue;
			}
			// division
		case '/':
			if (leftValue.getClass() == Integer.class) {
				return (Integer) leftValue / (Integer) rightValue;
			} else if (leftValue.getClass() == Double.class) {
				return (Double) leftValue / (Double) rightValue;
			} else if (leftValue.getClass() == Long.class) {
				return (Long) leftValue / (Long) rightValue;
			}
			// multiplication
		case '*':
			if (leftValue.getClass() == Integer.class) {
				return (Integer) leftValue * (Integer) rightValue;
			} else if (leftValue.getClass() == Double.class) {
				return (Double) leftValue * (Double) rightValue;
			} else if (leftValue.getClass() == Long.class) {
				return (Long) leftValue * (Long) rightValue;
			}
			// power
		case '^':
			return Math.pow((Double) leftValue, (Double) rightValue);
			// shift right
		case 'x':
			return ((Integer) leftValue) >> ((Integer) rightValue);
			// shift left
		case 'y':
			return ((Integer) leftValue) << ((Integer) rightValue);
			// comparision: smaller than
		case '<':
			cLeft = (Comparable) leftValue;
			cRight = (Comparable) rightValue;
			return cLeft.compareTo(cRight) < 0;
			// comparision: greater than
		case '>':
			cLeft = (Comparable) leftValue;
			cRight = (Comparable) rightValue;
			return cLeft.compareTo(cRight) > 0;
		case 'l':
			cLeft = (Comparable) leftValue;
			cRight = (Comparable) rightValue;
			return (cLeft.compareTo(cRight) < 0)
					|| (leftValue.equals(rightValue));
		case 'g':
			cLeft = (Comparable) leftValue;
			cRight = (Comparable) rightValue;
			return (cLeft.compareTo(cRight) > 0)
					|| (leftValue.equals(rightValue));
		case '=':
			// assignment
			locals.put((String) leftValue, rightValue);
			break;
		// comparision: equals
		case 'e':
			return leftValue.equals(rightValue);
			// equals not
		case 'n':
			return !leftValue.equals(rightValue);
			// and
		case 'a':
			return ((Boolean) leftValue && (Boolean) rightValue);
			// or
		case 'o':
			return ((Boolean) leftValue || (Boolean) rightValue);
			// someone screwed up
		case '&':
			return ((Integer) leftValue & (Integer) rightValue);
			// or
		case '|':
			return ((Integer) leftValue | (Integer) rightValue);
			// method invocatoin
		case '.':
			return evaluator.callFunction(leftValue, this.functionToCall,
					locals, evaluateParameters(this.functionParameters, locals,
							evaluator));
			// someone screwed up
		default:
			System.out.format("Operator %c unknown\n", operator);
		}
		return null;
	}

	private Object[] evaluateParameters(ADELAParseTree[] functionParameters,
			Map<String, Object> locals, IEvaluator evaluator)
			throws WrongParameterCountException, FunctionNotFoundException,
			ADELAException {
		Object[] params = new Object[functionParameters.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = functionParameters[i].doEvaluate(locals, evaluator);
		}
		return params;
	}

	/**
	 * The Parser.
	 * 
	 * @param s
	 *            String to parse
	 * @return Parsing result
	 * @throws ADELASyntaxException
	 * @throws ADELAException
	 *             On shit
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 */
	public static ADELAParseTree Parse(String s) throws ADELASyntaxException {
		char elem;
		char operator;
		int i;
		int iLen;
		int iBrackets;
		Boolean bFound;
		String sLeft;
		String sRight;
		ADELAParseTree leftTree;
		ADELAParseTree rightTree;

		boolean bNegate = false;

		if (s == null) {
			return null;
		}

		if (s.length() == 0) {
			return null;
		}

		/*
		 * trim us
		 */
		s = s.trim();

		/*
		 * our length
		 */

		iLen = s.length();

		/*
		 * Empty string; return null
		 */
		if (iLen == 0) {
			return null;
		}

		if (s.startsWith("return ")) {
			ADELAParseTree returnTree = Parse(s.substring(7));
			returnTree.returnStatement = true;
			return returnTree;
		}

		/*
		 * special cases
		 */
		// string? pass to evaluate
		if (s.charAt(0) == '"') {
			return PreEvaluate(s, false);
		}
		// negation? mark it, then strip the negation sign
		else if (s.charAt(0) == '-') {
			bNegate = true;
			s = s.substring(1);
			iLen--;
			if (iLen < 1)
				throw new RuntimeException("Error: invalid expression!");
		}

		/*
		 * Find the first operator that does not reside within brackets
		 */
		bFound = false;
		iBrackets = 0;
		for (i = 0; i < iLen; i++) {
			elem = s.charAt(i);

			switch (elem) {
			case '*':
			case '/':
			case '+':
			case '^':
			case '-':
			case '<':
			case '>':
			case '&':
			case '|':
			case '=':
			case ':':
			case '!':
				if (iBrackets == 0) {
					bFound = true;
				}

				break;
			case '.':
				if ((s.charAt(i + 1) < '0') || (s.charAt(i + 1) > '9')) {
					if (iBrackets == 0) {
						bFound = true;
					}
				}
				break;
			case '(':
			case '[':
				iBrackets++;
				break;
			case ')':
			case ']':
				iBrackets--;
				break;
			}

			if (bFound)
				break;
		}

		/*
		 * Equal number of opening and closing brackets!?
		 */
		assert (iBrackets == 0);

		/*
		 * Get the part left of the operator
		 */
		sLeft = s.substring(0, i).trim();

		// hack:
		// if we have _one_ equals sign ('='), the
		// left part is a string, never mind the
		// fact it isn't quoted ('"')
		if (bFound && ((s.length() - i) >= 2) && (s.charAt(i) == '=')
				&& (s.charAt(i + 1) != '=')) {
			leftTree = new ADELAParseTree(sLeft, false);
			leftTree.valueType = EValueType.variable;
		} else {
			leftTree = PreEvaluate(sLeft, bNegate);
		}

		/*
		 * We have a right part
		 */
		if (bFound) {
			operator = s.charAt(i);
			switch (operator) {
			case '&':
				if (s.charAt(i + 1) == '&') {
					operator = 'a';
					i++;
				}
				break;

			case '|':
				if (s.charAt(i + 1) == '|') {
					operator = 'o';
					i++;
				}
				break;

			case '=':
				if (s.charAt(i + 1) == '=') {
					i++;
					operator = 'e';
				}
				break;

			case '<':
				if (s.charAt(i + 1) == '=') {
					i++;
					operator = 'l';
				} else if (s.charAt(i + 1) == '<') {
					i++;
					operator = 'y';
				}
				break;

			case '>':
				if (s.charAt(i + 1) == '=') {
					i++;
					operator = 'g';
				} else if (s.charAt(i + 1) == '>') {
					i++;
					operator = 'x';
				}
				break;

			case '!':
				if (s.charAt(i + 1) == '=') {
					i++;
					operator = 'n';
				}
				break;

			default:
				;
			}

			// get the right term
			sRight = s.substring(i + 1);

			if (operator == '.') {
				// function call
				Object[] methodCallParts = getMethodCallParts(sRight);
				if (methodCallParts.length == 3) {

					rightTree = Parse((String) methodCallParts[2]);
				} else {
					rightTree = null;
				}
				String method = (String) methodCallParts[0];
				Iterator<String> methods = getMethods(method).iterator();
				while (methods.hasNext()) {
					leftTree = new ADELAParseTree(leftTree, methods.next());
				}

				if (rightTree != null) {
					return new ADELAParseTree(leftTree, rightTree,
							(Character) methodCallParts[1]);
				}
				return leftTree;
			} else {
				// parse it
				rightTree = Parse(sRight);

				return new ADELAParseTree(leftTree, rightTree, operator);
			}
		}

		return leftTree;
	}

	private static ArrayList<String> getMethods(String s) {
		char elem;
		int i;
		int iLen;
		int iBrackets;
		Boolean bFound;
		ArrayList<String> methods = new ArrayList<String>();

		if (s == null) {
			return null;
		}
		if (s.length() == 0) {
			return null;
		}

		bFound = true;
		while (bFound) {
			s = s.trim();
			iLen = s.length();

			/*
			 * Find the first operator that does not reside within brackets
			 */
			bFound = false;
			iBrackets = 0;
			for (i = 0; i < iLen; i++) {
				elem = s.charAt(i);

				switch (elem) {
				case '.':

					if (iBrackets == 0) {
						methods.add(s.substring(0, i));
						s = s.substring(i + 1);
						bFound = true;
					}

					break;
				case '(':
					iBrackets++;
					break;
				case ')':
					iBrackets--;
					break;
				}
				if (bFound) {
					break;
				}
			}

		}

		s = s.trim();
		if (s.length() != 0) {
			methods.add(s);
		}
		return methods;
	}

	private static Object[] getMethodCallParts(String s) {
		char elem;
		char operator;
		int i;
		int iLen;
		int iBrackets;
		Boolean bFound;

		if (s == null) {
			return null;
		}
		if (s.length() == 0) {
			return null;
		}

		s = s.trim();
		iLen = s.length();

		/*
		 * Find the first operator that does not reside within brackets
		 */
		bFound = false;
		iBrackets = 0;
		for (i = 0; i < iLen; i++) {
			elem = s.charAt(i);

			switch (elem) {
			case '*':
			case '/':
			case '+':
			case '-':
			case '^':
			case '<':
			case '>':
			case '&':
			case '|':
			case '=':
			case ':':
			case '!':
				if (iBrackets == 0) {
					bFound = true;
				}

				break;
			case '(':
				iBrackets++;
				break;
			case ')':
				iBrackets--;
				break;
			}

			if (bFound)
				break;
		}

		/*
		 * Equal number of opening and closing brackets!?
		 */
		assert (iBrackets == 0);

		if (bFound) {
			Object[] res = new Object[3];
			res[0] = s.substring(0, i);
			operator = s.charAt(i);
			if (operator == '>') {
				if (s.charAt(i + 1) == '=') {
					operator = 'g';
					i++;
				}
			}
			if (operator == '<') {
				if (s.charAt(i + 1) == '=') {
					operator = 'l';
					i++;
				}
			}
			if (operator == '=') {
				if (s.charAt(i + 1) == '=') {
					operator = 'e';
					i++;
				}
			}
			if (operator == '!') {
				if (s.charAt(i + 1) == '=') {
					operator = 'n';
					i++;
				}
			}
			if (operator == '&') {
				if (s.charAt(i + 1) == '&') {
					operator = 'a';
					i++;
				}
			}
			if (operator == '|') {
				if (s.charAt(i + 1) == '|') {
					operator = 'o';
					i++;
				}
			}
			res[1] = Character.valueOf(operator);
			res[2] = s.substring(i + 1);
			return res;
		} else {
			Object[] res = new Object[1];
			res[0] = s;
			return res;
		}
	}

	/**
	 * Evaulate a function string
	 * 
	 * @param s
	 *            String to evaluate
	 * @param locals
	 *            Local variables
	 * @param negate
	 *            negate value?
	 * @return Evaluation result
	 * @throws ADELASyntaxException
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 */
	private static ADELAParseTree PreEvaluate(String s, boolean negate)
			throws ADELASyntaxException {
		int iPos;
		char cAT;
		String sFuncName;
		Vector<String> sParams;
		Iterator<String> iParams;
		ADELAParseTree[] vParams;

		// are we zero-length?
		if (s.length() == 0) {
			// return null
			return null;
		}

		/*
		 * Determine the type of term by looking at the very first character
		 */
		cAT = s.charAt(0);
		/*
		 * Are we a bracket? Find the last bracket, then strip them and parse
		 * the rest
		 */
		if (cAT == '(') {
			iPos = s.lastIndexOf(')');
			assert (iPos != -1);

			return Parse(s.substring(1, iPos));
		}
		/*
		 * Are we a string? Return a string class
		 */
		else if (cAT == '"') {
			iPos = s.lastIndexOf('"');
			assert (iPos != -1);

			return new ADELAParseTree(s.substring(1, iPos), false);
		}

		/*
		 * Are we a number?
		 */
		else if ((cAT >= '0') && (cAT <= '9')) {
			/*
			 * Int or Double? The '.' says it all...
			 */
			if (s.indexOf('.') == -1) {
				String trimmedS = s.trim();
				if (trimmedS.endsWith("L")) {
					if (!negate) {
						return new ADELAParseTree(Long.parseLong(trimmedS
								.substring(0, trimmedS.length() - 1)));
					} else {
						return new ADELAParseTree(Long.valueOf(0 - Long
								.parseLong(trimmedS.substring(0, trimmedS
										.length() - 1))));
					}
				} else {
					if (!negate) {
						return new ADELAParseTree(Integer.parseInt(trimmedS
								.trim()));
					} else {
						return new ADELAParseTree(Integer.valueOf(0 - Integer
								.parseInt(trimmedS.trim())));
					}
				}
			} else {
				if (!negate) {
					return new ADELAParseTree(Double.parseDouble(s.trim()));
				} else {
					return new ADELAParseTree(Double.valueOf(0 - Double
							.parseDouble(s.trim())));
				}
			}
		}
		/*
		 * We're neither. We must be a function or a variable
		 */
		else {
			iPos = s.indexOf('(');
			sFuncName = s;
			/*
			 * function?
			 */
			if (iPos != -1) {
				/*
				 * yes, collect parameters, then jump into the callback
				 * interface
				 */
				sFuncName = s.substring(0, iPos);
				s = s.substring(iPos);

				iPos = s.lastIndexOf(')');
				if (iPos == -1) {
					throw new ADELASyntaxException("Invalid function call: "
							+ s);
				}

				// sParams = s.substring(1, iPos).split(",");
				sParams = GetParams(s.substring(1, iPos));
				iParams = sParams.iterator();
				vParams = new ADELAParseTree[sParams.size()];

				for (int i = 0; i < vParams.length; i++) {
					vParams[i] = Parse(iParams.next());
				}

				return new ADELAParseTree(sFuncName, vParams);
				// return CallFunction(sFuncName, locals, vParams);
			} else {
				/*
				 * Variable, get our value
				 */
				// if (!mVariables.containsKey(sFuncName.trim()))
				String sVarName = sFuncName.trim();
				return new ADELAParseTree(sVarName, true);
			}
		}
	}

	/**
	 * Get function call parameters
	 * 
	 * @param s
	 *            parameter string, params must be separated by commas.
	 * @return param string array
	 */
	public static Vector<String> GetParams(String s) {
		int i;
		boolean bInString;
		int iBR;
		boolean bFound;
		Vector<String> sParams;

		sParams = new Vector<String>();

		// iterate linewise
		while (s.length() != 0) {
			bInString = false;
			iBR = 0;
			bFound = false;
			for (i = 0; i < s.length(); i++) {
				switch (s.charAt(i)) {
				case '"':
					bInString = !bInString;
					break;
				case '(':
				case '{':
					iBR++;
					break;
				case ')':
				case '}':
					iBR--;
					break;
				case ',':
					if ((!bInString) && (iBR == 0)) {
						sParams.add(s.substring(0, i));
						s = s.substring(i + 1);
						bFound = true;
					}
					break;
				default:
					break;
				}

				if (bFound)
					break;
			}

			if (!bFound) {
				sParams.add(s);
				s = "";
			}
		}

		// return the result
		return sParams;
	}

	public void compile(GOVMWriter gw, StatementResolver sr,
			PrintStream logStream) throws ADELACompilerException {
		if (prng.nextBoolean()) {
			gw.writeOpcode(Opcode.NOP);
		}
		switch (this.valueType) {
		case floatValue:
			break;
		case functioncall:
			if (this.leftTree == null) {
				// syscall
				if ("putc".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 1);
					gw.writeOpcode(Opcode.SYSCALL);
					gw.writeOpcode(Opcode.POP);
					gw.writeOpcode(Opcode.POP);
				} else if ("open".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					this.functionParameters[1].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 5);
					gw.writeOpcode(Opcode.SYSCALL);
				} else if ("close".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 6);
					gw.writeOpcode(Opcode.SYSCALL);
				} else if ("fputc".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					this.functionParameters[1].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 8);
					gw.writeOpcode(Opcode.SYSCALL);
				} else if ("fgetc".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 7);
					gw.writeOpcode(Opcode.SYSCALL);
				} else if ("halt".equals(this.functionToCall)) {
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 0);
					gw.writeOpcode(Opcode.SYSCALL);
				} else if ("getc".equals(this.functionToCall)) {
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 2);
					gw.writeOpcode(Opcode.SYSCALL);
					gw.writeOpcode(Opcode.ROT);
					gw.writeOpcode(Opcode.POP);
				} else if ("info".equals(this.functionToCall)) {
					gw.writeOpcode(Opcode.LI);
					gw.writeShort((short) 3);
					gw.writeOpcode(Opcode.SYSCALL);
					gw.writeOpcode(Opcode.POP);
				} else if ("peekw".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.LW);
					gw.writeOpcode(Opcode.ROT);
					gw.writeOpcode(Opcode.POP);
				} else if ("peekb".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.LB);
					gw.writeOpcode(Opcode.ROT);
					gw.writeOpcode(Opcode.POP);
				} else if ("pokew".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					this.functionParameters[1].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.SW);
					gw.writeOpcode(Opcode.POP);
					gw.writeOpcode(Opcode.POP);
				} else if ("pokeb".equals(this.functionToCall)) {
					this.functionParameters[0].compile(gw, sr, logStream);
					this.functionParameters[1].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.SB);
					gw.writeOpcode(Opcode.POP);
					gw.writeOpcode(Opcode.POP);
				} else if ("JMPABSOLUTE".equals(this.functionToCall)) {
					logStream
							.println("WARNING: using JMPABSOLUTE, which is undocumented.");
					this.functionParameters[0].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.JMP);
				} else if ("CALLABSOLUTE".equals(this.functionToCall)) {
					logStream
							.println("WARNING: using CALLABSOLUTE, which is undocumented.");
					this.functionParameters[0].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.CALL);
				} else if ("POKES".equals(this.functionToCall)) {
					logStream
							.println("WARNING: using POKES, which is undocumented.");
					this.functionParameters[0].compile(gw, sr, logStream);
					this.functionParameters[1].compile(gw, sr, logStream);
					gw.writeOpcode(Opcode.SWS);
				} else if ("POP".equals(this.functionToCall)) {
					logStream
							.println("WARNING: using POP, which is undocumented.");
					gw.writeOpcode(Opcode.POP);
				} else {
					for (ADELAParseTree apt : this.functionParameters) {
						apt.compile(gw, sr, logStream);
					}
					gw.setGoto("__FUNCTION__" + this.functionToCall,
							Opcode.CALL);
					// Save retval
					gw.writeOpcode(Opcode.MOVA);
					// Remove parameters from stack
					for (int i = 0; i < this.functionParameters.length; i++) {
						gw.writeOpcode(Opcode.POP);
					}
					// Push retval back on the stack
					gw.writeOpcode(Opcode.AMOV);
				}
			}
			break;
		case intValue:
			gw.writeOpcode(Opcode.LI);
			gw.writeShort((short) this.intValue);
			break;
		case longValue:
			break;
		case operation:
			this.leftTree.compile(gw, sr, logStream);
			this.rightTree.compile(gw, sr, logStream);
			switch (this.operator) {
			case '+':
				gw.writeOpcode(Opcode.ADD);
				break;
			case '-':
				gw.writeOpcode(Opcode.SUB);
				break;
			case '=': {
				String[] varParts = leftTree.stringValue.split("[]\\[]");
				String varName = varParts[0];
				if (sr.globorloc(varName) < 0) {
					gw.writeOpcode(Opcode.SW);
				} else {
					gw.writeOpcode(Opcode.ROT);
					gw.writeOpcode(Opcode.FMOV);
					gw.writeOpcode(Opcode.ADD);
					gw.writeOpcode(Opcode.ROT);
					gw.writeOpcode(Opcode.SWS);
				}
				gw.writeOpcode(Opcode.POP);
				gw.writeOpcode(Opcode.POP);
			}
				break;
			case 'e':
				gw.writeOpcode(Opcode.EQU);
				break;
			case 'n':
				gw.writeOpcode(Opcode.EQU);
				gw.writeOpcode(Opcode.NOT);
				break;
			case 'l':
				gw.writeOpcode(Opcode.LOE);
				break;
			case 'g':
				gw.writeOpcode(Opcode.GOE);
				break;
			case '<':
				gw.writeOpcode(Opcode.LT);
				break;
			case '^':
				gw.writeOpcode(Opcode.XOR);
				break;
			case '>':
				gw.writeOpcode(Opcode.GT);
				break;
			case '*':
				gw.writeOpcode(Opcode.MUL);
				break;
			case '/':
				gw.writeOpcode(Opcode.DIV);
				break;
			case 'y':
				gw.writeOpcode(Opcode.SHL);
				break;
			case 'x':
				gw.writeOpcode(Opcode.SHR);
				break;
			case '&':
				gw.writeOpcode(Opcode.AND);
				break;
			case '|':
				gw.writeOpcode(Opcode.OR);
				break;
			}
			break;
		case statement:
		case variable:
			/*
			 * A variable.
			 * 
			 * Reserves or resolves the symbol on the (initialized) data
			 * segment. It emits a load instant opcode to push the address onto
			 * the stack. In theory, not all symbols have to point to the
			 * initialized part of the data segment, but this allows for simpler
			 * code :-) And, it's for a ctf service only, anyway
			 */
			gw.writeOpcode(Opcode.LI);
			String[] varParts = ((this.valueType == EValueType.statement) ? this.statement
					: this.stringValue).split("[]\\[]");
			String varName = varParts[0];
			int addr = sr.globorloc(varName);
			if (addr < 0) {
				gw.writeShort((short) (-addr));
			} else {
				gw.writeShort((short) addr);
			}
			if (varParts.length > 1) {
				ADELAParseTree valOffset;
				try {
					valOffset = ADELAParseTree.Parse(varParts[1]);
				} catch (ADELASyntaxException e) {
					throw new ADELACompilerException(e);/* ugly */
				}
				valOffset.compile(gw, sr, logStream);
				gw.writeOpcode(Opcode.ADD);
			}
			if (this.valueType == EValueType.statement) {
				/*
				 * A statement. Do an LW here. The alternative is to do a SW
				 * later.
				 */
				if (addr < 0) {
					gw.writeOpcode(Opcode.LW);
				} else {
					// PUSH BP
					gw.writeOpcode(Opcode.FMOV);
					gw.writeOpcode(Opcode.ADD);
					gw.writeOpcode(Opcode.LWS);
				}
				gw.writeOpcode(Opcode.ROT);
				gw.writeOpcode(Opcode.POP);
			}
			break;
		case stringValue:
			byte[] bytes;
			try {
				bytes = (this.stringValue + "\0").getBytes("utf-8");
			} catch (UnsupportedEncodingException e) {
				throw new ADELACompilerException(e);
			}
			if (bytes.length > 65530) {
				throw new ADELACompilerException(
						"WTF!? Who needs strings of that length? No one will ever need string constants longer than 65530 bytes!");
			}
			int addrpos = sr.reserveBytes(null, bytes);
			logStream.println("String constant \"" + this.stringValue + "\" ("
					+ bytes.length + " bytes) at " + addrpos);
			gw.writeOpcode(Opcode.LI);
			gw.writeShort((short) addrpos);
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		switch (this.valueType) {
		case floatValue:
			return Double.valueOf(this.doubleValue).toString();
		case intValue:
			return Integer.valueOf(this.intValue).toString();
		case longValue:
			return Long.valueOf(this.longValue).toString();
		case functioncall:
			String parameters = "";
			for (int i = 0; i < this.functionParameters.length; i++) {
				if (parameters.length() != 0) {
					parameters = parameters + ", ";
				}
				parameters = parameters + this.functionParameters[i].toString();
			}
			return this.functionToCall + "(" + parameters + ")";
		case operation:
			if (this.functionToCall == null) {
				return "(" + this.leftTree.toString()
						+ Helpers.opToString(this.operator)
						+ this.rightTree.toString() + ")";
			}
			if (this.rightTree != null) {
				return "" + this.leftTree.toString()
						+ Helpers.opToString(this.operator) + this.stringValue
						+ this.rightTree.toString() + "";
			} else {
				String params = "(";
				for (int i = 0; i < this.functionParameters.length; i++) {
					if (params.length() > 1) {
						params = params + "; ";
					}
					params = params + this.functionParameters[i].toString();
				}
				params = params + ")";
				return "" + this.leftTree.toString()
						+ Helpers.opToString(this.operator)
						+ this.functionToCall + params + "";
			}
		case statement:
			return this.statement;
		case stringValue:
			return "\"" + this.stringValue + "\"";
		case variable:
			return this.stringValue + ":";
		}
		return null;
	}

	/**
	 * Like toString, but tries to draw an ASCII-art tree ;-)
	 * 
	 * @return ascii art tree
	 */
	public String toStringNonFlat() {
		String leftPart = null, rightPart = null;
		String mePart;
		switch (this.valueType) {
		case floatValue:
			mePart = Double.valueOf(this.doubleValue).toString();
			break;
		case intValue:
			mePart = Integer.valueOf(this.intValue).toString();
			break;
		case longValue:
			mePart = Long.valueOf(this.longValue).toString();
			break;
		case functioncall:
			String parameters = "";
			for (int i = 0; i < this.functionParameters.length; i++) {
				if (parameters.length() != 0) {
					parameters = Helpers.joinTwo(parameters, ", ");
				}
				parameters = Helpers.joinTwo(parameters,
						this.functionParameters[i].toStringNonFlat());
			}
			mePart = Helpers.joinTwo(Helpers.joinTwo(this.functionToCall + "(",
					parameters), ")");
			break;
		case operation:
			if (this.functionToCall == null) {
				mePart = Helpers.opToString(this.operator);
				leftPart = "\n" + this.leftTree.toStringNonFlat();
				rightPart = "\n" + this.rightTree.toStringNonFlat();
				break;
			}
			if (this.rightTree != null) {
				leftPart = "\n" + this.leftTree.toStringNonFlat();
				mePart = this.operator + this.stringValue;
				rightPart = "\n" + this.rightTree.toStringNonFlat();
				break;
			} else {
				String params = "(";
				for (int i = 0; i < this.functionParameters.length; i++) {
					if (params.length() > 1) {
						params = Helpers.joinTwo(params, ", ");
					}
					params = Helpers.joinTwo(params, this.functionParameters[i]
							.toStringNonFlat());
				}
				leftPart = "\n" + this.leftTree.toStringNonFlat();
				mePart = ".";
				rightPart = Helpers.joinTwo(Helpers.joinTwo(
						this.functionToCall, params), ")");
				break;
			}
		case statement:
			mePart = this.statement;
			break;
		case stringValue:
			mePart = "\"" + this.stringValue + "\"";
			break;
		case variable:
			mePart = this.stringValue;
			break;
		default:
			mePart = "";
		}

		return Helpers.makeTree(leftPart, mePart, rightPart);
	}

	public boolean isReturnStatement() {
		return returnStatement;
	}

	public void saveTree(DataOutput out) throws IOException {
		out.writeInt(this.valueType.ordinal());
		out.writeBoolean(this.returnStatement);
		switch (this.valueType) {
		case floatValue:
			out.writeDouble(this.doubleValue);
			break;
		case intValue:
			out.writeInt(this.intValue);
			break;
		case longValue:
			out.writeLong(this.longValue);
			break;
		case stringValue:
			Helpers.writeString(out, this.stringValue);
			break;
		case functioncall:
			Helpers.writeString(out, this.functionToCall);
			out.writeInt(functionParameters.length);
			for (int i = 0; i < functionParameters.length; i++) {
				functionParameters[i].saveTree(out);
			}
			break;
		case operation:
			leftTree.saveTree(out);
			out.writeChar(this.operator);
			if (this.operator == '.') {
				// method calls are special
				Helpers.writeString(out, this.functionToCall);
				out.writeInt(functionParameters.length);
				for (int i = 0; i < functionParameters.length; i++) {
					functionParameters[i].saveTree(out);
				}
			} else {
				rightTree.saveTree(out);
			}
			break;
		case statement:
			Helpers.writeString(out, this.statement);
			break;
		}
	}

	public static ADELAParseTree loadTree(DataInput in) throws IOException {
		ADELAParseTree tree = new ADELAParseTree();
		loadTree(in, tree);
		return tree;
	}

	private static void loadTree(DataInput in, ADELAParseTree tree)
			throws IOException {
		tree.valueType = EValueType.values()[in.readInt()];
		tree.returnStatement = in.readBoolean();
		switch (tree.valueType) {
		case floatValue:
			tree.doubleValue = in.readDouble();
			break;
		case intValue:
			tree.intValue = in.readInt();
			break;
		case longValue:
			tree.longValue = in.readLong();
			break;
		case stringValue:
			tree.stringValue = Helpers.readString(in);
			break;
		case functioncall:
			tree.functionToCall = Helpers.readString(in);
			tree.functionParameters = new ADELAParseTree[in.readInt()];
			for (int i = 0; i < tree.functionParameters.length; i++) {
				tree.functionParameters[i] = loadTree(in);
			}
			break;
		case operation:
			tree.leftTree = loadTree(in);
			tree.operator = in.readChar();
			if (tree.operator == '.') {
				// method calls are special
				tree.functionToCall = Helpers.readString(in);
				tree.functionParameters = new ADELAParseTree[in.readInt()];
				for (int i = 0; i < tree.functionParameters.length; i++) {
					tree.functionParameters[i] = loadTree(in);
				}
			} else {
				tree.rightTree = loadTree(in);
			}
			break;

		case statement:
			tree.statement = Helpers.readString(in);
			break;

		}
	}

	private void handleFunction(String s) throws ADELASyntaxException {
		int iPos;

		iPos = s.indexOf('(');
		/*
		 * function?
		 */
		if (iPos != -1) {
			/*
			 * yes, collect parameters, then jump into the callback interface
			 */
			this.functionToCall = s.substring(0, iPos);
			s = s.substring(iPos);

			iPos = s.lastIndexOf(')');
			if (iPos == -1) {
				throw new ADELASyntaxException("Invalid function call: " + s);
			}

			// sParams = s.substring(1, iPos).split(",");
			Vector<String> sParams = GetParams(s.substring(1, iPos));
			Iterator<String> iParams = sParams.iterator();
			this.functionParameters = new ADELAParseTree[sParams.size()];

			for (int i = 0; i < this.functionParameters.length; i++) {
				String nextParameter = iParams.next();
				this.functionParameters[i] = Parse(nextParameter);
			}
		} else {
			this.functionParameters = new ADELAParseTree[0];
			this.functionToCall = "get" + s;
		}
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		loadTree(in, this);
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		saveTree(out);
	}
}
