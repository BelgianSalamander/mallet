package me.salamander.mallet.shaders.compiler.cfg;

import me.salamander.mallet.shaders.compiler.instruction.Instruction;

import java.io.PrintStream;
import java.util.*;

public class BasicBlock {
    private List<Instruction> instructions;
    private final boolean mergable; //END and START blocks should be unmergeable
    final List<BasicBlock> next = new ArrayList<>();
    final List<BasicBlock> prev = new ArrayList<>();

    public BasicBlock(List<Instruction> instructions, boolean mergable) {
        this.instructions = instructions;
        this.mergable = mergable;
    }

    public BasicBlock(List<Instruction> instructions) {
        this.instructions = instructions;
        this.mergable = true;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public List<BasicBlock> getNext() {
        return next;
    }

    public List<BasicBlock> getPrev() {
        return prev;
    }

    public void reduce() {
        Stack<BasicBlock> toProcess = new Stack<>();
        Set<BasicBlock> processed = new HashSet<>();

        toProcess.add(this);

        while (!toProcess.isEmpty()) {
            BasicBlock current = toProcess.pop();
            if (processed.contains(current)) {
                continue;
            }
            processed.add(current);

            while (current.tryMergeWithNext()) {

            }

            toProcess.addAll(current.getNext());
        }
    }

    private boolean tryMergeWithNext() {
        if(next.size() != 1) return false;
        BasicBlock next = this.next.get(0);
        if(next.prev.size() != 1) return false;

        if(!mergable || !next.mergable) return false;

        this.instructions.addAll(next.instructions);
        this.next.clear();
        this.next.addAll(next.next);

        //Fix up the next nodes
        for(BasicBlock b : next.next) {
            b.prev.remove(next);
            b.prev.add(this);
        }

        return true;
    }

    public List<BasicBlock> getAll(){
        List<BasicBlock> all = new ArrayList<>();
        this.getAll(all);
        return all;
    }

    public void getAll(Collection<BasicBlock> allVisited){
        if(allVisited.contains(this)) return;
        allVisited.add(this);
        for(BasicBlock next : this.next){
            next.getAll(allVisited);
        }
    }

    public List<BasicBlock> getAllBackwards(){
        List<BasicBlock> all = new ArrayList<>();
        this.getAllBackwards(all);
        return all;
    }

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }

    public void getAllBackwards(Collection<BasicBlock> allVisited){
        if(allVisited.contains(this)) return;
        allVisited.add(this);
        for(BasicBlock prev : this.prev){
            prev.getAllBackwards(allVisited);
        }
    }

    public void print(PrintStream out){
        out.println("Basic Block:");
        for(Instruction i : instructions){
            out.println("\t" + i.toString());
        }
    }
}
