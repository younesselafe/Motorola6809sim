package com.sim6809.model;

public class Cpu6809 {
    public Bus bus;
    
    public int A = 0, B = 0;
    public int X = 0, Y = 0;
    public int U = 0, S = 0;
    public int PC = 0, DP = 0, CC = 0; // CC: E F H I N Z V C
    
    public boolean lineHALT=false, lineIRQ=false, lineFIRQ=false, lineNMI=false, isWaiting=false;

    public Cpu6809(Bus bus) { this.bus = bus; }

    public void reset() {
        A=0; B=0; X=0; Y=0; DP=0; S=0x7FFF; U=0x6FFF;
        PC = bus.readWord(0xFFFE); if(PC==0) PC=0x8000;
        CC = 0x50; lineHALT=false; isWaiting=false;
    }

    public int step() {
        if(lineHALT) return 0;
        if(checkInterrupts()) return 10;
        if(isWaiting) return 1;
        return execute(fetchByte());
    }

    private boolean checkInterrupts() {
        if(lineNMI) { lineNMI=false; serviceInt(0xFFFC, true, false); return true; }
        if(lineFIRQ && (CC&0x40)==0) { lineFIRQ=false; serviceInt(0xFFF6, false, true); return true; }
        if(lineIRQ && (CC&0x10)==0) { lineIRQ=false; serviceInt(0xFFF8, true, false); return true; }
        return false;
    }
    
    private void serviceInt(int vec, boolean all, boolean firq) {
        isWaiting=false;
        if(!firq) { CC|=0x80; pushAll(true); } 
        else { CC&=~0x80; pushWord(S,PC); S-=2; pushByte(S,CC); S-=1; }
        CC|=0x50; PC=bus.readWord(vec);
    }

    private void pushAll(boolean s) {
        pushWord(S, PC); S-=2;
        pushWord(S, U); S-=2;
        pushWord(S, Y); S-=2;
        pushWord(S, X); S-=2;
        pushByte(S, DP); S-=1;
        pushByte(S, B); S-=1;
        pushByte(S, A); S-=1;
        pushByte(S, CC); S-=1;
    }

