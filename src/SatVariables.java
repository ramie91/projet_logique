final class SatVariables {
    private SatVariables() {
    }

    static int variable(int row, int col, int letter, int gridSize) {
        return row * gridSize * gridSize + col * gridSize + letter + 1;
    }
}
