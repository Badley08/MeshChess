"""
Hand-written chess rules engine: legal move generation, check/checkmate/
stalemate detection, castling, en passant, promotion, draw detection,
and FEN import/export. No external dependencies (stdlib only), so it can
be embedded directly in the Android app (e.g. via Chaquopy) or run as a
plain CLI tool with any Python 3.

This is a line-for-line port of the Node.js engine used by the online
server, kept as the single source of truth for the app's own logic.
Board representation: board[rank][file], rank 0 = rank 1, file 0 = 'a'.
Each square is None or a 2-character string: color ('w'/'b') + piece type
('P','N','B','R','Q','K'), e.g. 'wP', 'bK'.
"""

from copy import deepcopy

KNIGHT_OFFSETS = [(1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2)]
DIAGONAL_DIRS = [(1, 1), (1, -1), (-1, 1), (-1, -1)]
ORTHOGONAL_DIRS = [(1, 0), (-1, 0), (0, 1), (0, -1)]


# ---------- Board helpers ----------

def create_initial_board():
    board = [[None] * 8 for _ in range(8)]
    back_rank = ['R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R']
    for file in range(8):
        board[0][file] = 'w' + back_rank[file]
        board[1][file] = 'wP'
        board[6][file] = 'bP'
        board[7][file] = 'b' + back_rank[file]
    return board


def clone_board(board):
    return [row[:] for row in board]


def square_to_coord(sq):
    file = ord(sq[0]) - ord('a')
    rank = int(sq[1:]) - 1
    return {'file': file, 'rank': rank}


def coord_to_square(file, rank):
    return chr(ord('a') + file) + str(rank + 1)


def in_bounds(file, rank):
    return 0 <= file < 8 and 0 <= rank < 8


def piece_color(piece):
    return piece[0] if piece else None


def piece_type(piece):
    return piece[1] if piece else None


def get_position_key(state):
    board_str = '/'.join(''.join(c if c else '--' for c in row) for row in state['board'])
    castling = state['castlingRights']
    castling_str = ''.join([
        'K' if castling['wK'] else '', 'Q' if castling['wQ'] else '',
        'k' if castling['bK'] else '', 'q' if castling['bQ'] else '',
    ])
    return board_str + state['turn'] + castling_str + (state['enPassantTarget'] or '-')


def create_game_state():
    state = {
        'board': create_initial_board(),
        'turn': 'w',
        'castlingRights': {'wK': True, 'wQ': True, 'bK': True, 'bQ': True},
        'enPassantTarget': None,
        'halfmoveClock': 0,
        'fullmoveNumber': 1,
        'moveHistory': [],
        'positionCounts': {},
    }
    state['positionCounts'][get_position_key(state)] = 1
    return state


# ---------- Attack detection ----------

def is_square_attacked(board, file, rank, by_color):
    pawn_source_rank = rank - 1 if by_color == 'w' else rank + 1
    for df in (-1, 1):
        sf = file + df
        if in_bounds(sf, pawn_source_rank):
            p = board[pawn_source_rank][sf]
            if p and piece_color(p) == by_color and piece_type(p) == 'P':
                return True

    for df, dr in KNIGHT_OFFSETS:
        sf, sr = file + df, rank + dr
        if in_bounds(sf, sr):
            p = board[sr][sf]
            if p and piece_color(p) == by_color and piece_type(p) == 'N':
                return True

    for df in (-1, 0, 1):
        for dr in (-1, 0, 1):
            if df == 0 and dr == 0:
                continue
            sf, sr = file + df, rank + dr
            if in_bounds(sf, sr):
                p = board[sr][sf]
                if p and piece_color(p) == by_color and piece_type(p) == 'K':
                    return True

    for df, dr in DIAGONAL_DIRS:
        sf, sr = file + df, rank + dr
        while in_bounds(sf, sr):
            p = board[sr][sf]
            if p:
                if piece_color(p) == by_color and piece_type(p) in ('B', 'Q'):
                    return True
                break
            sf += df
            sr += dr

    for df, dr in ORTHOGONAL_DIRS:
        sf, sr = file + df, rank + dr
        while in_bounds(sf, sr):
            p = board[sr][sf]
            if p:
                if piece_color(p) == by_color and piece_type(p) in ('R', 'Q'):
                    return True
                break
            sf += df
            sr += dr

    return False


