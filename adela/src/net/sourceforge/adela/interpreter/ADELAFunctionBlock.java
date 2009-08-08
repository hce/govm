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

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.govm.Opcode;
import net.sourceforge.adela.interfaces.IEvaluator;
import net.sourceforge.adela.interfaces.IPreInput;
import net.sourceforge.adela.tests.GOVMCodeGenerator;

/**
 * An if block: condition -and- statements. Whoa!
 * 
 * @author hc
 * 
 */
public class ADELAFunctionBlock implements IPreInput, Serializable {
	/**
	 * Our UID
	 */
	private static final long serialVersionUID = 2765022589236284943L;

	/**
	 * IF sCondition is TRUE,
	 */
	private ADELAParseTree condition;

	/**
	 * do sStatements
	 */
	private ADELAParseTree[] statements;

	/**
	 * Sub ifs
	 */
	protected List<ADELAFunctionBlock> vSubIfs;

	/**
	 * Are we a while loop?
	 */
	protected boolean bWhile;

	/**
	 * Elif blocks
	 */
	private List<ADELAFunctionBlock> elifBlocks;

	/**
	 * What to execute if condition ain't met
	 */
	private ADELAFunctionBlock elseIfBlock;

	/**
	 * List of parameter names
	 */
	private String[] paramNames;

	/**
	 * Are we a for loop?
	 */
	private boolean bFor;

	/**
	 * If we're a for loop, what's the name of the iterating variable?
	 */
	private String iteratorVariable;

	/**
	 * If we're a for loop, what's the statement to iterate over?
	 */
	private ADELAParseTree iterateOverStatement;

	/**
	 * for code generation
	 */
	private LinkedList<String[]> localVars = new LinkedList<String[]>();

	/**
	 * 
	 */
	public final String labelID;

	/**
	 * initialize us
	 * 
	 * @throws UnsupportedEncodingException
	 * 
	 */
	ADELAFunctionBlock() {
		this.condition = null;
		this.statements = null;
		this.bWhile = false;
		this.vSubIfs = new LinkedList<ADELAFunctionBlock>();
		this.elifBlocks = new LinkedList<ADELAFunctionBlock>();
		this.labelID = genLabel();
	}

	/**
	 * initialize with params
	 * 
	 * @param sCondition
	 *            When to execute sStatements or null if always
	 * @param sStatements
	 *            Statements to execute when sCondition is met or is null
	 * @throws ADELASyntaxException
	 * @throws UnsupportedEncodingException
	 */
	ADELAFunctionBlock(String sCondition, String sStatements)
			throws ADELASyntaxException {
		this.labelID = genLabel();
		this.condition = ADELAParseTree.Parse(sCondition);
		String[] statements = sStatements.split("\n");
		int i = 0;
		this.statements = new ADELAParseTree[statements.length];
		for (String statement : statements) {
			if (statement.startsWith("local ")) {
				String[] parts = statement.split("[] :\\[]");
				if ("uint".equals(parts[2])) {
					if (parts.length > 3) {
						throw new ADELASyntaxException(
								"uint arrays are not supported");
					}
				} else if ("byte".equals(parts[2])) {
					if (parts.length < 4) {
						throw new ADELASyntaxException("[size] expected");
					}
				} else {
					throw new ADELASyntaxException(
							":uint or :byte[size] expected");
				}

				this.localVars.add(parts);
			} else {
				this.statements[i++] = ADELAParseTree.Parse(statement);
			}
		}
		this.bWhile = false;
		this.vSubIfs = new LinkedList<ADELAFunctionBlock>();
		this.elifBlocks = new LinkedList<ADELAFunctionBlock>();
	}

	private String genLabel() {
		char[] chars = new char[32];
		Random r = new Random();
		for (int i = 0; i < 32; i++) {
			if (r.nextBoolean()) {
				chars[i] = (char) ('A' + (char) r.nextInt(26));
			} else {
				chars[i] = (char) ('a' + (char) r.nextInt(26));
			}
		}
		return new String(chars);
	}

	void SetCondition(String condition) throws ADELASyntaxException {
		if (bFor) {
			String[] forParts = condition.split(" in ", 2);
			if (forParts.length != 2) {
				throw new ADELASyntaxException("Invalid for loop: " + condition);
			}
			if (forParts[0].split("[A-Za-z]+[A-Za-z0-9]*").length != 0) {
				throw new ADELASyntaxException("Invalid iterator variable: "
						+ forParts[0]);
			}
			this.iteratorVariable = forParts[0];
			this.iterateOverStatement = ADELAParseTree.Parse(forParts[1]);
		} else {
			this.condition = ADELAParseTree.Parse(condition);
		}
	}

	public void AddPreInput(ADELAFunctionBlock ib) {
		vSubIfs.add(ib);
	}

