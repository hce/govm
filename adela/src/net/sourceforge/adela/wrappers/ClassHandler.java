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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IClassWrapper;
import net.sourceforge.adela.interpreter.ADELARuntimeException;

/**
 * Handles classes of type 'class'
 * 
 * @author hc
 */
public class ClassHandler implements IClassWrapper {
	/**
	 * Classes that may be instanciiated
	 */
	private Map<Class<?>, Boolean> classesToInstanciiate = new HashMap<Class<?>, Boolean>();

	/**
	 * All classes may be instanciiated (This breaches security!)
	 */
	private boolean trusted = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sourceforge.adela.interfaces.IClassWrapper#FunctionCallback(java.lang.String,
	 *      java.lang.Object, java.lang.Object[])
	 */
	@SuppressWarnings("unchecked")
	public Object FunctionCallback(String funcName, Object variable,
			Object[] vParams) throws WrongParameterCountException,
			FunctionNotFoundException {
		Class c = (Class) variable;
		ClassHandlerFunctions f;
		try {
			f = ClassHandlerFunctions.valueOf("chf_" + funcName);
		} catch (Exception e) {
			throw new FunctionNotFoundException(funcName);
		}
		switch (f) {
		case chf_dir:
			Constructor[] cnstrs = c.getConstructors();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < cnstrs.length; i++) {
				Constructor ctr = cnstrs[i];
				Class[] types = ctr.getParameterTypes();
				sb.append(c.getSimpleName() + "(");
				for (int j = 0; j < types.length; j++) {
					sb.append(types[j].getSimpleName());
					if (j < (types.length - 1)) {
						sb.append(", ");
					}
				}
				sb.append(")\n");
			}
			return sb.toString();

		case chf_new:
			if ((!trusted) && (!this.getAllowInstanciiation(c))) {
				throw new ADELARuntimeException(
						"Instanciiating classes of type " + c
								+ " is not allowed");
			}
			Class[] parameterTypes = new Class[vParams.length];
			for (int i = 0; i < vParams.length; i++) {
				parameterTypes[i] = vParams[i].getClass();
			}
			try {
				Constructor ctr = c.getConstructor(parameterTypes);
				return ctr.newInstance(vParams);
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
				Constructor[] ctrs = c.getConstructors();
				Constructor ctr;
				for (int i = 0; i < ctrs.length; i++) {
					ctr = ctrs[i];

					try {
						return ctr.newInstance(vParams);
					} catch (IllegalArgumentException e1) {
					} catch (InstantiationException e1) {
					} catch (IllegalAccessException e1) {
					} catch (InvocationTargetException e1) {
					}
				}
				throw new ADELARuntimeException("Constructor for " + c
						+ " not found");
			} catch (IllegalArgumentException e) {
			} catch (InstantiationException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
			break;
		}
		return null;
	}

	/**
	 * set if class c may be instanciiated
	 * 
	 * @param c
	 *            class to set allowance for
	 * @param allow
	 *            allow instanciiation?
	 */
	public void setAllowInstanciiation(Class<?> c, boolean allow) {
		this.classesToInstanciiate.put(c, allow);
	}

	/**
	 * Returns if class c may be instanciiated by scripts
	 * 
	 * @param c
	 *            class to look up allowance for
	 * @return if class c may be instanciiated
	 */
	public boolean getAllowInstanciiation(Class<?> c) {
		Boolean tmp = this.classesToInstanciiate.get(c);
		if (tmp == null) {
			return false;
		}
		return tmp;
	}

	/**
	 * Return if all classes may be instanciiated without restrictions
	 * 
	 * @return if all classes may be instanciiated without restrictions
	 */
	public boolean isTrusted() {
		return trusted;
	}

	/**
	 * Set trust status
	 * 
	 * @param trusted
	 *            true if all classes may be instanciiated without restrictions,
	 *            otherwise false
	 */
	public void setTrusted(boolean trusted) {
		this.trusted = trusted;
	}

	/**
	 * Get classes that may be instantiated
	 * 
	 * @return array of classes that may be instantiated
	 */
	public Class<?>[] getInstantiableClasses() {
		return classesToInstanciiate.keySet().toArray(new Class[0]);
	}
}
