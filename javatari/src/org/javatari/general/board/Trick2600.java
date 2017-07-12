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
    int cycle;
    byte []arr = new byte[256];
    byte []rom;
    HashMap<Integer, Boolean> r = new HashMap<Integer,Boolean>();
    HashMap<Integer, Boolean> w = new HashMap<Integer,Boolean>();

    public Trick2600() { }
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

    @Override
    public byte readByte(int address) {
        address &= 0x1fff;
        if (address < 0x1000) {
            r.put(address & 0xff, true);
            return arr[address & 0xff];
        } else {
            return rom[address - 0x1000];
        }
    }

    @Override
    public void writeByte(int address, byte b) {
        address &= 0x1fff;
        if (address < 0x1000) {
            w.put(address & 0xff, true);
            arr[address & 0xff] = b;
        }
    }
}
