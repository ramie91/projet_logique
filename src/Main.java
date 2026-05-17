import java.io.IOException;

public class Main {
    private static void run(String puzzleFile, String dimacsFile) throws IOException {
        EasyAsABCSolver solver = new EasyAsABCSolver();
        solver.generateDimacsFile(puzzleFile, dimacsFile);
        solver.solve(dimacsFile);
        if (solver.hasSolution()) {
            solver.printSolutionGrid();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Veuillez entrer deux noms de fichiers valides : puzzle_file et dimacs_file");
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
}
