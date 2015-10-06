/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

import java.util.ArrayList;

/**
 *
 * @author daveti
 * Execute instructions (Simulation)
 * Oct 3, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 */
public class Executor {

    private final ArrayList<Binary> mem;
    private final Reg regFile;
    private final boolean debug = false;
    private int pc;
    private int cycle;
    private int dataIdx;
    private int dataNum;
    private boolean breaked = false;
    private Instruction ins;

    Executor(ArrayList<Binary> mem, Reg regFile) {
        this.mem = mem;
        this.regFile = regFile;
        this.pc = 0;
        this.cycle = 0;
        this.ins = null;
    }

    private int getDataIdxMem() {
        for (Binary bin : mem) {
            if (bin.isDat) {
                return mem.indexOf(bin);
            }
        }

        return -1;
    }

    private int getDataNumMem() {
        int t = 0;
        for (Binary bin : mem) {
            if (bin.isDat) {
                t++;
            }
        }

        return t;
    }

    private int getInsIdxMemAddr(int addr) {
        for (Binary bin : mem) {
            if (bin.isDat) {
                break;
            }
            if (bin.ins.address == addr) {
                return mem.indexOf(bin);
            }
        }

        return -1;
    }

    private int getDataIdxMemAddr(int addr) {
        for (Binary bin : mem) {
            if ((bin.isDat) && (bin.dat.address == addr)) {
                return mem.indexOf(bin);
            }
        }

        return -1;
    }

    public void execute() {
        boolean jmp;

        // After decoding, the memory is not empty
        dataIdx = getDataIdxMem();
        dataNum = getDataNumMem();

        while (true) {
            // Start clock
            cycle += 1;
            jmp = false;

            // Read in the instruction
            Binary bin = mem.get(pc);
            if (bin.isDat) {
                System.out.println("Error: data during execution");
                break;
            }
            ins = bin.ins;

            // Run the damn code
            switch (ins.category) {
                case 1:
                    switch (ins.opcode) {
                        case "J":
                            // Jump to the target address
                            jmp = true;
                            pc = getInsIdxMemAddr(Integer.parseInt(ins.targetAdr));
                            break;

                        case "BEQ":
                        case "BNE":
                        case "BGTZ":
                            // Get rt, rs, offset values
                            int rtB = regFile.getRegVal(ins.targetReg);
                            int rsB = regFile.getRegVal(ins.sourceReg);
                            int offsetB = Integer.parseInt(ins.offset);
                            int addr;
                            if (("BEQ".equals(ins.opcode) && (rtB == rsB))
                                    || ("BNE".equals(ins.opcode) && (rtB != rsB))
                                    || ("BGTZ".equals(ins.opcode) && (rsB > 0))) {
                                // Branch to 18-bit(offset)+address(nextIns)
                                addr = offsetB + mem.get(pc + 1).ins.address;
                                jmp = true;
                                pc = getInsIdxMemAddr(addr);
                            }
                            break;

                        case "SW":
                            // Get rt, base, offset values
                            int rt = regFile.getRegVal(ins.targetReg);
                            int base = regFile.getRegVal(ins.sourceReg);
                            int offset = Integer.parseInt(ins.offset);
                            // memory[base+offset] <- rt
                            int idx = getDataIdxMemAddr(base + offset);
                            if (idx != -1) {
                                mem.get(idx).dat.data = rt;
                            } else {
                                System.out.println("Error: invalid idx from getDataIdxMemAddr for SW");
                            }
                            break;

                        case "LW":
                            // Get base, offset values
                            int baseL = regFile.getRegVal(ins.sourceReg);
                            int offsetL = Integer.parseInt(ins.offset);
                            // rt <- memory[base+offset]
                            int idxL = getDataIdxMemAddr(baseL + offsetL);
                            if (idxL != -1) {
                                regFile.setRegVal(ins.targetReg, mem.get(idxL).dat.data);
                            } else {
                                System.out.println("Error invalid idx from getDataIdxMemAddr for LW");
                            }
                            break;

                        case "BREAK":
                            breaked = true;
                            break;

                        default:
                            System.out.println("Error: cate 1 unsupported opcode " + ins.opcode);
                            break;
                    }
                    break;

                case 2:
                    // Get src1 and src2 values
                    if (debug) {
                        System.out.println("Debug: sourceReg=" + ins.sourceReg
                                + ", source2nd=" + ins.source2nd);
                        regFile.dumpReg();
                    }
                    int src1 = regFile.getRegVal(ins.sourceReg);
                    int src2 = regFile.getRegVal(ins.source2nd);
                    switch (ins.opcode) {
                        case "ADD":
                            // rt <- rs1 + rs2
                            regFile.setRegVal(ins.targetReg, src1 + src2);
                            break;

                        case "SUB":
                            regFile.setRegVal(ins.targetReg, src1 - src2);
                            break;

                        case "AND":
                            regFile.setRegVal(ins.targetReg, src1 & src2);
                            break;

                        case "OR":
                            regFile.setRegVal(ins.targetReg, src1 | src2);
                            break;

                        case "SRL":
                            // Logical right shift
                            regFile.setRegVal(ins.targetReg, src1 >>> src2);
                            break;

                        case "SRA":
                            // Arithmetic right shift
                            regFile.setRegVal(ins.targetReg, src1 >> src2);
                            break;

                        default:
                            System.out.println("Error: cate 2 unsupported opcode " + ins.opcode);
                            break;
                    }
                    break;

                case 3:
                    // Get source, immediate values
                    if (debug) {
                        System.out.println("Debug: sourceReg=" + ins.sourceReg
                                + ", sourceImm=" + ins.sourceImm);
                    }
                    int src = regFile.getRegVal(ins.sourceReg);
                    int imm = Integer.parseInt(ins.sourceImm);
                    switch (ins.opcode) {
                        case "ADDI":
                            // rt <- rs + imm
                            regFile.setRegVal(ins.targetReg, src + imm);
                            break;

                        case "ANDI":
                            // rt <- rs AND imm
                            regFile.setRegVal(ins.targetReg, src & imm);
                            break;

                        case "ORI":
                            // rt <- rs OR imm
                            regFile.setRegVal(ins.targetReg, src | imm);
                            break;

                        default:
                            System.out.println("Error: cate 3 unsupported opcode " + ins.opcode);
                            break;
                    }
                    break;

                case 4:
                    // Get rs, rt values
                    int rs = regFile.getRegVal(ins.sourceReg);
                    int rt = regFile.getRegVal(ins.source2nd);
                    switch (ins.opcode) {
                        case "MULT":
                            // (HI,LO) <- rs*rt
                            long mul = (long) rs * (long) rt;
                            // Split the 64 bits into 2 32-bits
                            String hex = Long.toHexString(mul);
                            if (debug) {
                                System.out.println("Debug: hex=" + hex);
                            }
                            int padLen = 16 - hex.length();
                            String pad = "";
                            for (int i = 0; i < padLen; i++) {
                                pad += "0";
                            }
                            hex = pad + hex;
                            String hiStr = hex.substring(0, 8);
                            String loStr = hex.substring(8);
                            Long hiL = Long.parseLong(hiStr, 16);
                            int hiVal = hiL.intValue();
                            Long loL = Long.parseLong(loStr, 16);
                            int loVal = loL.intValue();
                            // Copy high 32-bit to HI
                            regFile.setRegHiVal(hiVal);
                            // Copy low 32-bit to LO
                            regFile.setRegLoVal(loVal);
                            break;

                        case "DIV":
                            // (HI,LO) <- rs/rt
                            int quo = rs / rt;
                            int rem = rs % rt;
                            // Copy quotient to LO
                            regFile.setRegLoVal(quo);
                            // Copy remainder to HI
                            regFile.setRegHiVal(rem);
                            break;

                        default:
                            System.out.println("Error: cate 4 unsupported opcode " + ins.opcode);
                            break;
                    }
                    break;

                case 5:
                    switch (ins.opcode) {
                        case "MFHI":
                            // Copy HI to targetReg
                            int hiVal = regFile.getRegHiVal();
                            regFile.setRegVal(ins.targetReg, hiVal);
                            break;

                        case "MFLO":
                            // Copy LO to targetReg
                            int loVal = regFile.getRegLoVal();
                            regFile.setRegVal(ins.targetReg, loVal);
                            break;

                        default:
                            System.out.println("Error: cate 5 unsupported opcode " + ins.opcode);
                            break;
                    }
                    break;

                default:
                    System.out.println("Error: unknown category " + Integer.toString(ins.category));
                    break;
            }

            // Dump the simulation results
            dumpSim();

            // Increment the PC
            if (!jmp) {
                pc += 1;
            }

            if (breaked) {
                break;
            }
        }
    }

