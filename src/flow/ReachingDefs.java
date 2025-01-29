package flow;

// some useful things to import. add any additional imports you need.
import java.util.*;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;


/**
 * Skeleton class for implementing a reaching definition analysis
 * using the Flow.Analysis interface.
 */
public class ReachingDefs implements Flow.Analysis {

    private Map<Integer, String> definitions;
    public class DefSet implements Flow.DataflowObject {
        private Set<Integer> set;
        public DefSet() { set = new TreeSet<Integer>(); }

        public void setToTop() { set = new TreeSet<Integer>(); }
        public void setToBottom() { set = definitions.keySet(); }

        public void meetWith (Flow.DataflowObject o)
        {
            DefSet a = (DefSet) o;
            set.addAll(a.set);
        }

        public void copy (Flow.DataflowObject o)
        {
            DefSet a = (DefSet) o;
            set = new TreeSet<Integer>(a.set);
        }

        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[ID0, ID1, ID2, ...]", where each ID is
         * the identifier of a quad defining some register, and the
         * list of IDs must be sorted.  See src/test/Test.rd.out
         * for example output of the analysis.  The output format of
         * your reaching definitions analysis must match this exactly.
         */
        @Override
        public String toString()
        {
            return set.toString();
        }

        @Override
        public boolean equals(Object o)
        {
            if (o instanceof DefSet)
            {
                DefSet a = (DefSet) o;
                return set.equals(a.set);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }

        private void add(int i) { set.add(i); }

        private void kill(String id) {
            Set<Integer> killedDef = new HashSet<Integer>();
            for (int i: set) {
                if (definitions.get(i).equals(id)) {
                    killedDef.add(i);
                }
            }
            set.removeAll(killedDef);
        }
    }

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private DefSet[] in, out;
    private DefSet entry, exit;

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
        in = new DefSet[max];
        out = new DefSet[max];

        // initialize the contents of  in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new DefSet();
            out[id] = new DefSet();
        }

        // initialize the entry and exit points.
        entry = new DefSet();
        exit = new DefSet();
        transferfn.val = new DefSet();

        definitions = new TreeMap<Integer, String>();
        QuadIterator it = new QuadIterator(cfg);
        while (it.hasNext()) {
            Quad quad = it.next();
            if (quad.getDefinedRegisters().size() > 0) {
                definitions.put(quad.getID(), quad.getDefinedRegisters().get(0).getRegister().toString());
            }
        }
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
        for (int i=0; i<in.length; i++) {
            if (in[i] != null) {
                System.out.println(i + " in:  " + in[i].toString());
                System.out.println(i + " out: " + out[i].toString());
            }
        }
        System.out.println("exit: " + exit.toString());
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward () { return true; }

    /* Routines for interacting with dataflow values. */

    public Flow.DataflowObject getEntry() {
        Flow.DataflowObject result = newTempVar();
        result.copy(entry);
        return result;
    }
    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit);
        return result;
    }
    public void setEntry(Flow.DataflowObject value)
    {
        entry.copy(value);
    }
    public void setExit(Flow.DataflowObject value) {
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

    public Flow.DataflowObject newTempVar() { return new DefSet(); }

    private TransferFunction transferfn = new TransferFunction();
    public void processQuad(Quad q) {
        transferfn.val = (DefSet) getIn(q);
        transferfn.visitQuad(q);
        setOut(q, transferfn.val);
    }

    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        DefSet val;
        @Override
        public void visitQuad(Quad q) {
            for (RegisterOperand r: q.getDefinedRegisters()) {
                val.kill(r.getRegister().toString());
            }
            if (q.getDefinedRegisters().size() > 0) { val.add(q.getID()); }
        }
    }
}