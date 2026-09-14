#!/usr/bin/env python3
"""
Validate level (puzzle) and game-export JSON files: checks the shape of
the file, then replays startingFen + moves/solution through the real
chess engine so an illegal move or malformed FEN fails loudly instead of
shipping silently inside the app.

Usage:
    python3 validate_levels.py <file-or-directory> [...]
"""

import json
import os
import re
import sys

import chess_rules as engine

SQUARE_RE = re.compile(r'^[a-h][1-8]$')
PROMO_VALUES = ('Q', 'R', 'B', 'N', None)


def is_square(value):
    return isinstance(value, str) and bool(SQUARE_RE.match(value))


def check_level_shape(data, errors):
    if not isinstance(data, dict):
        errors.append('root value must be a JSON object')
        return

    for field in ('id', 'title', 'startingFen', 'solution'):
        if field not in data:
            errors.append(f'missing required field "{field}"')

    if 'startingFen' in data and not isinstance(data['startingFen'], str):
        errors.append('"startingFen" must be a string')

    if 'solution' in data:
        solution = data['solution']
        if not isinstance(solution, list) or len(solution) < 1:
            errors.append('"solution" must be a non-empty array')
        else:
            for i, m in enumerate(solution):
                if not isinstance(m, dict):
                    errors.append(f'solution[{i}] must be an object')
                    continue
                if not is_square(m.get('from')):
                    errors.append(f'solution[{i}].from is not a valid square: {m.get("from")!r}')
                if not is_square(m.get('to')):
                    errors.append(f'solution[{i}].to is not a valid square: {m.get("to")!r}')
                if m.get('promotion') not in PROMO_VALUES:
                    errors.append(f'solution[{i}].promotion is invalid: {m.get("promotion")!r}')

    if 'difficulty' in data:
        d = data['difficulty']
        if not isinstance(d, int) or isinstance(d, bool) or d < 1 or d > 10:
            errors.append('"difficulty" must be an integer from 1 to 10')


def check_level_legality(data, errors):
    try:
        state = engine.parse_fen(data['startingFen'])
    except (ValueError, KeyError) as e:
        errors.append(f'startingFen is not a valid position: {e}')
        return

    solution = data.get('solution')
    if not isinstance(solution, list):
        return  # shape check already reported this

    for i, step in enumerate(solution):
        legal = engine.find_legal_move(state, step.get('from'), step.get('to'), step.get('promotion'))
        if not legal:
            errors.append(f'solution[{i}] ({step.get("from")}-{step.get("to")}) is not a legal move in the position at that point')
            return  # later steps depend on this one; stop replaying
        engine.apply_move(state, legal)


def check_export(data, errors):
    if not isinstance(data, dict):
        errors.append('root value must be a JSON object')
        return

    for field in ('exportDate', 'version', 'mode', 'players', 'startingFen', 'moves'):
        if field not in data:
            errors.append(f'missing required field "{field}"')

    if 'mode' in data and data['mode'] not in ('wifi', 'bluetooth', 'ai', 'online'):
        errors.append(f'"mode" must be one of wifi/bluetooth/ai/online, got {data["mode"]!r}')

    moves = data.get('moves')
    if not isinstance(moves, list):
        errors.append('"moves" must be an array')
        return

    try:
        state = engine.parse_fen(data['startingFen'])
    except (ValueError, KeyError) as e:
        errors.append(f'startingFen is not a valid position: {e}')
        return

    for i, mv in enumerate(moves):
        if not is_square(mv.get('from')) or not is_square(mv.get('to')):
            errors.append(f'moves[{i}] has an invalid from/to square')
            return
        legal = engine.find_legal_move(state, mv['from'], mv['to'], mv.get('promotion'))
        if not legal:
            errors.append(f'moves[{i}] ({mv["from"]}-{mv["to"]}) is not a legal move at that point in the game')
            return
        engine.apply_move(state, legal)


def detect_kind(data):
    if isinstance(data, dict) and isinstance(data.get('moves'), list):
        return 'export'
    if isinstance(data, dict) and isinstance(data.get('solution'), list):
        return 'level'
    return 'unknown'


def validate_file(file_path):
    with open(file_path, encoding='utf-8') as fh:
        raw = fh.read()
    try:
        data = json.loads(raw)
    except json.JSONDecodeError as e:
        return file_path, False, [f'invalid JSON: {e}']

    errors = []
    kind = detect_kind(data)
    if kind == 'level':
        check_level_shape(data, errors)
        if not errors:
            check_level_legality(data, errors)
    elif kind == 'export':
        check_export(data, errors)
    else:
        errors.append('could not tell if this is a level (needs "solution") or an export (needs "moves")')

    return file_path, len(errors) == 0, errors


def collect_json_files(input_paths):
    files = []
    for p in input_paths:
        if os.path.isdir(p):
            for entry in sorted(os.listdir(p)):
                if entry.endswith('.json') and not entry.endswith('.schema.json'):
                    files.append(os.path.join(p, entry))
        else:
            files.append(p)
    return files


def main():
    input_paths = sys.argv[1:]
    if not input_paths:
        print('Usage: python3 validate_levels.py <file-or-directory> [...]', file=sys.stderr)
        sys.exit(1)

    any_failed = False
    for file_path in collect_json_files(input_paths):
        path, ok, errors = validate_file(file_path)
        if ok:
            print(f'OK   {path}')
        else:
            any_failed = True
            print(f'FAIL {path}')
            for err in errors:
                print(f'     - {err}')

    sys.exit(1 if any_failed else 0)


if __name__ == '__main__':
    main()
