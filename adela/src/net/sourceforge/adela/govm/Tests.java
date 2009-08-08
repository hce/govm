package net.sourceforge.adela.govm;

import java.io.FileOutputStream;
import java.io.IOException;

import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.interpreter.ADELACompilerException;
import net.sourceforge.adela.tests.GOVMCodeGenerator;

public class Tests {
	public static void main(String[] args) throws ADELACompilerException,
			ADELASyntaxException, IOException {
		GOVMCodeGenerator gcc = new GOVMCodeGenerator(System.out);

		gcc.beginFunction(new String[0]);
		gcc.declareShort("a");
		gcc.reserveStack();
		gcc.writeStatement("a = 20");
		gcc.setLabel("test");
		gcc.writeStatement("b = 64");
		gcc.writeStatement("putc(a + b)");
		gcc.writeStatement("a = a - 1");
		gcc.writeStatement("a");
		gcc.setGoto("finito", Opcode.JZ);
		gcc.setGoto("test", Opcode.JMP);
		gcc.setLabel("finito");
		gcc.writeStatement("halt()");
		gcc.endFunction();

		FileOutputStream fos = new FileOutputStream("/var/tmp/a.out");
		fos.write(gcc.generate());
		fos.close();
	}
}
