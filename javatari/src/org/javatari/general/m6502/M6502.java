// Copyright 2011-2012 Paulo Augusto Peccin. See licence.txt distributed with this file.

package org.javatari.general.m6502;

import static org.javatari.general.m6502.OperandType.ABS;
import static org.javatari.general.m6502.OperandType.ABS_X;
import static org.javatari.general.m6502.OperandType.ABS_Y;
import static org.javatari.general.m6502.OperandType.ACC;
import static org.javatari.general.m6502.OperandType.IMM;
import static org.javatari.general.m6502.OperandType.IND;
import static org.javatari.general.m6502.OperandType.IND_X;
import static org.javatari.general.m6502.OperandType.IND_Y;
import static org.javatari.general.m6502.OperandType.Z_PAGE;
import static org.javatari.general.m6502.OperandType.Z_PAGE_X;
import static org.javatari.general.m6502.OperandType.Z_PAGE_Y;
import static org.javatari.general.m6502.Register.rA;
import static org.javatari.general.m6502.Register.rSP;
import static org.javatari.general.m6502.Register.rX;
import static org.javatari.general.m6502.Register.rY;
import static org.javatari.general.m6502.StatusBit.bCARRY;
import static org.javatari.general.m6502.StatusBit.bDECIMAL_MODE;
import static org.javatari.general.m6502.StatusBit.bINTERRUPT_DISABLE;
import static org.javatari.general.m6502.StatusBit.bNEGATIVE;
import static org.javatari.general.m6502.StatusBit.bOVERFLOW;
import static org.javatari.general.m6502.StatusBit.bZERO;

import java.io.Serializable;

import org.javatari.general.board.BUS16Bits;
import org.javatari.general.board.Clock;
import org.javatari.general.board.ClockDriven;
import org.javatari.general.board.Trick2600;
import org.javatari.general.m6502.instructions.ADC;
import org.javatari.general.m6502.instructions.AND;
import org.javatari.general.m6502.instructions.ASL;
import org.javatari.general.m6502.instructions.BIT;
import org.javatari.general.m6502.instructions.BRK;
import org.javatari.general.m6502.instructions.Bxx;
import org.javatari.general.m6502.instructions.CLx;
import org.javatari.general.m6502.instructions.CPx;
import org.javatari.general.m6502.instructions.DEC;
import org.javatari.general.m6502.instructions.DEx;
import org.javatari.general.m6502.instructions.EOR;
import org.javatari.general.m6502.instructions.INC;
import org.javatari.general.m6502.instructions.INx;
import org.javatari.general.m6502.instructions.JMP;
import org.javatari.general.m6502.instructions.JSR;
import org.javatari.general.m6502.instructions.LDx;
import org.javatari.general.m6502.instructions.LSR;
import org.javatari.general.m6502.instructions.NOP;
import org.javatari.general.m6502.instructions.ORA;
import org.javatari.general.m6502.instructions.PHA;
import org.javatari.general.m6502.instructions.PHP;
import org.javatari.general.m6502.instructions.PLA;
import org.javatari.general.m6502.instructions.PLP;
import org.javatari.general.m6502.instructions.ROL;
import org.javatari.general.m6502.instructions.ROR;
import org.javatari.general.m6502.instructions.RTI;
import org.javatari.general.m6502.instructions.RTS;
import org.javatari.general.m6502.instructions.SBC;
import org.javatari.general.m6502.instructions.SEx;
import org.javatari.general.m6502.instructions.STx;
import org.javatari.general.m6502.instructions.Txx;
import org.javatari.general.m6502.instructions.uANC;
import org.javatari.general.m6502.instructions.uANE;
import org.javatari.general.m6502.instructions.uARR;
import org.javatari.general.m6502.instructions.uASR;
import org.javatari.general.m6502.instructions.uDCP;
import org.javatari.general.m6502.instructions.uISB;
import org.javatari.general.m6502.instructions.uKIL;
import org.javatari.general.m6502.instructions.uLAS;
import org.javatari.general.m6502.instructions.uLAX;
import org.javatari.general.m6502.instructions.uLXA;
import org.javatari.general.m6502.instructions.uNOP;
import org.javatari.general.m6502.instructions.uRLA;
import org.javatari.general.m6502.instructions.uRRA;
import org.javatari.general.m6502.instructions.uSAX;
import org.javatari.general.m6502.instructions.uSBX;
import org.javatari.general.m6502.instructions.uSHA;
import org.javatari.general.m6502.instructions.uSHS;
import org.javatari.general.m6502.instructions.uSHX;
import org.javatari.general.m6502.instructions.uSHY;
import org.javatari.general.m6502.instructions.uSLO;
import org.javatari.general.m6502.instructions.uSRE;
import org.javatari.utils.Debugger;


public final class M6502 implements ClockDriven {
	int t;
    int frame;

	public int getT() { return t; }

	public int getPC() { return PC; }

	public M6502() {
	}

	public void connectBus(BUS16Bits bus) {
		this.bus = bus;
		currentInstruction = instructions(toUnsignedByte(bus.readByte(PC)));	// Reads the instruction to be executed
	}
	public BUS16Bits getBus() { return bus; }
	public M6502 withBus(BUS16Bits bus) {
		M6502 c = new M6502();
		c.connectBus(bus);
		return c;
	}

	public void reset() {
		PC = memoryReadWord(POWER_ON_RESET_ADDRESS);
		INTERRUPT_DISABLE = true;
		currentInstruction = null;
		remainingCycles = -1;
	}
	
