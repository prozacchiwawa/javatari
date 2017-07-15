// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;
import org.javatari.general.m6502.OperandType;

import static org.javatari.general.m6502.OperandType.*;

public final class LSR extends Instruction {

	public LSR(M6502 cpu, int type) {
		super(cpu);
		this.type = type;
	}

	@Override
	public int fetch() {
		if (type == OperandType.ACC) 		{ ea = -1; return 2; }
		if (type == OperandType.Z_PAGE) 	{ ea = cpu.fetchZeroPageAddress(); return 5; }
		if (type == OperandType.Z_PAGE_X) 	{ ea = cpu.fetchZeroPageXAddress(); return 6; }
		if (type == OperandType.ABS) 		{ ea = cpu.fetchAbsoluteAddress(); return 6; }
		if (type == OperandType.ABS_X) 		{ ea = cpu.fetchAbsoluteXAddress(); return 7; }
		throw new IllegalStateException("LSR Invalid Operand Type: " + type);
	}

	@Override
	public void execute() {
		// Special case for ACC
		if (type == ACC) {
			byte val = cpu.getA();
			cpu.setCARRY((val & 0x01) > 0);		// bit 0 was set
			val = (byte) ((val & 0xff) >>> 1);
			cpu.setA(val);
			cpu.setZERO(val == 0);
			cpu.setNEGATIVE(false);
		} else {
			byte val = cpu.readByte(ea);
			cpu.setCARRY((val & 0x01) != 0);		// bit 0 was set
			val = (byte) ((val & 0xff) >>> 1);
			cpu.setZERO(val == 0);
			cpu.setNEGATIVE(false);
			cpu.writeByte(ea, val);
		}
	}

	private final int type;
	
	private int ea;
	

	public static final long serialVersionUID = 1L;

}
