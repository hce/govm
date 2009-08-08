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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Map;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interpreter.ADELAException;
import net.sourceforge.adela.interpreter.ADELAFunctionBlock;
import net.sourceforge.adela.interpreter.ADELAInterpreter;

public class LCTest {

	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ClassNotFoundException
	 * @throws ADELAException
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws FileNotFoundException,
			IOException, ClassNotFoundException, WrongParameterCountException,
			FunctionNotFoundException, ADELAException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				"/var/tmp/test.ac"));
		Map<String, ADELAFunctionBlock> functions = (Map<String, ADELAFunctionBlock>) ois
				.readObject();
		ADELAFunctionBlock main = functions.get("main");
		if (main != null) {
			ADELAInterpreter evaluator = new ADELAInterpreter();
			evaluator.AddBuiltinHandlers();
			evaluator.SetVariable("window", new GOVMCodeGenerator(System.out));
			Object[] parameters = new Object[1];
			parameters[0] = args;
			System.out.println("Result: " + main.Handle(evaluator, parameters));
		} else {
			System.err.println("Error: can't find function main!");
		}
	}

}
