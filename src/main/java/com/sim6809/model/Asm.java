package com.sim6809.model;

import java.util.*;

public class Asm {
    
    public static class DisRes { 
        public String text; 
        public int bytesUsed; 
        public String hexDump; 
    }

    static class Def { String name; int mode; public Def(String n, int m) { name=n; mode=m; } }
    private static final Def[] MAP = new Def[256];
    private static final Def[] MAP2 = new Def[256]; // Page 2

    static {
        // --- PAGE 1 ---
        MAP[0x12]=new Def("NOP",0); MAP[0x39]=new Def("RTS",0); MAP[0x3B]=new Def("RTI",0);
        MAP[0x3A]=new Def("ABX",0); MAP[0x19]=new Def("DAA",0); MAP[0x1F]=new Def("TFR",6);
        MAP[0x3D]=new Def("MUL",0); MAP[0x1D]=new Def("SEX",0); MAP[0x3F]=new Def("SWI",0);

        // Branch
        MAP[0x20]=new Def("BRA",5); MAP[0x26]=new Def("BNE",5); MAP[0x27]=new Def("BEQ",5);
        MAP[0x8D]=new Def("BSR",5); MAP[0x7E]=new Def("JMP",4); MAP[0xBD]=new Def("JSR",4);
        
        // Loads / Stores
        addGrp(0x86, "LDA"); addGrp(0xC6, "LDB");
        addGrp(0x8E, "LDX"); addGrp(0xCE, "LDU");
        addGrp(0xCC, "LDD");
        
        addSto(0x97, "STA"); addSto(0xD7, "STB"); 
        addSto(0x9F, "STX"); addSto(0xDF, "STU");
        addSto(0xDD, "STD");

        // ALU
        addGrp(0x8B, "ADD"); addGrp(0xCB, "ADD");
        addGrp(0x80, "SUB"); addGrp(0xC0, "SUB");
        addGrp(0x83, "SUBD"); addGrp(0xC3, "ADDD");
        
        addGrp(0x81, "CMP"); addGrp(0xC1, "CMP");
        addGrp(0x8C, "CMPX"); 
        
        // Logic
        addGrp(0x84, "AND"); addGrp(0xC4, "AND");
        addGrp(0x85, "BIT"); addGrp(0xC5, "BIT");
        addGrp(0x88, "EOR"); addGrp(0xC8, "EOR");
        addGrp(0x89, "ADC"); addGrp(0xC9, "ADC");
        addGrp(0x8A, "ORA"); addGrp(0xCA, "ORA");

        // Stack & LEA
        MAP[0x34]=new Def("PSHS",8); MAP[0x35]=new Def("PULS",8);
        MAP[0x36]=new Def("PSHU",8); MAP[0x37]=new Def("PULU",8);
        MAP[0x30]=new Def("LEAX",7); MAP[0x31]=new Def("LEAY",7);
        MAP[0x32]=new Def("LEAS",7); MAP[0x33]=new Def("LEAU",7);

        // Unaires REGISTRES (Mode 0)
        MAP[0x4F]=new Def("CLRA",0); MAP[0x5F]=new Def("CLRB",0);
        MAP[0x43]=new Def("COMA",0); MAP[0x53]=new Def("COMB",0);
        MAP[0x4C]=new Def("INCA",0); MAP[0x5C]=new Def("INCB",0);
        MAP[0x4A]=new Def("DECA",0); MAP[0x5A]=new Def("DECB",0);
        MAP[0x40]=new Def("NEGA",0); MAP[0x50]=new Def("NEGB",0);
        MAP[0x48]=new Def("ASLA",0); MAP[0x58]=new Def("ASLB",0);
        MAP[0x49]=new Def("ROLA",0); MAP[0x59]=new Def("ROLB",0);
        MAP[0x44]=new Def("LSRA",0); MAP[0x54]=new Def("LSRB",0);
        MAP[0x46]=new Def("RORA",0); MAP[0x56]=new Def("RORB",0);
        MAP[0x4D]=new Def("TSTA",0); MAP[0x5D]=new Def("TSTB",0);
        
        // Unaires MÉMOIRE (Mode 3=Dir, 7=Ind, 4=Ext)
        addUni(0x03, "COM"); addUni(0x00, "NEG"); addUni(0x0F, "CLR"); 
        addUni(0x0C, "INC"); addUni(0x0A, "DEC"); addUni(0x0D, "TST");
        addUni(0x08, "ASL"); addUni(0x09, "ROL"); addUni(0x04, "LSR");
        addUni(0x06, "ROR");
        
        MAP[0x1E]=new Def("EXG",6);
        
        // CC
        MAP[0x1C]=new Def("ANDCC",1); MAP[0x1A]=new Def("ORCC",1);
        
        // PAGE 2
        MAP[0x10]=new Def("PAGE2",9);
        MAP2[0xCE]=new Def("LDS",2); MAP2[0x8E]=new Def("LDY",2); 
        MAP2[0x83]=new Def("CMPD",2); MAP2[0x8C]=new Def("CMPY",2);
        MAP2[0x9E]=new Def("LDY",3); MAP2[0xBE]=new Def("LDY",4); MAP2[0xAE]=new Def("LDY",7);
        MAP2[0xDE]=new Def("LDS",3); MAP2[0xFE]=new Def("LDS",4); MAP2[0xEE]=new Def("LDS",7);
        MAP2[0x9F]=new Def("STY",3); MAP2[0xBF]=new Def("STY",4); MAP2[0xAF]=new Def("STY",7);
        MAP2[0xDF]=new Def("STS",3); MAP2[0xFF]=new Def("STS",4); MAP2[0xEF]=new Def("STS",7);
        MAP2[0x3F]=new Def("SWI2",0);
    }

