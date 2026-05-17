import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

class EasyAsABCSolver {
    private static final String RED = "\033[91m";
    private static final String GREEN = "\033[92m";
    private static final String BLUE = "\033[94m";
    private static final String ENDC = "\033[0m";

    private String variant;
    private String topClues;
    private String rightClues;
    private String bottomClues;
    private String leftClues;
    private int gridSize;
    private int totalVars;
    private List<int[]> clauses;
    private Set<Integer> solution;
    private Set<Integer> secondSolution;
    private List<Character> letters;
    private List<Character> initialGrid;

    boolean hasSolution() {
        return solution != null;
    }

    void generateDimacsFile(String inputFile, String outputFile) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(inputFile)).stream()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.size() < 5) {
            throw new IllegalArgumentException("Au moins 5 lignes sont attendues (variante + 4 indications), "
                    + lines.size() + " ligne(s) presente(s)");
        }

        variant = lines.get(0);
        topClues = lines.get(1);
        rightClues = lines.get(2);
        bottomClues = lines.get(3);
        leftClues = lines.get(4);
        gridSize = topClues.length();

        checkClueSize("du haut", topClues);
        checkClueSize("de droite", rightClues);
        checkClueSize("du bas", bottomClues);
        checkClueSize("de gauche", leftClues);

        initialGrid = new ArrayList<>();
        if (lines.size() > 5) {
            List<String> gridLines = lines.subList(5, Math.min(5 + gridSize, lines.size()));
            if (gridLines.size() < gridSize) {
                System.out.println("Attention: seulement " + gridLines.size()
                        + " lignes de grille renseignees sur " + gridSize);
            }

            for (String line : gridLines) {
                if (line.length() != gridSize) {
                    throw new IllegalArgumentException("Ligne '" + line + "' devrait avoir "
                            + gridSize + " cases, mais en a " + line.length());
                }
                for (char cell : line.toCharArray()) {
                    initialGrid.add(cell);
                }
            }

            while (initialGrid.size() < gridSize * gridSize) {
                initialGrid.add('.');
            }
        } else {
            for (int i = 0; i < gridSize * gridSize; i++) {
                initialGrid.add('.');
            }
        }

        totalVars = gridSize * gridSize * gridSize;
        printInitialGrid();
        printPuzzleData(inputFile, outputFile, lines.size());

        clauses = new ArrayList<>();

        switch (variant) {
            case "Basic" -> addBasicClauses();
            case "Easy1" -> addEasy1Clauses();
            case "Easy2" -> addEasy2Clauses();
            default -> throw new IllegalArgumentException(
                    "Variante '" + variant + "' incorrecte, choix possibles : Basic, Easy1, Easy2");
        }

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(outputFile))) {
            writer.write("p cnf " + totalVars + " " + clauses.size());
            writer.newLine();
            for (int[] clause : clauses) {
                for (int lit : clause) {
                    writer.write(lit + " ");
                }
                writer.write("0");
                writer.newLine();
            }
        }

        printDimacsData(outputFile);
    }

    private void checkClueSize(String name, String clues) {
        if (clues.length() != gridSize) {
            throw new IllegalArgumentException("Les indications " + name + " sont de taille "
                    + clues.length() + " : la grille est de taille " + gridSize);
        }
    }

    private void printInitialGrid() {
        System.out.println("Grille initiale lue (" + gridSize + "x" + gridSize + ") :");
        System.out.println("    " + spaced(topClues));
        System.out.println("   -" + "-".repeat(2 * gridSize));
        for (int i = 0; i < gridSize; i++) {
            int start = i * gridSize;
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < gridSize; j++) {
                if (j > 0) {
                    row.append(' ');
                }
                row.append(initialGrid.get(start + j));
            }
            System.out.println(leftClues.charAt(i) + " | " + row + " | " + rightClues.charAt(i));
        }
        System.out.println("   -" + "-".repeat(2 * gridSize));
        System.out.println("    " + spaced(bottomClues));
    }

    private void printPuzzleData(String inputFile, String outputFile, int nonEmptyLines) {
        System.out.println("\nDonnees du puzzle :");
        System.out.println("  Fichier puzzle       : " + inputFile);
        System.out.println("  Fichier DIMACS       : " + outputFile);
        System.out.println("  Lignes non vides     : " + nonEmptyLines);
        System.out.println("  Variante             : " + variant);
        System.out.println("  Taille grille        : " + gridSize + "x" + gridSize);
        System.out.println("  Nombre de cases      : " + (gridSize * gridSize));
        System.out.println("  Variables SAT prevues: " + totalVars);
        System.out.println("  Symboles autorises   : " + symbolsForVariant());
        System.out.println("  Indices haut         : " + topClues + " (" + spaced(topClues) + ")");
        System.out.println("  Indices droite       : " + rightClues + " (" + spaced(rightClues) + ")");
        System.out.println("  Indices bas          : " + bottomClues + " (" + spaced(bottomClues) + ")");
        System.out.println("  Indices gauche       : " + leftClues + " (" + spaced(leftClues) + ")");
        System.out.println("  Cases pre-remplies   : " + countCellsDifferentFrom('.'));
        System.out.println("  Cases inconnues      : " + countCellsEqualTo('.'));
        System.out.println("  Cases X imposees     : " + countCellsEqualTo('X'));
        System.out.println("  Grille brute         : " + rawInitialGrid());
        System.out.println("  Formule variable     : r * n * n + c * n + l + 1");
    }

    private String symbolsForVariant() {
        int letterCount = switch (variant) {
            case "Basic" -> gridSize;
            case "Easy1" -> gridSize - 1;
            case "Easy2" -> gridSize - 2;
            default -> 0;
        };

        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < letterCount; i++) {
            symbols.add(String.valueOf((char) ('A' + i)));
        }
        if (!"Basic".equals(variant)) {
            symbols.add("X");
        }
        return String.join(", ", symbols);
    }

    private int countCellsEqualTo(char expected) {
        int count = 0;
        for (char cell : initialGrid) {
            if (cell == expected) {
                count++;
            }
        }
        return count;
    }

    private int countCellsDifferentFrom(char ignored) {
        int count = 0;
        for (char cell : initialGrid) {
            if (cell != ignored) {
                count++;
            }
        }
        return count;
    }

    private String rawInitialGrid() {
        StringBuilder builder = new StringBuilder();
        for (char cell : initialGrid) {
            builder.append(cell);
        }
        return builder.toString();
    }

    private void printDimacsData(String outputFile) {
        System.out.println("\nDonnees DIMACS :");
        System.out.println("  Fichier genere       : " + outputFile);
        System.out.println("  En-tete              : p cnf " + totalVars + " " + clauses.size());
        System.out.println("  Variables            : " + totalVars);
        System.out.println("  Clauses              : " + clauses.size());
        System.out.println("  Literaux             : " + countLiterals(clauses));
        System.out.println("  Longueur min clause  : " + minClauseLength(clauses));
        System.out.println("  Longueur max clause  : " + maxClauseLength(clauses));
        System.out.println("  Clauses par longueur : " + clauseLengthDistribution(clauses));
        System.out.println("  Detail exhaustif     : disponible dans " + outputFile);
    }

    private int countLiterals(List<int[]> clausesToInspect) {
        int total = 0;
        for (int[] clause : clausesToInspect) {
            total += clause.length;
        }
        return total;
    }

    private int minClauseLength(List<int[]> clausesToInspect) {
        int min = Integer.MAX_VALUE;
        for (int[] clause : clausesToInspect) {
            min = Math.min(min, clause.length);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    private int maxClauseLength(List<int[]> clausesToInspect) {
        int max = 0;
        for (int[] clause : clausesToInspect) {
            max = Math.max(max, clause.length);
        }
        return max;
    }

    private Map<Integer, Integer> clauseLengthDistribution(List<int[]> clausesToInspect) {
        Map<Integer, Integer> distribution = new TreeMap<>();
        for (int[] clause : clausesToInspect) {
            distribution.merge(clause.length, 1, Integer::sum);
        }
        return distribution;
    }

    private String spaced(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(text.charAt(i));
        }
        return builder.toString();
    }

    private Map<Character, Integer> letterToIdx() {
        Map<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < letters.size(); i++) {
            map.put(letters.get(i), i);
        }
        return map;
    }

    private void setLetters(int count) {
        letters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            letters.add((char) ('A' + i));
        }
    }

    private void addClause(int... clause) {
        clauses.add(clause);
    }

    private void addBasicClauses() {
        setLetters(gridSize);
        Map<Character, Integer> letterToIdx = letterToIdx();

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                int[] clause = new int[gridSize];
                for (int l = 0; l < gridSize; l++) {
                    clause[l] = SatVariables.variable(r, c, l, gridSize);
                }
                clauses.add(clause);

                for (int l1 = 0; l1 < gridSize; l1++) {
                    for (int l2 = l1 + 1; l2 < gridSize; l2++) {
                        addClause(-SatVariables.variable(r, c, l1, gridSize),
                                -SatVariables.variable(r, c, l2, gridSize));
                    }
                }
            }
        }

        for (int r = 0; r < gridSize; r++) {
            for (int l = 0; l < gridSize; l++) {
                int[] clause = new int[gridSize];
                for (int c = 0; c < gridSize; c++) {
                    clause[c] = SatVariables.variable(r, c, l, gridSize);
                }
                clauses.add(clause);

                for (int c1 = 0; c1 < gridSize; c1++) {
                    for (int c2 = c1 + 1; c2 < gridSize; c2++) {
                        addClause(-SatVariables.variable(r, c1, l, gridSize),
                                -SatVariables.variable(r, c2, l, gridSize));
                    }
                }
            }
        }

        for (int c = 0; c < gridSize; c++) {
            for (int l = 0; l < gridSize; l++) {
                int[] clause = new int[gridSize];
                for (int r = 0; r < gridSize; r++) {
                    clause[r] = SatVariables.variable(r, c, l, gridSize);
                }
                clauses.add(clause);

                for (int r1 = 0; r1 < gridSize; r1++) {
                    for (int r2 = r1 + 1; r2 < gridSize; r2++) {
                        addClause(-SatVariables.variable(r1, c, l, gridSize),
                                -SatVariables.variable(r2, c, l, gridSize));
                    }
                }
            }
        }

        for (int col = 0; col < gridSize; col++) {
            char clue = topClues.charAt(col);
            if (clue != '.') {
                addClause(SatVariables.variable(0, col, requireLetter(letterToIdx, clue, 0, col), gridSize));
            }
        }

        for (int col = 0; col < gridSize; col++) {
            char clue = bottomClues.charAt(col);
            if (clue != '.') {
                addClause(SatVariables.variable(gridSize - 1, col,
                        requireLetter(letterToIdx, clue, gridSize - 1, col), gridSize));
            }
        }

        for (int row = 0; row < gridSize; row++) {
            char clue = leftClues.charAt(row);
            if (clue != '.') {
                addClause(SatVariables.variable(row, 0, requireLetter(letterToIdx, clue, row, 0), gridSize));
            }
        }

        for (int row = 0; row < gridSize; row++) {
            char clue = rightClues.charAt(row);
            if (clue != '.') {
                addClause(SatVariables.variable(row, gridSize - 1,
                        requireLetter(letterToIdx, clue, row, gridSize - 1), gridSize));
            }
        }

        for (int idx = 0; idx < initialGrid.size(); idx++) {
            char cell = initialGrid.get(idx);
            if (cell == '.') {
                continue;
            }
            int r = idx / gridSize;
            int c = idx % gridSize;
            int l = requireLetter(letterToIdx, cell, r, c);
            addClause(SatVariables.variable(r, c, l, gridSize));
            for (int other = 0; other < gridSize; other++) {
                if (other != l) {
                    addClause(-SatVariables.variable(r, c, other, gridSize));
                }
            }
        }
    }

    private void addEasy1Clauses() {
        setLetters(gridSize - 1);
        int empty = gridSize - 1;
        Map<Character, Integer> letterToIdx = letterToIdx();

        addSymbolClauses(range(0, gridSize));
        addEachLetterOncePerRow(gridSize - 1);
        addEachLetterOncePerColumn(gridSize - 1);
        addExactlyOneEmptyPerRow(empty);
        addExactlyOneEmptyPerColumn(empty);
        addAllClueClauses(letterToIdx, empty);
        addInitialGridClauses(letterToIdx, empty, range(0, gridSize), gridSize - 1);
    }

    private void addEasy2Clauses() {
        setLetters(gridSize - 2);
        int empty = gridSize - 1;
        Map<Character, Integer> letterToIdx = letterToIdx();
        int[] validSymbols = new int[gridSize - 1];
        for (int i = 0; i < gridSize - 2; i++) {
            validSymbols[i] = i;
        }
        validSymbols[gridSize - 2] = empty;

        addSymbolClauses(validSymbols);
        addEachLetterOncePerRow(gridSize - 2);
        addEachLetterOncePerColumn(gridSize - 2);
        addExactlyTwoEmptiesPerRow(empty);
        addExactlyTwoEmptiesPerColumn(empty);
        addAllClueClauses(letterToIdx, empty);
        addInitialGridClauses(letterToIdx, empty, validSymbols, gridSize - 2);
    }

    private int[] range(int start, int endExclusive) {
        int[] values = new int[endExclusive - start];
        for (int i = 0; i < values.length; i++) {
            values[i] = start + i;
        }
        return values;
    }

    private void addSymbolClauses(int[] validSymbols) {
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                int[] clause = new int[validSymbols.length];
                for (int i = 0; i < validSymbols.length; i++) {
                    clause[i] = SatVariables.variable(r, c, validSymbols[i], gridSize);
                }
                clauses.add(clause);

                for (int i = 0; i < validSymbols.length; i++) {
                    for (int j = i + 1; j < validSymbols.length; j++) {
                        addClause(-SatVariables.variable(r, c, validSymbols[i], gridSize),
                                -SatVariables.variable(r, c, validSymbols[j], gridSize));
                    }
                }
            }
        }
    }

    private void addEachLetterOncePerRow(int letterCount) {
        for (int r = 0; r < gridSize; r++) {
            for (int l = 0; l < letterCount; l++) {
                int[] clause = new int[gridSize];
                for (int c = 0; c < gridSize; c++) {
                    clause[c] = SatVariables.variable(r, c, l, gridSize);
                }
                clauses.add(clause);

                for (int c1 = 0; c1 < gridSize; c1++) {
                    for (int c2 = c1 + 1; c2 < gridSize; c2++) {
                        addClause(-SatVariables.variable(r, c1, l, gridSize),
                                -SatVariables.variable(r, c2, l, gridSize));
                    }
                }
            }
        }
    }

    private void addEachLetterOncePerColumn(int letterCount) {
        for (int c = 0; c < gridSize; c++) {
            for (int l = 0; l < letterCount; l++) {
                int[] clause = new int[gridSize];
                for (int r = 0; r < gridSize; r++) {
                    clause[r] = SatVariables.variable(r, c, l, gridSize);
                }
                clauses.add(clause);

                for (int r1 = 0; r1 < gridSize; r1++) {
                    for (int r2 = r1 + 1; r2 < gridSize; r2++) {
                        addClause(-SatVariables.variable(r1, c, l, gridSize),
                                -SatVariables.variable(r2, c, l, gridSize));
                    }
                }
            }
        }
    }

    private void addExactlyOneEmptyPerRow(int empty) {
        for (int r = 0; r < gridSize; r++) {
            int[] clause = new int[gridSize];
            for (int c = 0; c < gridSize; c++) {
                clause[c] = SatVariables.variable(r, c, empty, gridSize);
            }
            clauses.add(clause);

            for (int c1 = 0; c1 < gridSize; c1++) {
                for (int c2 = c1 + 1; c2 < gridSize; c2++) {
                    addClause(-SatVariables.variable(r, c1, empty, gridSize),
                            -SatVariables.variable(r, c2, empty, gridSize));
                }
            }
        }
    }

    private void addExactlyOneEmptyPerColumn(int empty) {
        for (int c = 0; c < gridSize; c++) {
            int[] clause = new int[gridSize];
            for (int r = 0; r < gridSize; r++) {
                clause[r] = SatVariables.variable(r, c, empty, gridSize);
            }
            clauses.add(clause);

            for (int r1 = 0; r1 < gridSize; r1++) {
                for (int r2 = r1 + 1; r2 < gridSize; r2++) {
                    addClause(-SatVariables.variable(r1, c, empty, gridSize),
                            -SatVariables.variable(r2, c, empty, gridSize));
                }
            }
        }
    }

    private void addExactlyTwoEmptiesPerRow(int empty) {
        for (int r = 0; r < gridSize; r++) {
            for (int excluded = 0; excluded < gridSize; excluded++) {
                int[] clause = new int[gridSize - 1];
                int idx = 0;
                for (int c = 0; c < gridSize; c++) {
                    if (c != excluded) {
                        clause[idx++] = SatVariables.variable(r, c, empty, gridSize);
                    }
                }
                clauses.add(clause);
            }

            for (int c1 = 0; c1 < gridSize; c1++) {
                for (int c2 = c1 + 1; c2 < gridSize; c2++) {
                    for (int c3 = c2 + 1; c3 < gridSize; c3++) {
                        addClause(-SatVariables.variable(r, c1, empty, gridSize),
                                -SatVariables.variable(r, c2, empty, gridSize),
                                -SatVariables.variable(r, c3, empty, gridSize));
                    }
                }
            }
        }
    }

    private void addExactlyTwoEmptiesPerColumn(int empty) {
        for (int c = 0; c < gridSize; c++) {
            for (int excluded = 0; excluded < gridSize; excluded++) {
                int[] clause = new int[gridSize - 1];
                int idx = 0;
                for (int r = 0; r < gridSize; r++) {
                    if (r != excluded) {
                        clause[idx++] = SatVariables.variable(r, c, empty, gridSize);
                    }
                }
                clauses.add(clause);
            }

            for (int r1 = 0; r1 < gridSize; r1++) {
                for (int r2 = r1 + 1; r2 < gridSize; r2++) {
                    for (int r3 = r2 + 1; r3 < gridSize; r3++) {
                        addClause(-SatVariables.variable(r1, c, empty, gridSize),
                                -SatVariables.variable(r2, c, empty, gridSize),
                                -SatVariables.variable(r3, c, empty, gridSize));
                    }
                }
            }
        }
    }

    private void addAllClueClauses(Map<Character, Integer> letterToIdx, int empty) {
        for (int col = 0; col < gridSize; col++) {
            List<Position> positions = new ArrayList<>();
            for (int row = 0; row < gridSize; row++) {
                positions.add(new Position(row, col));
            }
            addClueClauses(positions, topClues.charAt(col), letterToIdx, empty);
        }

        for (int col = 0; col < gridSize; col++) {
            List<Position> positions = new ArrayList<>();
            for (int row = gridSize - 1; row >= 0; row--) {
                positions.add(new Position(row, col));
            }
            addClueClauses(positions, bottomClues.charAt(col), letterToIdx, empty);
        }

        for (int row = 0; row < gridSize; row++) {
            List<Position> positions = new ArrayList<>();
            for (int col = 0; col < gridSize; col++) {
                positions.add(new Position(row, col));
            }
            addClueClauses(positions, leftClues.charAt(row), letterToIdx, empty);
        }

        for (int row = 0; row < gridSize; row++) {
            List<Position> positions = new ArrayList<>();
            for (int col = gridSize - 1; col >= 0; col--) {
                positions.add(new Position(row, col));
            }
            addClueClauses(positions, rightClues.charAt(row), letterToIdx, empty);
        }
    }

    private void addClueClauses(List<Position> positions, char clue, Map<Character, Integer> letterToIdx, int empty) {
        if (clue == '.') {
            return;
        }

        int l = requireLetter(letterToIdx, clue, -1, -1);
        for (int k = 0; k < positions.size(); k++) {
            Position current = positions.get(k);
            for (int i = 0; i < k; i++) {
                Position before = positions.get(i);
                addClause(-SatVariables.variable(current.row, current.col, l, gridSize),
                        SatVariables.variable(before.row, before.col, empty, gridSize));
            }
        }

        int[] atLeastOne = new int[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            Position pos = positions.get(i);
            atLeastOne[i] = SatVariables.variable(pos.row, pos.col, l, gridSize);
        }
        clauses.add(atLeastOne);
    }

    private void addInitialGridClauses(Map<Character, Integer> letterToIdx, int empty, int[] validSymbols,
            int letterCount) {
        for (int idx = 0; idx < initialGrid.size(); idx++) {
            char cell = initialGrid.get(idx);
            if (cell == '.') {
                continue;
            }

            int r = idx / gridSize;
            int c = idx % gridSize;
            if (cell == 'X') {
                addClause(SatVariables.variable(r, c, empty, gridSize));
                for (int l = 0; l < letterCount; l++) {
                    addClause(-SatVariables.variable(r, c, l, gridSize));
                }
                continue;
            }

            int l = requireLetter(letterToIdx, cell, r, c);
            addClause(SatVariables.variable(r, c, l, gridSize));
            for (int other : validSymbols) {
                if (other != l) {
                    addClause(-SatVariables.variable(r, c, other, gridSize));
                }
            }
        }
    }

    private int requireLetter(Map<Character, Integer> letterToIdx, char letter, int row, int col) {
        Integer idx = letterToIdx.get(letter);
        if (idx == null) {
            String position = row >= 0 ? " a la position (" + row + "," + col + ")" : "";
            throw new IllegalArgumentException("Lettre '" + letter + "' invalide" + position);
        }
        return idx;
    }

    void solve(String dimacsFile) throws IOException {
        Cnf cnf = Cnf.read(dimacsFile);
        System.out.println("\nDonnees CNF chargees :");
        System.out.println("  Fichier lu           : " + dimacsFile);
        System.out.println("  Variables            : " + cnf.totalVars);
        System.out.println("  Clauses              : " + cnf.clauses.size());
        System.out.println("  Literaux             : " + countLiterals(cnf.clauses));
        System.out.println("  Clauses par longueur : " + clauseLengthDistribution(cnf.clauses));

        SatSolver solver = new SatSolver(cnf.totalVars, cnf.clauses);

        int[] model = solver.solve();
        if (model != null) {
            System.out.println("\nPuzzle satisfaisable.");
            solution = positiveLiterals(model);
            printModelData("Premiere solution", model, solution);

            int[] blockingClause = solution.stream().mapToInt(lit -> -lit).toArray();
            solver.addClause(blockingClause);
            System.out.println("\nRecherche deuxieme solution :");
            System.out.println("  Clause de blocage    : " + blockingClause.length + " litteraux");

            int[] secondModel = solver.solve();
            if (secondModel != null) {
                System.out.println("\nDeuxieme solution trouvee.");
                secondSolution = positiveLiterals(secondModel);
                printModelData("Deuxieme solution", secondModel, secondSolution);
            } else {
                System.out.println("\nAucune deuxieme solution trouvee.");
            }
        } else {
            System.out.println("\nPuzzle insatisfaisable.");
            solution = null;
        }
    }

    private void printModelData(String title, int[] model, Set<Integer> positives) {
        int trueCount = 0;
        int falseCount = 0;
        int unassignedCount = 0;
        for (int var = 1; var < model.length; var++) {
            if (model[var] == 1) {
                trueCount++;
            } else if (model[var] == -1) {
                falseCount++;
            } else {
                unassignedCount++;
            }
        }

        System.out.println("\nDonnees modele - " + title + " :");
        System.out.println("  Variables vraies     : " + trueCount);
        System.out.println("  Variables fausses    : " + falseCount);
        System.out.println("  Variables non fixees : " + unassignedCount);
        System.out.println("  Literaux positifs    : " + positives.size());
        System.out.println("  Cases affichees      : " + (gridSize * gridSize));
    }

    private Set<Integer> positiveLiterals(int[] model) {
        Set<Integer> positives = new HashSet<>();
        for (int var = 1; var < model.length; var++) {
            if (model[var] == 1) {
                positives.add(var);
            }
        }
        return positives;
    }

    void printSolutionGrid() {
        if (solution == null || solution.isEmpty()) {
            System.out.println("Aucune solution a afficher.");
            return;
        }

        printGrid("Grille solution :", solution);
        if (secondSolution != null && !secondSolution.isEmpty()) {
            printGrid("Seconde solution :", secondSolution);
        }
    }

    private void printGrid(String title, Set<Integer> model) {
        String[][] grid = new String[gridSize][gridSize];
        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                grid[r][c] = RED + "X" + ENDC;
            }
        }

        for (int r = 0; r < gridSize; r++) {
            for (int c = 0; c < gridSize; c++) {
                for (int l = 0; l < letters.size(); l++) {
                    if (model.contains(SatVariables.variable(r, c, l, gridSize))) {
                        grid[r][c] = String.valueOf(letters.get(l));
                        break;
                    }
                }
            }
        }

        System.out.println();
        System.out.println(title + "\n");
        System.out.println("    " + spaced(topClues));
        System.out.println("   -" + "-".repeat(2 * gridSize));
        for (int r = 0; r < gridSize; r++) {
            List<String> rowDisplay = new ArrayList<>();
            for (int c = 0; c < gridSize; c++) {
                String cell = grid[r][c];
                int idx = r * gridSize + c;
                if (idx < initialGrid.size() && initialGrid.get(idx) != '.') {
                    rowDisplay.add(BLUE + cell + ENDC);
                } else {
                    rowDisplay.add(GREEN + cell + ENDC);
                }
            }
            System.out.println(leftClues.charAt(r) + " | " + String.join(" ", rowDisplay)
                    + " | " + rightClues.charAt(r));
        }
        System.out.println("   -" + "-".repeat(2 * gridSize));
        System.out.println("    " + spaced(bottomClues));
    }
}
