// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;

public final class uSBX extends Instruction {

	public uSBX(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {

		cpu.debug(">>> Undocumented opcode SBX");

		ea = cpu.fetchImmediateAddress(); return 2;		
	}

	@Override
	public void execute() {
		byte b = (byte) (cpu.getA() & cpu.getX());
		int uB = M6502.toUnsignedByte(b);
		int uVal = M6502.toUnsignedByte(cpu.readByte(ea));
		byte newX = (byte)(uB - uVal);
		cpu.setX(newX);
		
		cpu.setCARRY(uB >= uVal);
		cpu.setZERO(newX == 0);
		cpu.setNEGATIVE(newX < 0);
	}

	private int ea;
	

	public static final long serialVersionUID = 1L;

}
