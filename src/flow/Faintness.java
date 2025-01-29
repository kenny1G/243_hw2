package flow;

import java.util.*;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Main.Helper;

public class Faintness implements Flow.Analysis {

    public static class VarSet implements Flow.DataflowObject {
        private Set<String> set;
        public static Set<String> universalSet;
        public VarSet() { set = new TreeSet<String>(); }

        public void setToTop() { set = new TreeSet<String>(universalSet); }
        public void setToBottom() { set = new TreeSet<String>(); }

        public void meetWith(Flow.DataflowObject o)
        {
            VarSet a = (VarSet) o;
            set.retainAll(a.set);
        }

        public void copy(Flow.DataflowObject o)
        {
            VarSet a = (VarSet) o;
            set = new TreeSet<String>(a.set);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof VarSet)
            {
                VarSet a = (VarSet) o;
                return set.equals(a.set);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return set.hashCode();
        }
        @Override
        public String toString()
        {
            return set.toString();
        }

        private void unfaintVar(String uv, String dv) {
            if (dv == null || !set.contains(dv)) {
                set.remove(uv);
            }
        }
    }

    private VarSet[] in, out;
    private VarSet entry, exit;

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg  The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        System.out.println("Method: "+cfg.getMethod().getName().toString());

        // get the amount of space we need to allocate for the in/out arrays.
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max)
                max = id;
        }
        max += 1;

        // allocate the in and out arrays.
        in = new VarSet[max];
        out = new VarSet[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new VarSet();
            out[id] = new VarSet();
        }

        // initialize the entry and exit points.
        entry = new VarSet();
        exit = new VarSet();

        /************************************************
         * Your remaining initialization code goes here *
         ************************************************/

        Set<String> s = new TreeSet<String>();
        VarSet.universalSet = s;

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            s.add("R"+i);
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                s.add(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                s.add(use.getRegister().toString());
            }
        }

        exit.setToTop();
        transferfn.val = new VarSet();
        System.out.println("Initialization completed.");
    }

    /**
     * This method is called after the fixpoint is reached.
     * It must print out the dataflow objects associated with
     * the entry, exit, and all interior points of the CFG.
     * Unless you modify in, out, entry, or exit you shouldn't
     * need to change this method.
     *
     * @param cfg  Unused.
     */
    public void postprocess (ControlFlowGraph cfg) {
        System.out.println("entry: " + entry.toString());
        for (int i=1; i<in.length; i++) {
            if (in[i] != null) {
                System.out.println(i + " in:  " + in[i].toString());
                System.out.println(i + " out: " + out[i].toString());
            }
        }
        System.out.println("exit: " + exit.toString());
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward () { return false; }

    /* Routines for interacting with dataflow values. */
    public Flow.DataflowObject getEntry()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }
    public Flow.DataflowObject getExit()
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }
    public void setEntry(Flow.DataflowObject value)
    {
        entry.copy(value);
    }
    public void setExit(Flow.DataflowObject value)
    {
        exit.copy(value);
    }
    public Flow.DataflowObject getIn(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]);
        return result;
    }
    public Flow.DataflowObject getOut(Quad q)
    {
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]);
        return result;
    }
    public void setIn(Quad q, Flow.DataflowObject value)
    {
        in[q.getID()].copy(value);
    }
    public void setOut(Quad q, Flow.DataflowObject value)
    {
        out[q.getID()].copy(value);
    }

    public Flow.DataflowObject newTempVar() { return new VarSet(); }

    private TransferFunction transferfn = new TransferFunction ();
    public void processQuad(Quad q) {
        transferfn.val = (VarSet) getOut(q);
        Helper.runPass(q, transferfn);
        setIn(q, transferfn.val);
    }

    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        VarSet val;
        @Override
        public void visitMove(Quad q) {
            Operand op = Operator.Move.getSrc(q);
            String key = Operator.Move.getDest(q).getRegister().toString();
            if (op instanceof RegisterOperand) {
                String used = ((RegisterOperand) op).getRegister().toString();
                val.unfaintVar(used, key);
            }
        }
        @Override
        public void visitBinary(Quad q) {
            Operand op1 = Operator.Binary.getSrc1(q);
            Operand op2 = Operator.Binary.getSrc2(q);
            String key = Operator.Binary.getDest(q).getRegister().toString();
            if (op1 instanceof RegisterOperand) {
                String used = ((RegisterOperand) op1).getRegister().toString();
                val.unfaintVar(used, key);
            }
            if (op2 instanceof RegisterOperand) {
                String used = ((RegisterOperand) op2).getRegister().toString();
                val.unfaintVar(used, key);
            }
        }

        @Override
        public void visitQuad(Quad q) {
            Operator op = q.getOperator();
            if (!(op instanceof Operator.Move || op instanceof Operator.Binary)) {
                for (RegisterOperand used: q.getUsedRegisters()) {
                    val.unfaintVar(used.getRegister().toString(), null);
                }
            }
        }
    }
}