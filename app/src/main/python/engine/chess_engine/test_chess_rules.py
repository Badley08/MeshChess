import copy
import chess_rules as engine

passed = 0
failed = 0


def check(cond, label):
    global passed, failed
    if cond:
        passed += 1
    else:
        failed += 1
        print('FAIL:', label)


def move(state, frm, to, promotion=None):
    m = engine.find_legal_move(state, frm, to, promotion)
    if not m:
        return None
    engine.apply_move(state, m)
    return m


# Test 1: initial position has 20 legal moves for white
state = engine.create_game_state()
moves = engine.generate_legal_moves(state, 'w')
check(len(moves) == 20, f'initial white legal move count (got {len(moves)}, expected 20)')

# Test 2: Fool's mate
state = engine.create_game_state()
check(move(state, 'f2', 'f3') is not None, 'foolsmate f2f3')
check(move(state, 'e7', 'e5') is not None, 'foolsmate e7e5')
check(move(state, 'g2', 'g4') is not None, 'foolsmate g2g4')
check(move(state, 'd8', 'h4') is not None, 'foolsmate d8h4')
status = engine.get_game_status(state)
check(status['checkmate'] is True, f'fools mate checkmate detected (got {status})')
check(status['winner'] == 'b', 'fools mate winner is black')

# Test 3: Scholar's mate
state = engine.create_game_state()
move(state, 'e2', 'e4'); move(state, 'e7', 'e5')
move(state, 'f1', 'c4'); move(state, 'b8', 'c6')
move(state, 'd1', 'h5'); move(state, 'g8', 'f6')
move(state, 'h5', 'f7')
status = engine.get_game_status(state)
check(status['checkmate'] is True, f'scholars mate checkmate (got {status})')
check(status['winner'] == 'w', 'scholars mate winner is white')

# Test 4: illegal move rejected (pawn cannot jump 3 squares)
state = engine.create_game_state()
check(engine.find_legal_move(state, 'e2', 'e5') is None, 'illegal pawn triple push rejected')

# Test 5: direct check detection
s3 = engine.create_game_state()
for r in range(8):
    for f in range(8):
        s3['board'][r][f] = None
s3['board'][0][4] = 'wK'
s3['board'][7][4] = 'bR'
s3['board'][7][0] = 'bK'
s3['turn'] = 'w'
status = engine.get_game_status(s3)
check(status['inCheck'] is True, f'direct rook check on e-file detected (got {status})')

# Test 6: castling kingside and queenside
state = engine.create_game_state()
state['board'][0][1] = None; state['board'][0][2] = None; state['board'][0][3] = None
state['board'][0][5] = None; state['board'][0][6] = None
king_side_move = engine.find_legal_move(state, 'e1', 'g1')
check(king_side_move is not None and king_side_move['isCastle'] == 'K', 'white kingside castle available')

state2 = engine.create_game_state()
state2['board'][0][1] = None; state2['board'][0][2] = None; state2['board'][0][3] = None
engine.apply_move(state2, engine.find_legal_move(state2, 'e1', 'c1'))
check(
    state2['board'][0][3] == 'wR' and state2['board'][0][2] == 'wK' and state2['board'][0][0] is None,
    'white queenside castle moves king and rook correctly',
)

# Test 7: castling blocked when king passes through attacked square
state = engine.create_game_state()
state['board'][0][5] = None; state['board'][0][6] = None
state['board'][1][5] = None
state['board'][3][5] = 'bR'
check(engine.find_legal_move(state, 'e1', 'g1') is None, 'castling blocked when passing through attacked square')

# Test 8: en passant capture
state = engine.create_game_state()
move(state, 'e2', 'e4'); move(state, 'a7', 'a6')
move(state, 'e4', 'e5'); move(state, 'd7', 'd5')
ep = move(state, 'e5', 'd6')
check(ep is not None and ep['isEnPassant'] is True, 'en passant move recognized')
check(state['board'][4][3] is None, 'captured pawn removed after en passant')

