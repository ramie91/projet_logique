# Variables

JAVAC = javac
JAVA = java
MVN = mvn
SRC_DIR = src
BUILD_DIR = build/classes
DIMACS_DIR = Dimacs
LIB_DIR = lib
MAVEN_REPO = .m2/repository
SAT4J_CP = $(LIB_DIR)/*

# Vérifie si Java est installé
check-java:
	@command -v $(JAVA) > /dev/null 2>&1 || ( \
		echo "Java n'est pas installé."; \
		echo "Pour installer Java, se référer au README.md"; \
		exit 1; \
	)
	@command -v $(JAVAC) > /dev/null 2>&1 || ( \
		echo "javac n'est pas installé."; \
		echo "Pour installer le JDK, se référer au README.md"; \
		exit 1; \
	)

# Vérifie si Maven est installé
check-maven:
	@command -v $(MVN) > /dev/null 2>&1 || ( \
		echo "Maven n'est pas installé."; \
		echo "Pour installer Maven, se référer au README.md"; \
		exit 1; \
	)

# Téléchargement des dépendances Java
deps: check-maven
	mkdir -p $(LIB_DIR)
	$(MVN) -q -Dmaven.repo.local=$(MAVEN_REPO) org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies -DoutputDirectory=$(LIB_DIR)

# Compilation du projet
build: check-java deps
	mkdir -p $(DIMACS_DIR)
	mkdir -p $(BUILD_DIR)
	$(JAVAC) -cp "$(SAT4J_CP)" -d $(BUILD_DIR) $(SRC_DIR)/*.java

# Lancement du projet
run: build
	@if [ -z "$(PUZZLE)" ] || [ -z "$(DIMACS)" ]; then \
		echo "Usage : make run PUZZLE=<puzzle_x.txt> DIMACS=<dimacs_x.txt>"; \
		exit 1; \
	fi
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main $(PUZZLE) $(DIMACS)

# Tests sur les instances fournies
test: build
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main puzzle_test_seconde_solution.txt test_seconde_solution.cnf
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main puzzle.txt test_basic.cnf
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main puzzle_easy1.txt test_easy1.cnf
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main puzzle_easy2.txt test_easy2.cnf

# Génération automatique d'instances
generate: build
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" PuzzleGenerator

# Tests sur les instances générées automatiquement
test-generated: generate
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main generated_basic_4.txt generated_basic_4.cnf
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main generated_basic_6.txt generated_basic_6.cnf
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main generated_easy1_5.txt generated_easy1_5.cnf
	$(JAVA) -cp "$(BUILD_DIR):$(SAT4J_CP)" Main generated_easy2_6.txt generated_easy2_6.cnf

# Nettoyage
clean:
	rm -rf build *.class Dimacs/* Puzzle/generated_*.txt

# Nettoyage complet des fichiers générés, dépendances Maven comprises
distclean: clean
	rm -rf $(LIB_DIR) .m2
