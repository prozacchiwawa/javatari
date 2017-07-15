// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;

public final class uANC extends Instruction {

	public uANC(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {

		cpu.debug(">>> Undocumented opcode ANC");

		ea = cpu.fetchImmediateAddress(); return 2;		
	}
	
	@Override
	public void execute() {
		final byte val = (byte) (cpu.getA() & cpu.readByte(ea));
		cpu.setA(val);
		cpu.setZERO(val == 0);
		boolean v = val < 0;
		cpu.setNEGATIVE(v);
		cpu.setCARRY(v);
	}

	private int ea;

	
	public static final long serialVersionUID = 1L;

}