    private static void addGrp(int b, String n) { MAP[b]=new Def(n, (n.endsWith("D")||n.endsWith("X"))?2:1); MAP[b+0x10]=new Def(n,3); MAP[b+0x20]=new Def(n,7); MAP[b+0x30]=new Def(n,4); }
    private static void addSto(int b, String n) { MAP[b]=new Def(n,3); MAP[b+0x10]=new Def(n,7); MAP[b+0x20]=new Def(n,4); }
    private static void addUni(int b, String n) { MAP[b]=new Def(n,3); MAP[b+0x60]=new Def(n,7); MAP[b+0x70]=new Def(n,4); }

    public static DisRes disassemble(Bus bus, int pc) {
        DisRes res = new DisRes(); int op = bus.read(pc);
        Def def = (op == 0x10) ? MAP2[bus.read(pc+1)] : MAP[op];
        res.bytesUsed = (op == 0x10) ? 2 : 1;
        
        res.hexDump = String.format("%02X", op);
        if (op == 0x10) res.hexDump += String.format(" %02X", bus.read(pc+1));

        if (def == null) { res.text = "DB $"+String.format("%02X", op); return res; }
        
        res.text = def.name;
        if(def.mode==0) return res;
        
        int next = bus.read(pc + res.bytesUsed);
        
        if(def.mode==1) { res.text += " #$"+String.format("%02X", next); res.bytesUsed++; res.hexDump += " " + String.format("%02X", next); }
        else if(def.mode==2) { 
            int val = bus.readWord(pc+res.bytesUsed);
            res.text += " #$"+String.format("%04X", val); res.bytesUsed+=2; 
            res.hexDump += String.format(" %02X %02X", val>>8, val&0xFF);
        }
        else if(def.mode==3) { res.text += " $"+String.format("%02X", next); res.bytesUsed++; res.hexDump += " " + String.format("%02X", next); }
        else if(def.mode==4) { 
            int val = bus.readWord(pc+res.bytesUsed);
            res.text += " $"+String.format("%04X", val); res.bytesUsed+=2; 
            res.hexDump += String.format(" %02X %02X", val>>8, val&0xFF);
        }
        else if(def.mode==5) { res.text += " $"+String.format("%04X", (pc+2+(byte)next)&0xFFFF); res.bytesUsed++; res.hexDump += " " + String.format("%02X", next); }
        else if(def.mode==6) { 
            res.text += " " + decodeReg((next>>4)&0xF) + "," + decodeReg(next&0xF); 
            res.bytesUsed++; res.hexDump += " " + String.format("%02X", next);
        }
        else if(def.mode==7) { 
            int pb = next; res.bytesUsed++; res.hexDump += " " + String.format("%02X", pb);
            String reg = decodeIdxReg((pb >> 5) & 3);
            if ((pb & 0x80) == 0) {
                int off = pb & 0x1F; if ((off & 0x10) != 0) off |= 0xFFFFFFE0;
                res.text += (off==0 ? " ," : " " + off + ",") + reg;
            } else {
                int mode = pb & 0x1F;
                if (mode == 0x04) res.text += " ," + reg;
                else if (mode == 0x08) {
                    int off = bus.read(pc + res.bytesUsed); if (off > 127) off -= 256;
                    res.text += " " + off + "," + reg; res.bytesUsed++; res.hexDump += String.format(" %02X", off & 0xFF);
                }
                else if (mode == 0x09) {
                    int off = bus.readWord(pc + res.bytesUsed);
                    res.text += " " + off + "," + reg; res.bytesUsed+=2; res.hexDump += String.format(" %02X %02X", (off>>8)&0xFF, off&0xFF);
                }
                else res.text += " ??," + reg;
            }
        }
        else if(def.mode==8) { 
            res.text += " " + decodePushPull(next, def.name.endsWith("S"));
            res.bytesUsed++; res.hexDump += " " + String.format("%02X", next);
        }
        return res;
    }
    
