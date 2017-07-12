// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;

public final class uLXA extends Instruction {

	public uLXA(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {

		cpu.debug(">>> Undocumented opcode LXA");

		ea = cpu.fetchImmediateAddress(); return 2;		
	}
	
	@Override
	// Some sources say its an OR with $EE then AND with IMM, others exclude the OR, others exclude both the OR and the AND. Excluding just the OR
	public void execute() {
		byte val = (byte) (cpu.getA() /* | 0xEE) */ & cpu.bus.readByte(ea));
		cpu.setA(val);
		cpu.setX(val);
		cpu.setZERO(val == 0);
		cpu.setNEGATIVE(val < 0);
	}

	private int ea;

	
	public static final long serialVersionUID = 1L;

}
