import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class PuzzleGenerator {
    private static final List<GeneratedPuzzle> DEFAULT_PUZZLES = List.of(
            new GeneratedPuzzle("Basic", 4, "generated_basic_4.txt"),
            new GeneratedPuzzle("Basic", 6, "generated_basic_6.txt"),
            new GeneratedPuzzle("Easy1", 5, "generated_easy1_5.txt"),
            new GeneratedPuzzle("Easy2", 6, "generated_easy2_6.txt"));

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                generateDefaults();
            } else if (args.length == 3) {
                generate(args[0], Integer.parseInt(args[1]), Path.of("Puzzle", args[2]));
            } else {
                System.out.println("Usage : java PuzzleGenerator [Basic|Easy1|Easy2 taille fichier_sortie]");
                System.exit(1);
            }
        } catch (IOException | IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void generateDefaults() throws IOException {
        for (GeneratedPuzzle puzzle : DEFAULT_PUZZLES) {
            generate(puzzle.variant, puzzle.size, Path.of("Puzzle", puzzle.outputFile));
        }
    }

    private static void generate(String variant, int size, Path outputFile) throws IOException {
        validate(variant, size);
        Files.createDirectories(outputFile.getParent());

        char[][] grid = createSolvedGrid(variant, size);
        String top = cluesFromTop(grid);
        String right = cluesFromRight(grid);
        String bottom = cluesFromBottom(grid);
        String left = cluesFromLeft(grid);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write(variant);
            writer.newLine();
            writer.write(top);
            writer.newLine();
            writer.write(right);
            writer.newLine();
            writer.write(bottom);
            writer.newLine();
            writer.write(left);
            writer.newLine();
        }

        System.out.println("Instance generee : " + outputFile + " (" + variant + ", " + size + "x" + size + ")");
    }

    private static void validate(String variant, int size) {
        if (!variant.equals("Basic") && !variant.equals("Easy1") && !variant.equals("Easy2")) {
            throw new IllegalArgumentException("Variante incorrecte : " + variant);
        }
        if (variant.equals("Basic") && size < 1) {
            throw new IllegalArgumentException("Basic demande une taille superieure ou egale a 1");
        }
        if (variant.equals("Easy1") && size < 2) {
            throw new IllegalArgumentException("Easy1 demande une taille superieure ou egale a 2");
        }
        if (variant.equals("Easy2") && size < 3) {
            throw new IllegalArgumentException("Easy2 demande une taille superieure ou egale a 3");
        }
        if (size > 26) {
            throw new IllegalArgumentException("La taille maximale geree est 26, pour rester dans les lettres A-Z");
        }
    }

    private static char[][] createSolvedGrid(String variant, int size) {
        char[][] grid = new char[size][size];
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int value = (row + col) % size;
                grid[row][col] = symbolFor(variant, size, value);
            }
        }
        return grid;
    }

    private static char symbolFor(String variant, int size, int value) {
        if (variant.equals("Basic")) {
            return (char) ('A' + value);
        }
        if (variant.equals("Easy1")) {
            return value == size - 1 ? 'X' : (char) ('A' + value);
        }
        return value >= size - 2 ? 'X' : (char) ('A' + value);
    }

    private static String cluesFromTop(char[][] grid) {
        StringBuilder clues = new StringBuilder();
        for (int col = 0; col < grid.length; col++) {
            clues.append(firstVisible(grid, 0, col, 1, 0));
        }
        return clues.toString();
    }

    private static String cluesFromBottom(char[][] grid) {
        StringBuilder clues = new StringBuilder();
        for (int col = 0; col < grid.length; col++) {
            clues.append(firstVisible(grid, grid.length - 1, col, -1, 0));
        }
        return clues.toString();
    }

    private static String cluesFromLeft(char[][] grid) {
        StringBuilder clues = new StringBuilder();
        for (int row = 0; row < grid.length; row++) {
            clues.append(firstVisible(grid, row, 0, 0, 1));
        }
        return clues.toString();
    }

    private static String cluesFromRight(char[][] grid) {
        StringBuilder clues = new StringBuilder();
        for (int row = 0; row < grid.length; row++) {
            clues.append(firstVisible(grid, row, grid.length - 1, 0, -1));
        }
        return clues.toString();
    }

    private static char firstVisible(char[][] grid, int row, int col, int rowStep, int colStep) {
        int r = row;
        int c = col;
        while (r >= 0 && r < grid.length && c >= 0 && c < grid.length) {
            if (grid[r][c] != 'X') {
                return grid[r][c];
            }
            r += rowStep;
            c += colStep;
        }
        return '.';
    }

    private record GeneratedPuzzle(String variant, int size, String outputFile) {
    }
}