    private static String decodeIdxReg(int rr) { if(rr==0) return "X"; if(rr==1) return "Y"; if(rr==2) return "U"; return "S"; }
    private static String decodeIdx(int pb) { return decodeIdxReg((pb>>5)&3); }
    private static String decodeReg(int r) { switch(r) { case 0:return "D"; case 1:return "X"; case 2:return "Y"; case 3:return "U"; case 4:return "S"; case 5:return "PC"; case 8:return "A"; case 9:return "B"; case 10:return "CC"; case 11:return "DP"; default:return "?"; } }
    private static String decodePushPull(int m, boolean isS) {
        List<String> r = new ArrayList<>();
        if((m&0x80)!=0) r.add("PC"); if((m&0x40)!=0) r.add(isS?"U":"S"); if((m&0x20)!=0) r.add("Y");
        if((m&0x10)!=0) r.add("X"); if((m&0x08)!=0) r.add("DP"); 
        if((m&0x04)!=0 && (m&0x02)!=0) { r.add("D"); } else { if((m&0x04)!=0) r.add("B"); if((m&0x02)!=0) r.add("A"); }
        if((m&0x01)!=0) r.add("CC");
        return String.join(",", r);
    }

    public static String assemble(String source, Bus bus, Cpu6809 cpu) {
        StringBuilder log = new StringBuilder(); Map<String, Integer> labels = new HashMap<>(); String[] lines = source.split("\n");
        int pc = 0x8000;
        
        for (String line : lines) {
            line = clean(line); if (line.isEmpty()) continue;
            if (line.startsWith("ORG")) { pc = parseNum(line.substring(3)); continue; }
            if (line.contains(":")) { labels.put(line.split(":")[0].trim(), pc); if (line.endsWith(":")) continue; line = line.substring(line.indexOf(":")+1).trim(); }
            if (line.isEmpty()) continue;
            pc += assembleLine(null, pc, line, true, null);
        }

        pc = 0x8000; bus.clear();
        for (String line : lines) {
            String original = line; line = clean(line); if (line.isEmpty()) continue;
            try {
                if (line.startsWith("ORG")) { pc = parseNum(line.substring(3)); continue; }
                if (line.contains(":")) { if (line.endsWith(":")) continue; line = line.substring(line.indexOf(":")+1).trim(); }
                if (line.isEmpty()) continue;
                int size = assembleLine(bus, pc, line, false, labels);
                pc += size;
            } catch (Exception e) { log.append("Err: "+original+"\n"); }
        }
        if(bus.readWord(0xFFFE) == 0) bus.writeWord(0xFFFE, 0x8000);
        return log.toString();
    }