    private int execute(int op) {
        int r;
        switch(op) {
            case 0x12: return 2; // NOP
            case 0x3A: X=(X+B)&0xFFFF; return 3; // ABX
            case 0x19: doDAA(); return 2; // DAA
            case 0x10: return execPage2(fetchByte());
            case 0x3D: // MUL
                r = (A & 0xFF) * (B & 0xFF);
                A = (r >> 8) & 0xFF; B = r & 0xFF;
                CC &= ~0x01; if((r&0x80)!=0) CC|=1; updateNZ16(r);
                return 11;
            case 0x1D: A = (B & 0x80) != 0 ? 0xFF : 0x00; updateNZ8(A); return 2; // SEX

            // BRANCH
            case 0x20: branch(true); return 3; 
            case 0x26: branch((CC&4)==0); return 3; 
            case 0x27: branch((CC&4)!=0); return 3; 
            case 0x8D: branchSub(true); return 7; 
            case 0x7E: PC=fetchWord(); return 3; 
            case 0x6E: PC=resolveIndexed(); return 3; 
            case 0xBD: pushWord(S,PC+2); S-=2; PC=fetchWord(); return 8; 
            case 0xAD: pushWord(S,PC+2); S-=2; PC=resolveIndexed(); return 8; 
            case 0x39: S+=2; PC=bus.readWord(S-1); return 5; 
            case 0x3B: return opRTI();

            // LOADS
            case 0x86: A=fetchByte(); updateNZ8(A); return 2; 
            case 0x96: A=bus.read(fetchDir()); updateNZ8(A); return 4;
            case 0xB6: A=bus.read(fetchWord()); updateNZ8(A); return 5;
            case 0xA6: A=bus.read(resolveIndexed()); updateNZ8(A); return 4;
            
            case 0xC6: B=fetchByte(); updateNZ8(B); return 2; 
            case 0xD6: B=bus.read(fetchDir()); updateNZ8(B); return 4;
            case 0xF6: B=bus.read(fetchWord()); updateNZ8(B); return 5;
            case 0xE6: B=bus.read(resolveIndexed()); updateNZ8(B); return 4;

            case 0x8E: X=fetchWord(); updateNZ16(X); return 3;
            case 0x9E: X=bus.readWord(fetchDir()); updateNZ16(X); return 5;
            case 0xBE: X=bus.readWord(fetchWord()); updateNZ16(X); return 6;
            case 0xAE: X=bus.readWord(resolveIndexed()); updateNZ16(X); return 5;

            case 0xCE: U=fetchWord(); updateNZ16(U); return 3;
            case 0xDE: U=bus.readWord(fetchDir()); updateNZ16(U); return 5;
            case 0xFE: U=bus.readWord(fetchWord()); updateNZ16(U); return 6;
            case 0xEE: U=bus.readWord(resolveIndexed()); updateNZ16(U); return 5;

            case 0xCC: r=fetchWord(); A=hi(r); B=lo(r); updateNZ16(r); CC&=~2; return 3;
            case 0xDC: r=bus.readWord(fetchDir()); A=hi(r); B=lo(r); updateNZ16(r); CC&=~2; return 5;
            case 0xFC: r=bus.readWord(fetchWord()); A=hi(r); B=lo(r); updateNZ16(r); CC&=~2; return 6;
            case 0xEC: r=bus.readWord(resolveIndexed()); A=hi(r); B=lo(r); updateNZ16(r); CC&=~2; return 5;

            // STORES
            case 0x97: bus.write(fetchDir(), A); updateNZ8(A); return 4;
            case 0xB7: bus.write(fetchWord(), A); updateNZ8(A); return 5;
            case 0xA7: bus.write(resolveIndexed(), A); updateNZ8(A); return 4;
            
            case 0xD7: bus.write(fetchDir(), B); updateNZ8(B); return 4;
            case 0xF7: bus.write(fetchWord(), B); updateNZ8(B); return 5;
            case 0xE7: bus.write(resolveIndexed(), B); updateNZ8(B); return 4;
            
            case 0x9F: bus.writeWord(fetchDir(), X); updateNZ16(X); return 5;
            case 0xBF: bus.writeWord(fetchWord(), X); updateNZ16(X); return 6;
            case 0xAF: bus.writeWord(resolveIndexed(), X); updateNZ16(X); return 5;

            case 0xDF: bus.writeWord(fetchDir(), U); updateNZ16(U); return 5;
            case 0xFF: bus.writeWord(fetchWord(), U); updateNZ16(U); return 6;
            case 0xEF: bus.writeWord(resolveIndexed(), U); updateNZ16(U); return 5;

            case 0xDD: bus.writeWord(fetchDir(), getD()); updateNZ16(getD()); CC&=~2; return 5;
            case 0xFD: bus.writeWord(fetchWord(), getD()); updateNZ16(getD()); CC&=~2; return 6;
            case 0xED: bus.writeWord(resolveIndexed(), getD()); updateNZ16(getD()); CC&=~2; return 5;

            // ALU
            case 0x8B: doAddA(fetchByte()); return 2; case 0x9B: doAddA(bus.read(fetchDir())); return 4;
            case 0xBB: doAddA(bus.read(fetchWord())); return 5; case 0xAB: doAddA(bus.read(resolveIndexed())); return 4;
            case 0xCB: doAddB(fetchByte()); return 2; case 0xDB: doAddB(bus.read(fetchDir())); return 4;
            case 0xFB: doAddB(bus.read(fetchWord())); return 5; case 0xEB: doAddB(bus.read(resolveIndexed())); return 4;
            case 0xC3: doAddD(fetchWord()); return 4; case 0xD3: doAddD(bus.readWord(fetchDir())); return 6;
            case 0xF3: doAddD(bus.readWord(fetchWord())); return 7; case 0xE3: doAddD(bus.readWord(resolveIndexed())); return 6;
            case 0x83: doSubD(fetchWord()); return 4; case 0x93: doSubD(bus.readWord(fetchDir())); return 6;
            case 0xB3: doSubD(bus.readWord(fetchWord())); return 7; case 0xA3: doSubD(bus.readWord(resolveIndexed())); return 6;

            case 0x89: doAdcA(fetchByte()); return 2; case 0x80: doSubA(fetchByte()); return 2; case 0x82: doSbcA(fetchByte()); return 2;
            case 0x84: doLogicA(fetchByte(), 0); return 2; case 0x8A: doLogicA(fetchByte(), 1); return 2;
            case 0x88: doLogicA(fetchByte(), 2); return 2; case 0x85: doLogicA(fetchByte(), 3); return 2;

            case 0x81: doCmp(A, fetchByte()); return 2; case 0xC1: doCmp(B, fetchByte()); return 2;
            case 0x8C: doCmp16(X, fetchWord()); return 4;

            // UNARY REGISTER
            case 0x4F: A=0; CC|=4; CC&=~0xA; return 2; case 0x5F: B=0; CC|=4; CC&=~0xA; return 2;
            case 0x43: A=doCom(A); return 2; case 0x53: B=doCom(B); return 2;
            case 0x40: A=doNeg(A); return 2; case 0x50: B=doNeg(B); return 2;
            case 0x4C: A=doInc(A); return 2; case 0x5C: B=doInc(B); return 2;
            case 0x48: A=doAsl(A); return 2; case 0x58: B=doAsl(B); return 2;
            case 0x47: A=doAsr(A); return 2; case 0x57: B=doAsr(B); return 2;
            case 0x49: A=doRol(A); return 2; case 0x59: B=doRol(B); return 2;
            case 0x46: A=doRor(A); return 2; case 0x56: B=doRor(B); return 2;
            case 0x44: A=doLsr(A); return 2; case 0x54: B=doLsr(B); return 2;
            case 0x4D: updateNZ8(A); CC&=~2; return 2; // TSTA
            case 0x5D: updateNZ8(B); CC&=~2; return 2; // TSTB

            // --- UNARY MEMORY (Direct) ---
            case 0x00: doNegMem(fetchDir()); return 6;
            case 0x03: doComMem(fetchDir()); return 6;
            case 0x04: doLsrMem(fetchDir()); return 6;
            case 0x06: doRorMem(fetchDir()); return 6;
            case 0x07: doAsrMem(fetchDir()); return 6;
            case 0x08: doAslMem(fetchDir()); return 6;
            case 0x09: doRolMem(fetchDir()); return 6;
            case 0x0A: doDecMem(fetchDir()); return 6;
            case 0x0C: doIncMem(fetchDir()); return 6;
            case 0x0D: doTstMem(fetchDir()); return 6;
            case 0x0F: doClrMem(fetchDir()); return 6;

            // --- UNARY MEMORY (Extended) ---
            case 0x70: doNegMem(fetchWord()); return 7;
            case 0x73: doComMem(fetchWord()); return 7;
            case 0x74: doLsrMem(fetchWord()); return 7;
            case 0x76: doRorMem(fetchWord()); return 7;
            case 0x77: doAsrMem(fetchWord()); return 7;
            case 0x78: doAslMem(fetchWord()); return 7;
            case 0x79: doRolMem(fetchWord()); return 7;
            case 0x7A: doDecMem(fetchWord()); return 7;
            case 0x7C: doIncMem(fetchWord()); return 7;
            case 0x7D: doTstMem(fetchWord()); return 7;
            case 0x7F: doClrMem(fetchWord()); return 7;

            // --- UNARY MEMORY (Indexed) ---
            case 0x60: doNegMem(resolveIndexed()); return 6;
            case 0x63: doComMem(resolveIndexed()); return 6;
            case 0x64: doLsrMem(resolveIndexed()); return 6;
            case 0x66: doRorMem(resolveIndexed()); return 6;
            case 0x67: doAsrMem(resolveIndexed()); return 6;
            case 0x68: doAslMem(resolveIndexed()); return 6;
            case 0x69: doRolMem(resolveIndexed()); return 6;
            case 0x6A: doDecMem(resolveIndexed()); return 6;
            case 0x6C: doIncMem(resolveIndexed()); return 6;
            case 0x6D: doTstMem(resolveIndexed()); return 6;
            case 0x6F: doClrMem(resolveIndexed()); return 6;

            case 0x30: X=resolveIndexed(); updateNZ16(X); return 4;
            case 0x31: Y=resolveIndexed(); updateNZ16(Y); return 4;
            case 0x32: S=resolveIndexed(); return 4;
            case 0x33: U=resolveIndexed(); return 4;

            case 0x34: return opPush(true); case 0x35: return opPull(true);
            case 0x36: return opPush(false); case 0x37: return opPull(false);
            case 0x1F: return opTFR();
            case 0x1E: return opEXG(); 
            case 0x3F: serviceInt(0xFFFA, true, false); return 19;

            case 0x1C: CC &= fetchByte(); return 3; 
            case 0x1A: CC |= fetchByte(); return 3; 

            default: return 0;
        }
    }

