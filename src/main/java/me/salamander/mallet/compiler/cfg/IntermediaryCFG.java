package me.salamander.mallet.compiler.cfg;

import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.Label;
import me.salamander.mallet.compiler.instruction.LabelInstruction;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntermediaryCFG {
    private final List<BasicBlock> blocks = new ArrayList<>();
    private final Map<BasicBlock, Integer> blockIndices = new HashMap<>();
    private final BasicBlock startBlock;
    private final BasicBlock endBlock;
    private int startIndex;
    private int endIndex;

    public IntermediaryCFG(List<Instruction> instructions) {
        startBlock = new BasicBlock(List.of(), false);
        endBlock = new BasicBlock(List.of(), false);

        construct(instructions);
    }

    public IntermediaryCFG(List<BasicBlock> blocks, BasicBlock startBlock, BasicBlock endBlock, int startIndex, int endIndex) {
        this.blocks.addAll(blocks);
        this.startBlock = startBlock;
        this.endBlock = endBlock;
        this.startIndex = startIndex;
        this.endIndex = endIndex;

        for (int i = 0; i < this.blocks.size(); i++) {
            this.blockIndices.put(this.blocks.get(i), i);
        }
    }

    private void construct(List<Instruction> instructions) {
        //We first make a bunch of basic block with one instruction each and try to merge them together

        Map<Label, Integer> labelToIndex = new HashMap<>();

        //Generate label to index
        for(int i = 0; i < instructions.size(); i++){
            Instruction instruction = instructions.get(i);
            if(instruction instanceof LabelInstruction labelInstruction) {
                labelToIndex.put(labelInstruction.getLabel(), i);
            }
        }

        List<Integer>[] nextIndices = new List[instructions.size()];
        BasicBlock[] blocks = new BasicBlock[instructions.size()];

        for (int i = 0; i < instructions.size(); i++) {
            List<Instruction> instructionsForBlock = new ArrayList<>();
            Instruction instruction = instructions.get(i);
            instructionsForBlock.add(instruction);
            blocks[i] = new BasicBlock(instructionsForBlock);
            nextIndices[i] = instructions.get(i).getNextIndices(labelToIndex, i);
        }

        //Set next and previous
        blocks[0].prev.add(startBlock);
        startBlock.next.add(blocks[0]);

        for (int i = 0; i < instructions.size(); i++) {
            for (int nextIndex : nextIndices[i]) {
                if(nextIndex == -1){
                    blocks[i].next.add(endBlock);
                    endBlock.prev.add(blocks[i]);
                    continue;
                }

                blocks[i].next.add(blocks[nextIndex]);
                blocks[nextIndex].prev.add(blocks[i]);
            }
        }

        startBlock.reduce();

        this.blocks.addAll(startBlock.getAll());

        this.startIndex = this.blocks.indexOf(startBlock);
        this.endIndex = this.blocks.indexOf(endBlock);

        for (int i = 0; i < this.blocks.size(); i++) {
            this.blockIndices.put(this.blocks.get(i), i);
        }
    }

    public void print(PrintStream out){
        for(BasicBlock block : blocks){
            if(block == startBlock){
                out.println("Start");
            }else if(block == endBlock){
                out.println("End");
            }

            block.print(out);
            out.print("\n");
        }
    }

    public List<BasicBlock> getBlocks() {
        return blocks;
    }

    public BasicBlock getStartBlock() {
        return startBlock;
    }

    public BasicBlock getEndBlock() {
        return endBlock;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public int getIndexOf(BasicBlock b){
        return blockIndices.get(b);
    }
}
