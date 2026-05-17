# SatElite (Nasseraldin Ramie)

## Description

SatElite est un projet développé en Java.  
Le programme lit un puzzle EasyAsABC, génère les clauses logiques au format DIMACS, puis résout le puzzle avec un solveur SAT intégré.

---

## Pré-requis

Avant de lancer le projet, il est nécessaire d’installer :

- Java
- Le JDK (`javac`)
- Maven

Sous Ubuntu / Debian / WSL, utilisez la commande suivante :

```bash
sudo apt update
sudo apt install default-jdk maven
```

---

## Compilation et vérification

Pour vérifier que tous les pré-requis sont installés et compiler le projet :

```bash
make build
```

Cette commande :

- vérifie la présence de `java`
- vérifie la présence de `javac`
- vérifie la présence de `maven`
- télécharge la dépendance Sat4J dans `lib/`
- compile `Main.java`

---

## Lancement du projet
A l'exécution du programme, il faut choisir un fichier puzzle et un fichier dimacs qui sont stockés respectivement dans les dossiers Puzzle et Dimacs.<br>
Pour exécuter le programme :
```bash
make run PUZZLE=<puzzle_x.txt> DIMACS=<dimacs_x.txt>
```

---

## Nettoyage

Pour supprimer les fichiers générés :
```bash
make clean
```

---

## Structure du projet
```pgsql
.
├── src/               # Fichiers source Java
│   ├── Main.java
│   ├── EasyAsABCSolver.java
│   ├── SatSolver.java  # Adaptateur vers Sat4J
│   ├── Cnf.java
│   ├── Position.java
│   └── SatVariables.java
├── pom.xml            # Dépendance Sat4J
├── makefile           # Automatisation des commandes
├── Dimacs/            # Dossier qui contient les clauses logiques du puzzle.
├── Puzzle/            # Dossier qui contient les puzzles.
└── README.md          # Manuel d'utilisation.
```

---

## Commandes disponibles
| Commande     | Description                                              |
| ------------ | -------------------------------------------------------- |
| `make build` | Compile le projet Java                                   |
| `make run`   | Lance le projet                                          |
| `make test`  | Lance les instances de test fournies                     |
| `make clean` | Supprime les fichiers compilés et les fichiers DIMACS    |

---

## Authors
- @ramie.nasseraldin  (Ramie NASSERALDIN)