    private int execPage2(int op) {
        switch(op) {
            case 0xCE: S=fetchWord(); updateNZ16(S); return 4;
            case 0xDE: S=bus.readWord(fetchDir()); updateNZ16(S); return 6;
            case 0xFE: S=bus.readWord(fetchWord()); updateNZ16(S); return 7;
            case 0xEE: S=bus.readWord(resolveIndexed()); updateNZ16(S); return 6;

            case 0x8E: Y=fetchWord(); updateNZ16(Y); return 4;
            case 0x9E: Y=bus.readWord(fetchDir()); updateNZ16(Y); return 6;
            case 0xBE: Y=bus.readWord(fetchWord()); updateNZ16(Y); return 7;
            case 0xAE: Y=bus.readWord(resolveIndexed()); updateNZ16(Y); return 6;

            case 0xDF: bus.writeWord(fetchDir(), S); updateNZ16(S); return 6;
            case 0xFF: bus.writeWord(fetchWord(), S); updateNZ16(S); return 7;
            case 0xEF: bus.writeWord(resolveIndexed(), S); updateNZ16(S); return 6;

            case 0x9F: bus.writeWord(fetchDir(), Y); updateNZ16(Y); return 6;
            case 0xBF: bus.writeWord(fetchWord(), Y); updateNZ16(Y); return 7;
            case 0xAF: bus.writeWord(resolveIndexed(), Y); updateNZ16(Y); return 6;

            case 0x83: doCmp16(getD(), fetchWord()); return 5;
            case 0x8C: doCmp16(Y, fetchWord()); return 5;
            
            case 0x3F: serviceInt(0xFFFA, true, false); return 20;
            
            case 0x24: case 0x25: case 0x2C: case 0x2E: PC=fetchWord(); return 5;
            default: return 0;
        }
    }