	/** This implementation executes all fetch operations on the FIRST cycle, 
	  * then read and write operations on the LAST cycle of each instruction, doing nothing in cycles in between */
	@Override
	public void clockPulse() {
		// If this is the last execution cycle of the instruction, execute it ignoring the !RDY signal 
		if (remainingCycles == 1) {
			// if (trace) showDebug(">>> TRACE");
			currentInstruction.execute();
			remainingCycles = 0;
			return;
		}
		if (!RDY) return;						// CPU is halted

        if ((getPC() & 0xfff) == 0x2cb && remainingCycles == 0) {
        	int controller = bus.readByte(0x280);
        	int switches = bus.readByte(0x282);
        	int paddle = bus.readByte(12);
        	int input = ((controller & 0xff) << 16) | ((switches & 0xff) << 8) | (paddle & 0xff);
            System.out.println("Frame " + frame++);
			System.out.println("Input " + String.format("%06x", input));
            System.out.println(printState());
            System.out.println("Timer " + String.format("0x%02x", bus.readByte(0xb3)) + "-" + String.format("0x%02x", bus.readByte(0xb5)) + "-" + String.format("0x%02x", bus.readByte(0xb7)));
            System.out.println("Position " + String.format("0x%02x", bus.readByte(0xba)) + "-" + String.format("0x%02x", bus.readByte(0xc2)));
            int i;
            for (i = 0x80; i < 0xe0; i++) {
                byte b = bus.readByte(i);
                if (i % 8 == 0) {
                    if (i != 0x80) {
                        System.out.println("]");
                    }
                    System.out.print(String.format("%02x", i) + " [");
                }
                System.out.print(String.format("0x%02x", (int)b & 0xff));
                if ((i % 8) != 7) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
            System.out.flush();
        }
            
		if (remainingCycles-- > 0) return;		// CPU is still "executing" remaining instruction cycles
		currentInstruction = instructions(toUnsignedByte(bus.readByte(PC++)));	// Reads the instruction to be executed
		remainingCycles = currentInstruction.fetch() - 1;						// One cycle was just executed already!
	}

	public void powerOn() {	// Initializes the CPU as if it were just powered on
		PC = 0;
		SP = STACK_INITIAL_SP;
		Y = 0;
		X = 0;
		A = 0;
		PS((byte)0);
		INTERRUPT_DISABLE = true;
		RDY = true;
		reset();
	}
	
	public void powerOff() {
		// Nothing
	}

	public void fetchImpliedAddress() {
		bus.readByte(PC);						// Worthless read, discard data. PC unchanged
	}											// TODO Make instructions call here

	public int fetchImmediateAddress() {		// No memory being read here!
		return PC++;
	}

	public int fetchRelativeAddress() {
		int res = bus.readByte(PC++) + PC;  	// PC should be get AFTER the increment and be added to the offset that was read
		pageCrossed = (res & 0xff00) != (PC & 0xff00);		// TODO Implement additional bad reads
		return res;		
	}

	public int fetchZeroPageAddress() {
		return toUnsignedByte(bus.readByte(PC++));
	}

	public int fetchZeroPageXAddress() {
		byte base = bus.readByte(PC++);
		bus.readByte(toUnsignedByte(base));		// Additional bad read, discard data
		return toUnsignedByte(base + getX());		// Sum should wrap the byte and always be in range 00 - ff
	}

	public int fetchZeroPageYAddress() {
		byte base = bus.readByte(PC++);
		bus.readByte(toUnsignedByte(base));		// Additional bad read, discard data
		return toUnsignedByte(base + getY());		// Sum should wrap the byte and always be in range 00 - ff
	}
	
	public int fetchAbsoluteAddress() {
		return memoryReadWord((PC+=2) - 2);		// PC should be get BEFORE the double increment 
	}

	public int fetchAbsoluteXAddress() {
		final int addr = fetchAbsoluteAddress();
		final int res = addr + toUnsignedByte(getX());
		pageCrossed = (res & 0xff00) != (addr & 0xff00);
		if (pageCrossed) bus.readByte((addr & 0xff00) | (res & 0x00ff));		// Additional bad read, discard data
		return res;
	}

	public int fetchAbsoluteYAddress() {
		final int addr = fetchAbsoluteAddress();
		final int res = addr + toUnsignedByte(getY());
		pageCrossed = (res & 0xff00) != (addr & 0xff00);
		if (pageCrossed) bus.readByte((addr & 0xff00) | (res & 0x00ff));		// Additional bad read, discard data
		return res;
	}

	public int fetchIndirectAddress() {
		return memoryReadWordWrappingPage(fetchAbsoluteAddress());				// Should wrap page reading effective address
	}

	public int fetchIndirectXAddress() {
		return memoryReadWordWrappingPage(fetchZeroPageXAddress());				// Should wrap page (the zero page) reading effective address
	}

	public int fetchIndirectYAddress() {
		final int addr = memoryReadWordWrappingPage(fetchZeroPageAddress());	// Should wrap page (the zero page) reading effective address
		final int res = addr + toUnsignedByte(getY());
		pageCrossed = (res & 0xff00) != (addr & 0xff00);
		if (pageCrossed) bus.readByte((addr & 0xff00) | (res & 0x00ff));		// Additional bad read, discard data
		return res; 
	}

	public int memoryReadWord(int address) {
		return toUnsignedByte(bus.readByte(address)) | (toUnsignedByte(bus.readByte(address + 1)) << 8);	// Address + 1 may wrap, LSB first
	}

	public int memoryReadWordWrappingPage(int address) {	// Accounts for the page-cross problem  (should wrap page)
		if ((address & 0xff) == 0xff)		
			// Get hi byte from the page-wrap &xx00 (addr + 1 wraps to beginning of page)
			return toUnsignedByte(bus.readByte(address)) | (toUnsignedByte(bus.readByte(address & 0xff00)) << 8);
		else
			return memoryReadWord(address);
	}

	public void pushByte(byte b) {
		bus.writeByte(STACK_PAGE + toUnsignedByte(getSP()), b);
		setSP((byte)(getSP() - 1));
	}

	public void dummyStackRead() {
		bus.readByte(STACK_PAGE + toUnsignedByte(getSP()));		// Additional dummy stack read before SP increment, discard data
	}
	
	public byte pullByte() {
		setSP((byte)(getSP() + 1));
		return bus.readByte(STACK_PAGE + toUnsignedByte(getSP()));
	}
	
	public void pushWord(int w) {
		pushByte((byte) (w >>> 8));
		pushByte((byte) w);
	}

	public int pullWord() {
		return (pullByte() & 0xff) | ((pullByte() & 0xff) << 8);
	}

	public byte PS() {
		byte b = (byte)(
			(NEGATIVE ?0x80:0) | (OVERFLOW ?0x40:0) | (DECIMAL_MODE ?0x08:0) |
			(INTERRUPT_DISABLE?0x04:0) | (ZERO ?0x02:0) | (CARRY ?0x01:0) |
			BREAK_COMMAND_FLAG	// Software instructions always push PS with BREAK_COMMAND set;
		);
		return b;
	}
	
	public void PS(byte b) {
		NEGATIVE = (b & 0x80) != 0; OVERFLOW = (b & 0x40) != 0;
		DECIMAL_MODE = (b & 0x08) != 0; INTERRUPT_DISABLE = (b & 0x04) != 0; ZERO = (b & 0x02) != 0; CARRY = (b & 0x01) != 0;
		// BREAK_COMMAND actually does not exist as a real flag
	}

	public M6502 next() {
		M6502State s = saveState();
		M6502 c = new M6502();
		c.loadState(s);
		if (bus.getClass().isAssignableFrom(Trick2600.class)) {
			Trick2600 t = (Trick2600)bus;
			c.connectBus(t.clone());
			c.step();
		}
		c.t = t + 1;
		return c;
	}

	public M6502[] futures() {
		M6502State s = saveState();
		M6502 c = new M6502();
		c.loadState(s);
		if (bus.getClass().isAssignableFrom(Trick2600.class)) {
			Trick2600 t = (Trick2600)bus;
			c.connectBus(t.clone());
			int []f = c.getFuturePC();
			M6502 []res = new M6502[f.length];
			for (int i = 0; i < f.length; i++) {
				s.PC = f[i];
				c = new M6502();
				c.loadState(s);
				c.connectBus(t.clone());
				c.step();
				res[i] = c;
			}
			return res;
		}
		return new M6502[] { c };
	}

	public M6502 setInput(int i) {
		byte controller = (byte)(i >> 16);
		byte switches = (byte)(i >> 8);
		byte paddle = (byte)i;
		M6502State s = saveState();
		M6502 c = new M6502();
		c.loadState(s);
		if (bus.getClass().isAssignableFrom(Trick2600.class)) {
			Trick2600 t = (Trick2600)bus;
			Trick2600 tc = (Trick2600)t.clone();
			tc.writeByte(640, controller);
			tc.writeByte(642, switches);
			tc.writeByte(12, paddle);
			c.connectBus(tc);
		}
		c.t = t;
		return c;
	}

	public int []getFuturePC() {
		Instruction i = instructions(toUnsignedByte(bus.readByte(PC++)));	// Reads the instruction to be executed
		i.fetch();
		int unbranched = PC;
		int branched = i.branchTarget();
		if (branched != -1) {
			return new int [] { unbranched, branched };
		} else {
			return new int [] { unbranched };
		}
	}

	public void step() {
		currentInstruction = instructions(toUnsignedByte(bus.readByte(PC++)));	// Reads the instruction to be executed
		currentInstruction.fetch();
		currentInstruction.execute();
	}

	public Trick2600 getCurrentBus() {
		if (bus.getClass().isAssignableFrom(Trick2600.class)) {
			return (Trick2600)bus;
		} else {
			return null;
		}
	}

	public Instruction getCurrentInstruction() {
		return currentInstruction;
	}

	public String printState() {
		String str = "";
		str = str +
		"T: " + String.format("%05x", 0) +
		", A: " + String.format("%02x", A) +
		", X: " + String.format("%02x", X) +
		", Y: " + String.format("%02x", Y) +
		", SP: " + String.format("%02x", SP) +
		", PC: " + String.format("%04x", (int)PC) +
		", Flags: " + String.format("%08d", Integer.parseInt(Integer.toBinaryString(PS() & 0xff))) +
		", Instr: " + (currentInstruction != null ? currentInstruction.getClass().getSimpleName() : "none" ) +  
		", RemCycles: " + remainingCycles;
		return str;
	}

	public String printMemory(int fromAddress, int count) {
		String str = "";
		for(int i = 0; i < count; i++)
			str = str + String.format("%02x ", bus.readByte(fromAddress + i));
		return str;
	}

	public void debug(String title) {
		if (debug) {
			System.out.println(title);
			if (trace) showDebug(title);
		}
	}
	
	public void showDebug(String title) {
		System.out.println("PROCESSOR PAUSED\n" + printState());
		int res;
		do {
			res = Debugger.show(title, "PROCESSOR STATUS:\n\n" + printState() + "\n\n", new String[] {"Continue", trace?"Stop Trace":"Start Trace", "Abort"}); 
			if (res == 1) trace = !trace;
		} while(res != 0 && res != 2);
		if (res == 2) ((Clock)Thread.currentThread()).terminate();
	}

	public M6502State saveState() {
		M6502State state = new M6502State();
		state.PC = PC; state.A = A; state.X = X; state.Y = Y; state.SP = SP;
		state.CARRY = CARRY; state.ZERO = ZERO; state.OVERFLOW = OVERFLOW; state.NEGATIVE = NEGATIVE;
		state.DECIMAL_MODE = DECIMAL_MODE; state.INTERRUPT_DISABLE = INTERRUPT_DISABLE;
		state.RDY = RDY;
		state.trace = trace; state.debug = debug;
		state.pageCrossed = pageCrossed;
		if (currentInstruction != null) state.currentInstruction = currentInstruction.clone();
		state.remainingCycles = remainingCycles;
		return state;
	}
	
	public void loadState(M6502State state) {
		PC = state.PC; A = state.A; X = state.X; Y = state.Y; SP = state.SP;
		CARRY = state.CARRY; ZERO = state.ZERO; OVERFLOW = state.OVERFLOW; NEGATIVE = state.NEGATIVE;
		DECIMAL_MODE = state.DECIMAL_MODE; INTERRUPT_DISABLE = state.INTERRUPT_DISABLE;
		RDY = state.RDY;
		trace = state.trace; debug = state.debug;
		pageCrossed = state.pageCrossed;
		currentInstruction = state.currentInstruction;
		if (currentInstruction != null)	currentInstruction.cpu = this;
		remainingCycles = state.remainingCycles;
	}

	// Public real 6502 registers and memory, for instructions and general access

	private byte A;
	private byte X;
	private byte Y;
	private byte SP;
	public int PC = 0x1000;			// Assumes anyone reading PC should mask it to 0xffff
	private boolean CARRY;
	private boolean ZERO;
	private boolean OVERFLOW;
	private boolean NEGATIVE;
	private boolean DECIMAL_MODE;
	public boolean INTERRUPT_DISABLE;
	public boolean RDY;		// RDY pin, used to halt the processor
	public BUS16Bits bus;


	// Auxiliary flags and variables for internal, debugging and instructions use
	
	public boolean trace = false;
	public boolean debug = false;
	public boolean pageCrossed = false;
	private int remainingCycles = -1;
	private Instruction currentInstruction;

	// Instructions map. # = Undocumented Instruction
	
	public Instruction instructions(int i) {
		switch (i & 0xff) {
        case 0x00: /* - BRK                  */  return new BRK(this);
        case 0x01: /* - ORA  - (Indirect,X)  */  return new ORA(this, IND_X);
        case 0x02: /* - uKIL                 */  return new uKIL(this);
        case 0x03: /* - uSLO - (Indirect,X)  */  return new uSLO(this, IND_X);
        case 0x04: /* - uNOP - Zero Page     */  return new uNOP(this, Z_PAGE);
        case 0x05: /* - ORA  - Zero Page     */  return new ORA(this, Z_PAGE);
        case 0x06: /* - ASL  - Zero Page     */  return new ASL(this, Z_PAGE);
        case 0x07: /* - uSLO - Zero Page     */  return new uSLO(this, Z_PAGE);
        case 0x08: /* - PHP                  */  return new PHP(this);
        case 0x09: /* - ORA  - Immediate     */  return new ORA(this, IMM);
        case 0x0A: /* - ASL  - Accumulator   */  return new ASL(this, ACC);
        case 0x0B: /* - uANC - Immediate     */  return new uANC(this);
        case 0x0C: /* - uNOP - Absolute      */  return new uNOP(this, ABS);
        case 0x0D: /* - ORA  - Absolute      */  return new ORA(this, ABS);
        case 0x0E: /* - ASL  - Absolute      */  return new ASL(this, ABS);
        case 0x0F: /* - uSLO - Absolute      */  return new uSLO(this, ABS);
        case 0x10: /* - BPL                  */  return new Bxx(this, bNEGATIVE, false);
        case 0x11: /* - ORA  - (Indirect),Y  */  return new ORA(this, IND_Y);
        case 0x12: /* - uKIL                 */  return new uKIL(this);
        case 0x13: /* - uSLO - (Indirect),Y  */  return new uSLO(this, IND_Y);
        case 0x14: /* - uNOP - Zero Page,X   */  return new uNOP(this, Z_PAGE_X);
        case 0x15: /* - ORA  - Zero Page,X   */  return new ORA(this, Z_PAGE_X);
        case 0x16: /* - ASL  - Zero Page,X   */  return new ASL(this, Z_PAGE_X);
        case 0x17: /* - uSLO - Zero Page,X   */  return new uSLO(this, Z_PAGE_X);
        case 0x18: /* - CLC                  */  return new CLx(this, bCARRY);
        case 0x19: /* - ORA  - Absolute,Y    */  return new ORA(this, ABS_Y);
        case 0x1A: /* - uNOP                 */  return new NOP(this);
        case 0x1B: /* - uSLO - Absolute,Y    */  return new uSLO(this, ABS_Y);
        case 0x1C: /* - uNOP - Absolute,X    */  return new uNOP(this, ABS_X);
        case 0x1D: /* - ORA  - Absolute,X    */  return new ORA(this, ABS_X);
        case 0x1E: /* - ASL  - Absolute,X    */  return new ASL(this, ABS_X);
        case 0x1F: /* - uSLO - Absolute,X    */  return new uSLO(this, ABS_X);
        case 0x20: /* - JSR                  */  return new JSR(this);
        case 0x21: /* - AND  - (Indirect,X)  */  return new AND(this, IND_X);
        case 0x22: /* - uKIL                 */  return new uKIL(this);
        case 0x23: /* - uRLA - (Indirect,X)  */  return new uRLA(this, IND_X);
        case 0x24: /* - BIT  - Zero Page     */  return new BIT(this, Z_PAGE);
        case 0x25: /* - AND  - Zero Page     */  return new AND(this, Z_PAGE);
        case 0x26: /* - ROL  - Zero Page     */  return new ROL(this, Z_PAGE);
        case 0x27: /* - uRLA - Zero Page     */  return new uRLA(this, Z_PAGE);
        case 0x28: /* - PLP                  */  return new PLP(this);
        case 0x29: /* - AND  - Immediate     */  return new AND(this, IMM);
        case 0x2A: /* - ROL  - Accumulator   */  return new ROL(this, ACC);
        case 0x2B: /* - uANC - Immediate     */  return new uANC(this);
        case 0x2C: /* - BIT  - Absolute      */  return new BIT(this, ABS);
        case 0x2D: /* - AND  - Absolute      */  return new AND(this, ABS);
        case 0x2E: /* - ROL  - Absolute      */  return new ROL(this, ABS);
        case 0x2F: /* - uRLA - Absolute      */  return new uRLA(this, ABS);
        case 0x30: /* - BMI                  */  return new Bxx(this, bNEGATIVE, true);
        case 0x31: /* - AND  - (Indirect),Y  */  return new AND(this, IND_Y);
        case 0x32: /* - uKIL                 */  return new uKIL(this);
        case 0x33: /* - uRLA - (Indirect),Y  */  return new uRLA(this, IND_Y);
        case 0x34: /* - uNOP - Zero Page,X   */  return new uNOP(this, Z_PAGE_X);
        case 0x35: /* - AND  - Zero Page,X   */  return new AND(this, Z_PAGE_X);
        case 0x36: /* - ROL  - Zero Page,X   */  return new ROL(this, Z_PAGE_X);
        case 0x37: /* - uRLA - Zero Page,X   */  return new uRLA(this, Z_PAGE_X);
        case 0x38: /* - SEC                  */  return new SEx(this, bCARRY);
        case 0x39: /* - AND  - Absolute,Y    */  return new AND(this, ABS_Y);
        case 0x3A: /* - uNOP                 */  return new NOP(this);
        case 0x3B: /* - uRLA - Absolute,Y    */  return new uRLA(this, ABS_Y);
        case 0x3C: /* - uNOP - Absolute,X    */  return new uNOP(this, ABS_X);
        case 0x3D: /* - AND  - Absolute,X    */  return new AND(this, ABS_X);
        case 0x3E: /* - ROL  - Absolute,X    */  return new ROL(this, ABS_X);
        case 0x3F: /* - uRLA - Absolute,X    */  return new uRLA(this, ABS_X);
        case 0x40: /* - RTI                  */  return new RTI(this);
        case 0x41: /* - EOR  - (Indirect,X)  */  return new EOR(this, IND_X);
        case 0x42: /* - uKIL                 */  return new uKIL(this);
        case 0x43: /* - uSRE - (Indirect,X)  */  return new uSRE(this, IND_X);
        case 0x44: /* - uNOP - Zero Page     */  return new uNOP(this, Z_PAGE);
        case 0x45: /* - EOR  - Zero Page     */  return new EOR(this, Z_PAGE);
        case 0x46: /* - LSR  - Zero Page     */  return new LSR(this, Z_PAGE);
        case 0x47: /* - uSRE - Zero Page     */  return new uSRE(this, Z_PAGE);
        case 0x48: /* - PHA                  */  return new PHA(this);
        case 0x49: /* - EOR  - Immediate     */  return new EOR(this, IMM);
        case 0x4A: /* - LSR  - Accumulator   */  return new LSR(this, ACC);
        case 0x4B: /* - uASR - Immediate     */  return new uASR(this);
        case 0x4C: /* - JMP  - Absolute      */  return new JMP(this, ABS);
        case 0x4D: /* - EOR  - Absolute      */  return new EOR(this, ABS);
        case 0x4E: /* - LSR  - Absolute      */  return new LSR(this, ABS);
        case 0x4F: /* - uSRE - Absolute      */  return new uSRE(this, ABS);
        case 0x50: /* - BVC                  */  return new Bxx(this, bOVERFLOW, false);
        case 0x51: /* - EOR  - (Indirect),Y  */  return new EOR(this, IND_Y);
        case 0x52: /* - uKIL                 */  return new uKIL(this);
        case 0x53: /* - uSRE - (Indirect),Y  */  return new uSRE(this, IND_Y);
        case 0x54: /* - uNOP - Zero Page,X   */  return new uNOP(this, Z_PAGE_X);
        case 0x55: /* - EOR  - Zero Page,X   */  return new EOR(this, Z_PAGE_X);
        case 0x56: /* - LSR  - Zero Page,X   */  return new LSR(this, Z_PAGE_X);
        case 0x57: /* - uSRE - Zero Page,X   */  return new uSRE(this, Z_PAGE_X);
        case 0x58: /* - CLI                  */  return new CLx(this, bINTERRUPT_DISABLE);
        case 0x59: /* - EOR  - Absolute,Y    */  return new EOR(this, ABS_Y);
        case 0x5A: /* - uNOP                 */  return new NOP(this);
        case 0x5B: /* - uSRE - Absolute,Y    */  return new uSRE(this, ABS_Y);
        case 0x5C: /* - uNOP - Absolute,X    */  return new uNOP(this, ABS_X);
        case 0x5D: /* - EOR  - Absolute,X    */  return new EOR(this, ABS_X);
        case 0x5E: /* - LSR  - Absolute,X    */  return new LSR(this, ABS_X);
        case 0x5F: /* - uSRE - Absolute,X    */  return new uSRE(this, ABS_X);
        case 0x60: /* - RTS                  */  return new RTS(this);
        case 0x61: /* - ADC  - (Indirect,X)  */  return new ADC(this, IND_X);
        case 0x62: /* - uKIL                 */  return new uKIL(this);
        case 0x63: /* - uRRA - (Indirect,X)  */  return new uRRA(this, IND_X);
        case 0x64: /* - uNOP - Zero Page     */  return new uNOP(this, Z_PAGE);
        case 0x65: /* - ADC  - Zero Page     */  return new ADC(this, Z_PAGE);
        case 0x66: /* - ROR  - Zero Page     */  return new ROR(this, Z_PAGE);
        case 0x67: /* - uRRA - Zero Page     */  return new uRRA(this, Z_PAGE);
        case 0x68: /* - PLA                  */  return new PLA(this);
        case 0x69: /* - ADC  - Immediate     */  return new ADC(this, IMM);
        case 0x6A: /* - ROR  - Accumulator   */  return new ROR(this, ACC);
        case 0x6B: /* - uARR - Immediate     */  return new uARR(this);
        case 0x6C: /* - JMP  - Indirect      */  return new JMP(this, IND);
        case 0x6D: /* - ADC  - Absolute      */  return new ADC(this, ABS);
        case 0x6E: /* - ROR  - Absolute      */  return new ROR(this, ABS);
        case 0x6F: /* - uRRA - Absolute      */  return new uRRA(this, ABS);
        case 0x70: /* - BVS                  */  return new Bxx(this, bOVERFLOW, true);
        case 0x71: /* - ADC  - (Indirect),Y  */  return new ADC(this, IND_Y);
        case 0x72: /* - uKIL                 */  return new uKIL(this);
        case 0x73: /* - uRRA - (Indirect),Y  */  return new uRRA(this, IND_Y);
        case 0x74: /* - uNOP - Zero Page,X   */  return new uNOP(this, Z_PAGE_X);
        case 0x75: /* - ADC  - Zero Page,X   */  return new ADC(this, Z_PAGE_X);
        case 0x76: /* - ROR  - Zero Page,X   */  return new ROR(this, Z_PAGE_X);
        case 0x77: /* - uRRA - Zero Page,X   */  return new uRRA(this, Z_PAGE_X);
        case 0x78: /* - SEI                  */  return new SEx(this, bINTERRUPT_DISABLE);
        case 0x79: /* - ADC  - Absolute,Y    */  return new ADC(this, ABS_Y);
        case 0x7A: /* - uNOP                 */  return new NOP(this);
        case 0x7B: /* - uRRA - Absolute,Y    */  return new uRRA(this, ABS_Y);
        case 0x7C: /* - uNOP - Absolute,X    */  return new uNOP(this, ABS_X);
        case 0x7D: /* - ADC  - Absolute,X    */  return new ADC(this, ABS_X);
        case 0x7E: /* - ROR  - Absolute,X    */  return new ROR(this, ABS_X);
        case 0x7F: /* - uRRA - Absolute,X    */  return new uRRA(this, ABS_X);
        case 0x80: /* - uNOP - Immediate     */  return new uNOP(this, IMM);
        case 0x81: /* - STA  - (Indirect,X)  */  return new STx(this, rA, IND_X);
        case 0x82: /* - uNOP - Immediate     */  return new uNOP(this, IMM);
        case 0x83: /* - uSAX - (Indirect,X)  */  return new uSAX(this, IND_X);
        case 0x84: /* - STY  - Zero Page     */  return new STx(this, rY, Z_PAGE);
        case 0x85: /* - STA  - Zero Page     */  return new STx(this, rA, Z_PAGE);
        case 0x86: /* - STX  - Zero Page     */  return new STx(this, rX, Z_PAGE);
        case 0x87: /* - uSAX - Zero Page     */  return new uSAX(this, Z_PAGE);
        case 0x88: /* - DEY                  */  return new DEx(this, rY);
        case 0x89: /* - uNOP - Immediate     */  return new uNOP(this, IMM);
        case 0x8A: /* - TXA                  */  return new Txx(this, rX, rA);
        case 0x8B: /* - uANE - Immediate     */  return new uANE(this);
        case 0x8C: /* - STY  - Absolute      */  return new STx(this, rY, ABS);
        case 0x8D: /* - STA  - Absolute      */  return new STx(this, rA, ABS);
        case 0x8E: /* - STX  - Absolute      */  return new STx(this, rX, ABS);
        case 0x8F: /* - uSAX - Absolute      */  return new uSAX(this, ABS);
        case 0x90: /* - BCC                  */  return new Bxx(this, bCARRY, false);
        case 0x91: /* - STA  - (Indirect),Y  */  return new STx(this, rA, IND_Y);
        case 0x92: /* - uKIL                 */  return new uKIL(this);        // Only Implied
        case 0x93: /* - uSHA - (Indirect),Y  */  return new uSHA(this, IND_Y);
        case 0x94: /* - STY  - Zero Page,X   */  return new STx(this, rY, Z_PAGE_X);
        case 0x95: /* - STA  - Zero Page,X   */  return new STx(this, rA, Z_PAGE_X);
        case 0x96: /* - STX  - Zero Page,Y   */  return new STx(this, rX, Z_PAGE_Y);
        case 0x97: /* - uSAX - Zero Page,Y   */  return new uSAX(this, Z_PAGE_Y);
        case 0x98: /* - TYA                  */  return new Txx(this, rY, rA);
        case 0x99: /* - STA  - Absolute,Y    */  return new STx(this, rA, ABS_Y);
        case 0x9A: /* - TXS                  */  return new Txx(this, rX, rSP);
        case 0x9B: /* - uSHS - Absolute,Y    */  return new uSHS(this);
        case 0x9C: /* - uSHY - Absolute,X    */  return new uSHY(this);
        case 0x9D: /* - STA  - Absolute,X    */  return new STx(this, rA, ABS_X);
        case 0x9E: /* - uSHX - Absolute,Y    */  return new uSHX(this);
        case 0x9F: /* - uSHA - Absolute, Y   */  return new uSHA(this, ABS_Y);
        case 0xA0: /* - LDY  - Immediate     */  return new LDx(this, rY, IMM);
        case 0xA1: /* - LDA  - (Indirect,X)  */  return new LDx(this, rA, IND_X);
        case 0xA2: /* - LDX  - Immediate     */  return new LDx(this, rX, IMM);
        case 0xA3: /* - uLAX - (Indirect,X)  */  return new uLAX(this, IND_X);
        case 0xA4: /* - LDY  - Zero Page     */  return new LDx(this, rY, Z_PAGE);
        case 0xA5: /* - LDA  - Zero Page     */  return new LDx(this, rA, Z_PAGE);
        case 0xA6: /* - LDX  - Zero Page     */  return new LDx(this, rX, Z_PAGE);
        case 0xA7: /* - uLAX - Zero Page     */  return new uLAX(this, Z_PAGE);
        case 0xA8: /* - TAY                  */  return new Txx(this, rA, rY);
        case 0xA9: /* - LDA  - Immediate     */  return new LDx(this, rA, IMM);
        case 0xAA: /* - TAX                  */  return new Txx(this, rA, rX);
        case 0xAB: /* - uLXA - Immediate     */  return new uLXA(this);
        case 0xAC: /* - LDY  - Absolute      */  return new LDx(this, rY, ABS);
        case 0xAD: /* - LDA  - Absolute      */  return new LDx(this, rA, ABS);
        case 0xAE: /* - LDX  - Absolute      */  return new LDx(this, rX, ABS);
        case 0xAF: /* - uLAX - Absolute      */  return new uLAX(this, ABS);
        case 0xB0: /* - BCS                  */  return new Bxx(this, bCARRY, true);
        case 0xB1: /* - LDA  - (Indirect),Y  */  return new LDx(this, rA, IND_Y);
        case 0xB2: /* - uKIL                 */  return new uKIL(this);
        case 0xB3: /* - uLAX - (Indirect),Y  */  return new uLAX(this, IND_Y);
        case 0xB4: /* - LDY  - Zero Page,X   */  return new LDx(this, rY, Z_PAGE_X);
        case 0xB5: /* - LDA  - Zero Page,X   */  return new LDx(this, rA, Z_PAGE_X);
        case 0xB6: /* - LDX  - Zero Page,Y   */  return new LDx(this, rX, Z_PAGE_Y);
        case 0xB7: /* - uLAX - Zero Page,Y   */  return new uLAX(this, Z_PAGE_Y);
        case 0xB8: /* - CLV                  */  return new CLx(this, bOVERFLOW);
        case 0xB9: /* - LDA  - Absolute,Y    */  return new LDx(this, rA, ABS_Y);
        case 0xBA: /* - TSX                  */  return new Txx(this, rSP, rX);
        case 0xBB: /* - uLAS - Absolute,Y    */  return new uLAS(this);
        case 0xBC: /* - LDY  - Absolute,X    */  return new LDx(this, rY, ABS_X);
        case 0xBD: /* - LDA  - Absolute,X    */  return new LDx(this, rA, ABS_X);
        case 0xBE: /* - LDX  - Absolute,Y    */  return new LDx(this, rX, ABS_Y);
        case 0xBF: /* - uLAX - Absolute,Y    */  return new uLAX(this, ABS_Y);
        case 0xC0: /* - CPY  - Immediate     */  return new CPx(this, rY, IMM);
        case 0xC1: /* - CMP  - (Indirect,X)  */  return new CPx(this, rA, IND_X);
        case 0xC2: /* - uNOP - Immediate     */  return new uNOP(this, IMM);
        case 0xC3: /* - uDCP - (Indirect,X)  */  return new uDCP(this, IND_X);
        case 0xC4: /* - CPY  - Zero Page     */  return new CPx(this, rY, Z_PAGE);
        case 0xC5: /* - CMP  - Zero Page     */  return new CPx(this, rA, Z_PAGE);
        case 0xC6: /* - DEC  - Zero Page     */  return new DEC(this, Z_PAGE);
        case 0xC7: /* - uDCP - Zero Page     */  return new uDCP(this, Z_PAGE);
        case 0xC8: /* - INY                  */  return new INx(this, rY);
        case 0xC9: /* - CMP  - Immediate     */  return new CPx(this, rA, IMM);
        case 0xCA: /* - DEX                  */  return new DEx(this, rX);
        case 0xCB: /* - uSBX - Immediate     */  return new uSBX(this);
        case 0xCC: /* - CPY  - Absolute      */  return new CPx(this, rY, ABS);
        case 0xCD: /* - CMP  - Absolute      */  return new CPx(this, rA, ABS);
        case 0xCE: /* - DEC  - Absolute      */  return new DEC(this, ABS);
        case 0xCF: /* - uDCP - Absolute      */  return new uDCP(this, ABS);
        case 0xD0: /* - BNE                  */  return new Bxx(this, bZERO, false);
        case 0xD1: /* - CMP  - (Indirect),Y  */  return new CPx(this, rA, IND_Y);
        case 0xD2: /* - uKIL                 */  return new uKIL(this);
        case 0xD3: /* - uDCP - (Indirect),Y  */  return new uDCP(this, IND_Y);
        case 0xD4: /* - uNOP - Zero Page,X   */  return new uNOP(this, Z_PAGE_X);
        case 0xD5: /* - CMP  - Zero Page,X   */  return new CPx(this, rA, Z_PAGE_X);
        case 0xD6: /* - DEC  - Zero Page,X   */  return new DEC(this, Z_PAGE_X);
        case 0xD7: /* - uDCP - Zero Page, X  */  return new uDCP(this, Z_PAGE_X);
        case 0xD8: /* - CLD                  */  return new CLx(this, bDECIMAL_MODE);
        case 0xD9: /* - CMP  - Absolute,Y    */  return new CPx(this, rA, ABS_Y);
        case 0xDA: /* - uNOP                 */  return new NOP(this);
        case 0xDB: /* - uDCP - Absolute,Y    */  return new uDCP(this, ABS_Y);
        case 0xDC: /* - uNOP - Absolute,X    */  return new uNOP(this, ABS_X);
        case 0xDD: /* - CMP  - Absolute,X    */  return new CPx(this, rA, ABS_X);
        case 0xDE: /* - DEC  - Absolute,X    */  return new DEC(this, ABS_X);
        case 0xDF: /* - uDCP - Absolute,X    */  return new uDCP(this, ABS_X);
        case 0xE0: /* - CPX  - Immediate     */  return new CPx(this, rX, IMM);
        case 0xE1: /* - SBC  - (Indirect,X)  */  return new SBC(this, IND_X);
        case 0xE2: /* - uNOP - Immediate     */  return new uNOP(this, IMM);
        case 0xE3: /* - uISB - (Indirect,X)  */  return new uISB(this, IND_X);
        case 0xE4: /* - CPX  - Zero Page     */  return new CPx(this, rX, Z_PAGE);
        case 0xE5: /* - SBC  - Zero Page     */  return new SBC(this, Z_PAGE);
        case 0xE6: /* - INC  - Zero Page     */  return new INC(this, Z_PAGE);
        case 0xE7: /* - uISB - Zero Page     */  return new uISB(this, Z_PAGE);
        case 0xE8: /* - INX                  */  return new INx(this, rX);
        case 0xE9: /* - SBC  - Immediate     */  return new SBC(this, IMM);
        case 0xEA: /* - NOP                  */  return new NOP(this);
        case 0xEB: /* - uSBC - Immediate     */  return new SBC(this, IMM);
        case 0xEC: /* - CPX  - Absolute      */  return new CPx(this, rX, ABS);
        case 0xED: /* - SBC  - Absolute      */  return new SBC(this, ABS);
        case 0xEE: /* - INC  - Absolute      */  return new INC(this, ABS);
        case 0xEF: /* - uISB - Absolute      */  return new uISB(this, ABS);
        case 0xF0: /* - BEQ                  */  return new Bxx(this, bZERO, true);
        case 0xF1: /* - SBC  - (Indirect),Y  */  return new SBC(this, IND_Y);
        case 0xF2: /* - uKIL                 */  return new uKIL(this);
        case 0xF3: /* - uISB - (Indirect),Y  */  return new uISB(this, IND_Y);
        case 0xF4: /* - uNOP - Zero Page,X   */  return new uNOP(this, Z_PAGE_X);
        case 0xF5: /* - SBC  - Zero Page,X   */  return new SBC(this, Z_PAGE_X);
        case 0xF6: /* - INC  - Zero Page,X   */  return new INC(this, Z_PAGE_X);
        case 0xF7: /* - uISB - Zero Page,X   */  return new uISB(this, Z_PAGE_X);
        case 0xF8: /* - SED                  */  return new SEx(this, bDECIMAL_MODE);
        case 0xF9: /* - SBC  - Absolute,Y    */  return new SBC(this, ABS_Y);
        case 0xFA: /* - uNOP                 */  return new NOP(this);
        case 0xFB: /* - uISB - Absolute,Y    */  return new uISB(this, ABS_Y);
        case 0xFC: /* - uNOP - Absolute,X    */  return new uNOP(this, ABS_X);
        case 0xFD: /* - SBC  - Absolute,X    */  return new SBC(this, ABS_X);
        case 0xFE: /* - INC  - Absolute,X    */  return new INC(this, ABS_X);
        case 0xFF: /* - uISB - Absolute,X    */  return new uISB(this, ABS_X);
		}
        return null;
	}

	// Constants
	public static final byte STACK_INITIAL_SP = (byte)0xff;
	public static final int STACK_PAGE = 0x0100;
	public static final byte BREAK_COMMAND_FLAG = 0x10;
	
	// Vectors
	public static final int NMI_HANDLER_ADDRESS = 0xfffa;
	public static final int POWER_ON_RESET_ADDRESS = 0xfffc;
	public static final int IRQ_HANDLER_ADDRESS = 0xfffe;

	
	// Convenience methods
	public static int toUnsignedByte(byte b) {	// ** NOTE does not return a real byte for signed operations
		return b & 0xff;
	}
	public static int toUnsignedByte(int i) {	// ** NOTE does not return a real byte for signed operations
		return i & 0xff;
	}

	private boolean RA, RX, RY, rC, rZ, rV, rN, rD, RSP;
	private boolean wA, wX, wY, wC, wZ, wV, wN, wD, wSP;

	public String[] getInstructionSources() {
		String[] res = {
				RA ? "rA" : "",
				RX ? "rX" : "",
				RY ? "rY" : "",
				rC ? "rC" : "",
				rZ ? "rZ" : "",
				rV ? "rV" : "",
				rD ? "rD" : "",
				RSP ? "rSP" : "",

				wA ? "wA" : "",
				wX ? "wX" : "",
				wY ? "wY" : "",
				wC ? "wC" : "",
				wZ ? "wZ" : "",
				wV ? "wV" : "",
				wD ? "wD" : "",
				wSP ? "wSP" : "",
		};
		return res;
	}

	public byte getA() {
		RA = true;
		return A;
	}

	public void setA(byte a) {
		wA = true;
		A = a;
	}

	public byte getX() {
		RX = true;
		return X;
	}

	public void setX(byte x) {
		wX = true;
		X = x;
	}

	public byte getY() {
		RY = true;
		return Y;
	}

	public void setY(byte y) {
		wY = true;
		Y = y;
	}

	public byte getSP() {
		RSP = true;
		return SP;
	}

	public void setSP(byte SP) {
		wSP = true;
		this.SP = SP;
	}

	public boolean isCARRY() {
		rC = true;
		return CARRY;
	}

	public void setCARRY(boolean CARRY) {
		wC = true;
		this.CARRY = CARRY;
	}

	public boolean isZERO() {
		rZ = true;
		return ZERO;
	}

	public void setZERO(boolean ZERO) {
		wZ = true;
		this.ZERO = ZERO;
	}

	public boolean isOVERFLOW() {
		rV = true;
		return OVERFLOW;
	}

	public void setOVERFLOW(boolean OVERFLOW) {
		wV = true;
		this.OVERFLOW = OVERFLOW;
	}

	public boolean isNEGATIVE() {
		rN = true;
		return NEGATIVE;
	}

	public void setNEGATIVE(boolean NEGATIVE) {
		wN = true;
		this.NEGATIVE = NEGATIVE;
	}

	public boolean isDECIMAL_MODE() {
		rD = true;
		return DECIMAL_MODE;
	}

	public void setDECIMAL_MODE(boolean DECIMAL_MODE) {
		wD = true;
		this.DECIMAL_MODE = DECIMAL_MODE;
	}

	// Used to save/load states
	public static class M6502State implements Serializable {
		public byte A;
		public byte X;
		public byte Y;
		public byte SP;
		public int PC;
		private boolean CARRY;
		private boolean ZERO;
		private boolean OVERFLOW;
		private boolean NEGATIVE;
		boolean DECIMAL_MODE;
		boolean INTERRUPT_DISABLE;
		boolean BREAK_COMMAND;
		boolean RDY;
		boolean trace;
		boolean debug;
		boolean pageCrossed;
		Instruction currentInstruction;
		int remainingCycles;

		public static final long serialVersionUID = 2L;
	}

}