    private static int assembleLine(Bus bus, int pc, String line, boolean dry, Map<String, Integer> labels) {
        String[] p = line.split("\\s+", 2); String m = p[0]; String args = p.length > 1 ? p[1] : "";
        if(m.equals("ADD")) m="ADDA"; if(m.equals("SUB")) m="SUBA"; if(m.equals("CMP")) m="CMPA";

        if(match(m,"NOP",0x12,bus,pc,dry)) return 1; if(match(m,"MUL",0x3D,bus,pc,dry)) return 1;
        if(match(m,"RTS",0x39,bus,pc,dry)) return 1; if(match(m,"RTI",0x3B,bus,pc,dry)) return 1;
        if(match(m,"ABX",0x3A,bus,pc,dry)) return 1; if(match(m,"DAA",0x19,bus,pc,dry)) return 1;
        if(match(m,"SEX",0x1D,bus,pc,dry)) return 1; if(match(m,"SWI",0x3F,bus,pc,dry)) return 1;
        
        if(match(m,"CLRA",0x4F,bus,pc,dry)) return 1; if(match(m,"CLRB",0x5F,bus,pc,dry)) return 1;
        if(match(m,"INCA",0x4C,bus,pc,dry)) return 1; if(match(m,"INCB",0x5C,bus,pc,dry)) return 1;
        if(match(m,"DECA",0x4A,bus,pc,dry)) return 1; if(match(m,"DECB",0x5A,bus,pc,dry)) return 1;
        if(match(m,"COMA",0x43,bus,pc,dry)) return 1; if(match(m,"COMB",0x53,bus,pc,dry)) return 1;
        if(match(m,"NEGA",0x40,bus,pc,dry)) return 1; if(match(m,"NEGB",0x50,bus,pc,dry)) return 1;
        if(match(m,"ASLA",0x48,bus,pc,dry)) return 1; if(match(m,"ASLB",0x58,bus,pc,dry)) return 1;
        if(match(m,"ROLA",0x49,bus,pc,dry)) return 1; if(match(m,"ROLB",0x59,bus,pc,dry)) return 1;
        if(match(m,"LSRA",0x44,bus,pc,dry)) return 1; if(match(m,"LSRB",0x54,bus,pc,dry)) return 1;
        if(match(m,"RORA",0x46,bus,pc,dry)) return 1; if(match(m,"RORB",0x56,bus,pc,dry)) return 1;
        if(match(m,"TSTA",0x4D,bus,pc,dry)) return 1; if(match(m,"TSTB",0x5D,bus,pc,dry)) return 1;

        if(m.equals("LDA")) return encodeGen(bus, pc, args, 0x86, 0x96, 0xB6, 0xA6, dry);
        if(m.equals("LDB")) return encodeGen(bus, pc, args, 0xC6, 0xD6, 0xF6, 0xE6, dry);
        if(m.equals("LDX")) return encodeGen(bus, pc, args, 0x8E, 0x9E, 0xBE, 0xAE, dry);
        if(m.equals("LDD")) return encodeGen(bus, pc, args, 0xCC, 0xDC, 0xFC, 0xEC, dry); 
        
        if(m.equals("LDS")) return encodePage2(bus, pc, args, 0xCE, 0xDE, 0xFE, 0xEE, dry);
        if(m.equals("LDY")) return encodePage2(bus, pc, args, 0x8E, 0x9E, 0xBE, 0xAE, dry);
        if(m.equals("LDU")) return encodeGen(bus, pc, args, 0xCE, 0xDE, 0xFE, 0xEE, dry);

        if(m.equals("ADDA")) return encodeGen(bus, pc, args, 0x8B, 0x9B, 0xBB, 0xAB, dry);
        if(m.equals("ADDB")) return encodeGen(bus, pc, args, 0xCB, 0xDB, 0xFB, 0xEB, dry);
        if(m.equals("ADDD")) return encodeGen(bus, pc, args, 0xC3, 0xD3, 0xF3, 0xE3, dry);
        if(m.equals("SUBA")) return encodeGen(bus, pc, args, 0x80, 0x90, 0xA0, 0xB0, dry);
        if(m.equals("SUBB")) return encodeGen(bus, pc, args, 0xC0, 0xD0, 0xE0, 0xF0, dry);
        if(m.equals("SUBD")) return encodeGen(bus, pc, args, 0x83, 0x93, 0xB3, 0xA3, dry);
        if(m.equals("CMPA")) return encodeGen(bus, pc, args, 0x81, 0x91, 0xB1, 0xA1, dry);
        if(m.equals("CMPB")) return encodeGen(bus, pc, args, 0xC1, 0xD1, 0xF1, 0xE1, dry);
        
        if(m.equals("ANDA")) return encodeGen(bus, pc, args, 0x84, 0x94, 0xB4, 0xA4, dry);
        if(m.equals("ORA")) return encodeGen(bus, pc, args, 0x8A, 0x9A, 0xBA, 0xAA, dry);
        if(m.equals("EORA")) return encodeGen(bus, pc, args, 0x88, 0x98, 0xB8, 0xA8, dry);

        if(m.equals("STA")) return encodeStore(bus, pc, args, 0x97, 0xB7, 0xA7, dry);
        if(m.equals("STB")) return encodeStore(bus, pc, args, 0xD7, 0xF7, 0xE7, dry);
        if(m.equals("STD")) return encodeStore(bus, pc, args, 0xDD, 0xFD, 0xED, dry);
        if(m.equals("STX")) return encodeStore(bus, pc, args, 0x9F, 0xBF, 0xAF, dry);
        if(m.equals("STU")) return encodeStore(bus, pc, args, 0xDF, 0xFF, 0xEF, dry);
        if(m.equals("STS")) return encodePage2Store(bus, pc, args, 0xDF, 0xFF, 0xEF, dry);
        if(m.equals("STY")) return encodePage2Store(bus, pc, args, 0x9F, 0xBF, 0xAF, dry);

        if(m.equals("LEAX")) return encodeLea(bus, pc, args, 0x30, dry);
        if(m.equals("LEAY")) return encodeLea(bus, pc, args, 0x31, dry);

        if(m.equals("BRA")||m.equals("BNE")||m.equals("BEQ")||m.equals("BSR")) {
            if(!dry) {
                int op = m.equals("BRA")?0x20 : m.equals("BNE")?0x26 : m.equals("BEQ")?0x27 : 0x8D;
                int target = labels.containsKey(args) ? labels.get(args) : parseNum(args);
                bus.write(pc, op); bus.write(pc+1, (target-(pc+2))&0xFF);
            }
            return 2;
        }
        if(m.equals("JMP")) {
            if(args.contains(",")) { if(!dry) { bus.write(pc, 0x6E); writeIndexed(bus, pc+1, args); } return getIndexedSize(args); }
            else { if(!dry) { bus.write(pc, 0x7E); bus.writeWord(pc+1, labels.containsKey(args)?labels.get(args):parseNum(args)); } return 3; }
        }

        if(m.equals("TFR")||m.equals("EXG")) {
            if(!dry) {
                bus.write(pc, m.equals("TFR")?0x1F:0x1E); String[] r=args.split(",");
                bus.write(pc+1, (parseReg(r[0])<<4)|parseReg(r[1]));
            }
            return 2;
        }
        if(m.equals("PSHS")||m.equals("PULS")||m.equals("PSHU")||m.equals("PULU")) {
            if(!dry) {
                bus.write(pc, m.equals("PSHS")?0x34 : m.equals("PULS")?0x35 : m.equals("PSHU")?0x36 : 0x37);
                bus.write(pc+1, parseRegList(args, m.endsWith("S")));
            }
            return 2;
        }
        if(m.equals("ANDCC")) { if(!dry) { bus.write(pc, 0x1C); bus.write(pc+1, parseNum(args)); } return 2; }
        if(m.equals("ORCC"))  { if(!dry) { bus.write(pc, 0x1A); bus.write(pc+1, parseNum(args)); } return 2; }

        // --- CORRECTION CRITIQUE DU CALCUL DE TAILLE ---
        if(m.equals("INC") || m.equals("DEC") || m.equals("CLR") || m.equals("NEG") || m.equals("COM") || m.equals("TST") || 
           m.equals("ASL") || m.equals("LSL") || m.equals("LSR") || m.equals("ROL") || m.equals("ROR")) {
            int opDir=0, opExt=0, opInd=0;
            if(m.equals("NEG")) { opDir=0x00; opExt=0x70; opInd=0x60; }
            if(m.equals("COM")) { opDir=0x03; opExt=0x73; opInd=0x63; }
            if(m.equals("LSR")) { opDir=0x04; opExt=0x74; opInd=0x64; }
            if(m.equals("ROR")) { opDir=0x06; opExt=0x76; opInd=0x66; }
            if(m.equals("ASR")) { opDir=0x07; opExt=0x77; opInd=0x67; }
            if(m.equals("ASL") || m.equals("LSL")) { opDir=0x08; opExt=0x78; opInd=0x68; }
            if(m.equals("ROL")) { opDir=0x09; opExt=0x79; opInd=0x69; }
            if(m.equals("DEC")) { opDir=0x0A; opExt=0x7A; opInd=0x6A; }
            if(m.equals("INC")) { opDir=0x0C; opExt=0x7C; opInd=0x6C; }
            if(m.equals("TST")) { opDir=0x0D; opExt=0x7D; opInd=0x6D; }
            if(m.equals("CLR")) { opDir=0x0F; opExt=0x7F; opInd=0x6F; }
            return encodeStore(bus, pc, args, opDir, opExt, opInd, dry);
        }
        
        // --- ANCIEN CALCUL DE TAILLE (Supprimé car inexact pour INC memory) ---
        // calculateSize n'est plus utilisé car assembleLine retourne la taille exacte
        
        return 0;
    }

