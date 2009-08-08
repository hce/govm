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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sourceforge.adela.enums.EFunction;
import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interfaces.IClassWrapper;
import net.sourceforge.adela.interfaces.IEvaluator;
import net.sourceforge.adela.interfaces.IFunctionCallback;
import net.sourceforge.adela.wrappers.ArrayHandler;
import net.sourceforge.adela.wrappers.ClassHandler;
import net.sourceforge.adela.wrappers.JavaClassWrapper;
import net.sourceforge.adela.wrappers.NumberHandler;
import net.sourceforge.adela.wrappers.StringHandler;

/**
 * parse a single statement
 * 
 * @author hc
 * 
 */
public class ADELAInterpreter implements IEvaluator {
	/**
	 * builtin function table
	 */
	private Map<String, EFunction> mapTable;

	/**
	 * variable table
	 */
	private Map<String, Object> mVariables;

	/**
	 * callback for function calls
	 */
	private IFunctionCallback iCallback;

	/**
	 * Class handlers
	 */
	@SuppressWarnings("unchecked")
	private Map<Class, IClassWrapper> classHandlers;

	/**
	 * Class that handles arrays.
	 */
	private ArrayHandler arrayHandler = new ArrayHandler();

	/**
	 * Are the interpreted scripts completely trusted?
	 */
	private boolean trusted;

	/**
	 * Handler to handle classes of type 'class'
	 */
	private ClassHandler classClassHandler = new ClassHandler();

	/**
	 * initialize us
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ADELAInterpreter() {
		/*
		 * create variable map
		 */
		mVariables = new HashMap<String, Object>();

		mapTable = new HashMap<String, EFunction>();
		mapTable.put("format", EFunction.Format);
		mapTable.put("log", EFunction.Log);
		mapTable.put("deref", EFunction.Deref);
		mapTable.put("isnull", EFunction.IsNull);
		mapTable.put("array", EFunction.Array);
		mapTable.put("range", EFunction.Range);
		mapTable.put("list", EFunction.List);
		mapTable.put("dict", EFunction.Dict);

		/*
		 * add booleans
		 */
		mVariables.put("true", true);
		mVariables.put("false", false);

