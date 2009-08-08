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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.exceptions.FunctionNotFoundException;
import net.sourceforge.adela.exceptions.WrongParameterCountException;
import net.sourceforge.adela.interpreter.ADELAException;
import net.sourceforge.adela.interpreter.ADELAInterpreter;
import net.sourceforge.adela.interpreter.ADELAParseTree;

public class ParserTest {

	/**
	 * @param args
	 * @throws FunctionNotFoundException
	 * @throws WrongParameterCountException
	 * @throws ADELAException
	 * @throws ADELASyntaxException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws ADELAException,
			WrongParameterCountException, FunctionNotFoundException,
			ADELASyntaxException, FileNotFoundException, IOException {
		ADELAParseTree tree1, tree2, tree3;

		ADELAParseTree loadedTree = ADELAParseTree
				.loadTree(new DataInputStream(new FileInputStream(
						"/var/tmp/foo")));
		System.out.println(loadedTree.toStringNonFlat());
		ADELAParseTree.Parse("foo(1+2,x,\"hello\").toString().length()")
				.saveTree(
						new DataOutputStream(new FileOutputStream(
								"/var/tmp/foo")));
		if (0 == 0)
			return;
		tree1 = ADELAParseTree.Parse("args.length() <= 3");
		tree2 = ADELAParseTree.Parse("argc = args.length().dude() + 2");
		tree3 = ADELAParseTree.Parse("16>>3");

		System.out.println(tree1.toStringNonFlat());
		System.out.println(tree2.toStringNonFlat());
		System.out.println(tree3.toString());
		ADELAInterpreter evaluator = new ADELAInterpreter();
		System.out.println(tree3.doEvaluate(new HashMap<String, Object>(),
				evaluator));

		// System.out.println(tree1.doEvaluate(null, interpreter));
		// tree3.doEvaluate(null, interpreter);
	}

}
