// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;

public final class uARR extends Instruction {

	public uARR(M6502 cpu) {
		super(cpu);
	}

	@Override
	public int fetch() {

		cpu.debug(">>> Undocumented opcode ARR");

		ea = cpu.fetchImmediateAddress(); return 2;
	}

	@Override
	// Some sources say flags are affected per ROR, others say its more complex. The complex one is chosen
	public void execute() {
		byte val = (byte) (cpu.getA() & cpu.readByte(ea));
		int oldCarry = cpu.isCARRY() ? 0x80 : 0;

		// Per ROR
		// cpu.CARRY = (val & 0x01) > 0;		// bit 0 was set

		val = (byte) (((val & 0xff) >>> 1) | oldCarry);
		cpu.setA(val);
		cpu.setZERO(val == 0);
		cpu.setNEGATIVE(val < 0);
		
		// Complex
		int comp = cpu.getA() & 0x60;
		if (comp == 0x60) 		{ cpu.setCARRY(true); cpu.setOVERFLOW(false); }
		else if (comp == 0x00) 	{ cpu.setCARRY(false); cpu.setOVERFLOW(false); }
		else if (comp == 0x20) 	{ cpu.setCARRY(false); cpu.setOVERFLOW(true); }
		else if (comp == 0x40) 	{ cpu.setCARRY(true); cpu.setOVERFLOW(true); }
	}

	private int ea;
	

	public static final long serialVersionUID = 1L;

}