    private static boolean match(String m, String t, int op, Bus b, int pc, boolean dry) {
        if(m.equals(t)) { if(!dry) b.write(pc, op); return true; } return false;
    }
    
    private static int encodeGen(Bus b, int pc, String a, int imm, int dir, int ext, int ind, boolean dry) {
        if(a.startsWith("#")) {
            boolean is16 = (imm==0x8E || imm==0xCE || imm==0xCC || imm==0xC3 || imm==0x83 || imm==0x8C);
            if(!dry) { b.write(pc, imm); if(is16) b.writeWord(pc+1, parseNum(a)); else b.write(pc+1, parseNum(a)); }
            return is16 ? 3 : 2;
        }
        return encodeStore(b, pc, a, dir, ext, ind, dry);
    }

    private static int encodePage2(Bus b, int pc, String a, int imm, int dir, int ext, int ind, boolean dry) {
        if(a.startsWith("#")) { 
            if(!dry) { b.write(pc, 0x10); b.write(pc+1, imm); b.writeWord(pc+2, parseNum(a)); }
            return 4; 
        }
        return encodePage2Store(b, pc, a, dir, ext, ind, dry);
    }

    private static int encodeStore(Bus b, int pc, String a, int dir, int ext, int ind, boolean dry) {
        if(a.contains(",")) { if(!dry) { b.write(pc, ind); writeIndexed(b, pc+1, a); } return getIndexedSize(a); }
        int val = parseNum(a);
        if(val<256 && dir!=-1) { if(!dry) { b.write(pc, dir); b.write(pc+1, val); } return 2; }
        else { if(!dry) { b.write(pc, ext); b.writeWord(pc+1, val); } return 3; }
    }

