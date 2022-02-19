package me.salamander.mallet.compiler;

import me.salamander.mallet.compiler.analysis.Analysis;
import me.salamander.mallet.compiler.analysis.AnalysisInfo;
import me.salamander.mallet.compiler.analysis.AnalysisResults;
import me.salamander.mallet.compiler.analysis.livevar.LiveVarValue;
import me.salamander.mallet.compiler.analysis.livevar.LiveVariables;
import me.salamander.mallet.compiler.analysis.mutability.MutabilitySemiLattice;
import me.salamander.mallet.compiler.analysis.usage.PossibleValuesTracker;
import me.salamander.mallet.compiler.analysis.valuetrack.ValueTrackValue;
import me.salamander.mallet.compiler.analysis.valuetrack.ValueTracker;
import me.salamander.mallet.compiler.cfg.BasicBlock;
import me.salamander.mallet.compiler.cfg.instruction.InstructionCFG;
import me.salamander.mallet.compiler.cfg.IntermediaryCFG;
import me.salamander.mallet.compiler.instruction.AssignmentInstruction;
import me.salamander.mallet.compiler.instruction.Instruction;
import me.salamander.mallet.compiler.instruction.IntermediaryInstructionMaker;
import me.salamander.mallet.compiler.instruction.Label;
import me.salamander.mallet.compiler.instruction.value.Location;
import me.salamander.mallet.compiler.instruction.value.MethodCallValue;
import me.salamander.mallet.compiler.instruction.value.Value;
import me.salamander.mallet.compiler.instruction.value.Variable;
import me.salamander.mallet.util.MethodInvocation;
import me.salamander.mallet.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class JavaDecompiler {
    private final MethodNode methodNode;
    private final ClassNode classNode;
    private final ShaderCompiler shaderCompiler;
    private final AtomicInteger tempVarCounter = new AtomicInteger();

    public JavaDecompiler(ClassNode classNode, MethodNode methodNode, ShaderCompiler shaderCompiler) {
        this.methodNode = methodNode;
        this.classNode = classNode;
        this.shaderCompiler = shaderCompiler;

        //Verify compatibility
        if(this.methodNode.tryCatchBlocks.size() > 0) {
            throw new IllegalArgumentException("Method contains try-catch blocks");
        }
    }

    public void decompile(){
        //Make the intermediate representation
        List<Instruction> instructions = makeIntermediateRepresentation();
        IntermediaryCFG intermediaryCFG = createIntermediaryCFG(instructions);

        InstructionCFG instructionCFG = new InstructionCFG(intermediaryCFG, this);
        instructionCFG.detectLoops();
    }

    public int getNextTempVar(){
        return tempVarCounter.getAndIncrement();
    }

    @NotNull
    private IntermediaryCFG createIntermediaryCFG(List<Instruction> instructions) {
        IntermediaryCFG cfg;
        cfg = new IntermediaryCFG(instructions);

        boolean changed = true;
        while (changed){
            ValueTracker valueTracker = new ValueTracker(this.shaderCompiler);
            AnalysisResults<ValueTrackValue> results = Analysis.analyze(valueTracker, cfg);

            PossibleValuesTracker possibleValuesTracker = new PossibleValuesTracker();
            Analysis.analyze(possibleValuesTracker, cfg); //Discard results

            results.print(System.out, cfg);
            changed = inlineValues(cfg, results, possibleValuesTracker);
            System.out.println("\n");
        }

        //Remove unneeded assignments
        System.out.println("Live variables:");
        AnalysisResults<LiveVarValue> liveVariableResults = Analysis.analyze(new LiveVariables(), cfg);
        liveVariableResults.print(System.out, cfg);
        removeUnneededAssignments(cfg, liveVariableResults);
        System.out.println("\n");

        System.out.println("Final:");
        cfg.print(System.out);

        checkMutability(cfg);
        return cfg;
    }

    private void checkMutability(IntermediaryCFG cfg) {
        /*
         * Mutability rules:
         *  - For nearly all a = b, 'a' is immutable after this assignment
         *  - The exception is for things like a = new T; Where a is mutable but in a future
         *    assignment such as b = a, b is mutable but a is made immutable.
         *  - Any field's value is immutable
         *  - Immutable value's fields cannot be modified
         *  - Immutable values cannot be passed as mutable arguments
         */

        Analysis.analyze(new MutabilitySemiLattice(MethodInvocation.of(this.classNode.name, methodNode), shaderCompiler), cfg);
    }

    private void removeUnneededAssignments(IntermediaryCFG cfg, AnalysisResults<LiveVarValue> results) {
        for(BasicBlock block : cfg.getBlocks()) {
            List<Instruction> instructions = block.getInstructions();

            if(instructions.size() == 0) continue; //It is START or END

            AnalysisInfo<LiveVarValue> info = results.getBlocks().get(block);

            for (int i = instructions.size() - 1; i >= 0; i--) {
                Instruction instruction = instructions.get(i);

                if(instruction instanceof AssignmentInstruction assign){
                    //Check if can remove
                    if(assign.getLocation() instanceof Variable var){
                        if(!info.out[i].isLive(var)){
                            if(assign.getValue() instanceof MethodCallValue methodCall){
                                //Check the method call doesn't mutate anything
                                if(shaderCompiler.getMutatedArgs(methodCall.getMethodCall().getInvocation()).size() > 0){
                                    continue;
                                }
                            }

                            instructions.remove(i);
                        }
                    }
                }
            }
        }
    }

    //Returns whether a change occurred
    private boolean inlineValues(IntermediaryCFG cfg, AnalysisResults<ValueTrackValue> results, PossibleValuesTracker valueTracker) {
        Ref<Boolean> changed = new Ref<>(false);

        for(BasicBlock block: cfg.getBlocks()){
            List<Instruction> newInstructions = new ArrayList<>();

            AnalysisInfo<ValueTrackValue> info = results.getBlocks().get(block);

            for (int i = 0; i < block.getInstructions().size(); i++) {
                Instruction instruction = block.getInstructions().get(i);
                ValueTrackValue in = info.in[i];
                ValueTrackValue out = info.out[i];

                class ValueCopier implements Function<Value, Value> {
                    private final ValueTrackValue data;

                    public ValueCopier(ValueTrackValue data) {
                        this.data = data;
                    }

                    @Override
                    public Value apply(Value value) {
                        if(value instanceof Variable variable) {
                            if(this.data.get(variable) != null){
                                Value maybeInlined = this.data.get(variable);

                                //Check that can inline
                                if(maybeInlined.allowInline()) {
                                    //Check duplicate inline
                                    Collection<Instruction> usedBy = valueTracker.getUsedBy().get(maybeInlined);
                                    boolean allowDuplicateInline = maybeInlined.allowDuplicateInline();
                                    if((usedBy != null && usedBy.size() <= 1) || allowDuplicateInline) {
                                        changed.value = true;

                                        return maybeInlined;
                                    }
                                }
                            }
                        }

                        return value.copyValue(this);
                    }
                }

                ValueCopier locationCopier = new ValueCopier(out);

                newInstructions.add(instruction.copy(
                        new ValueCopier(in),
                        loc -> (Location) loc.copyValue(locationCopier)
                ));
            }

            if(newInstructions.size() > 0) {
                block.setInstructions(newInstructions);
            }
        }

        return changed.value;
    }

    private List<Instruction> makeIntermediateRepresentation() {
        SimpleVerifier verifier = new SimpleVerifier();
        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);
        Frame<BasicValue>[] frames;

        try {
            frames = analyzer.analyze(classNode.name, methodNode);
        }catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }

        List<Instruction> instructions = new ArrayList<>();

        for (int i = 0; i < frames.length; i++) {
            Frame<BasicValue> frame = frames[i];
            AbstractInsnNode insn = methodNode.instructions.get(i);

            if(frame == null) {
                continue;
            }

            IntermediaryInstructionMaker maker = new IntermediaryInstructionMaker(frame, this);
            maker.interpret(insn);
            instructions.addAll(maker.getInstructions());
        }

        return instructions;
    }

    private final Map<LabelNode, Label> labelLookup = new HashMap<>();

    public Label getLabel(LabelNode node) {
        return labelLookup.computeIfAbsent(node, (key) -> this.makeLabel());
    }

    public Label makeLabel() {
        return new Label("LABEL_" + labelLookup.size());
    }

    public ShaderCompiler getShaderCompiler() {
        return shaderCompiler;
    }
}
