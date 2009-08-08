/**
 * adela
 * An embeddable, secure scripting language
 * for java applications
 * 
 * Copyright (c) 2007, Hans-Christian Esperer
 * hc at hcesperer org
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

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.adela.enums.EBlockType;
import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.interfaces.IPreInput;

/**
 * This class creates an automat based on an ADL description file.
 * 
 * @author hc
 * 
 */
public class ADELAParser {
	/**
	 * block types
	 */
	protected Map<String, EBlockType> hBTypes;

	/**
	 * where to get subautomats from
	 */
	protected String sSubAutomatPath;

	/**
	 * Initialize global variables
	 * 
	 */
	public ADELAParser() {
		hBTypes = new HashMap<String, EBlockType>();

		/*
		 * initialize known block in a hash table for faster lookup
		 */
		hBTypes.put("def", EBlockType.def);
		hBTypes.put("if", EBlockType.block_if);
		hBTypes.put("elif", EBlockType.block_elif);
		hBTypes.put("else", EBlockType.block_else);
		hBTypes.put("while", EBlockType.block_while);
		hBTypes.put("for", EBlockType.block_for);

		sSubAutomatPath = "";
	}

	/**
	 * Set subautomat load path
	 * 
	 * @param s
	 *            subautomat load path
	 */
	public void SetSubautomatPath(String s) {
		if (s.length() == 0)
			this.sSubAutomatPath = "";
		else
			this.sSubAutomatPath = s + "/";
	}

	/**
	 * deindent a block one step
	 * 
	 * @param s
	 *            block to deindent
	 * @return one step deindendet block
	 */
	public static String StripOneTab(String s) {
		return s.replace("\n\t", "\n");
	}

	/**
	 * strip trans blocks in the top level (not indended)
	 * 
	 * @param s
	 *            block to strip
	 * @return block with stripped top-level trans statements
	 */
	public static String StripTopLevelTranss(String s) {
		int i;
		String lines[];
		String line;
		lines = s.split("\n");

		// iterate linewise
		s = "";
		for (i = 0; i < lines.length; i++) {
			line = lines[i];
			// are we a trans block? are we top-level? no? add us!
			if ((line.charAt(0) == '\t') || (!line.startsWith("trans ")))
				s = s + lines[i] + "\n";
		}

		// return the result
		return s;
	}

	/**
	 * Get rid of empty lines
	 * 
	 * @param s
	 *            block
	 * @return block without empty lines
	 */
	public static String StripEmptyLines(String s) {
		int i;
		String lines[];
		String line;
		lines = s.split("\n");

		// iterate linewise
		s = "";
		for (i = 0; i < lines.length; i++) {
			line = lines[i].trim();

			if (line.trim().length() != 0) {
				// empty line? no? add it!
				if (!line.endsWith("\t"))
					s = s + lines[i] + "\n";
			}
		}

		// return the result
		return s;
	}

	/**
	 * Who needs comments? This function gets rid of them
	 * 
	 * @param s
	 *            block with comments
	 * @return block without comments
	 */
	public static String StripComments(String s) {
		int i, j;
		boolean bInString;
		boolean bFound;
		int iPos;
		String lines[];
		lines = s.split("\n");

		// iterate linewise
		s = "";
		for (i = 0; i < lines.length; i++) {
			// got a non-zero length line
			if (lines[i].length() > 0) {
				bInString = false;
				bFound = false;
				iPos = 0;
				for (j = 0; j < lines[i].length(); j++) {
					switch (lines[i].charAt(j)) {
					case '"':
						bInString = !bInString;
						break;
					case '#':
						if (!bInString) {
							bFound = true;
							iPos = j;
						}
						break;
					default:
						break;
					}

					if (bFound)
						break;
				}
				if (bFound)
					s = s + lines[i].substring(0, iPos) + "\n";
				else
					s = s + lines[i] + "\n";
			}
		}

		// return the result
		return s;
	}

