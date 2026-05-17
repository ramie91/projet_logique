# SatElite (Nasseraldin Ramie)

## Description

SatElite est un projet que j'ai développé en Java.  
Mon programme lit un puzzle EasyAsABC, génère les clauses logiques au format DIMACS, puis résout le puzzle avec le solveur SAT Sat4J.

---

## Pré-requis

Avant de lancer mon projet, il faut installer :

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

Pour vérifier que tous les pré-requis sont installés et compiler mon projet :

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
A l'exécution de mon programme, je choisis un fichier puzzle et un fichier DIMACS. Les puzzles sont stockés dans `Puzzle/` et les fichiers DIMACS sont générés dans `Dimacs/`.<br>
Pour exécuter mon programme :
```bash
make run PUZZLE=<puzzle_x.txt> DIMACS=<dimacs_x.txt>
```

---

## Nettoyage

Pour supprimer les fichiers générés par mon projet :
```bash
make clean
```

---

## Structure du projet
```pgsql
.
├── src/               # Mes fichiers source Java
│   ├── Main.java
│   ├── EasyAsABCSolver.java
│   ├── SatSolver.java  # Adaptateur vers Sat4J
│   ├── Cnf.java
│   ├── Position.java
│   └── SatVariables.java
├── pom.xml            # Dépendance Sat4J
├── makefile           # Automatisation des commandes
├── Dimacs/            # Dossier dans lequel je génère les clauses logiques.
├── Puzzle/            # Dossier dans lequel je stocke les puzzles.
└── README.md          # Manuel d'utilisation.
```

---

## Commandes disponibles
| Commande     | Description                                              |
| ------------ | -------------------------------------------------------- |
| `make build` | Compile mon projet Java                                  |
| `make run`   | Lance mon programme                                      |
| `make test`  | Lance mes instances de test                              |
| `make clean` | Supprime les fichiers compilés et les fichiers DIMACS    |

---

## Authors
- @ramie.nasseraldin  (Ramie NASSERALDIN)
