// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;

public final class uASR extends Instruction {

	public uASR(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {

		cpu.debug(">>> Undocumented opcode ASR");

		ea = cpu.fetchImmediateAddress(); return 2;
	}

	@Override
	public void execute() {
		byte val = (byte) (cpu.getA() & cpu.bus.readByte(ea));
		cpu.setCARRY((val & 0x01) > 0);		// bit 0 was set
		val = (byte) ((val & 0xff) >>> 1);
		cpu.setA(val);
		cpu.setZERO(val == 0);
		cpu.setNEGATIVE(false);
	}

	private int ea;
	

	public static final long serialVersionUID = 1L;
	
}