	/**
	 * Builds the automat
	 * 
	 * @param str
	 *            ADL language buffer
	 * @param baseAutomat
	 *            Parent automat
	 * @throws ADELASyntaxException
	 * @throws FSMException
	 *             Things can go wrong, after all...
	 */
	public Map<String, ADELAFunctionBlock> BuildAutomat(String str)
			throws ADELASyntaxException {
		String sParts[], sParams[];
		String sLine;
		int pos;
		int iLen;
		EBlockType eType;
		HashMap<String, ADELAFunctionBlock> blocks = new HashMap<String, ADELAFunctionBlock>();
		ADELAFunctionBlock ifBlock;

		// rid us of empty lines and comments
		str = StripEmptyLines(StripComments(str));

		// we have something to process
		while (str.length() > 0) {
			pos = 0;
			iLen = str.length();
			/*
			 * get one full block
			 */
			do {
				pos = str.indexOf("\n", pos + 1);
				sLine = str.substring(pos + 1);
			} while ((pos != -1) && (iLen > (pos + 1))
					&& (str.charAt(pos + 1) == '\t'));

			/*
			 * found the last block
			 */
			if (pos == -1) {
				sLine = str;
				str = "";
			}
			/*
			 * found one block
			 */
			else {
				sLine = str.substring(0, pos);
				str = str.substring(pos + 1);
			}

			/*
			 * get the block parts
			 */
			sParts = sLine.split(" ", 2);
			if (sParts.length >= 2) {
				sParams = sParts[1].split(":\n", 2);
				if (sParams.length == 2) {
					sParams[1] = "\n" + sParams[1];
				}
			} else {
				sParams = "".split(" ");
			}

			/*
			 * we found a statement
			 */
			if ((sParts.length != 2) || (sParams.length != 2)) {
				throw new ADELASyntaxException("Illegal statement: " + sLine);
			} else {
				/*
				 * we're dealing with a subblock. Do we know the type?
				 */
				if (hBTypes.containsKey(sParts[0])) {
					/*
					 * yes, we do. Which one is it?
					 */
					eType = hBTypes.get(sParts[0]);
					switch (eType) {
					/*
					 * Okay, we found a state block. Find out its name, then
					 * create a sub-automat. De-indent the block and call us
					 * recursively
					 */
					case def:
						ifBlock = new ADELAFunctionBlock();
						HandleIfBlock(StripOneTab(sParams[1]), ifBlock);
						String[] funcSignature = sParams[0].split("[\\(\\)]");
						String funcName = funcSignature[0];
						if (funcSignature.length > 1) {
							String[] funcParams = funcSignature[1].split(",");
							for (int pi = 0; pi < funcParams.length; pi++) {
								funcParams[pi] = funcParams[pi].trim();
							}
							ifBlock.setParamerets(funcParams);
						}
						blocks.put(funcName, ifBlock);
						break;
					default:
						throw new ADELASyntaxException("Statement " + sParts[0]
								+ " unknown!");
					}
				}
				/*
				 * The block is not known
				 */
				else {
					throw new ADELASyntaxException("Illegal statement " + sLine);
				}
			}
		}

		return blocks;
	}

	/**
	 * Process pre-input parts
	 * 
	 * @param str
	 *            pre-input block
	 * @param myAutomat
	 *            automat to add the preinputs to
	 * @throws ADELASyntaxException
	 */
	public void HandleIfBlock(String str, IPreInput myAutomat)
			throws ADELASyntaxException {
		String sParts[], sParams[];
		String sLine;
		int pos;
		int iLen;
		EBlockType eType;
		ADELAFunctionBlock myIfBlock = null;
		String lines = "";

		// get rid of empty lines and comments
		str = StripEmptyLines(StripComments(str));

		// traverse the string
		while (str.length() > 0) {
			/*
			 * find a block
			 */
			pos = 0;
			iLen = str.length();
			do {
				pos = str.indexOf("\n", pos + 1);
				sLine = str.substring(pos + 1);
			} while ((pos != -1) && (iLen > (pos + 1))
					&& (str.charAt(pos + 1) == '\t'));

			/*
			 * last block
			 */
			if (pos == -1) {
				sLine = str;
				str = "";
			}
			/*
			 * found a block
			 */
			else {
				sLine = str.substring(0, pos);
				str = str.substring(pos + 1);
			}

			/*
			 * split block
			 */
			sParts = sLine.split(" ", 2);
			if ((sParts.length >= 2)
					|| (/* hack */sParts[0].startsWith("else:"))) {
				// hack
				if (sParts[0].startsWith("else:")) {
					sParams = sLine.split(":", 2);
					sParts = new String[2];
					sParts[0] = sParams[0];
				} else {
					sParams = sParts[1].split(":\n", 2);
					if (sParams.length == 2) {
						sParams[1] = "\n" + sParams[1];
					}
				}
			} else {
				sParams = "".split(" ");
			}

			/*
			 * simple statement
			 */
			if ((sParts.length != 2) || (sParams.length != 2)) {
				if (sLine.contains("\n\t")) {
					throw new ADELASyntaxException("Wrong indendation: "
							+ sLine);
				}
				lines = lines + sLine + "\n";
			} else {
				if (lines.length() != 0) {
					myAutomat.AddPreInput(new ADELAFunctionBlock(null, lines));
					lines = "";
				}
				/*
				 * sub-block
				 */
				if (hBTypes.containsKey(sParts[0])) {
					eType = hBTypes.get(sParts[0]);
					switch (eType) {
					case block_if:
					case block_while:
					case block_for:
						myIfBlock = new ADELAFunctionBlock();

						myIfBlock.SetWhile(eType == EBlockType.block_while);
						myIfBlock.SetFor(eType == EBlockType.block_for);
						myIfBlock.SetCondition(sParams[0]);
						HandleIfBlock(StripOneTab(sParams[1]), myIfBlock);
						myAutomat.AddPreInput(myIfBlock);
						break;

					case block_elif:
						if (myIfBlock == null) {
							break;
						}
						ADELAFunctionBlock myElifBlock = new ADELAFunctionBlock();
						HandleIfBlock(StripOneTab(sParams[1]), myElifBlock);
						myElifBlock.SetCondition(sParams[0]);
						myIfBlock.AddElifBlock(myElifBlock);
						break;

					case block_else:
						if (myIfBlock == null) {
							break;
						}
						ADELAFunctionBlock myElseBlock = new ADELAFunctionBlock();
						HandleIfBlock(StripOneTab(sParams[1]), myElseBlock);
						myIfBlock.SetElseIfBlock(myElseBlock);
						myIfBlock = null;
						break;
					}
				}
			}
		}
		if (lines.length() != 0) {
			myAutomat.AddPreInput(new ADELAFunctionBlock(null, lines));
			lines = "";
		}
	}
}
