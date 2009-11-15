package net.sourceforge.adela.govm;

public enum Opcode {
	SYSCALL(0), LI(1), JMP(2), JZ(3), LB(4), LW(5), SB(6), SW(7), ADD(8), SALLOC(
			9), DIV(10), NOR(11), POP(12), DUP(13), ROT(14), ROT3(15), MOVA(16), MOVB(
			17), MOVC(18), MOVD(19), MOVE(20), MOVF(21), AMOV(22), BMOV(23), CMOV(
			24), DMOV(25), EMOV(26), FMOV(27), CALL(28), LWS(29), SWS(30), SUB(
			31), NOT(32), EQU(33), LOE(34), GOE(35), LT(36), GT(37), AND(38), OR(
			39), SHL(40), SHR(41), MUL(42), NOP(43), XOR(44);

	private byte firstNibble;
	private byte secondNibble;

	private Opcode(int code) {
		byte bcode = (byte) code;
		firstNibble = (byte) (bcode & 0x07);
		if (bcode > 0x07) {
			firstNibble |= 0x08;
			secondNibble = (byte) (bcode >> 3);
		}
	}

	public byte getFirstNibble() {
		return firstNibble;
	}

	public byte getSecondNibble() {
		return secondNibble;
	}

}