    private void dumpData() {
        int rep = dataNum / regFile.getRegSep();
        int rem = dataNum % regFile.getRegSep();
        if (debug) {
            System.out.println("dataNum=" + dataNum + ", sep="
                    + regFile.getRegSep() + ", rep=" + rep + ", rem=" + rem);
        }
        String tmp;
        String tmp2;
        for (int i = 0; i < rep; i++) {
            tmp = "";
            for (int j = 0; j < regFile.getRegSep(); j++) {
                tmp += String.format("%8d", mem.get(dataIdx + i * regFile.getRegSep() + j).dat.data);
            }
            tmp2 = String.format("%d:", mem.get(dataIdx + i * regFile.getRegSep()).dat.address);
            System.out.println(tmp2 + tmp);
        }

        if (rem != 0) {
            // Handle the left ones
            tmp = "";
            for (int k = 1; k <= rem; k++) {
                tmp += String.format("%8d", mem.get(dataIdx + rep * regFile.getRegSep() + k).dat.data);
            }
            tmp2 = String.format("%d:", mem.get(dataIdx + rep * regFile.getRegSep()).dat.address);
            System.out.println(tmp2 + tmp);
        }
    }

    private void dumpSim() {

        System.out.println("--------------------");
        System.out.println("Cycle " + cycle + ":\t" + ins.address + "\t" + ins.dump());
        System.out.println();

        // Dump the reg files
        System.out.println("Registers");
        regFile.dumpReg();
        System.out.println();

        // Dump the data
        System.out.println("Data");
        dumpData();
        System.out.println();
    }

}