# Test 9: pawn promotion
state = engine.create_game_state()
for r in range(8):
    for f in range(8):
        state['board'][r][f] = None
state['board'][0][4] = 'wK'; state['board'][7][4] = 'bK'
state['board'][6][0] = 'wP'
state['turn'] = 'w'
m = engine.find_legal_move(state, 'a7', 'a8', 'Q')
check(m is not None and m['promotion'] == 'Q', 'promotion move found for requested piece')
engine.apply_move(state, m)
check(state['board'][7][0] == 'wQ', 'pawn promoted to queen on board')

# Test 10: stalemate detection
state = engine.create_game_state()
for r in range(8):
    for f in range(8):
        state['board'][r][f] = None
state['board'][7][0] = 'bK'
state['board'][6][2] = 'wK'
state['board'][5][1] = 'wQ'
state['turn'] = 'b'
status = engine.get_game_status(state)
check(status['stalemate'] is True, f'stalemate detected (got {status})')

# Test 11: threefold repetition
state = engine.create_game_state()
for _ in range(2):
    move(state, 'g1', 'f3'); move(state, 'g8', 'f6')
    move(state, 'f3', 'g1'); move(state, 'f6', 'g8')
status = engine.get_game_status(state)
check(status['result'] == 'threefold_repetition', f'threefold repetition detected (got {status})')

# Test 12: FEN round trip (starting position)
state = engine.create_game_state()
fen = engine.to_fen(state)
reparsed = engine.parse_fen(fen)
fen2 = engine.to_fen(reparsed)
check(fen == fen2, f'round-trip starting FEN (got {fen2})')
check(fen == 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1', 'starting FEN matches standard string')

# Test 13: known mid-game FEN
known = 'rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2'
state = engine.parse_fen(known)
check(engine.to_fen(state) == known, f'known mid-game FEN round-trips (got {engine.to_fen(state)})')
check(state['turn'] == 'b', 'turn parsed correctly as black to move')
moves = engine.generate_legal_moves(state, 'b')
check(len(moves) == 29, f'black has 29 legal moves in this position (got {len(moves)})')

# Test 14: invalid FEN raises instead of failing silently
threw = False
try:
    engine.parse_fen('not a fen at all')
except ValueError:
    threw = True
check(threw, 'malformed FEN raises instead of failing silently')


# Test 15: perft correctness benchmark
def perft(state, depth):
    if depth == 0:
        return 1
    moves = engine.generate_legal_moves(state, state['turn'])
    if depth == 1:
        return len(moves)
    nodes = 0
    for m in moves:
        snapshot = copy.deepcopy({
            'board': state['board'], 'turn': state['turn'], 'castlingRights': state['castlingRights'],
            'enPassantTarget': state['enPassantTarget'], 'halfmoveClock': state['halfmoveClock'],
            'fullmoveNumber': state['fullmoveNumber'],
        })
        engine.apply_move(state, m)
        nodes += perft(state, depth - 1)
        state['board'] = snapshot['board']
        state['turn'] = snapshot['turn']
        state['castlingRights'] = snapshot['castlingRights']
        state['enPassantTarget'] = snapshot['enPassantTarget']
        state['halfmoveClock'] = snapshot['halfmoveClock']
        state['fullmoveNumber'] = snapshot['fullmoveNumber']
        state['moveHistory'].pop()
    return nodes


s = engine.create_game_state()
p1 = perft(s, 1)
p2 = perft(s, 2)
p3 = perft(s, 3)
print(f'perft(1)={p1} perft(2)={p2} perft(3)={p3}')
perft_ok = p1 == 20 and p2 == 400 and p3 == 8902
print('PERFT OK' if perft_ok else 'PERFT MISMATCH (expected 20/400/8902)')

print(f'\n{passed} passed, {failed} failed')
raise SystemExit(1 if (failed > 0 or not perft_ok) else 0)
