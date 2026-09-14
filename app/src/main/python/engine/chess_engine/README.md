# chess-python-engine

Port Python du moteur d'échecs (`chess-rules.js` -> `chess_rules.py`), pour
coller à la stack réelle de l'appli : UI en Kotlin + XML, moteur en Python.
Même comportement, vérifié coup pour coup par la même batterie de tests
(perft 20/400/8902 compris) — c'est un portage fidèle, pas une réécriture.

## Pourquoi ça remplace la version JS

Le serveur en ligne reste en Node.js (c'est un serveur séparé, aucune raison
qu'il parle la même langue que l'appli). Mais pour tout ce qui doit tourner
*dans* l'appli — validation des coups en local, moteur pour le mode IA,
logique des niveaux — Python est le bon choix puisque c'est ta stack. Garder
en plus une version JS du même moteur pour l'outillage aurait juste dupliqué
la logique dans deux langages ; ce dossier remplace `chess-json-tools/`
entièrement.

## Utiliser le moteur dans l'appli (Chaquopy)

Pour appeler du Python direct depuis Kotlin sur Android, l'outil standard
est [Chaquopy](https://chaquo.com/chaquopy/) (plugin Gradle qui embarque un
interpréteur CPython dans l'APK). `chess_rules.py` n'a aucune dépendance
externe (stdlib pure), donc il s'intègre sans complication de build. Exemple
d'appel depuis Kotlin une fois Chaquopy configuré :

```kotlin
val py = Python.getInstance()
val chessRules = py.getModule("chess_rules")
val state = chessRules.callAttr("create_game_state")
```

Je n'ai pas configuré Chaquopy ici (ça touche à ton `build.gradle` et je ne
veux pas deviner ta structure de projet Android) — dis-moi si tu veux de
l'aide pour le brancher une fois le projet en main.

## Fichiers

- `chess_rules.py` — le moteur (règles, échec/mat/pat, roque, en passant,
  promotion, nulles, FEN <-> position). Zéro dépendance.
- `test_chess_rules.py` — la suite de tests complète, `python3 test_chess_rules.py`.
- `validate_levels.py` / `validate.sh` — même validateur qu'avant, en Python :
  rejoue chaque niveau/export à travers le moteur et signale tout coup illégal.
- `game-export.schema.json`, `level.schema.json` — inchangés (un schéma JSON
  ne dépend pas du langage qui le lit).
- `example_export.json`, `level_1.json` — mêmes exemples qu'avant, revalidés
  avec le moteur Python.

## Utiliser le validateur

```bash
./validate.sh level_1.json
./validate.sh dossier-de-niveaux/
```

## Ajouter un niveau

1. Copier `level_1.json`, changer `id`, `title`, `startingFen` et `solution`.
2. `./validate.sh ton_fichier.json`
3. En cas d'erreur, le message dit exactement quel coup ne passe pas et pourquoi.