def find_king(board, color):
    for r in range(8):
        for f in range(8):
            p = board[r][f]
            if p and piece_color(p) == color and piece_type(p) == 'K':
                return {'file': f, 'rank': r}
    return None


def is_king_in_check(board, color):
    king_pos = find_king(board, color)
    if not king_pos:
        return False
    enemy_color = 'b' if color == 'w' else 'w'
    return is_square_attacked(board, king_pos['file'], king_pos['rank'], enemy_color)


# ---------- Move generation ----------

def _new_move(file, rank, tf, tr, **extra):
    move = {
        'from': {'file': file, 'rank': rank},
        'to': {'file': tf, 'rank': tr},
        'promotion': None,
        'isEnPassant': False,
        'isCastle': None,
        'isDoublePush': False,
    }
    move.update(extra)
    return move


def generate_piece_moves(state, file, rank):
    board = state['board']
    piece = board[rank][file]
    if not piece:
        return []
    color = piece_color(piece)
    ptype = piece_type(piece)
    moves = []

    if ptype == 'P':
        direction = 1 if color == 'w' else -1
        start_rank = 1 if color == 'w' else 6
        promo_rank = 7 if color == 'w' else 0

        if in_bounds(file, rank + direction) and not board[rank + direction][file]:
            if rank + direction == promo_rank:
                for promo in ('Q', 'R', 'B', 'N'):
                    moves.append(_new_move(file, rank, file, rank + direction, promotion=promo))
            else:
                moves.append(_new_move(file, rank, file, rank + direction))
                if rank == start_rank and not board[rank + 2 * direction][file]:
                    moves.append(_new_move(file, rank, file, rank + 2 * direction, isDoublePush=True))

        for df in (-1, 1):
            tf, tr = file + df, rank + direction
            if not in_bounds(tf, tr):
                continue
            target = board[tr][tf]
            if target and piece_color(target) != color:
                if tr == promo_rank:
                    for promo in ('Q', 'R', 'B', 'N'):
                        moves.append(_new_move(file, rank, tf, tr, promotion=promo))
                else:
                    moves.append(_new_move(file, rank, tf, tr))
            elif not target and state['enPassantTarget']:
                ep = square_to_coord(state['enPassantTarget'])
                if ep['file'] == tf and ep['rank'] == tr:
                    moves.append(_new_move(file, rank, tf, tr, isEnPassant=True))

    elif ptype == 'N':
        for df, dr in KNIGHT_OFFSETS:
            tf, tr = file + df, rank + dr
            if not in_bounds(tf, tr):
                continue
            target = board[tr][tf]
            if not target or piece_color(target) != color:
                moves.append(_new_move(file, rank, tf, tr))

    elif ptype in ('B', 'R', 'Q'):
        dirs = []
        if ptype in ('B', 'Q'):
            dirs += DIAGONAL_DIRS
        if ptype in ('R', 'Q'):
            dirs += ORTHOGONAL_DIRS
        for df, dr in dirs:
            tf, tr = file + df, rank + dr
            while in_bounds(tf, tr):
                target = board[tr][tf]
                if not target:
                    moves.append(_new_move(file, rank, tf, tr))
                else:
                    if piece_color(target) != color:
                        moves.append(_new_move(file, rank, tf, tr))
                    break
                tf += df
                tr += dr

    elif ptype == 'K':
        for df in (-1, 0, 1):
            for dr in (-1, 0, 1):
                if df == 0 and dr == 0:
                    continue
                tf, tr = file + df, rank + dr
                if not in_bounds(tf, tr):
                    continue
                target = board[tr][tf]
                if not target or piece_color(target) != color:
                    moves.append(_new_move(file, rank, tf, tr))

        home_rank = 0 if color == 'w' else 7
        if rank == home_rank and file == 4:
            enemy_color = 'b' if color == 'w' else 'w'
            if not is_square_attacked(board, 4, home_rank, enemy_color):
                can_k = state['castlingRights']['wK' if color == 'w' else 'bK']
                if (
                    can_k and not board[home_rank][5] and not board[home_rank][6]
                    and board[home_rank][7] == color + 'R'
                    and not is_square_attacked(board, 5, home_rank, enemy_color)
                    and not is_square_attacked(board, 6, home_rank, enemy_color)
                ):
                    moves.append(_new_move(file, rank, 6, home_rank, isCastle='K'))

                can_q = state['castlingRights']['wQ' if color == 'w' else 'bQ']
                if (
                    can_q and not board[home_rank][1] and not board[home_rank][2] and not board[home_rank][3]
                    and board[home_rank][0] == color + 'R'
                    and not is_square_attacked(board, 3, home_rank, enemy_color)
                    and not is_square_attacked(board, 2, home_rank, enemy_color)
                ):
                    moves.append(_new_move(file, rank, 2, home_rank, isCastle='Q'))

    return moves