    private void doAddA(int v) { int r=A+v; updateH(A,v,r); updateVCAdd(A,v,r); updateNZ8(r); A=r&0xFF; }
    private void doAddB(int v) { int r=B+v; updateH(B,v,r); updateVCAdd(B,v,r); updateNZ8(r); B=r&0xFF; }
    private void doAddD(int v) { int r=getD()+v; updateNZ16(r); if(r>0xFFFF) CC|=1; else CC&=~1; if(((getD()^v^r)&0x8000)!=0) CC|=2; else CC&=~2; A=hi(r); B=lo(r); }
    private void doSubD(int v) { int r=getD()-v; updateNZ16(r); if(getD()<v) CC|=1; else CC&=~1; if(((getD()^v)&(getD()^r)&0x8000)!=0) CC|=2; else CC&=~2; A=hi(r); B=lo(r); }

    private void doAdcA(int v) { int c=CC&1; int r=A+v+c; updateH(A,v,r); updateVCAdd(A,v,r); updateNZ8(r); A=r&0xFF; }
    private void doSbcA(int v) { int c=CC&1; int r=A-v-c; updateVCSub(A,v,r); updateNZ8(r); A=r&0xFF; }
    private void doSubA(int v) { int r=A-v; updateVCSub(A,v,r); updateNZ8(r); A=r&0xFF; }
    
    private void doLogicA(int v, int mode) {
        int r=0; if(mode==0) r=A&v; else if(mode==1) r=A|v; else if(mode==2) r=A^v; else r=A&v;
        CC &= ~0x0E; updateNZ8(r); if(mode!=3) A=r;
    }

    private void doCmp(int reg, int v) { int r=reg-v; updateVCSub(reg,v,r); updateNZ8(r); }
    private void doCmp16(int reg, int v) { int r=reg-v; updateNZ16(r); if(reg<v) CC|=1; else CC&=~1; if(((reg^v)&(reg^r))<0) CC|=2; else CC&=~2; }

    // --- CORRECTION SYNTAXE ICI (Parenthèses ajoutées) ---
    private void doIncMem(int addr) { int v=bus.read(addr); v=doInc(v); bus.write(addr, v); }
    private void doDecMem(int addr) { int v=bus.read(addr); int r=(v-1)&0xFF; if(r==0) CC|=4; else CC&=~4; if((r&0x80)!=0) CC|=8; else CC&=~8; if(v==0x80) CC|=2; else CC&=~2; bus.write(addr, r); }
    private void doClrMem(int addr) { bus.write(addr, 0); CC|=4; CC&=~0xA; }
    private void doNegMem(int addr) { int v=bus.read(addr); v=doNeg(v); bus.write(addr, v); }
    private void doComMem(int addr) { int v=bus.read(addr); v=doCom(v); bus.write(addr, v); }
    private void doTstMem(int addr) { int v=bus.read(addr); updateNZ8(v); CC&=~2; }
    
