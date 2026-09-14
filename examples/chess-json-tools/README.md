# chess-json-tools

Formats et validation pour l'export de parties et les niveaux (puzzles) de l'app.
Comme JSON n'accepte pas les commentaires, la doc vit ici et dans les champs
`description` des schémas — pas dans les fichiers de données eux-mêmes.

## Fichiers

- `game-export.schema.json` — format d'un export de partie. Documente chaque
  champ via `description` (voir `mode`, qui liste les valeurs valides :
  `wifi`, `bluetooth`, `ai`, `online` — j'ai ajouté `online` en plus des trois
  que tu avais notés, pour couvrir les parties jouées via le serveur ; change
  si ce n'est pas voulu).
- `level.schema.json` — format d'un niveau/puzzle : position de départ (FEN)
  + solution (liste de coups).
- `chess-rules.js` — le même moteur d'échecs que celui du serveur (règles,
  échec/mat/pat, roque, en passant, promotion, FEN <-> position), réutilisé
  ici pour valider les niveaux.
- `validate-levels.js` — rejoue chaque niveau/export à travers le moteur et
  signale toute case invalide, coup illégal, ou FEN mal formée. C'est ça qui
  répond au vrai problème : un schéma JSON peut vérifier la forme d'un
  fichier, mais seul le moteur peut vérifier qu'un coup est légal.
- `validate.sh` — wrapper bash : `./validate.sh level_1.json` ou
  `./validate.sh dossier-de-niveaux/`.
- `example_export.json`, `level_1.json` — exemples réels et valides,
  générés et vérifiés par le moteur.

## Utiliser le validateur

```bash
./validate.sh level_1.json
./validate.sh example_export.json
./validate.sh chemin/vers/dossier-de-niveaux/    # valide tous les .json du dossier
```

Sortie en cas de succès :

```
OK   level_1.json
```

Sortie en cas d'erreur (avec la ligne exacte qui pose problème) :

```
FAIL level_9.json
     - solution[2] (h5-f6) is not a legal move in the position at that point
```

Code de sortie non nul si un fichier échoue — utilisable dans un script de
build avant d'embarquer les niveaux dans l'app.

## Ajouter un niveau

1. Copier `level_1.json`, changer `id`, `title`, `startingFen` et `solution`.
2. `./validate.sh ton_fichier.json`
3. Si ça échoue, le message dit exactement quel coup ne passe pas et pourquoi.
