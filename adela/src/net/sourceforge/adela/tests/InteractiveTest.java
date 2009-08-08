/**
 * adela
 * An embeddable, secure scripting language
 * for java applications
 * 
 * -- EXAMPLE EMBEDDING APPLICATION --
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

package net.sourceforge.adela.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interpreter.ADELAException;
import net.sourceforge.adela.interpreter.ADELAInterpreter;
import net.sourceforge.adela.interpreter.ADELAParseTree;

/**
 * An interactive ADeLa interpreter
 * 
 * @author hc
 */
public class InteractiveTest {

	/**
	 * Main entry point
	 * 
	 * @param args
	 *            console arguments
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		ADELAInterpreter interpreter = new ADELAInterpreter();
		// interpreter.AddBuiltinHandlers();
		interpreter.DeclareClassSafe(String.class);

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));
		Map<String, Object> locals = new HashMap<String, Object>();
		while (true) {
			try {
				String s = reader.readLine();
				try {
					ADELAParseTree tree = ADELAParseTree.Parse(s);
					Object res = tree.doEvaluate(locals, interpreter);
					if (res != null) {
						System.err.println("Result: " + res + " ["
								+ res.getClass() + "]");
					}
					System.err.println("TM > "
							+ Runtime.getRuntime().freeMemory());
				} catch (ADELASyntaxException e) {
					e.printStackTrace();
				} catch (WrongParameterCountException e) {
					e.printStackTrace();
				} catch (FunctionNotFoundException e) {
					e.printStackTrace();
				} catch (ADELAException e) {
					e.printStackTrace();
				} catch (NullPointerException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				break;
			}
		}
	}

}
