import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;
import org.sat4j.specs.TimeoutException;

class SatSolver {
    private final int totalVars;
    private final ISolver solver;
    private boolean contradictory;

    SatSolver(int totalVars, List<int[]> clauses) {
        this.totalVars = totalVars;
        this.solver = SolverFactory.newDefault();
        this.solver.newVar(totalVars);
        this.solver.setExpectedNumberOfClauses(clauses.size());

        for (int[] clause : clauses) {
            addClause(clause);
        }
    }

    void addClause(int[] clause) {
        if (contradictory) {
            return;
        }

        try {
            solver.addClause(new VecInt(clause));
        } catch (ContradictionException e) {
            contradictory = true;
        }
    }

    int[] solve() {
        if (contradictory) {
            return null;
        }

        try {
            if (!solver.isSatisfiable()) {
                return null;
            }
            return toAssignment(solver.model());
        } catch (TimeoutException e) {
            throw new IllegalStateException("Sat4J a depasse le temps de resolution autorise", e);
        }
    }

    private int[] toAssignment(int[] model) {
        int[] assignment = new int[totalVars + 1];
        for (int literal : model) {
            int variable = Math.abs(literal);
            if (variable <= totalVars) {
                assignment[variable] = literal > 0 ? 1 : -1;
            }
        }
        return assignment;
    }
}
