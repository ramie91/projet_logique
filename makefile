# Variables

PYTHON = .venv/bin/python
PIP = .venv/bin/pip

# Vérifie si python3 est installé
check-python:
	@command -v python3 > /dev/null 2>&1 || ( \
		echo "Python3 n'est pas installé."; \
		echo "Pour installer Python3, se référer au README.md"; \
	)

# Vérifie si venv est installé
check-venv:
	@python3 -m venv --help > /dev/null 2>&1 || ( \
		echo "Le module venv n'est pas installé."; \
		echo "Pour installer le module venv, se référer au README.md"; \
	)

# Création du venv
venv: check-python check-venv
	python3 -m venv .venv
	$(PIP) install -r requirements.txt

# Lancement du projet
run:
	@if [ -z "$(PUZZLE)" ] || [ -z "$(DIMACS)" ]; then \
		echo "Usage : make run PUZZLE=<puzzle_x.txt> DIMACS=<dimacs_x.txt>"; \
		exit 1; \
	fi
	$(PYTHON) main.py $(PUZZLE) $(DIMACS)

# Nettoyage
clean:
	rm -rf .venv Dimacs/*