    private static int encodePage2Store(Bus b, int pc, String a, int dir, int ext, int ind, boolean dry) {
        if(a.contains(",")) { if(!dry) { b.write(pc, 0x10); b.write(pc+1, ind); writeIndexed(b, pc+2, a); } return 1 + getIndexedSize(a); }
        int val = parseNum(a);
        if(val<256 && dir!=-1) { if(!dry) { b.write(pc, 0x10); b.write(pc+1, dir); b.write(pc+2, val); } return 3; }
        else { if(!dry) { b.write(pc, 0x10); b.write(pc+1, ext); b.writeWord(pc+2, val); } return 4; }
    }

    private static int encodeLea(Bus b, int pc, String a, int op, boolean dry) {
        if(!dry) { b.write(pc, op); writeIndexed(b, pc+1, a); } return getIndexedSize(a);
    }

    private static int getIndexedSize(String arg) {
        String[] parts = arg.split(",");
        int offset = (parts.length > 0 && !parts[0].trim().isEmpty()) ? parseNum(parts[0]) : 0;
        if (offset == 0) return 2; 
        if (offset >= -16 && offset <= 15) return 2; 
        if (offset >= -128 && offset <= 127) return 3; 
        return 4;
    }

    private static void writeIndexed(Bus b, int addr, String arg) {
        String[] parts = arg.split(",");
        String rStr = parts.length > 1 ? parts[1].trim().toUpperCase() : "X";
        int rr = rStr.contains("Y")?1 : rStr.contains("U")?2 : rStr.contains("S")?3 : 0;
        int offset = (parts.length > 0 && !parts[0].trim().isEmpty()) ? parseNum(parts[0]) : 0;

        if (offset == 0) { b.write(addr, 0x84 | (rr<<5)); return; }
        if (offset >= -16 && offset <= 15) { b.write(addr, (rr<<5) | (offset & 0x1F)); return; }
        if (offset >= -128 && offset <= 127) { b.write(addr, 0x88 | (rr<<5)); b.write(addr+1, offset & 0xFF); return; }
        b.write(addr, 0x89 | (rr<<5)); b.writeWord(addr+1, offset & 0xFFFF);
    }

