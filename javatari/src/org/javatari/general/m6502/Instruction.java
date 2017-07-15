// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502;

import java.io.Serializable;

public abstract class Instruction implements Serializable, Cloneable {

	public Instruction(M6502 cpu) {
		this.cpu = cpu;
	}

	public abstract int fetch();	// Should return the number of cycles needed to complete execution

	public abstract void execute();

	public int branchTarget() { return -1; }

	@Override
	protected Instruction clone() {
		try { 
			return (Instruction)super.clone(); 
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}

	public String getName() { return getClass().getSimpleName(); }

	protected transient M6502 cpu;

	
	public static final long serialVersionUID = 1L;

}
