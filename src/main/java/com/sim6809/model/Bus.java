package com.sim6809.model;

import java.util.ArrayList;
import java.util.List;

public class Bus {
    private final int[] ram = new int[65536];
    private final List<MemoryListener> listeners = new ArrayList<>();

    // Vecteurs d'interruption standards 6809+
    public static final int VEC_RESET = 0xFFFE;
    public static final int VEC_NMI   = 0xFFFC;
    public static final int VEC_SWI   = 0xFFFA;
    public static final int VEC_IRQ   = 0xFFF8;
    public static final int VEC_FIRQ  = 0xFFF6;

    public interface MemoryListener {
        void onMemoryChanged(int addr, int value);
    }

    public void addListener(MemoryListener l) { listeners.add(l); }

    public Bus() { clear(); }

    public void clear() {
        for(int i=0; i<ram.length; i++) ram[i] = 0;
    }

    public int read(int addr) {
        return ram[addr & 0xFFFF] & 0xFF;
    }

    public void write(int addr, int data) {
        int address = addr & 0xFFFF;
        int value = data & 0xFF;
        ram[address] = value;
        notifyListeners(address, value);
    }

    public int readWord(int addr) {
        int high = read(addr);
        int low = read(addr + 1);
        return ((high << 8) | low) & 0xFFFF;
    }

    public void writeWord(int addr, int data) {
        write(addr, (data >> 8) & 0xFF);
        write(addr + 1, data & 0xFF);
    }

    private void notifyListeners(int addr, int val) {
        for (MemoryListener l : listeners) {
            l.onMemoryChanged(addr, val);
        }
    }
}