    private static int parseReg(String r) { r=r.trim(); if(r.equals("D")) return 0; if(r.equals("X")) return 1; if(r.equals("Y")) return 2; if(r.equals("U")) return 3; if(r.equals("S")) return 4; if(r.equals("PC")) return 5; if(r.equals("A")) return 8; if(r.equals("B")) return 9; if(r.equals("CC")) return 10; if(r.equals("DP")) return 11; return 0; }
    private static int parseRegList(String a, boolean s) { int m=0; a=a.toUpperCase(); if(a.contains("PC")) m|=0x80; if(a.contains(s?"U":"S")) m|=0x40; if(a.contains("Y")) m|=0x20; if(a.contains("X")) m|=0x10; if(a.contains("DP")) m|=0x08; if(a.contains("B")) m|=0x04; if(a.contains("A")) m|=0x02; if(a.contains("CC")) m|=0x01; if(a.contains("D")) m|=0x06; return m; }
    private static int parseNum(String s) { s=s.replace("#","").replace("$","").trim(); try { return Integer.parseInt(s, 16); } catch(Exception e) { return 0; } }
    private static String clean(String s) { if(s.contains(";")) s=s.substring(0,s.indexOf(";")); if(s.trim().startsWith("#")) return ""; return s.trim().toUpperCase(); }
}