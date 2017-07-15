// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;

public final class uSHY extends Instruction {

	public uSHY(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {

		cpu.debug(">>> Undocumented opcode SHY");

		ea = cpu.fetchAbsoluteXAddress(); return 5;		
	}
	
	@Override
	public void execute() {
		final byte val = (byte) (cpu.getY() & (byte)(((ea >>> 8) & 0xff) + 1));  // Y & (High byte of address + 1) !!!
		cpu.writeByte(ea, val);
	}

	private int ea;

	
	public static final long serialVersionUID = 1L;

}
