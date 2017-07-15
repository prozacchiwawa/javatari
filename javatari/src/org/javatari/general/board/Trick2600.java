package org.javatari.general.board;

import org.javatari.atari.board.BUS;

import java.io.IOException;
import java.util.HashMap;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Created by arty on 7/12/17.
 */
public class Trick2600 implements BUS16Bits {
    byte []arr = new byte[256 + 32];
    byte []rom;
    HashMap<Integer, Boolean> r = new HashMap<Integer,Boolean>();
    HashMap<Integer, Boolean> w = new HashMap<Integer,Boolean>();

    public Trick2600() {
    }
    public Trick2600(Trick2600 t) {
        arr = t.arr.clone();
        rom = t.rom;
    }
    public Trick2600(String rom) {
        try {
            FileInputStream bis = new FileInputStream(rom);
            this.rom = new byte[4096];
            bis.read(this.rom);
            bis.close();
        } catch (IOException e) {
            //
        }
    }

    public BUS16Bits clone() {
        return new Trick2600(this);
    }

    public int[] getReads() {
        int[] rr = new int [r.size()];
        int i = 0;
        for (Map.Entry<Integer,Boolean> entry : r.entrySet()) {
            rr[i++] = entry.getKey();
        }
        return rr;
    }

    public int[] getWrites() {
        int[] rr = new int [w.size()];
        int i = 0;
        for (Map.Entry<Integer,Boolean> entry : w.entrySet()) {
            rr[i++] = entry.getKey();
        }
        return rr;
    }

    public byte[] get() { return arr; }

    @Override
    public byte readByte(int address) {
        address &= 0x1fff;
        if (address < 0x200) {
            r.put(address & 0xff, true);
            return arr[address & 0xff];
        } else if (address == 0x284) {
            return 0; // Hack: $F046
        } else if (address >= 0x280 && address < 0x2a0) {
            r.put(address, true);
            return arr[address - 0x180];
        } else if (address >= 0x1000) {
            return rom[address & 0xfff];
        } else {
            return 0;
        }
    }

    @Override
    public void writeByte(int address, byte b) {
        System.out.flush();
        address &= 0x1fff;
        if (address < 0x200) {
            address &= 0xff;
            if (address >= 0x80) {
                w.put(address & 0xff, true);
                arr[address & 0xff] = b;
            }
        } else if (address >= 0x280 && address < 0x2a0) {
            arr[address - 0x180] = b;
        }
    }
}