		this.classHandlers = new HashMap<Class, IClassWrapper>();
		this.classHandlers.put(Class.class, this.classClassHandler);
	}

	/**
	 * Adds builtin handlers for various standard java classes
	 */
	public void AddBuiltinHandlers() {
		classHandlers.put(String.class, StringHandler.getHandler());

		classHandlers.put(Integer.class, NumberHandler.getInstance());
		classHandlers.put(Byte.class, NumberHandler.getInstance());
		classHandlers.put(Double.class, NumberHandler.getInstance());
		classHandlers.put(Float.class, NumberHandler.getInstance());
		classHandlers.put(Integer.class, NumberHandler.getInstance());
		classHandlers.put(Long.class, NumberHandler.getInstance());
		classHandlers.put(Short.class, NumberHandler.getInstance());
	}

	/**
	 * Adds builtin handlers for various math classes
	 */
	public void AddMathHandlers() {
		classHandlers.put(BigDecimal.class, NumberHandler.getInstance());
		classHandlers.put(BigInteger.class, NumberHandler.getInstance());
	}

	/**
	 * Declare a java class c as completely safe and export all methods
	 * 
	 * @param c
	 *            class to declare safe
	 */
	public void DeclareClassSafe(Class<?> c) {
		classHandlers.put(c, new JavaClassWrapper(c));
	}

	/**
	 * allow an external variable map to be set
	 * 
	 * @param map
	 *            Variable map
	 */
	public void SetVariableMap(Map<String, Object> map) {
		mVariables = map;

		mVariables.put("true", true);
		mVariables.put("false", false);
		mVariables.put("null", null);
	}

	/**
	 * Set a callback to handle a specific class
	 * 
	 * @param handledClass
	 *            class that is handled by this handler
	 * @param handler
	 *            handler that handles classes of type handledClass
	 */
	public void SetClassHandler(Class<Object> handledClass,
			IClassWrapper handler) {
		this.classHandlers.put(handledClass, handler);
	}

	/**
	 * set the callback function
	 * 
	 * @param nCB
	 *            Function callback function
	 */
	public void SetCallback(IFunctionCallback nCB) {
		iCallback = nCB;
	}

	/**
	 * Call a function using the set callback interface
	 * 
	 * @param sFuncName
	 *            Function name
	 * @param vParams
	 *            Function parameters
	 * @return Function result
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 * @throws ADELAException
	 */
	public Object CallFunction(Object variable, String funcname,
			Map<String, Object> locals, Object[] vParams)
			throws WrongParameterCountException, FunctionNotFoundException,
			ADELAException {
		IFunctionCallback callback = null;
		IClassWrapper classHandler = null;
		if (variable == null) {
			callback = iCallback;
		} else {
			try {
				callback = (IFunctionCallback) variable;
			} catch (ClassCastException e) {
			}
		}

		if (variable != null) {
			if (variable.getClass().isArray()) {
				return this.arrayHandler.FunctionCallback(funcname, variable,
						vParams);
			}
		}

		if (callback != null) {
			try {
				return callback.FunctionCallback(funcname, vParams);
			} catch (FunctionNotFoundException e) {
			} catch (WrongParameterCountException e) {
			}
		}

		if (variable != null) {
			classHandler = this.classHandlers.get(variable.getClass());
			if (this.trusted) {
				// THIS IS INSECURE!!
				if (classHandler == null) {
					classHandler = new JavaClassWrapper(variable.getClass());
					this.classHandlers.put(variable.getClass(), classHandler);
					System.err.format(
							"Warning: adding complete trust for class %s\n",
							variable.getClass());
				}
			}
			if (classHandler != null) {
				return classHandler.FunctionCallback(funcname, variable,
						vParams);
			}
		}

		EFunction builtin = mapTable.get(funcname);
		if (builtin != null) {
			return ADELAFunctions.CallFunction(funcname, this, locals, builtin,
					vParams);
		}

		return null;
	}

	public void SetVariable(String key, Object value) {
		this.mVariables.put(key, value);
	}

	public Object GetVariable(String key) {
		return this.mVariables.get(key);
	}

	public Object evaluate(Map<String, Object> locals, String statement) {
		if ((locals != null) && (locals.containsKey(statement))) {
			return locals.get(statement);
		}
		return mVariables.get(statement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sourceforge.adela.interfaces.IEvaluator#callFunction(java.lang.Object,
	 *      java.lang.String, java.util.Map, java.lang.Object[])
	 */
	public Object callFunction(Object variable, String funcname,
			Map<String, Object> locals, Object[] params)
			throws WrongParameterCountException, FunctionNotFoundException,
			ADELAException {
		return this.CallFunction(variable, funcname, locals, params);
	}

	public boolean isTrusted() {
		return trusted;
	}

	public void setTrusted(boolean trusted) {
		if ((this.trusted == true) && (trusted == false)) {
			throw new RuntimeException(
					"Cannot reset trusted to false once it has been set to true!");
		}
		this.trusted = trusted;
	}

	/**
	 * Set if class c may be instanciiated
	 * 
	 * @param c
	 *            class to set allowance for
	 * @param allow
	 *            may class c be instanciiated?
	 */
	public void setAllowInstanciiation(Class<?> c, boolean allow) {
		this.classClassHandler.setAllowInstanciiation(c, allow);
	}

	/**
	 * Get if class c may be instanciiated
	 * 
	 * @param c
	 *            class to get allowance for
	 * @return true if class c may be instanciiated, otherwise false
	 */
	public boolean getAllowInstanciation(Class<?> c) {
		return this.classClassHandler.getAllowInstanciiation(c);
	}

	/**
	 * Get classes that may be instantiated
	 * 
	 * @return array of classes that may be instantiated
	 */
	public Class<?>[] getInstantiableClasses() {
		return this.classClassHandler.getInstantiableClasses();
	}

	/**
	 * Set if all classes may be instanciiated without restrictions
	 * 
	 * @param allow
	 *            true if allowed, otherwise false
	 */
	public void setAllowArbitraryInstanciiation(boolean allow) {
		this.classClassHandler.setTrusted(allow);
	}

	/**
	 * Return if all classes may be instanciiated without restrictions
	 * 
	 * @return if all classes may be instanciiated without restrictions
	 */
	public boolean getAllowArbitraryInstanciiation() {
		return this.classClassHandler.isTrusted();
	}

	/**
	 * Allow the scripts full access to a class
	 * 
	 * @param name
	 *            of the class in the script
	 * @param c
	 *            class to allow full access to
	 */
	public void AllowFullAccess(String name, Class<?> c) {
		DeclareClassSafe(c);
		setAllowInstanciiation(c, true);
		SetVariable(name, c);
	}

	public String[] GetVariables() {
		Set<String> ks = this.mVariables.keySet();
		String[] vars = new String[ks.size()];
		Iterator<String> itr = ks.iterator();
		int i = 0;
		while (itr.hasNext()) {
			vars[i++] = itr.next();
		}
		return vars;
	}
}