def simulate_move(board, move):
    new_board = clone_board(board)
    frm, to = move['from'], move['to']
    piece = new_board[frm['rank']][frm['file']]
    color = piece_color(piece)

    if move['isEnPassant']:
        new_board[frm['rank']][to['file']] = None

    new_board[to['rank']][to['file']] = (color + move['promotion']) if move['promotion'] else piece
    new_board[frm['rank']][frm['file']] = None

    if move['isCastle'] == 'K':
        home_rank = frm['rank']
        new_board[home_rank][5] = new_board[home_rank][7]
        new_board[home_rank][7] = None
    elif move['isCastle'] == 'Q':
        home_rank = frm['rank']
        new_board[home_rank][3] = new_board[home_rank][0]
        new_board[home_rank][0] = None

    return new_board


def is_move_safe(state, move):
    frm = move['from']
    piece = state['board'][frm['rank']][frm['file']]
    color = piece_color(piece)
    simulated = simulate_move(state['board'], move)
    return not is_king_in_check(simulated, color)


def generate_legal_moves(state, color):
    legal = []
    for r in range(8):
        for f in range(8):
            p = state['board'][r][f]
            if p and piece_color(p) == color:
                for m in generate_piece_moves(state, f, r):
                    if is_move_safe(state, m):
                        legal.append(m)
    return legal


def find_legal_move(state, from_sq, to_sq, promotion=None):
    frm = square_to_coord(from_sq)
    to = square_to_coord(to_sq)
    for m in generate_legal_moves(state, state['turn']):
        if m['from'] == frm and m['to'] == to:
            if m['promotion']:
                if m['promotion'] == (promotion or 'Q'):
                    return m
            else:
                return m
    return None


# ---------- Applying a validated move ----------

