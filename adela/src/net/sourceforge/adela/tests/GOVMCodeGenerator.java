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

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import net.sourceforge.adela.exceptions.ADELASyntaxException;
import net.sourceforge.adela.govm.GOVMWriter;
import net.sourceforge.adela.govm.Opcode;
import net.sourceforge.adela.govm.StatementResolver;
import net.sourceforge.adela.interpreter.ADELACompilerException;
import net.sourceforge.adela.interpreter.ADELAParseTree;

/**
 * ADeLa test class
 * 
 * @author hc
 */
public class GOVMCodeGenerator implements StatementResolver {
	private HashMap<String, Integer> addresses;
	private HashMap<String, Integer> gaddresses = new HashMap<String, Integer>();
	private ByteBuffer ds = ByteBuffer.allocate(65536);
	private byte[] ids = new byte[65535];
	private int curAddr;
	private int curStack;
	private int gcurAddr = 2;
	private GOVMWriter gw;
	private int numArguments = -1;
	private PrintStream logStream;

	public GOVMCodeGenerator(PrintStream logStream) {
		this.logStream = logStream;
		gw = new GOVMWriter(logStream);
	}

	public byte[] generate() {
		byte[] header_and_cs = gw.getCS(this.ds.position());
		byte[] ds = this.ds.array();
		ByteBuffer result = ByteBuffer.allocate(header_and_cs.length
				+ this.ds.position());
		result.put(header_and_cs);
		result.put(ds, 0, this.ds.position());
		return result.array();
	}

	public void beginFunction(String[] params) throws ADELACompilerException {
		addresses = new HashMap<String, Integer>();
		curAddr = 0;
		curStack = 0;
		numArguments = params.length;
		// PUSH BP
		gw.writeOpcode(Opcode.FMOV);

		// MOV BP, SP (PUSH SP, POP BP)
		gw.writeOpcode(Opcode.EMOV);
		gw.writeOpcode(Opcode.LI);
		gw.writeShort((short) (2 + numArguments));
		gw.writeOpcode(Opcode.SUB);
		gw.writeOpcode(Opcode.MOVF);

		for (int i = 0; i < params.length; i++) {
			String[] param = params[i].trim().split(":", 2);
			if (param.length != 2) {
				throw new ADELACompilerException("Illegal function arguments");
			}
			if (!"uint".equals(param[1])) {
				throw new ADELACompilerException(
						"Only uints may be passed as parameters for now");
			}
			this.addresses.put(param[0], curAddr);
			curAddr += 1;
			curStack += 1;
		}
		/*
		 * here is the RET address. The RET address is placed AFTER the
		 * parameters but IN FRONT OF the local variables
		 */
		curAddr += 2;
		curStack += 2;
	}

	public void setLabel(String name) {
		this.gw.setLabel(name);
	}

	public void setGoto(String name, Opcode gotoOpcode) {
		this.gw.setGoto(name, gotoOpcode);
	}

	public void endFunction() {
		// MOV SP, BP (PUSH BP, POP SP);
		gw.writeOpcode(Opcode.FMOV);
		gw.writeOpcode(Opcode.LI);
		gw.writeShort((short) (2 + numArguments));
		gw.writeOpcode(Opcode.ADD);
		gw.writeOpcode(Opcode.MOVE);

		// POP BP
		gw.writeOpcode(Opcode.MOVF);

		// RET
		gw.writeOpcode(Opcode.JMP);
	}

	public void endFunctionWithRetval() {
		// POP EAX (return value)
		gw.writeOpcode(Opcode.MOVA);

		// MOV SP, BP (PUSH BP, POP SP);
		gw.writeOpcode(Opcode.FMOV);
		gw.writeOpcode(Opcode.LI);
		gw.writeShort((short) (2 + numArguments));
		gw.writeOpcode(Opcode.ADD);
		gw.writeOpcode(Opcode.MOVE);

		// POP BP
		gw.writeOpcode(Opcode.MOVF);

		// PUSH EAX (retval)
		gw.writeOpcode(Opcode.AMOV);

		// rotate BP, EAX
		gw.writeOpcode(Opcode.ROT);

		// RET
		gw.writeOpcode(Opcode.JMP);
	}

	public int getCurIP() {
		return gw.getCurIP();
	}

	public void writeOpcode(Opcode opcode) {
		gw.writeOpcode(opcode);
	}

	public void writeShort(short s) {
		gw.writeShort(s);
	}

	public void writeStatement(ADELAParseTree apt) throws ADELASyntaxException,
			ADELACompilerException {
		gw.annotate("  ===== " + apt.toString() + " =====  ");
		apt.compile(gw, this, logStream);
	}

	public void writeStatement(String statement) throws ADELASyntaxException,
			ADELACompilerException {
		ADELAParseTree apt = ADELAParseTree.Parse(statement);
		writeStatement(apt);
	}

	public void reserveStack() {
		int stackRequired = curAddr - curStack;
		logStream.println("Reserving " + stackRequired
				+ "+1 words on the stack");
		gw.writeOpcode(Opcode.LI);
		gw.writeShort((short) (1 + stackRequired));
		gw.writeOpcode(Opcode.SALLOC);
	}

	public int reserveBytes(String name, byte[] ba) {
		int pos = ds.position();
		ds.put(ba);
		logStream.println("Reserved " + ba.length + " bytes, position is now "
				+ ds.position());
		return pos;
	}

	public int reserveShort(String name) throws ADELACompilerException {
		Integer addr = addresses.get(name);
		if (addr != null) {
			return addr;
		}
		throw new ADELACompilerException(String.format(
				"Use of undeclared variable %s!", name));
	}

	public int declareShort(String name) {
		Integer addr = Integer.valueOf(curAddr);
		addresses.put(name, addr);
		logStream.printf("Reserved 2 bytes for \"%s\" at %d\n", name, curAddr);
		try {
			return curAddr;
		} finally {
			curAddr += 1;
		}
	}

	public int reserveShortGlobal(String name, int initializeWith) {
		Integer addr = gaddresses.get(name);
		if (addr != null) {
			return addr;
		}
		addr = Integer.valueOf(gcurAddr);
		gaddresses.put(name, addr);
		logStream.printf("Reserved 2 bytes for global \"%s\" at %d\n", name,
				gcurAddr);
		try {
			ids[gcurAddr] = (byte) (initializeWith >> 8);
			ids[gcurAddr + 1] = (byte) (initializeWith & 0xFF);
			return gcurAddr;
		} finally {
			gcurAddr += 1;
		}
	}

	public int globorloc(String name) throws ADELACompilerException {
		Integer addr = addresses.get(name);
		if (addr == null) {
			addr = gaddresses.get(name);
			if (addr == null) {
				throw new ADELACompilerException(
						"Variable "
								+ name
								+ " not declared, and global variables are not supported (only string constants, hrhrhr)");
			}
			return -((int) addr);
		}
		return addr;
	}

	public int declareArray(String name, Integer size) {
		Integer addr = Integer.valueOf(curAddr);
		addresses.put(name, addr);
		int words = size;
		logStream.printf("Reserved " + words + " words for \"%s\" at %d\n",
				name, curAddr);
		try {
			return curAddr;
		} finally {
			curAddr += words;
		}
	}

	public int reserveShortGlobal(String name) {
		// TODO Auto-generated method stub
		return 0;
	}
}