	/**
	 * Execute if-block.
	 * 
	 * @param eParse
	 *            Parser to use for execution
	 * @param parameters
	 *            Parameters to function
	 * @return return value of script function
	 * @throws ADELAException
	 * @throws ADELAException
	 *             On parsing errors
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 */
	public Object Handle(IEvaluator evaluator, Object[] parameters)
			throws ADELAException, WrongParameterCountException,
			FunctionNotFoundException {
		Map<String, Object> localVars = new HashMap<String, Object>();
		return Handle(evaluator, parameters, localVars);
	}

	public Object Handle(IEvaluator evaluator, Object[] parameters,
			Map<String, Object> localVars) throws ADELAException,
			WrongParameterCountException, FunctionNotFoundException {

		List<Object> retvals = new LinkedList<Object>();

		if ((this.paramNames == null) && (parameters != null)
				&& (parameters.length != 0)) {
			throw new ADELAException("Function expects 0 parameters. "
					+ parameters.length + " given!");
		}
		if (((parameters != null) && (this.paramNames != null))
				&& (parameters.length != this.paramNames.length)) {
			throw new ADELAException("Function expects "
					+ this.paramNames.length + " parameters. "
					+ parameters.length + " given!");
		}

		if (this.paramNames != null) {
			if (parameters == null) {
				throw new ADELAException("Function expects "
						+ this.paramNames.length + " parameters; 0 given!");
			}
			for (int i = 0; i < this.paramNames.length; i++) {
				localVars.put(this.paramNames[i], parameters[i]);
			}
		}

		Handle(evaluator, retvals, localVars);

		if (retvals.size() != 0) {
			return retvals.get(0);
		} else {
			return null;
		}
	}

	/**
	 * Execute if-block.
	 * 
	 * @param eParse
	 *            Parser to use for execution
	 * @param returnVals
	 *            Reference to _empty_ LinkedList to fill with return value or
	 *            NULL if return values can be ignored
	 * @return true if if-block was executed, false if an elif or else block was
	 *         executed
	 * @throws ADELAException
	 *             On parsing errors
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 */
	@SuppressWarnings("unchecked")
	protected boolean Handle(IEvaluator evaluator, List<Object> returnVals,
			Map<String, Object> localVars) throws ADELAException,
			WrongParameterCountException, FunctionNotFoundException {
		Boolean bRes;
		int i;
		Iterator<ADELAFunctionBlock> iIFs;
		ADELAFunctionBlock subIf;

		if (returnVals == null) {
			returnVals = new LinkedList<Object>();
		}

		if (this.bFor) {
			if (this.iterateOverStatement == null) {
				return false;
			}
			Object iterateOver = this.iterateOverStatement.doEvaluate(
					localVars, evaluator);
			Iterator iterator = null;
			Object[] array = null;
			if (iterateOver.getClass().isArray()) {
				// iterate over array
				array = (Object[]) iterateOver;
			} else {
				// iterate over list
				try {
					List list = (List) iterateOver;
					iterator = list.iterator();
				} catch (ClassCastException e) {
					try {
						Map map = (Map) iterateOver;
						iterator = map.keySet().iterator();
					} catch (ClassCastException e2) {
					}
				}
			}
			if (array != null) {
				for (int iArray = 0; iArray < array.length; iArray++) {
					localVars.put(this.iteratorVariable, array[iArray]);
					if (this.statements != null) {
						for (i = 0; i < this.statements.length; i++) {
							if (statements[i].isReturnStatement()) {
								Object retval = statements[i].doEvaluate(
										localVars, evaluator);
								if (returnVals != null) {
									returnVals.add(retval);
								}
								return true;
							} else {
								statements[i].doEvaluate(localVars, evaluator);
							}
						}
					}
					iIFs = this.vSubIfs.iterator();
					while (iIFs.hasNext()) {
						subIf = iIFs.next();
						subIf.Handle(evaluator, returnVals, localVars);
						if (returnVals.size() != 0) {
							return true;
						}
					}
				}
				return true;
			}
			if (iterator != null) {
				while (iterator.hasNext()) {
					localVars.put(this.iteratorVariable, iterator.next());
					if (this.statements != null) {
						for (i = 0; i < this.statements.length; i++) {
							if (statements[i].isReturnStatement()) {
								Object retval = statements[i].doEvaluate(
										localVars, evaluator);
								if (returnVals != null) {
									returnVals.add(retval);
								}
								return true;
							} else {
								statements[i].doEvaluate(localVars, evaluator);
							}
						}
					}
					iIFs = this.vSubIfs.iterator();
					while (iIFs.hasNext()) {
						subIf = iIFs.next();
						subIf.Handle(evaluator, returnVals, localVars);
						if (returnVals.size() != 0) {
							return true;
						}
					}
				}
				return true;
			}

			return false;
		} else {
			do {
				// do we have conditions?
				if (condition == null) {
					// nope, always execute the block
					bRes = true;
				} else {
					// yep, check them
					try {
						bRes = (Boolean) condition.doEvaluate(localVars,
								evaluator);
					} catch (ClassCastException e) {
						bRes = false;
					}
				}
				// conditions met?
				if (bRes) {
					// yep, execute statements
					if (statements != null) {
						for (i = 0; i < statements.length; i++) {
							// hack
							if (statements[i].isReturnStatement()) {
								Object retval = statements[i].doEvaluate(
										localVars, evaluator);
								if (returnVals != null) {
									returnVals.add(retval);
								}
								return true;
							} else {
								statements[i].doEvaluate(localVars, evaluator);
							}
						}
					}

					iIFs = this.vSubIfs.iterator();
					while (iIFs.hasNext()) {
						subIf = iIFs.next();
						subIf.Handle(evaluator, returnVals, localVars);
						if (returnVals.size() != 0) {
							return true;
						}
					}
				}
			} while (this.bWhile && bRes);

			if ((!this.bWhile) && (!bRes)) {
				Iterator<ADELAFunctionBlock> elifBlocks = this.elifBlocks
						.iterator();
				bRes = false;
				while (elifBlocks.hasNext() && (!bRes)) {
					ADELAFunctionBlock elifBlock = elifBlocks.next();
					bRes = elifBlock.Handle(evaluator, returnVals, localVars);
					if (returnVals.size() != 0) {
						return true;
					}
				}
				if (!bRes) {
					if (this.elseIfBlock != null) {
						this.elseIfBlock.Handle(evaluator, returnVals,
								localVars);
						if (returnVals.size() != 0) {
							return true;
						}
					}
				}
				return false;
			} else {
				return true;
			}
		}
	}

