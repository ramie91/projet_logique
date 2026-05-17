# Rapport du projet SatElite

Ramie NASSERALDIN

## Nos choix

Nous avons choisi d'utiliser le langage de programmation Java afin de respecter le sujet, qui demande l'utilisation de Sat4J pour une implementation en Java. Java permet egalement d'organiser le projet de maniere claire avec plusieurs classes, chacune ayant un role precis dans la resolution du puzzle.

Nous avons utilise le solveur SAT Sat4J, ajoute au projet avec Maven. Ce choix permet de s'appuyer sur un solveur SAT reconnu, tout en gardant un programme completement integre au code Java. Le fichier `pom.xml` contient la dependance necessaire a Sat4J, et le `makefile` se charge de recuperer les dependances, de compiler le projet et de lancer les tests.

Le projet est structure autour d'une classe principale `EasyAsABCSolver`, qui centralise les
fonctionnalites necessaires a la resolution du puzzle. Cette classe gere la lecture du fichier
d'entree, la recuperation de la variante, des indices et de la grille initiale, puis la generation des clauses logiques correspondant au puzzle.

La structure du projet suit plusieurs etapes successives. Dans un premier temps, le programme lit et analyse le fichier d'entree. Celui-ci contient la variante du puzzle, les indices situes en haut, a droite, en bas et a gauche, ainsi qu'une grille initiale optionnelle.

Dans un second temps, le puzzle est transforme en probleme SAT. Chaque case de la grille est representee par des variables booleennes indiquant quelle lettre ou quel symbole se trouve dans la case. Les contraintes du puzzle sont ensuite converties en clauses CNF. Le programme prend en charge les trois variantes demandees : `Basic`, `Easy1` et `Easy2`.

La troisieme etape correspond a la generation d'un fichier DIMACS. Ce format standard permet de representer le probleme SAT de maniere lisible par un solveur. Le fichier DIMACS est genere dans le dossier `Dimacs`.

La quatrieme etape est celle de la resolution. Le programme recharge le fichier DIMACS, ajoute les clauses dans Sat4J, puis demande au solveur si le probleme est satisfaisable. Si une solution existe, le modele SAT retourne par Sat4J est recupere.

Enfin, la derniere etape concerne l'affichage des resultats. La solution est reconstruite sous forme de grille. Les valeurs deja presentes dans la grille initiale sont affichees differemment afin de les distinguer des valeurs trouvees par le solveur. Le programme affiche aussi des informations sur le puzzle, le fichier DIMACS, le nombre de variables, le nombre de clauses et le solveur utilise.

Cette organisation en etapes permet de separer clairement la generation du probleme SAT, son export au format DIMACS, sa resolution par Sat4J et l'affichage de la solution.

## Nos resultats

Le programme developpe permet de resoudre les differentes variantes du puzzle Easy as ABC en les
transformant en problemes SAT. Les resultats obtenus montrent que la modelisation est correcte pour les trois variantes prises en charge : `Basic`, `Easy1` et `Easy2`.

Le solveur Sat4J trouve une solution pour les instances fournies dans le projet. Les fichiers de test utilises sont :

- `puzzle.txt`
- `puzzle_easy1.txt`
- `puzzle_easy2.txt`
- `puzzle_test_seconde_solution.txt`

La commande suivante permet de lancer automatiquement tous les tests :

```bash

make test

```

Pour chaque instance, le programme genere d'abord un fichier DIMACS, puis appelle Sat4J pour resoudre le probleme. Lorsque le probleme est satisfaisable, la grille solution est affichee dans le terminal.

Le programme permet egalement de detecter l'existence d'une deuxieme solution. Pour cela, il ajoute une clause de blocage qui nie la premiere solution trouvee, puis relance Sat4J sur la nouvelle formule. Le fichier `puzzle_test_seconde_solution.txt` permet de verifier ce comportement, car le programme y trouve bien deux solutions distinctes.

Les tests montrent aussi que le programme gere correctement les cases vides des variantes `Easy1` et `Easy2`. Dans `Easy1`, chaque ligne et chaque colonne contiennent exactement une case vide. Dans `Easy2`, chaque ligne et chaque colonne contiennent exactement deux cases vides.

Ainsi, le projet repond aux objectifs principaux du sujet : lecture d'une instance, generation d'un fichier DIMACS, resolution avec Sat4J, affichage d'une solution et recherche eventuelle d'une seconde solution.
