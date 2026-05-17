import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {
    private static RunStats run(String puzzleFile, String dimacsFile) throws IOException {
        long start = System.nanoTime();
        EasyAsABCSolver solver = new EasyAsABCSolver();
        solver.generateDimacsFile(puzzleFile, dimacsFile);
        solver.solve(dimacsFile);
        if (solver.hasSolution()) {
            solver.printSolutionGrid();
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;
        RunStats stats = new RunStats(
                puzzleFile,
                dimacsFile,
                solver.getVariant(),
                solver.getGridSize(),
                solver.getTotalVars(),
                solver.getClauseCount(),
                solver.getLiteralCount(),
                solver.hasSolution(),
                solver.hasSecondSolution(),
                durationMs);
        stats.printSingleSummary();
        return stats;
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("all")) {
            try {
                runAllPuzzles();
            } catch (IOException | IllegalArgumentException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
            return;
        }

        if (args.length < 2) {
            System.out.println("Veuillez entrer deux noms de fichiers valides : puzzle_file et dimacs_file");
            System.out.println("Ou utiliser : java Main all");
            System.exit(1);
        }

        String puzzleFile = "Puzzle/" + args[0];
        String dimacsFile = "Dimacs/" + args[1];

        try {
            run(puzzleFile, dimacsFile);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void runAllPuzzles() throws IOException {
        List<Path> puzzles;
        try (var stream = Files.list(Path.of("Puzzle"))) {
            puzzles = stream
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        if (puzzles.isEmpty()) {
            System.out.println("Aucun fichier puzzle trouve dans Puzzle/");
            return;
        }

        List<RunStats> allStats = new ArrayList<>();
        System.out.println("Resolution de tous les puzzles du dossier Puzzle/");
        System.out.println("Nombre d'instances : " + puzzles.size());

        for (Path puzzle : puzzles) {
            String puzzleName = puzzle.getFileName().toString();
            String dimacsName = puzzleName.replaceFirst("\\.txt$", "_all.cnf");
            System.out.println("\n============================================================");
            System.out.println("Instance : " + puzzleName);
            System.out.println("============================================================");
            allStats.add(run(puzzle.toString(), Path.of("Dimacs", dimacsName).toString()));
        }

        printGlobalStats(allStats);
    }

    private static void printGlobalStats(List<RunStats> stats) {
        int satisfiable = 0;
        int withSecondSolution = 0;
        int totalVariables = 0;
        int totalClauses = 0;
        int totalLiterals = 0;
        long totalDurationMs = 0;

        for (RunStats stat : stats) {
            if (stat.satisfiable) {
                satisfiable++;
            }
            if (stat.secondSolution) {
                withSecondSolution++;
            }
            totalVariables += stat.variables;
            totalClauses += stat.clauses;
            totalLiterals += stat.literals;
            totalDurationMs += stat.durationMs;
        }

        System.out.println("\n============================================================");
        System.out.println("Statistiques globales");
        System.out.println("============================================================");
        System.out.println("Instances resolues        : " + stats.size());
        System.out.println("Instances satisfaisables  : " + satisfiable);
        System.out.println("Instances insatisfaisables: " + (stats.size() - satisfiable));
        System.out.println("Avec deuxieme solution    : " + withSecondSolution);
        System.out.println("Variables SAT totales     : " + totalVariables);
        System.out.println("Clauses totales           : " + totalClauses);
        System.out.println("Literaux totaux           : " + totalLiterals);
        System.out.println("Temps total               : " + totalDurationMs + " ms");
        System.out.println();
        System.out.println("Detail par instance :");
        for (RunStats stat : stats) {
            System.out.println("  - " + Path.of(stat.puzzleFile).getFileName()
                    + " | " + stat.variant
                    + " | " + stat.gridSize + "x" + stat.gridSize
                    + " | SAT=" + (stat.satisfiable ? "oui" : "non")
                    + " | 2e solution=" + (stat.secondSolution ? "oui" : "non")
                    + " | vars=" + stat.variables
                    + " | clauses=" + stat.clauses
                    + " | temps=" + stat.durationMs + " ms");
        }
    }

    private record RunStats(
            String puzzleFile,
            String dimacsFile,
            String variant,
            int gridSize,
            int variables,
            int clauses,
            int literals,
            boolean satisfiable,
            boolean secondSolution,
            long durationMs) {

        void printSingleSummary() {
            System.out.println("\nStatistiques d'execution :");
            System.out.println("  Puzzle               : " + puzzleFile);
            System.out.println("  DIMACS               : " + dimacsFile);
            System.out.println("  Variante             : " + variant);
            System.out.println("  Taille               : " + gridSize + "x" + gridSize);
            System.out.println("  Variables SAT        : " + variables);
            System.out.println("  Clauses              : " + clauses);
            System.out.println("  Literaux             : " + literals);
            System.out.println("  Satisfaisable        : " + (satisfiable ? "oui" : "non"));
            System.out.println("  Deuxieme solution    : " + (secondSolution ? "oui" : "non"));
            System.out.println("  Temps execution      : " + durationMs + " ms");
        }
    }
}
