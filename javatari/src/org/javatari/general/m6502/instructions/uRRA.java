// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502.instructions;

import org.javatari.general.m6502.Instruction;
import org.javatari.general.m6502.M6502;
import org.javatari.general.m6502.OperandType;

public final class uRRA extends Instruction {

	public uRRA(M6502 cpu, int type) {
		super(cpu);
		this.type = type;
	}

	@Override
	public int fetch() {

		cpu.debug(">>> Undocumented opcode RRA");

		if (type == OperandType.Z_PAGE) 	{ ea = cpu.fetchZeroPageAddress(); return 5; }
		if (type == OperandType.Z_PAGE_X) 	{ ea = cpu.fetchZeroPageXAddress(); return 6; }
		if (type == OperandType.ABS) 		{ ea = cpu.fetchAbsoluteAddress(); return 6; }
		if (type == OperandType.ABS_X) 		{ ea = cpu.fetchAbsoluteXAddress(); return 7; }
		if (type == OperandType.ABS_Y) 		{ ea = cpu.fetchAbsoluteYAddress(); return 7; }
		if (type == OperandType.IND_X) 		{ ea = cpu.fetchIndirectXAddress(); return 8; }
		if (type == OperandType.IND_Y) 		{ ea = cpu.fetchIndirectYAddress(); return 8; }
		throw new IllegalStateException("uRRA Invalid Operand Type: " + type);
	}

	@Override
	public void execute() {
		byte val = cpu.readByte(ea);
		final int oldCarry = cpu.isCARRY() ? 0x80 : 0;
		cpu.setCARRY((val & 0x01) != 0);		// bit 0 was set
		val = (byte) (((val & 0xff) >>> 1) | oldCarry);
		cpu.writeByte(ea, val);

		// Same as ADC from here
		final int b = val;
		final int uB = M6502.toUnsignedByte(b);
		final int oldA = cpu.getA();
		final int uOldA = M6502.toUnsignedByte(oldA);

		int aux = oldA + b + (cpu.isCARRY() ?1:0);
		int uAux = uOldA + uB + (cpu.isCARRY() ?1:0);

		// ZERO flag is affected always as in Binary mode
		final byte newA = (byte) M6502.toUnsignedByte(uAux);		// Could be aux 
		cpu.setZERO(newA == 0);

		// But the others flags and the ACC are computed differently in Decimal Mode
		if (!cpu.isDECIMAL_MODE()) {
			cpu.setNEGATIVE(newA < 0);
			cpu.setOVERFLOW(aux > 127 || aux < -128);
			cpu.setCARRY(uAux > 0xff);
			cpu.setA(newA);
			return;
		}

		// Decimal Mode computations
		uAux = (uOldA & 0x0f) + (uB & 0x0f) + (cpu.isCARRY() ?1:0);
		if (uAux >= 0x0A) uAux = ((uAux + 0x06) & 0x0f) + 0x10;
		aux = (byte)(uOldA & 0xf0) + (byte)(uB & 0xf0) + (byte)uAux;     // Holy shit, that was the *unsigned* operation
		cpu.setNEGATIVE((aux & 0x80) > 0);
		cpu.setOVERFLOW((aux > 127) | (aux < -128));
		uAux = (uOldA & 0xf0) + (uB & 0xf0) + uAux;
		if (uAux >= 0xA0) uAux += 0x60;
		cpu.setCARRY(uAux > 0xff);
		cpu.setA((byte) M6502.toUnsignedByte(uAux));
	}

	private final int type;
	
	private int ea;
	

	public static final long serialVersionUID = 1L;

}