    private void doAslMem(int addr) { int v=bus.read(addr); v=doAsl(v); bus.write(addr, v); }
    private void doAsrMem(int addr) { int v=bus.read(addr); v=doAsr(v); bus.write(addr, v); }
    private void doLsrMem(int addr) { int v=bus.read(addr); v=doLsr(v); bus.write(addr, v); }
    private void doRolMem(int addr) { int v=bus.read(addr); v=doRol(v); bus.write(addr, v); }
    private void doRorMem(int addr) { int v=bus.read(addr); v=doRor(v); bus.write(addr, v); }
    // -----------------------------------------------------

    private int doNeg(int v) { int r=0-v; updateVCSub(0,v,r); updateNZ8(r); return r&0xFF; }
    private int doCom(int v) { int r=~v & 0xFF; CC&=~0x0E; CC|=1; updateNZ8(r); return r; }
    private int doInc(int v) { int r=(v+1)&0xFF; if(r==0) CC|=4; else CC&=~4; if(r>127) CC|=8; else CC&=~8; if(v==0x7F) CC|=2; else CC&=~2; return r; }

    private int doAsl(int v) { int r=v<<1; CC&=~0x0F; if((v&0x80)!=0) CC|=1; updateNZ8(r); if(((r^v)&0x80)!=0) CC|=2; return r&0xFF; }
    private int doLsr(int v) { int r=v>>1; CC&=~0x0F; if((v&1)!=0) CC|=1; updateNZ8(r); return r; }
    private int doRol(int v) { int c=CC&1; int r=(v<<1)|c; CC&=~0x0F; if((v&0x80)!=0) CC|=1; updateNZ8(r); if(((r^v)&0x80)!=0) CC|=2; return r&0xFF; }
    private int doRor(int v) { int c=(CC&1)<<7; int r=(v>>1)|c; CC&=~0x0F; if((v&1)!=0) CC|=1; updateNZ8(r); return r; }
    private int doAsr(int v) { int r=(v>>1)|(v&0x80); CC&=~0x0F; if((v&1)!=0) CC|=1; updateNZ8(r); return r; }

    private void doDAA() {
        int cf = CC&1; int val = A; int l=val&0x0F; int h=val>>4;
        if(l>9 || (CC&0x20)!=0) { val+=0x06; }
        if(h>9 || cf!=0 || (val>0x9F)) { val+=0x60; cf=1; } else cf=0;
        A = val&0xFF; updateNZ8(A); if(cf!=0) CC|=1; else CC&=~1;
    }

    private int getD() { return (A<<8)|B; }
    private int hi(int v) { return (v>>8)&0xFF; }
    private int lo(int v) { return v&0xFF; }
    
    private void updateNZ8(int v) { CC&=~0x0C; if((v&0xFF)==0) CC|=4; if((v&0x80)!=0) CC|=8; }
    private void updateNZ16(int v) { CC&=~0x0C; if((v&0xFFFF)==0) CC|=4; if((v&0x8000)!=0) CC|=8; }
    private void updateH(int a, int b, int r) { if(((a^b^r)&0x10)!=0) CC|=0x20; else CC&=~0x20; }
    private void updateVCAdd(int a, int b, int r) { if(((a^b^r)&0x100)!=0) CC|=1; else CC&=~1; if(((a^r)&(b^r)&0x80)!=0) CC|=2; else CC&=~2; }
    private void updateVCSub(int a, int b, int r) { if(((a^b^r)&0x100)!=0) CC|=1; else CC&=~1; if(((a^b)&(a^r)&0x80)!=0) CC|=2; else CC&=~2; }

    private int resolveIndexed() {
        int pb = fetchByte(); int reg = (pb>>5)&3; int rVal = (reg==0)?X : (reg==1)?Y : (reg==2)?U : S;
        if((pb&0x80)==0) return (rVal + ((pb<<27)>>27)) & 0xFFFF; // 5-bit offset
        switch(pb & 0x1F) { 
            case 0x04: return rVal; 
            case 0x08: return (rVal + (byte)fetchByte()) & 0xFFFF; // 8-bit
            case 0x09: return (rVal + (short)fetchWord()) & 0xFFFF; // 16-bit
            default: return rVal; 
        }
    }