def apply_move(state, move):
    frm, to = move['from'], move['to']
    piece = state['board'][frm['rank']][frm['file']]
    color = piece_color(piece)
    ptype = piece_type(piece)
    captured_piece = state['board'][to['rank']][to['file']]
    is_pawn_move = ptype == 'P'
    is_capture = bool(captured_piece) or move['isEnPassant']

    if move['isEnPassant']:
        state['board'][frm['rank']][to['file']] = None

    state['board'][to['rank']][to['file']] = (color + move['promotion']) if move['promotion'] else piece
    state['board'][frm['rank']][frm['file']] = None

    if move['isCastle'] == 'K':
        home_rank = frm['rank']
        state['board'][home_rank][5] = state['board'][home_rank][7]
        state['board'][home_rank][7] = None
    elif move['isCastle'] == 'Q':
        home_rank = frm['rank']
        state['board'][home_rank][3] = state['board'][home_rank][0]
        state['board'][home_rank][0] = None

    if ptype == 'K':
        if color == 'w':
            state['castlingRights']['wK'] = False
            state['castlingRights']['wQ'] = False
        else:
            state['castlingRights']['bK'] = False
            state['castlingRights']['bQ'] = False
    if ptype == 'R':
        if color == 'w' and frm['rank'] == 0 and frm['file'] == 0:
            state['castlingRights']['wQ'] = False
        if color == 'w' and frm['rank'] == 0 and frm['file'] == 7:
            state['castlingRights']['wK'] = False
        if color == 'b' and frm['rank'] == 7 and frm['file'] == 0:
            state['castlingRights']['bQ'] = False
        if color == 'b' and frm['rank'] == 7 and frm['file'] == 7:
            state['castlingRights']['bK'] = False
    if captured_piece and piece_type(captured_piece) == 'R':
        if to['rank'] == 0 and to['file'] == 0:
            state['castlingRights']['wQ'] = False
        if to['rank'] == 0 and to['file'] == 7:
            state['castlingRights']['wK'] = False
        if to['rank'] == 7 and to['file'] == 0:
            state['castlingRights']['bQ'] = False
        if to['rank'] == 7 and to['file'] == 7:
            state['castlingRights']['bK'] = False

    if move['isDoublePush']:
        ep_rank = (frm['rank'] + to['rank']) // 2
        state['enPassantTarget'] = coord_to_square(frm['file'], ep_rank)
    else:
        state['enPassantTarget'] = None

    if is_pawn_move or is_capture:
        state['halfmoveClock'] = 0
    else:
        state['halfmoveClock'] += 1

    if color == 'b':
        state['fullmoveNumber'] += 1

    state['turn'] = 'b' if color == 'w' else 'w'

    captured_type = piece_type(captured_piece) if captured_piece else ('P' if move['isEnPassant'] else None)
    state['moveHistory'].append({
        'from': coord_to_square(frm['file'], frm['rank']),
        'to': coord_to_square(to['file'], to['rank']),
        'piece': ptype,
        'color': color,
        'captured': captured_type,
        'promotion': move['promotion'],
        'isCastle': move['isCastle'],
        'isEnPassant': move['isEnPassant'],
    })

    key = get_position_key(state)
    state['positionCounts'][key] = state['positionCounts'].get(key, 0) + 1

    return {'capturedPiece': captured_type}


# ---------- Game status ----------

def is_insufficient_material(board):
    pieces = []
    for r in range(8):
        for f in range(8):
            p = board[r][f]
            if p and piece_type(p) != 'K':
                pieces.append(p)

    if len(pieces) == 0:
        return True
    if len(pieces) == 1 and piece_type(pieces[0]) in ('B', 'N'):
        return True
    if len(pieces) == 2 and piece_type(pieces[0]) == 'B' and piece_type(pieces[1]) == 'B':
        square_colors = []
        for r in range(8):
            for f in range(8):
                p = board[r][f]
                if p and piece_type(p) == 'B':
                    square_colors.append((r + f) % 2)
        if len(square_colors) == 2 and square_colors[0] == square_colors[1]:
            return True
    return False


def get_game_status(state):
    color = state['turn']
    in_check = is_king_in_check(state['board'], color)
    legal_moves = generate_legal_moves(state, color)
    no_moves = len(legal_moves) == 0
    checkmate = in_check and no_moves
    stalemate = (not in_check) and no_moves
    insufficient_material = is_insufficient_material(state['board'])
    fifty_move_draw = state['halfmoveClock'] >= 100
    key = get_position_key(state)
    threefold_draw = state['positionCounts'].get(key, 0) >= 3

    result = None
    winner = None
    if checkmate:
        result = 'checkmate'
        winner = 'b' if color == 'w' else 'w'
    elif stalemate:
        result = 'stalemate'
    elif insufficient_material:
        result = 'insufficient_material'
    elif fifty_move_draw:
        result = 'fifty_move_rule'
    elif threefold_draw:
        result = 'threefold_repetition'

    return {
        'inCheck': in_check,
        'checkmate': checkmate,
        'stalemate': stalemate,
        'gameOver': result is not None,
        'result': result,
        'winner': winner,
        'turn': color,
    }


