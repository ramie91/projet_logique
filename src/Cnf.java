import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class Cnf {
    final int totalVars;
    final List<int[]> clauses;

    Cnf(int totalVars, List<int[]> clauses) {
        this.totalVars = totalVars;
        this.clauses = clauses;
    }

    static Cnf read(String dimacsFile) throws IOException {
        int totalVars = 0;
        List<int[]> clauses = new ArrayList<>();

        for (String rawLine : Files.readAllLines(Path.of(dimacsFile))) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("c")) {
                continue;
            }
            if (line.startsWith("p")) {
                String[] parts = line.split("\\s+");
                totalVars = Integer.parseInt(parts[2]);
                continue;
            }

            String[] parts = line.split("\\s+");
            List<Integer> literals = new ArrayList<>();
            for (String part : parts) {
                int literal = Integer.parseInt(part);
                if (literal == 0) {
                    break;
                }
                literals.add(literal);
            }
            int[] clause = literals.stream().mapToInt(Integer::intValue).toArray();
            clauses.add(clause);
        }

        return new Cnf(totalVars, clauses);
    }
}
