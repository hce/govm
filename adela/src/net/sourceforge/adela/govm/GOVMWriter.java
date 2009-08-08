package net.sourceforge.adela.govm;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import net.sourceforge.adela.interpreter.ADELACompilerException;

public class GOVMWriter {
	public enum BufferType {
		CS_Buffer, DS_Buffer
	}

	private int HEADERLEN;

	private ByteBuffer cs = ByteBuffer.allocate(65536);

	private ByteBuffer ds = ByteBuffer.allocate(65536);

	private BufferType bufferType = BufferType.CS_Buffer;

	private StringBuilder code_foo = new StringBuilder();

	HashMap<Integer, String> jumpMap = new HashMap<Integer, String>();

	private HashMap<String, Integer> labels = new HashMap<String, Integer>();
	
	private PrintStream logStream;

	public int getCurIP() {
		return this.cs.position() - HEADERLEN;
	}

	public GOVMWriter(PrintStream logStream) {
		this.logStream = logStream;
		writeHeader();
	}

	public void markJumpSource(String name) {
		jumpMap.put(this.cs.position(), name);
		writeShort((short) 0xFFFF);
	}

	public void writeShort(short s) {
		code_foo.append(" ");
		code_foo.append(s);
		code_foo.append("\n");
		writeByte((byte) (s >> 8));
		writeByte((byte) (s & 0xFF));
	}

	public void writeByte(byte b) {
		if (bufferType == BufferType.CS_Buffer) {
			cs.put((byte) (b >> 4));
			cs.put((byte) (b & 0x0F));
		} else if (bufferType == BufferType.DS_Buffer) {
			ds.put(b);
		}
	}

	public void writeNibble(byte nibble) {
		if (nibble > 15) {
			throw new IllegalArgumentException("nibble > 15");
		}
		code_foo.append('(');
		code_foo.append(nibble);
		code_foo.append(')');
		cs.put(nibble);
	}

	public void writeOpcode(Opcode opcode) {
		if (!code_foo.toString().endsWith("\n")) {
			code_foo.append("\n");
		}
		code_foo.append(opcode.toString());
		byte fn = opcode.getFirstNibble();
		byte sn = opcode.getSecondNibble();
		writeNibble(fn);
		if ((fn & 0x08) == 0x08) {
			writeNibble(sn);
		}
	}

	private void writeHeader() {
		/* Header */
		// writeByte((byte) 'G');
		// writeByte((byte) 'O');
		writeShort((short) ((71 << 8) + 79));
		writeByte((byte) 'V');
		writeByte((byte) 'M');
		/* Big endian */
		writeByte((byte) 0x11);
		writeShort((short) (1024 / 2));
		writeShort((short) 12);
		writeShort((short) 23);
		writeShort((short) 0); /* dummy */
		writeShort((short) 0); /* einsprungspunkt */
		HEADERLEN = this.cs.position();
		logStream.println("Header: " + HEADERLEN + " bytes");
	}

	public void resolveJumpMap(HashMap<String, Integer> jumpDestinations)
			throws ADELACompilerException {
		for (Integer i : this.jumpMap.keySet()) {
			String sym = this.jumpMap.get(i);
			Integer dest = jumpDestinations.get(sym);
			if (dest == null) {
				throw new ADELACompilerException("Label " + sym
						+ " not defined!");
			}
			int pos = this.cs.position();
			this.cs.position(i);
			writeShort((short) ((int) dest));
			this.cs.position(pos);
		}
		this.jumpMap = new HashMap<Integer, String>();
	}

	public byte[] getCS(int dataSegmentLength) {
		this.resolveJumpMap(labels);
		int ml = (this.cs.position() + 1) / 2;
		logStream.println("Codesegment: " + ml + " bytes");
		byte[] cs = this.cs.array();
		for (int i = 0; i < ml; i++) {
			cs[i] = (byte) ((cs[i * 2] << 4) + (cs[i * 2 + 1] & 0x0F));
		}
		int cs_size = (this.cs.position() - HEADERLEN + 1) / 2;
		cs[5] = (byte) (cs_size >> 8);
		cs[6] = (byte) (cs_size & 0x0FF);
		cs[7] = (byte) (dataSegmentLength >> 8);
		cs[8] = (byte) (dataSegmentLength & 0x0FF);
		cs[9] = cs[7];
		cs[10] = cs[8];
		logStream.println(code_foo.toString());
		try {
			byte[] retarr = new byte[ml];
			System.arraycopy(cs, 0, retarr, 0, ml);
			return retarr;
		} finally {
			this.cs = null;
		}
	}

	public void setGoto(String name, Opcode gotoOpcode) {
		this.writeOpcode(Opcode.LI);
		this.markJumpSource(name);
		this.writeOpcode(gotoOpcode);
		if (gotoOpcode == Opcode.JZ) {
			this.writeOpcode(Opcode.POP);
		}
	}

	public void setLabel(String name) {
		this.labels.put(name, this.getCurIP());
		logStream.println("Set a label " + name + " at IP " + this.getCurIP());
	}

	public void annotate(String string) {
		if (!code_foo.toString().endsWith("\n")) {
			code_foo.append("\n");
		}
		code_foo.append(string);
		code_foo.append("\n");
	}

}
