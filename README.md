# SatFisa (Matéo et Ines)

## Description

SatFisa est un projet développé en Python.  
Le projet utilise un environnement virtuel Python (`venv`) afin d’isoler les dépendances et garantir un fonctionnement propre du programme.

---

## Pré-requis

Avant de lancer le projet, il est nécessaire d’installer :

- Python 3
- Le module `venv`

Sous Ubuntu / Debian / WSL, utilisez la commande suivante :

```bash
sudo apt update
sudo apt install python3 python3-venv
```

---

## Installation et vérification

Pour vérifier que tous les pré-requis sont installés, créer l’environnement virtuel et installer les dépendances du projet :

```bash
make venv
```

Cette commande :

- vérifie la présence de `python3`
- vérifie la présence du module `venv`
- crée le dossier `.venv`
- installe les dépendances présentes dans `requirements.txt`

---

## Lancement du projet
A l'exécution du programme, il faut choisir un fichier puzzle et un fichier dimacs qui sont stockés respectivement dans les dossiers Puzzle et Dimacs.<br>
Pour exécuter le programme :
```bash
make run PUZZLE=<puzzle_x.txt> DIMACS=<dimacs_x.txt>
```

---

## Nettoyage

Pour supprimer l’environnement virtuel :
```bash
make clean
```

---

## Structure du projet
```pgsql
.
├── .venv/             # Environnement virtuel Python
├── requirements.txt   # Dépendances Python
├── Makefile           # Automatisation des commandes
├── main.py            # Point d’entrée du projet
├── Dimacs/            # Dossier qui contient les clauses logiques du puzzle.
├── Puzzle/            # Dossier qui contient les puzzles.
└── README.md          # Manuel d'utilisation.
```

---

## Commandes disponibles
| Commande     | Description                                              |
| ------------ | -------------------------------------------------------- |
| `make venv`  | Crée l’environnement virtuel et installe les dépendances |
| `make run`   | Lance le projet                                          |
| `make clean` | Supprime l’environnement virtuel                         |

---

## Authors
- @ines.bendar | Ines BENDAR
- @mateo.siuda | Matéo SIUDA