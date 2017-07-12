// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;
import org.javatari.general.m6502.Register;

public final class INx extends Instruction {

	public INx(M6502 cpu, int reg) {
		super(cpu);
		this.reg = reg;
	}

	@Override
	public int fetch() {
		return 2;
	}

	@Override
	public void execute() {
		final byte val;
		if (reg == Register.rX) 		{ val = (byte) (cpu.getX() + 1); cpu.setX(val); }
		else if (reg == Register.rY) 	{ val = (byte) (cpu.getY() + 1); cpu.setY(val); }
		else throw new IllegalStateException("INx Invalid Register: " + reg);
		cpu.setZERO(val == 0);
		cpu.setNEGATIVE(val < 0);
	}

	private final int reg;
	

	public static final long serialVersionUID = 1L;

}