# ---------- FEN import/export ----------

def to_fen(state):
    rows = []
    for r in range(7, -1, -1):
        row = ''
        empty = 0
        for f in range(8):
            p = state['board'][r][f]
            if not p:
                empty += 1
            else:
                if empty > 0:
                    row += str(empty)
                    empty = 0
                letter = piece_type(p)
                row += letter if piece_color(p) == 'w' else letter.lower()
        if empty > 0:
            row += str(empty)
        rows.append(row)
    placement = '/'.join(rows)

    castling = state['castlingRights']
    castling_str = ''.join([
        'K' if castling['wK'] else '', 'Q' if castling['wQ'] else '',
        'k' if castling['bK'] else '', 'q' if castling['bQ'] else '',
    ]) or '-'

    ep = state['enPassantTarget'] or '-'

    return f"{placement} {state['turn']} {castling_str} {ep} {state['halfmoveClock']} {state['fullmoveNumber']}"


def parse_fen(fen):
    parts = str(fen).strip().split()
    if not parts:
        raise ValueError('FEN is empty')
    placement = parts[0]
    turn_part = parts[1] if len(parts) > 1 else 'w'
    castling_part = parts[2] if len(parts) > 2 else '-'
    ep_part = parts[3] if len(parts) > 3 else '-'
    halfmove_part = parts[4] if len(parts) > 4 else '0'
    fullmove_part = parts[5] if len(parts) > 5 else '1'

    board = [[None] * 8 for _ in range(8)]
    ranks = placement.split('/')
    if len(ranks) != 8:
        raise ValueError(f'FEN placement must have 8 ranks, got {len(ranks)}')

    for i in range(8):
        rank = 7 - i
        file = 0
        for ch in ranks[i]:
            if ch.isdigit() and '1' <= ch <= '8':
                file += int(ch)
            elif ch.upper() in ('P', 'N', 'B', 'R', 'Q', 'K'):
                if file > 7:
                    raise ValueError(f'FEN rank {8 - i} has too many squares')
                color = 'w' if ch.isupper() else 'b'
                board[rank][file] = color + ch.upper()
                file += 1
            else:
                raise ValueError(f'FEN contains an invalid character: "{ch}"')
        if file != 8:
            raise ValueError(f'FEN rank {8 - i} does not add up to 8 squares')

    castling_rights = {'wK': False, 'wQ': False, 'bK': False, 'bQ': False}
    if castling_part and castling_part != '-':
        for ch in castling_part:
            if ch == 'K':
                castling_rights['wK'] = True
            elif ch == 'Q':
                castling_rights['wQ'] = True
            elif ch == 'k':
                castling_rights['bK'] = True
            elif ch == 'q':
                castling_rights['bQ'] = True
            else:
                raise ValueError(f'FEN contains an invalid castling flag: "{ch}"')

    state = {
        'board': board,
        'turn': 'b' if turn_part == 'b' else 'w',
        'castlingRights': castling_rights,
        'enPassantTarget': ep_part if ep_part and ep_part != '-' else None,
        'halfmoveClock': int(halfmove_part) if halfmove_part.lstrip('-').isdigit() else 0,
        'fullmoveNumber': int(fullmove_part) if fullmove_part.lstrip('-').isdigit() else 1,
        'moveHistory': [],
        'positionCounts': {},
    }
    state['positionCounts'][get_position_key(state)] = 1
    return state
