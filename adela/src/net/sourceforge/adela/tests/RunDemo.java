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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.Map;

import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IFunctionCallback;
import net.sourceforge.adela.interpreter.ADELAException;
import net.sourceforge.adela.interpreter.ADELAFunctionBlock;
import net.sourceforge.adela.interpreter.ADELAInterpreter;
import net.sourceforge.adela.interpreter.ADELAParser;

public class RunDemo implements IFunctionCallback {
	/**
	 * Our script's functions
	 */
	private Map<String, ADELAFunctionBlock> functions;

	/**
	 * Our interpreter
	 */
	private ADELAInterpreter interpreter = new ADELAInterpreter();

	/**
	 * @param args
	 * @throws IOException
	 * @throws ADELAException
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 * @throws ADELASyntaxException
	 */
	public static void main(String[] args) throws IOException, ADELAException,
			WrongParameterCountException, FunctionNotFoundException,
			ADELASyntaxException {
		new RunDemo().run(args);
	}

	private void run(String[] args) throws IOException, ADELASyntaxException,
			ADELAException, WrongParameterCountException,
			FunctionNotFoundException {
		File scriptFile = new File("src/net/sourceforge/adela/tests/demo.adela");
		InputStream reader = new FileInputStream(scriptFile);
		byte[] buffer = new byte[(int) scriptFile.length()];
		reader.read(buffer);
		String script = new String(buffer);
		ADELAParser compiler = new ADELAParser();
		functions = compiler.BuildAutomat(script);

		interpreter.AddBuiltinHandlers();
		interpreter.AllowFullAccess("StringWriter", StringWriter.class);
		interpreter.AllowFullAccess("StringReader", StringReader.class);
		interpreter.AllowFullAccess("LinkedList", LinkedList.class);

		interpreter.AllowFullAccess("Object", Object.class);

		interpreter.setAllowInstanciiation(FileWriter.class, true);
		interpreter.DeclareClassSafe(FileWriter.class);
		interpreter.SetCallback(this);

		interpreter.SetVariable("window", new GOVMCodeGenerator(System.out));
		ADELAFunctionBlock main = functions.get("main");
		if (main == null) {
			System.err.println("Function main cannot be found!");
		}
		Object[] parameters = new Object[1];
		parameters[0] = args;
		System.err.println("Function returned "
				+ main.Handle(interpreter, parameters));

		try {
			ObjectOutputStream oos = new ObjectOutputStream(
					new FileOutputStream("/var/tmp/test2.ac"));
			oos.writeObject(functions);
			oos.close();
		} catch (Exception e) {
		}
	}

	public Object FunctionCallback(String funcName, Object[] vParams)
			throws WrongParameterCountException, FunctionNotFoundException {
		ADELAFunctionBlock block = this.functions.get(funcName);
		if (block == null) {
			throw new FunctionNotFoundException(funcName);
		}
		try {
			return block.Handle(interpreter, vParams);
		} catch (ADELAException e) {
			System.out.println("Error: " + e);
		}
		return null;
	}
}
