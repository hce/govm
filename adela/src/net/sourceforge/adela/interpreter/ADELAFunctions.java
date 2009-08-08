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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.adela.enums.EFunction;
import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;

/**
 * Built-in functions
 * 
 * @author hc
 */
public class ADELAFunctions {

	/**
	 * Call a function
	 * 
	 * @params funcName string name of function
	 * @param builtin
	 *            function to call
	 * @param params
	 *            function parameters
	 * @return function result.
	 * @throws WrongParameterCountException
	 * @throws FunctionNotFoundException
	 */
	public static Object CallFunction(String funcName, ADELAInterpreter parser,
			Map<String, Object> locals, EFunction builtin, Object[] params)
			throws WrongParameterCountException, FunctionNotFoundException {
		switch (builtin) {
		case Format:
			return Format(params);
		case Log:
			return Log(params);
		case Deref:
			return Deref(parser, locals, params);
		case IsNull:
			return new Boolean(params[0] == null);
		case Array:
			return params;
		case Range:
			return Range(params);
		case List:
			ArrayList<Object> list = new ArrayList<Object>();
			for (int i = 0; i < params.length; i++) {
				list.add(params[i]);
			}
			return list;
		case Dict:
			if ((params.length % 2) != 0) {
				throw new ADELARuntimeException(
						"Dict function requires an even number of arguments!");
			}
			Map<Object, Object> dict = new HashMap<Object, Object>();
			for (int i = 0; i < params.length; i += 2) {
				dict.put(params[i], params[i + 1]);
			}
			return dict;
		}
		throw new FunctionNotFoundException(funcName);
	}

	private static Object Range(Object[] params)
			throws WrongParameterCountException {
		if ((params.length < 1) || (params.length > 2)) {
			throw new WrongParameterCountException("range", 1, params.length);
		}
		if (params.length == 1) {
			Integer[] rangeArray = new Integer[(Integer) params[0]];
			for (int i = 0; i < rangeArray.length; i++) {
				rangeArray[i] = i;
			}
			return rangeArray;
		}
		int from = (Integer) params[0];
		int to = (Integer) params[1];
		int range = to - from;
		Object[] rangeArray = new Object[range];
		for (int i = 0; i < range; i++) {
			rangeArray[i] = i + from;
		}
		return rangeArray;
	}

	/**
	 * Dereference a string x
	 * 
	 * @param parser
	 *            parser to use
	 * @param locals
	 *            locals to use
	 * @param params
	 *            [0]: name of variable to get
	 * @return dereferenced string x
	 * @throws WrongParameterCountException
	 */
	private static Object Deref(ADELAInterpreter parser,
			Map<String, Object> locals, Object[] params)
			throws WrongParameterCountException {
		if (params.length != 1) {
			throw new WrongParameterCountException("deref", 1, params.length);
		}
		if (locals.containsKey((String) params[0])) {
			return locals.get((String) params[0]);
		} else {
			return parser.GetVariable((String) params[0]);
		}
	}

	/**
	 * Logger.debug wrapper
	 * 
	 * @param params
	 *            String to log
	 * @return zero on success
	 * @throws WrongParameterCountException
	 */
	private static Object Log(Object[] params)
			throws WrongParameterCountException {
		if (params.length != 1) {
			throw new WrongParameterCountException("log", 1, params.length);
		}
		System.out.println("ScriptLog: " + params[0].toString());
		return Boolean.valueOf(true);
	}

	/**
	 * System.format wrapper
	 * 
	 * @param params
	 *            System.format params
	 * @return System.format result
	 * @throws WrongParameterCountException
	 */
	private static Object Format(Object[] params)
			throws WrongParameterCountException {
		if (params.length < 2) {
			throw new WrongParameterCountException("format", 2, params.length);
		}
		Object[] subparams = new Object[params.length + 1];
		for (int i = 1; i < params.length; i++) {
			subparams[i - 1] = params[i];
		}
		return String.format((String) params[0], subparams);
	}
}
