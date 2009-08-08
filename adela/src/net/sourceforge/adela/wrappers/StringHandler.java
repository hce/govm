/**
 * adela
 * An embeddable, secure scripting language
 * for java applications
 *
 * esperer AT sec.informatik.tu-darmstadt de
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

package net.sourceforge.adela.wrappers;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IClassWrapper;

/**
 * Wrapper functions for java strings
 * 
 * @author hc
 */
public class StringHandler implements IClassWrapper {
	/**
	 * Our singleton
	 */
	private static StringHandler handler = new StringHandler();

	/**
	 * Instanciiate
	 */
	private StringHandler() {
	}

	/**
	 * Get the instance
	 * 
	 * @return the instance
	 */
	public static StringHandler getHandler() {
		return handler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sourceforge.adela.interfaces.IClassWrapper#FunctionCallback(java.lang.String,
	 *      java.lang.Object, java.lang.Object[])
	 */
	public Object FunctionCallback(String funcName, Object variable,
			Object[] vParams) throws WrongParameterCountException,
			FunctionNotFoundException {
		StringHandlerFunctions function;
		try {
			function = StringHandlerFunctions.valueOf(funcName);
		} catch (Exception e) {
			throw new FunctionNotFoundException(funcName);
		}
		String string = (String) variable;
		switch (function) {
		case substring:
			if (vParams.length == 1) {
				return string.substring((Integer) vParams[0]);
			} else if (vParams.length == 2) {
				return string.substring((Integer) vParams[0],
						(Integer) vParams[1]);
			} else {
				throw new WrongParameterCountException(funcName, 2,
						vParams.length);
			}

		case charat:
			if (vParams.length != 1) {
				throw new WrongParameterCountException(funcName, 1,
						vParams.length);
			}
			return string.charAt((Integer) vParams[0]);

		case compareto:
			if (vParams.length != 1) {
				throw new WrongParameterCountException(funcName, 1,
						vParams.length);
			}
			return string.compareTo((String) vParams[0]);

		case comparetoignorecase:
			if (vParams.length != 1) {
				throw new WrongParameterCountException(funcName, 1,
						vParams.length);
			}
			return string.compareToIgnoreCase((String) vParams[0]);

		case length:
			if (vParams.length != 0) {
				throw new WrongParameterCountException(funcName, 0,
						vParams.length);
			}
			return string.length();

		case startswith:
			switch (vParams.length) {
			case 1:
				return string.startsWith((String) vParams[0]);
			case 2:
				return string.startsWith((String) vParams[0],
						(Integer) vParams[1]);
			default:
				throw new WrongParameterCountException(funcName, 1,
						vParams.length);
			}

		case endswith:
			if (vParams.length != 1) {
				throw new WrongParameterCountException(funcName, 1,
						vParams.length);
			}
			return string.endsWith((String) vParams[0]);

		case tolowercase:
			if (vParams.length != 0) {
				throw new WrongParameterCountException(funcName, 0,
						vParams.length);
			}
			return string.toLowerCase();
		case touppercase:
			if (vParams.length != 0) {
				throw new WrongParameterCountException(funcName, 0,
						vParams.length);
			}
			return string.toUpperCase();

		case split:
			switch (vParams.length) {
			case 1:
				return string.split((String) vParams[0]);
			case 2:
				return string.split((String) vParams[0], (Integer) vParams[1]);
			default:
				throw new WrongParameterCountException(funcName, 1,
						vParams.length);
			}
		}
		return null;
	}
}
