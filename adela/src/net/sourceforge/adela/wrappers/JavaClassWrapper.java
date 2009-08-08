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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IClassWrapper;

/**
 * A generic wrapper for completely safe classes
 * 
 * @author hc
 */
public class JavaClassWrapper implements IClassWrapper {

	/**
	 * Class we handle
	 */
	private Class<?> handledClass;

	/**
	 * Callable methods
	 */
	private Map<String, Method> methods = new HashMap<String, Method>();

	public JavaClassWrapper(Class<?> classToHandle) {
		this.handledClass = classToHandle;
		Method[] methods = classToHandle.getMethods();
		for (int i = 0; i < methods.length; i++) {
			this.methods.put(methods[i].getName(), methods[i]);
		}
	}

	@SuppressWarnings("unchecked")
	public Object FunctionCallback(String funcName, Object variable,
			Object[] vParams) throws WrongParameterCountException,
			FunctionNotFoundException {
		if (funcName.equals("dir")) {
			Method[] cnstrs = this.handledClass.getMethods();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < cnstrs.length; i++) {
				Method ctr = cnstrs[i];
				Class[] types = ctr.getParameterTypes();
				sb.append(ctr.getReturnType().getSimpleName() + " "
						+ ctr.getName() + "(");
				for (int j = 0; j < types.length; j++) {
					sb.append(types[j].getSimpleName());
					if (j < (types.length - 1)) {
						sb.append(", ");
					}
				}
				sb.append(")\n");
			}
			return sb.toString();
		}

		Class[] parameterTypes = new Class[vParams.length];
		for (int i = 0; i < vParams.length; i++) {
			Class pType;
			pType = vParams[i].getClass();
			// hack
			if (pType == Integer.class) {
				pType = int.class;
			} else if (pType == Long.class) {
				pType = long.class;
			} else if (pType == Double.class) {
				pType = double.class;
			} else if (pType == Character.class) {
				pType = char.class;
			} else if (pType == Float.class) {
				pType = float.class;
			} else if (pType == Short.class) {
				pType = short.class;
			}

			parameterTypes[i] = pType;
		}
		try {
			Method method = this.handledClass.getMethod(funcName,
					parameterTypes);

			try {
				return method.invoke(variable, vParams);
			} catch (IllegalAccessException e) {
				// hack
				throw new NoSuchMethodException();
			}
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
			Method[] methods = this.handledClass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				try {
					return method.invoke(variable, vParams);
				} catch (IllegalArgumentException e1) {
				} catch (IllegalAccessException e1) {
				} catch (InvocationTargetException e1) {
				}
			}
			if (this.methods.containsKey(funcName)) {
				Method method = this.methods.get(funcName);
				throw new WrongParameterCountException(funcName, method
						.getParameterTypes().length, vParams.length);
			}
			throw new FunctionNotFoundException(funcName);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return false;
	}
}
