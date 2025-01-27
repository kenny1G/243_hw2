package flow;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;

/**
 * Skeleton class for implementing the Flow.Solver interface.
 */
public class MySolver implements Flow.Solver {

    protected Flow.Analysis analysis;

    /**
     * Sets the analysis.  When visitCFG is called, it will
     * perform this analysis on a given CFG.
     *
     * @param analyzer The analysis to run
     */
    public void registerAnalysis(Flow.Analysis analyzer) {
        this.analysis = analyzer;
    }

    /**
     * Runs the solver over a given control flow graph.  Prior
     * to calling this, an analysis must be registered using
     * registerAnalysis
     *
     * @param cfg The control flow graph to analyze.
     */
    public void visitCFG(ControlFlowGraph cfg) {

        // this needs to come first.
        analysis.preprocess(cfg);

        Quad entry = null;
        java.util.ArrayList<Quad> exits= new java.util.ArrayList<Quad>();

        // for each basic block B other than ENTRY: OUT[B] = TOP
        Flow.DataflowObject top_elem = analysis.newTempVar();
        top_elem.setToTop();
        QuadIterator it = new QuadIterator(cfg);
        while (it.hasNext()) {
            Quad quad = it.next();
            if (analysis.isForward()) {
                analysis.setOut(quad, top_elem);
            } else {
                analysis.setIn(quad, top_elem);
            }
            if (it.successors1().contains(null)) {
                exits.add(quad);
            }
            if (it.predecessors1().contains(null)) {
                entry = quad;
            }
        }

        // while changes to any OUT occur
        boolean change;
        do {
            change = false;
            // for each basic block B other than ENTRY
            QuadIterator qit = new QuadIterator(cfg, analysis.isForward());
            while (analysis.isForward() ? qit.hasNext() : qit.hasPrevious()) {
                Quad quad = analysis.isForward() ? qit.next() : qit.previous();
                Flow.DataflowObject new_elem = analysis.newTempVar();
                new_elem.setToTop();

                // IN[B] = meet(p a predecessor of B) OUT[p]
                java.util.Iterator<Quad> neighbors = analysis.isForward() ? qit.predecessors() : qit.successors();
                while (neighbors.hasNext()) {
                    Quad neighbor = neighbors.next();
                    Flow.DataflowObject neighbor_elem;
                    if (analysis.isForward()) {
                        neighbor_elem = neighbor == null ? analysis.getEntry() : analysis.getOut(neighbor);
                    } else {
                        neighbor_elem = neighbor == null ? analysis.getExit() : analysis.getIn(neighbor);
                    }
                    new_elem.meetWith(neighbor_elem);
                }

                // Set the new value and process
                Flow.DataflowObject old_elem;
                if (analysis.isForward()) {
                    analysis.setIn(quad, new_elem);
                    old_elem = analysis.getOut(quad);
                } else {
                    analysis.setOut(quad, new_elem);
                    old_elem = analysis.getIn(quad);
                }

                // OUT[B] = fB(IN[B])
                analysis.processQuad(quad);

                // Check if value changed
                Flow.DataflowObject result_elem = analysis.isForward() ? analysis.getOut(quad) : analysis.getIn(quad);
                if (!result_elem.equals(old_elem)) {
                    change = true;
                }
            }
        } while (change);

        // handle entry and exit
        if (!analysis.isForward()) {
            Flow.DataflowObject entry_elem = analysis.getIn(entry);
            analysis.setEntry(entry_elem);
        } else {
            Flow.DataflowObject exit_elem = analysis.newTempVar();
            exit_elem.setToTop();
            for (Quad exit: exits) {
                exit_elem.meetWith(analysis.getOut(exit));
            }
            analysis.setExit(exit_elem);
        }

        // this needs to come last.
        analysis.postprocess(cfg);
    }
}