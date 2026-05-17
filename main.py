from pysat.solvers import Glucose3
from pysat.formula import CNF
import sys



# Classe pour les couleurs d'affichage dans le terminal, pour plus de lisibilité du puzzle
# Les croix (cases vides pour les variantes easy1 et easy2) sont ainsi affichées en rouge, et les lettres déjà présentes dans la grille de départ en bleu et les lettres de la solution en vert
class bcolors:
    RED = '\33[91m'
    GREEN = '\33[92m'
    BLUE = '\33[94m'
    ENDC = '\033[0m'



# Fonction d'aide permettant d'obtenir la variable logique associée à une case à la ligne r, colonne c et correspondant à la lettre l
def variable(r, c, l, grid_size):
    return r * grid_size * grid_size + c * grid_size + l + 1


class EasyAsABC_Solver:
    """
    Instance contenant les variables et fonctions permettant de résoudre le puzzle EasyAsABC.
    """
    
    def __init__(self):
        self.solver = None                  # Solver du module python-sat
        self.variant = None                 # Variante du puzzle
        self.top_clues = None               # Indications en haut du puzzle
        self.right_clues = None             # Indications à droite du puzzle
        self.bottom_clues = None            # Indications en bas du puzzle
        self.left_clues = None              # Indications à gauche du puzzle
        self.grid_size = None               # Taille de la grille
        self.total_vars = None              # Nombre total de clauses logiques, égal à grid_size x grid_size
        self.clauses = None                 # Tableau représentant les clauses logiques du puzzle
        self.solution = None                # Résultat du puzzle
        self.second_solution = None         # Deuxième résultat du puzzle, s'il existe


    def generate_dimacs_file(self, input_file, output_file):
        """
        Convertit une grille EasyAsABC de taille N x N en un fichier DIMACS. 

        Format du fichier d'entrée :
        - Ligne 1: Variante du puzzle (basique, easy1, easy2). 
        - Ligne 2: Indications de la ligne du haut. 
        - Ligne 3: Indications de la colonne de droite. 
        - Ligne 4: Indications de la ligne du bas. 
        - Ligne 5: Indications de la colonne de gauche. 
        - Lignes 6 à 5+N (optionnelles): Grille initiale (N lignes de N caractères chacune). 
        """
        
        # Lecture du fichier contenant le puzzle
        with open(input_file, 'r') as f:
            lines = [line.strip() for line in f.readlines() if line.strip()]
        
        # Vérification des lignes minimales nécessaires (5 lignes obligatoires : nom de la variante + 4 indications)
        if len(lines) < 5:
            raise ValueError(f"Au moins 5 lignes sont attendues (variante + 4 indications), {len(lines)} ligne(s) présente(s)")
        
        self.variant = lines[0]
        self.top_clues = lines[1]
        self.right_clues = lines[2]
        self.bottom_clues = lines[3]
        self.left_clues = lines[4]
        self.grid_size = len(self.top_clues)
        
        # Vérification de la longueur des indications (elles doivent être de la taille de la grille)
        for name, clues in [("du haut", self.top_clues), ("de droite", self.right_clues), ("du bas", self.bottom_clues), ("de gauche", self.left_clues)]:
            if len(clues) != self.grid_size:
                raise ValueError(f"Les indications {name} sont de taille {len(clues)} : la grille est de taille {self.grid_size}")
        
        # Lecture de la grille initiale (si présente, il s'agit des lignes optionnelles après les 5 premières lignes obligatoires)
        self.initial_grid = []
        if len(lines) > 5:

            grid_lines = lines[5:5+self.grid_size]
            grid_lines = [''.join(line) for line in grid_lines]
            
            if len(grid_lines) < self.grid_size:
                print(f"Attention: seulement {len(grid_lines)} lignes de grille renseignées sur {self.grid_size}")
            
            for line in grid_lines:
                if len(line) != self.grid_size:
                    raise ValueError(f"Ligne '{line}' devrait avoir {self.grid_size} cases, mais en a {len(line)}")
                self.initial_grid.extend(line)
            
            # Si la grille est incomplète, on complète avec des points pour représenter les cases vides
            while len(self.initial_grid) < self.grid_size * self.grid_size:
                self.initial_grid.append('.')
        else:
            # Grille vide si non fournie
            self.initial_grid = ['.'] * (self.grid_size * self.grid_size)
        
        # Afficher la grille initiale lue
        print(f"Grille initiale lue ({self.grid_size}x{self.grid_size}) :")
        print("    " + " ".join(self.top_clues))
        print("   —" + "".join(["—" for _ in range(2*self.grid_size)]))
        for i in range(self.grid_size):
            start = i * self.grid_size
            print(f"{self.left_clues[i]} | " + " ".join(self.initial_grid[start:start+self.grid_size]) + f" | {self.right_clues[i]}")
        print("   —" + "".join(["—" for _ in range(2*self.grid_size)]))
        print("    " + " ".join(self.bottom_clues))
    
        
        # Nombre total de variables logiques : pour chaque case du puzzle (grid_size x grid_size), on a grid_size possibilités (une pour chaque lettre)
        # Par exemple, pour une grille de taille 4x4, on a 4 possibilités pour chaque case (A, B, C ou D) ce qui fait 64 variables logiques pour ce puzzle
        self.total_vars = self.grid_size * self.grid_size * self.grid_size
        self.clauses = []
        

        # En fonction de la variante du puzzle, on ajoute définit les clauses logiques nécessaires 
        match self.variant:
            case "Basic":
                self.add_basic_clauses()
            case "Easy1":
                self.add_easy1_clauses()
            case "Easy2":
                self.add_easy2_clauses()
            case _:
                raise ValueError(f"Variante '{self.variant}' incorrecte, choix possibles : Basic, Easy1, Easy2")
        
        # Écriture du fichier DIMACS avec les clauses du puzzle
        with open(output_file, 'w') as f:
            f.write(f"p cnf {self.total_vars} {len(self.clauses)}\n")
            for clause in self.clauses:
                f.write(" ".join(map(str, clause)) + " 0\n")
        
        print(f"\nFichier DIMACS généré : {output_file}")
        print(f"  Grille {self.grid_size}x{self.grid_size}, variante {self.variant}, {self.total_vars} variables, {len(self.clauses)} clauses")



    def add_basic_clauses(self):
        """
        Ajoute les clauses logiques pour la variante basique de EasyAsABC.
        
        - La grille n×n utilise n lettres (de A à la (n)ième lettre).
        - Chaque ligne et colonne contient exactement une occurence de chaque lettre.
        - Les indications sur les bords désignent la première lettre visible depuis ce bord (les cases vides sont ignorées pour les indications).
        """

        # Déterminer les lettres à utiliser dans le puzzle et leur index (de la lettre A à la lettre correspondant au numéro de la taille de la grille)
        self.letters = [chr(ord('A') + i) for i in range(self.grid_size)]
        letter_to_idx = {l: i for i, l in enumerate(self.letters)}


        # Contrainte 1 : Chaque case contient exactement une seule lettre
        for r in range(self.grid_size):
            for c in range(self.grid_size):
                # Une case contient au moins une lettre (at_least)
                clause = [variable(r, c, l, self.grid_size) for l in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Une case contient au plus une lettre (at most)
                for l1 in range(self.grid_size):
                    for l2 in range(l1 + 1, self.grid_size):
                        self.clauses.append([-variable(r, c, l1, self.grid_size), -variable(r, c, l2, self.grid_size)])
        

        # Contrainte 2 : Chaque lettre apparaît exactement une fois par ligne
        for r in range(self.grid_size):
            for l in range(self.grid_size):
                # Au moins une occurence de la lettre l dans la ligne r (at least)
                clause = [variable(r, c, l, self.grid_size) for c in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Au plus une occurence de la lettre l dans la ligne r (at most)
                for c1 in range(self.grid_size):
                    for c2 in range(c1 + 1, self.grid_size):
                        self.clauses.append([-variable(r, c1, l, self.grid_size), -variable(r, c2, l, self.grid_size)])
        

        # Contrainte 3 : Chaque lettre apparaît exactement une fois par colonne
        for c in range(self.grid_size):
            for l in range(self.grid_size):
                # Au moins une occurence de la lettre l dans la colonne c (at least)
                clause = [variable(r, c, l, self.grid_size) for r in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Au plus une occurence de la lettre l dans la colonne c (at most)
                for r1 in range(self.grid_size):
                    for r2 in range(r1 + 1, self.grid_size):
                        self.clauses.append([-variable(r1, c, l, self.grid_size), -variable(r2, c, l, self.grid_size)])
        

        # Contrainte 4 : respect des indications données
        
        # Indications du haut : la première case de la colonne doit être celle de l'indication (si elle est donnée)
        for col in range(self.grid_size):
            clue = self.top_clues[col]
            if clue != '.':
                l = letter_to_idx[clue]
                self.clauses.append([variable(0, col, l, self.grid_size)])
        
        # Indications du bas : la dernière case de la colonne doit être celle de l'indication (si elle est donnée)
        for col in range(self.grid_size):
            clue = self.bottom_clues[col]
            if clue != '.':
                l = letter_to_idx[clue]
                self.clauses.append([variable(self.grid_size-1, col, l, self.grid_size)])
        
        # Indications de gauche : la première case de la ligne doit être celle de l'indication (si elle est donnée)
        for row in range(self.grid_size):
            clue = self.left_clues[row]
            if clue != '.':
                l = letter_to_idx[clue]
                self.clauses.append([variable(row, 0, l, self.grid_size)])
        
        # Indications de droite : la dernière case de la ligne doit être celle de l'indication (si elle est donnée)
        for row in range(self.grid_size):
            clue = self.right_clues[row]
            if clue != '.':
                l = letter_to_idx[clue]
                self.clauses.append([variable(row, self.grid_size-1, l, self.grid_size)])
        

        # Contrainte 5 : Cases déjà remplies dans la grille initiale, la case correspondante ne peut être que celle déjà présente dans la grille
        for idx, cell_char in enumerate(self.initial_grid):
            if cell_char != '.':
                r = idx // self.grid_size   # On récupère la ligne de la lettre
                c = idx % self.grid_size    # On récupère la colonne de la lettre
                
                if cell_char not in letter_to_idx:
                    raise ValueError(f"Lettre '{cell_char}' invalide à la position ({r},{c})")
                
                l = letter_to_idx[cell_char]
                # Cette case doit contenir cette lettre, on l'ajoute dans les clauses
                self.clauses.append([variable(r, c, l, self.grid_size)])
                
                # Exclure les autres lettres (nier dans la clause)
                for other_l in range(self.grid_size):
                    if other_l != l:
                        self.clauses.append([-variable(r, c, other_l, self.grid_size)])
                

    
    def add_easy1_clauses(self):
        """
        Ajoute les clauses logiques pour la variante Easy1 de EasyAsABC.
        
        - La grille n×n utilise n-1 lettres (de A à la (n-1)ème lettre).
        - Chaque ligne et colonne contient exactement une case vide (symbolisée par 'X').
        - Les indications sur les bords désignent la première lettre non-vide visible depuis ce bord (les cases vides sont ignorées pour les indications).
        """

        # Déterminer les lettres à utiliser dans le puzzle (n-1 lettres) et leur index
        self.letters = [chr(ord('A') + i) for i in range(self.grid_size - 1)]
        EMPTY = self.grid_size - 1  # indice du symbole "case vide"
        letter_to_idx = {l: i for i, l in enumerate(self.letters)}


        # Contrainte 1 : Chaque case contient exactement un symbole (lettre ou vide)
        for r in range(self.grid_size):
            for c in range(self.grid_size):
                # Une case contient au moins un symbole (at_least)
                clause = [variable(r, c, l, self.grid_size) for l in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Une case contient au plus un symbole (at_most)
                for l1 in range(self.grid_size):
                    for l2 in range(l1 + 1, self.grid_size):
                        self.clauses.append([-variable(r, c, l1, self.grid_size), -variable(r, c, l2, self.grid_size)])


        # Contrainte 2 : Chaque lettre apparaît exactement une fois par ligne
        for r in range(self.grid_size):
            for l in range(self.grid_size - 1):  # seulement les vraies lettres, pas le symbole vide
                # Au moins une occurrence de la lettre l dans la ligne r (at_least)
                clause = [variable(r, c, l, self.grid_size) for c in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Au plus une occurrence de la lettre l dans la ligne r (at_most)
                for c1 in range(self.grid_size):
                    for c2 in range(c1 + 1, self.grid_size):
                        self.clauses.append([-variable(r, c1, l, self.grid_size), -variable(r, c2, l, self.grid_size)])


        # Contrainte 3 : Chaque lettre apparaît exactement une fois par colonne
        for c in range(self.grid_size):
            for l in range(self.grid_size - 1):  # seulement les vraies lettres, pas le symbole vide
                # Au moins une occurrence de la lettre l dans la colonne c (at_least)
                clause = [variable(r, c, l, self.grid_size) for r in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Au plus une occurrence de la lettre l dans la colonne c (at_most)
                for r1 in range(self.grid_size):
                    for r2 in range(r1 + 1, self.grid_size):
                        self.clauses.append([-variable(r1, c, l, self.grid_size), -variable(r2, c, l, self.grid_size)])


        # Contrainte 4 : Exactement une case vide par ligne
        for r in range(self.grid_size):
            # Au moins une case vide dans la ligne (at_least)
            clause = [variable(r, c, EMPTY, self.grid_size) for c in range(self.grid_size)]
            self.clauses.append(clause)
            
            # Au plus une case vide dans la ligne (at_most)
            for c1 in range(self.grid_size):
                for c2 in range(c1 + 1, self.grid_size):
                    self.clauses.append([-variable(r, c1, EMPTY, self.grid_size), -variable(r, c2, EMPTY, self.grid_size)])


        # Contrainte 5 : Exactement une case vide par colonne
        for c in range(self.grid_size):
            # Au moins une case vide dans la colonne (at_least)
            clause = [variable(r, c, EMPTY, self.grid_size) for r in range(self.grid_size)]
            self.clauses.append(clause)
            
            # Au plus une case vide dans la colonne (at_most)
            for r1 in range(self.grid_size):
                for r2 in range(r1 + 1, self.grid_size):
                    self.clauses.append([-variable(r1, c, EMPTY, self.grid_size), -variable(r2, c, EMPTY, self.grid_size)])


        # Contrainte 6 : Respect des indications (première lettre non-vide visible depuis le bord)
        def add_clue_clauses(positions, clue_char):
            """
            Définit la contrainte d'indication pour une séquence de positions (r, c) dans l'ordre de lecture depuis le bord.
            - positions : liste de (row, col) dans l'ordre depuis le bord
            - clue_char : lettre de l'indication (ou '.' si pas d'indication)
            """
            if clue_char == '.':
                return

            l = letter_to_idx[clue_char]

            # Si la lettre l est en position k, alors toutes les cases avant k doivent être vides
            for k, (r, c) in enumerate(positions):
                for i in range(k):
                    ri, ci = positions[i]
                    self.clauses.append([-variable(r, c, l, self.grid_size), variable(ri, ci, EMPTY, self.grid_size)])

            # La lettre l doit apparaître dans au moins une position valide
            self.clauses.append([variable(r, c, l, self.grid_size) for r, c in positions])


        # Ajout des clauses pour chaque indication
        # Indications du haut : parcours de haut en bas
        for col in range(self.grid_size):
            positions = [(row, col) for row in range(self.grid_size)]
            add_clue_clauses(positions, self.top_clues[col])

        # Indications du bas : parcours de bas en haut
        for col in range(self.grid_size):
            positions = [(row, col) for row in range(self.grid_size - 1, -1, -1)]
            add_clue_clauses(positions, self.bottom_clues[col])

        # Indications de gauche : parcours de gauche à droite
        for row in range(self.grid_size):
            positions = [(row, col) for col in range(self.grid_size)]
            add_clue_clauses(positions, self.left_clues[row])

        # Indications de droite : parcours de droite à gauche
        for row in range(self.grid_size):
            positions = [(row, col) for col in range(self.grid_size - 1, -1, -1)]
            add_clue_clauses(positions, self.right_clues[row])


        # Contrainte 7 : Cases déjà remplies dans la grille initiale
        for idx, cell_char in enumerate(self.initial_grid):
            if cell_char == '.':
                continue
                
            r = idx // self.grid_size 
            c = idx % self.grid_size

            if cell_char == 'X':
                # Cette case doit être vide
                self.clauses.append([variable(r, c, EMPTY, self.grid_size)])
                # Exclure toutes les autres lettres
                for other_l in range(self.grid_size - 1):
                    self.clauses.append([-variable(r, c, other_l, self.grid_size)])
            else:
                if cell_char not in letter_to_idx:
                    raise ValueError(f"Lettre '{cell_char}' invalide à la position ({r},{c})")
                
                l = letter_to_idx[cell_char]
                # Cette case doit contenir cette lettre
                self.clauses.append([variable(r, c, l, self.grid_size)])
                # Exclure les autres symboles (lettres ou vide)
                for other_l in range(self.grid_size):
                    if other_l != l:
                        self.clauses.append([-variable(r, c, other_l, self.grid_size)])


    
    def add_easy2_clauses(self):
        """
        Ajoute les clauses logiques pour la variante Easy2 de EasyAsABC.
        
        - La grille n×n utilise n-2 lettres (de A à la (n-2)ème lettre).
        - Chaque ligne et colonne contient exactement deux cases vides (symbolisées par 'X').
        - Les indications sur les bords désignent la première lettre NON-VIDE visible depuis ce bord (les cases vides sont ignorées pour les indications).
        """

        # Déterminer les lettres à utiliser dans le puzzle (n-2 lettres) et leur index
        self.letters = [chr(ord('A') + i) for i in range(self.grid_size - 2)]
        EMPTY = self.grid_size - 1  # indice du symbole "case vide"
        letter_to_idx = {l: i for i, l in enumerate(self.letters)}


        # Contrainte 1 : Chaque case contient exactement un symbole (lettre ou vide)
        for r in range(self.grid_size):
            for c in range(self.grid_size):
                # Une case contient au moins un symbole (at_least)
                valid_symbols = list(range(self.grid_size - 2)) + [EMPTY]
                clause = [variable(r, c, l, self.grid_size) for l in valid_symbols]
                self.clauses.append(clause)
                
                # Une case contient au plus un symbole (at_most)
                for i, l1 in enumerate(valid_symbols):
                    for l2 in valid_symbols[i + 1:]:
                        self.clauses.append([-variable(r, c, l1, self.grid_size), -variable(r, c, l2, self.grid_size)])


        # Contrainte 2 : Chaque lettre apparaît exactement une fois par ligne
        for r in range(self.grid_size):
            for l in range(self.grid_size - 2):  # seulement les vraies lettres, pas le symbole vide
                # Au moins une occurrence de la lettre l dans la ligne r (at_least)
                clause = [variable(r, c, l, self.grid_size) for c in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Au plus une occurrence de la lettre l dans la ligne r (at_most)
                for c1 in range(self.grid_size):
                    for c2 in range(c1 + 1, self.grid_size):
                        self.clauses.append([-variable(r, c1, l, self.grid_size), -variable(r, c2, l, self.grid_size)])


        # Contrainte 3 : Chaque lettre apparaît exactement une fois par colonne
        for c in range(self.grid_size):
            for l in range(self.grid_size - 2):  # seulement les vraies lettres, pas le symbole vide
                # Au moins une occurrence de la lettre l dans la colonne c (at_least)
                clause = [variable(r, c, l, self.grid_size) for r in range(self.grid_size)]
                self.clauses.append(clause)
                
                # Au plus une occurrence de la lettre l dans la colonne c (at_most)
                for r1 in range(self.grid_size):
                    for r2 in range(r1 + 1, self.grid_size):
                        self.clauses.append([-variable(r1, c, l, self.grid_size), -variable(r2, c, l, self.grid_size)])


        # Contrainte 4 : Exactement deux cases vides par ligne
        for r in range(self.grid_size):
            empties = [variable(r, c, EMPTY, self.grid_size) for c in range(self.grid_size)]
            
            # Au moins deux cases vides dans la ligne : pour chaque colonne exclue, au moins une des autres colonnes doit être vide
            for c_excl in range(self.grid_size):
                clause = [variable(r, c, EMPTY, self.grid_size) for c in range(self.grid_size) if c != c_excl]
                self.clauses.append(clause)
            
            # Au plus deux cases vides dans la ligne : pour tout triplet de colonnes, elles ne peuvent pas être toutes vides
            for c1 in range(self.grid_size):
                for c2 in range(c1 + 1, self.grid_size):
                    for c3 in range(c2 + 1, self.grid_size):
                        self.clauses.append([
                            -variable(r, c1, EMPTY, self.grid_size),
                            -variable(r, c2, EMPTY, self.grid_size),
                            -variable(r, c3, EMPTY, self.grid_size)
                        ])


        # Contrainte 5 : Exactement deux cases vides par colonne
        for c in range(self.grid_size):
            # Au moins deux cases vides dans la colonne : pour chaque ligne exclue, au moins une des autres lignes doit être vide
            for r_excl in range(self.grid_size):
                clause = [variable(r, c, EMPTY, self.grid_size) for r in range(self.grid_size) if r != r_excl]
                self.clauses.append(clause)
            
            # Au plus deux cases vides dans la colonne : pour tout triplet de lignes, elles ne peuvent pas être toutes vides
            for r1 in range(self.grid_size):
                for r2 in range(r1 + 1, self.grid_size):
                    for r3 in range(r2 + 1, self.grid_size):
                        self.clauses.append([
                            -variable(r1, c, EMPTY, self.grid_size),
                            -variable(r2, c, EMPTY, self.grid_size),
                            -variable(r3, c, EMPTY, self.grid_size)
                        ])


        # Contrainte 6 : Respect des indications (première lettre NON-VIDE visible depuis le bord)
        def add_clue_clauses(positions, clue_char):
            """
            Encode la contrainte d'indication pour une séquence de positions (r, c) dans l'ordre de lecture depuis le bord.
            positions : liste de (row, col) dans l'ordre depuis le bord
            clue_char : lettre de l'indication (ou '.' si pas d'indication)
            """
            if clue_char == '.':
                return

            l = letter_to_idx[clue_char]

            # Si la lettre l est en position k, alors toutes les cases avant k doivent être vides
            for k, (r, c) in enumerate(positions):
                for i in range(k):
                    ri, ci = positions[i]
                    self.clauses.append([-variable(r, c, l, self.grid_size), variable(ri, ci, EMPTY, self.grid_size)])

            # La lettre l doit apparaître dans au moins une position valide
            self.clauses.append([variable(r, c, l, self.grid_size) for r, c in positions])


        # Ajout des clauses pour chaque indication
        # Indications du haut : parcours de haut en bas
        for col in range(self.grid_size):
            positions = [(row, col) for row in range(self.grid_size)]
            add_clue_clauses(positions, self.top_clues[col])

        # Indications du bas : parcours de bas en haut
        for col in range(self.grid_size):
            positions = [(row, col) for row in range(self.grid_size - 1, -1, -1)]
            add_clue_clauses(positions, self.bottom_clues[col])

        # Indications de gauche : parcours de gauche à droite
        for row in range(self.grid_size):
            positions = [(row, col) for col in range(self.grid_size)]
            add_clue_clauses(positions, self.left_clues[row])

        # Indications de droite : parcours de droite à gauche
        for row in range(self.grid_size):
            positions = [(row, col) for col in range(self.grid_size - 1, -1, -1)]
            add_clue_clauses(positions, self.right_clues[row])


        # Contrainte 7 : Cases déjà remplies dans la grille initiale
        for idx, cell_char in enumerate(self.initial_grid):
            if cell_char == '.':
                continue
                
            r = idx // self.grid_size
            c = idx % self.grid_size

            valid_symbols = list(range(self.grid_size - 2)) + [EMPTY]

            if cell_char == 'X':
                # Cette case doit être vide
                self.clauses.append([variable(r, c, EMPTY, self.grid_size)])
                # Exclure les lettres
                for l in range(self.grid_size - 2):
                    self.clauses.append([-variable(r, c, l, self.grid_size)])
            else:
                if cell_char not in letter_to_idx:
                    raise ValueError(f"Lettre '{cell_char}' invalide à la position ({r},{c})")
                
                l = letter_to_idx[cell_char]
                # Cette case doit contenir cette lettre
                self.clauses.append([variable(r, c, l, self.grid_size)])
                # Exclure les autres symboles (lettres ou vide)
                for other_l in valid_symbols:
                    if other_l != l:
                        self.clauses.append([-variable(r, c, other_l, self.grid_size)])




    def solve(self, dimacs_file):
        """
        Fais appel au solveur SAT Glucose afin de résoudre les clauses d'un fichier DIMACS pris en paramètre.
        Entrée : 
        - dimacs_file : le fichier DIMACS contenant les clauses logiques du puzzle.
        """ 

        self.solver = Glucose3()                # Appel au solveur glucose de python-sat
        self.cnf = CNF(from_file=dimacs_file)   # Conversion du fichier dimacs en clauses lisibles par le solveur 
        
        # Pour chaque clause, on les ajoute au solver
        for clause in self.cnf.clauses:         
            self.solver.add_clause(clause)

        # On fait appel au solver pour résoudre le puzzle et trouver une solution si elle existe, puis on l'enregistre
        if self.solver.solve():
            print('\nPuzzle satisfaisable.')
            self.solution = set(self.solver.get_model())

            # S'il existe une solution, on ajoute sa négation aux clauses pour trouver la deuxième et l'enregistrer aussi
            blocking_clause = [-lit for lit in self.solution if lit>0]
            self.solver.add_clause(blocking_clause)
            
            if self.solver.solve():
                print('\nDeuxième solution trouvée.')
                self.second_solution = set(self.solver.get_model())

        else:
            print('\nPuzzle insatisfaisable.')
            self.solution = None   


    def print_solution_grid(self):
        """Affiche la grille solution"""

        if not self.solution:
            print("Aucune solution à afficher.")
            return
        
        grid = [[bcolors.RED+'X'+bcolors.ENDC for _ in range(self.grid_size)] for _ in range(self.grid_size)]

        for r in range(self.grid_size):
            for c in range(self.grid_size):
                for l_idx, letter in enumerate(self.letters):
                    if variable(r, c, l_idx, self.grid_size) in self.solution:
                        grid[r][c] = letter
                        break

        print()
        print("Grille solution :\n")
        print("    " + " ".join(self.top_clues))
        print("   —" + "".join(["—" for _ in range(2*self.grid_size)]))
        
        # Appliquer les couleurs : bleu pour les lettres de la grille initiale, rouge pour 'X' par défaut
        for r in range(self.grid_size):
            row_display = []
            for c in range(self.grid_size):
                cell = grid[r][c]

                # Vérifier si la case était déjà présente dans la grille initiale
                idx = r * self.grid_size + c
                if idx < len(self.initial_grid) and self.initial_grid[idx] != '.':
                    # Lettre pré-remplie : afficher en bleu
                    row_display.append(bcolors.BLUE + cell + bcolors.ENDC)
                else:
                    # Case vide ou lettre trouvée par le solveur : afficher normalement
                    row_display.append(bcolors.GREEN + cell + bcolors.ENDC)
            print(f"{self.left_clues[r]} | " + " ".join(row_display) + f" | {self.right_clues[r]}")
        
        print("   —" + "".join(["—" for _ in range(2*self.grid_size)]))
        print("    " + " ".join(self.bottom_clues))


        # Afficher la seconde solution si elle existe
        if self.second_solution:
            grid = [[bcolors.RED+'X'+bcolors.ENDC for _ in range(self.grid_size)] for _ in range(self.grid_size)]

            for r in range(self.grid_size):
                for c in range(self.grid_size):
                    for l_idx, letter in enumerate(self.letters):
                        if variable(r, c, l_idx, self.grid_size) in self.second_solution:
                            grid[r][c] = letter
                            break

            print()
            print("Seconde solution :\n")
            print("    " + " ".join(self.top_clues))
            print("   —" + "".join(["—" for _ in range(2*self.grid_size)]))
            
            # Appliquer les couleurs : bleu pour les lettres de la grille initiale, rouge pour 'X' par défaut
            for r in range(self.grid_size):
                row_display = []
                for c in range(self.grid_size):
                    cell = grid[r][c]

                    # Vérifier si la case était déjà présente dans la grille initiale
                    idx = r * self.grid_size + c
                    if idx < len(self.initial_grid) and self.initial_grid[idx] != '.':
                        # Lettre pré-remplie : afficher en bleu
                        row_display.append(bcolors.BLUE + cell + bcolors.ENDC)
                    else:
                        # Case vide ou lettre trouvée par le solveur : afficher normalement
                        row_display.append(bcolors.GREEN + cell + bcolors.ENDC)
                print(f"{self.left_clues[r]} | " + " ".join(row_display) + f" | {self.right_clues[r]}")
            
            print("   —" + "".join(["—" for _ in range(2*self.grid_size)]))
            print("    " + " ".join(self.bottom_clues))




# Programme principal
def main(puzzle_file, dimacs_file):
    """
    Programme principal. Exécute dans l'ordre la séquence suivante :
    - Génère une instance de solveur 
    - Génère le fichier dimacs correspondant au puzzle en entrée
    - Fais appel au solveur pour résoudre les clauses du fichier dimacs
    - Si une solution existe, l'afficher
    - Si une seconde solution existe, l'afficher
    """
    s = EasyAsABC_Solver()
    s.generate_dimacs_file(puzzle_file, dimacs_file)
    s.solve(dimacs_file)
    if s.solution:
        s.print_solution_grid()



# Exécution du programme principal (main)
if __name__ == "__main__":

    # On lance une erreur si les noms des fichiers du puzzle ne sont pas en entrées
    if len(sys.argv) < 3:
        print("Veuillez entrer deux noms de fichiers valides : puzzle_file et dimacs_file")
        sys.exit(1)

    # On récupère les noms des fichiers du puzzle en entrée
    puzzle_file = "Puzzle/"+sys.argv[1]
    dimacs_file = "Dimacs/"+sys.argv[2]

    main(puzzle_file, dimacs_file)