    private int fetchByte() { int v = bus.read(PC); PC=(PC+1)&0xFFFF; return v; }
    private int fetchWord() { int v = bus.readWord(PC); PC=(PC+2)&0xFFFF; return v; }
    private int fetchDir() { return (DP<<8)|fetchByte(); }
    
    private void branch(boolean c) { byte o=(byte)fetchByte(); if(c) PC=(PC+o)&0xFFFF; }
    private void branchSub(boolean c) { byte o=(byte)fetchByte(); if(c) { pushWord(S,PC); S-=2; PC=(PC+o)&0xFFFF; } }

    private int opTFR() { int p=fetchByte(); int r1=(p>>4)&0xF, r2=p&0xF; setReg(r2, getReg(r1)); return 6; }
    
    private int opEXG() {
        int p = fetchByte();
        int r1 = (p >> 4) & 0xF;
        int r2 = p & 0xF;
        int val1 = getReg(r1);
        int val2 = getReg(r2);
        setReg(r1, val2);
        setReg(r2, val1);
        return 8; 
    }

    private int getReg(int i) { if(i==0) return getD(); if(i==1) return X; if(i==2) return Y; if(i==3) return U; if(i==4) return S; if(i==5) return PC; if(i==8) return A; if(i==9) return B; if(i==10) return CC; if(i==11) return DP; return 0; }
    private void setReg(int i, int v) { if(i==0) { A=hi(v); B=lo(v); } else if(i==1) X=v&0xFFFF; else if(i==2) Y=v&0xFFFF; else if(i==3) U=v&0xFFFF; else if(i==4) S=v&0xFFFF; else if(i==5) PC=v&0xFFFF; else if(i==8) A=v&0xFF; else if(i==9) B=v&0xFF; else if(i==10) CC=v&0xFF; else if(i==11) DP=v&0xFF; }

    private int opPush(boolean useS) {
        int m=fetchByte(); int sp = useS?S:U;
        if((m&0x80)!=0) { pushWord(sp,PC); sp-=2; }
        if((m&0x40)!=0) { pushWord(sp,useS?U:S); sp-=2; }
        if((m&0x20)!=0) { pushWord(sp,Y); sp-=2; }
        if((m&0x10)!=0) { pushWord(sp,X); sp-=2; }
        if((m&0x08)!=0) { pushByte(sp,DP); sp-=1; }
        if((m&0x04)!=0) { pushByte(sp,B); sp-=1; }
        if((m&0x02)!=0) { pushByte(sp,A); sp-=1; }
        if((m&0x01)!=0) { pushByte(sp,CC); sp-=1; }
        if(useS) S=sp; else U=sp; return 5;
    }
    private int opPull(boolean useS) {
        int m=fetchByte(); int sp = useS?S:U;
        if((m&1)!=0) { sp++; CC=bus.read(sp); }
        if((m&2)!=0) { sp++; A=bus.read(sp); }
        if((m&4)!=0) { sp++; B=bus.read(sp); }
        if((m&8)!=0) { sp++; DP=bus.read(sp); }
        if((m&16)!=0) { sp+=2; X=bus.readWord(sp-1); }
        if((m&32)!=0) { sp+=2; Y=bus.readWord(sp-1); }
        if((m&64)!=0) { sp+=2; int v=bus.readWord(sp-1); if(useS) U=v; else S=v; }
        if((m&128)!=0){ sp+=2; PC=bus.readWord(sp-1); }
        if(useS) S=sp; else U=sp; return 5;
    }
    
    private void pushByte(int p, int v) { bus.write(p, v); }
    private void pushWord(int p, int v) { bus.writeWord(p-1, v); }
    private int opRTI() {
        S++; CC=bus.read(S);
        if((CC&0x80)!=0) { S++; A=bus.read(S); S++; B=bus.read(S); S++; DP=bus.read(S); S+=2; X=bus.readWord(S-1); S+=2; Y=bus.readWord(S-1); S+=2; U=bus.readWord(S-1); }
        S+=2; PC=bus.readWord(S-1); return 6;
    }
    public String getFlags() { return String.format("E:%d F:%d H:%d I:%d N:%d Z:%d V:%d C:%d", (CC>>7)&1, (CC>>6)&1, (CC>>5)&1, (CC>>4)&1, (CC>>3)&1, (CC>>2)&1, (CC>>1)&1, CC&1); }
}