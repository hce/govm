package net.sourceforge.adela.govm;

import net.sourceforge.adela.interpreter.ADELACompilerException;

public interface StatementResolver {
	public int reserveShort(String name) throws ADELACompilerException;

	public int reserveBytes(String name, byte[] ba);

	public int reserveShortGlobal(String name);

	public int globorloc(String name) throws ADELACompilerException;
}
