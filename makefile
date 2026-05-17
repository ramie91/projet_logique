# Variables

JAVAC = javac
JAVA = java
SRC_DIR = src
BUILD_DIR = build/classes

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

# Compilation du projet
build: check-java
	mkdir -p $(BUILD_DIR)
	$(JAVAC) -d $(BUILD_DIR) $(SRC_DIR)/*.java

# Lancement du projet
run: build
	@if [ -z "$(PUZZLE)" ] || [ -z "$(DIMACS)" ]; then \
		echo "Usage : make run PUZZLE=<puzzle_x.txt> DIMACS=<dimacs_x.txt>"; \
		exit 1; \
	fi
	$(JAVA) -cp $(BUILD_DIR) Main $(PUZZLE) $(DIMACS)

# Nettoyage
clean:
	rm -rf build Dimacs/*
