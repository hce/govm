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

package net.sourceforge.adela.wrappers;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IClassWrapper;

public class NumberHandler implements IClassWrapper {
	/**
	 * Our singleton
	 */
	private static NumberHandler instance = new NumberHandler();

	/**
	 * Get the instance
	 * 
	 * @return the instance
	 */
	public static NumberHandler getInstance() {
		return instance;
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
		Number number = (Number) variable;
		NumberHandlerFunctions function;
		try {
			function = NumberHandlerFunctions.valueOf(funcName);
		} catch (Exception e) {
			throw new FunctionNotFoundException(funcName);
		}
		switch (function) {
		case bytevalue:
			return number.byteValue();
		case doublevalue:
			return number.doubleValue();
		case floatvalue:
			return number.floatValue();
		case hashcode:
			return number.hashCode();
		case intvalue:
			return number.intValue();
		case shortvalue:
			return number.shortValue();
		case longvalue:
			return number.longValue();
		case tostring:
			return number.toString();
		}
		return null;
	}
}
