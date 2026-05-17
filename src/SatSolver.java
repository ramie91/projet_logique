import java.util.ArrayList;
import java.util.List;

class SatSolver {
    private final int totalVars;
    private final List<int[]> clauses;

    SatSolver(int totalVars, List<int[]> clauses) {
        this.totalVars = totalVars;
        this.clauses = new ArrayList<>(clauses);
    }

    void addClause(int[] clause) {
        clauses.add(clause);
    }

    int[] solve() {
        return dpll(new int[totalVars + 1]);
    }

    private int[] dpll(int[] assignment) {
        int propagation = propagate(assignment);
        if (propagation == -1) {
            return null;
        }
        if (allSatisfied(assignment)) {
            return assignment;
        }

        int literal = chooseLiteral(assignment);
        if (literal == 0) {
            return assignment;
        }

        int var = Math.abs(literal);
        int preferredValue = literal > 0 ? 1 : -1;

        int[] first = assignment.clone();
        first[var] = preferredValue;
        int[] firstResult = dpll(first);
        if (firstResult != null) {
            return firstResult;
        }

        int[] second = assignment.clone();
        second[var] = -preferredValue;
        return dpll(second);
    }

    private int propagate(int[] assignment) {
        boolean changed;
        do {
            changed = false;
            for (int[] clause : clauses) {
                boolean satisfied = false;
                int unassignedCount = 0;
                int lastUnassigned = 0;

                for (int lit : clause) {
                    int value = assignment[Math.abs(lit)];
                    if (value == 0) {
                        unassignedCount++;
                        lastUnassigned = lit;
                    } else if ((lit > 0 && value == 1) || (lit < 0 && value == -1)) {
                        satisfied = true;
                        break;
                    }
                }

                if (satisfied) {
                    continue;
                }
                if (unassignedCount == 0) {
                    return -1;
                }
                if (unassignedCount == 1) {
                    int var = Math.abs(lastUnassigned);
                    int required = lastUnassigned > 0 ? 1 : -1;
                    if (assignment[var] != 0 && assignment[var] != required) {
                        return -1;
                    }
                    if (assignment[var] == 0) {
                        assignment[var] = required;
                        changed = true;
                    }
                }
            }
        } while (changed);
        return 0;
    }

    private boolean allSatisfied(int[] assignment) {
        for (int[] clause : clauses) {
            boolean satisfied = false;
            for (int lit : clause) {
                int value = assignment[Math.abs(lit)];
                if ((lit > 0 && value == 1) || (lit < 0 && value == -1)) {
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                return false;
            }
        }
        return true;
    }

    private int chooseLiteral(int[] assignment) {
        int bestLiteral = 0;
        int bestSize = Integer.MAX_VALUE;

        for (int[] clause : clauses) {
            boolean satisfied = false;
            int unassignedCount = 0;
            int candidate = 0;

            for (int lit : clause) {
                int value = assignment[Math.abs(lit)];
                if ((lit > 0 && value == 1) || (lit < 0 && value == -1)) {
                    satisfied = true;
                    break;
                }
                if (value == 0) {
                    unassignedCount++;
                    if (candidate == 0) {
                        candidate = lit;
                    }
                }
            }

            if (!satisfied && unassignedCount > 0 && unassignedCount < bestSize) {
                bestSize = unassignedCount;
                bestLiteral = candidate;
            }
        }

        return bestLiteral;
    }
}
