// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;

public final class PLA extends Instruction {

	public PLA(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {
		return 4;
	}

	@Override
	public void execute() {
		cpu.dummyStackRead();
		byte val = cpu.pullByte();
		cpu.setA(val);
		cpu.setNEGATIVE(val < 0);
		cpu.setZERO(val == 0);
	}
	

	public static final long serialVersionUID = 1L;

}