	void SetWhile(boolean b) {
		this.bWhile = b;
	}

	void SetElseIfBlock(ADELAFunctionBlock myElseBlock) {
		this.elseIfBlock = myElseBlock;
	}

	void AddElifBlock(ADELAFunctionBlock myElifBlock) {
		this.elifBlocks.add(myElifBlock);
	}

	void setParamerets(String[] funcParams) {
		this.paramNames = funcParams;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(condition);
		out.writeObject(statements);
		out.writeObject(vSubIfs);
		out.writeBoolean(bWhile);
		out.writeObject(elifBlocks);
		out.writeObject(elseIfBlock);
		out.writeObject(paramNames);
		out.writeBoolean(bFor);
		if (bFor) {
			out.writeObject(this.iteratorVariable);
			out.writeObject(this.iterateOverStatement);
		}
	}

	@SuppressWarnings("unchecked")
	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		condition = (ADELAParseTree) in.readObject();
		statements = (ADELAParseTree[]) in.readObject();
		vSubIfs = (List<ADELAFunctionBlock>) in.readObject();
		bWhile = in.readBoolean();
		elifBlocks = (List<ADELAFunctionBlock>) in.readObject();
		elseIfBlock = (ADELAFunctionBlock) in.readObject();
		paramNames = (String[]) in.readObject();
		bFor = in.readBoolean();
		if (bFor) {
			this.iteratorVariable = (String) in.readObject();
			this.iterateOverStatement = (ADELAParseTree) in.readObject();
		}
	}

	public void SetFor(boolean b) {
		bFor = b;
	}

	public void generateGOVMCode(GOVMCodeGenerator gcc)
			throws ADELACompilerException, ADELASyntaxException {
		if (this.paramNames == null) {
			gcc.beginFunction(new String[0]);
		} else {
			gcc.beginFunction(this.paramNames);
		}

		for (String[] var : this.vSubIfs.get(0).localVars) {
			if ("uint".equals(var[2])) {
				gcc.declareShort(var[1]);
			} else if ("byte".equals(var[2])) {
				gcc.declareArray(var[1], Integer.valueOf(var[3]));
			}
		}
		gcc.reserveStack();
		generateGOVMCode(gcc, false);

		gcc.endFunction();
	}

	private int generateGOVMCode(GOVMCodeGenerator gcc, boolean genHead)
			throws ADELACompilerException, ADELASyntaxException {
		gcc.setLabel(this.labelID + "__start");
		if (this.condition != null) {
			gcc.writeStatement(this.condition);
			gcc.setGoto(this.labelID + "__end", Opcode.JZ);
		}
		if (this.statements != null) {
			for (ADELAParseTree apt : this.statements) {
				if (apt != null) {
					gcc.writeStatement(apt);
					if (apt.isReturnStatement()) {
						gcc.endFunctionWithRetval();
					}
				}
			}
		}
		for (ADELAFunctionBlock afb : this.vSubIfs) {
			afb.generateGOVMCode(gcc, false);
		}
		if (this.bWhile) {
			gcc.setGoto(this.labelID + "__start", Opcode.JMP);
		}
		gcc.setLabel(this.labelID + "__end");
		return gcc.getCurIP();
	}

}
