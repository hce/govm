package net.sourceforge.adela.interfaces;

import java.util.Map;

import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interpreter.ADELAException;

/**
 * Classes implementing this interface must evaluate a part of a term
 * 
 * @author hc
 */
public interface IEvaluator {
	/**
	 * Evaluate statement statement. statement can be a number ([0-9]+ integer,
	 * [0-9]+\.[0-9]+ float, [0-9]+L long), a string ".*", a function call
	 * ([A-Za-z]+()), or a method call ([A-Za-z]+\.[A-Za-z]+)
	 * 
	 * @param local
	 *            variables
	 * @param statement
	 *            statement to evaluate
	 * @return result result of evaluation
	 */
	public Object evaluate(Map<String, Object> locals, String statement);

	/**
	 * Call function stringValue in leftValue with locals locals
	 * 
	 * @param variable
	 *            object to call function on
	 * @param funcname
	 *            name of function to call
	 * @param locals
	 *            locals to use
	 * @parm params parameters to pass to the function
	 * @return return value of called function
	 */
	public Object callFunction(Object variable, String funcname,
			Map<String, Object> locals, Object[] params)
			throws WrongParameterCountException, FunctionNotFoundException,
			ADELAException;
}
