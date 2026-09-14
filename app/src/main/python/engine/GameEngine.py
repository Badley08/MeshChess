"""
GameEngine.py — Bridge between the chess rules and the Android/Desktop UI.
Provides a simplified API for the Kotlin layer (via Chaquopy) or the terminal.
"""
from engine.chess_engine.chess_rules import (
    create_game_state,
    find_legal_move,
    apply_move,
    get_game_status,
    to_fen,
    parse_fen,
    generate_legal_moves,
)


class ChessGame:
    """High-level wrapper around the raw chess_rules functions."""

    def __init__(self, fen=None):
        if fen:
            self.state = parse_fen(fen)
        else:
            self.state = create_game_state()

    def get_fen(self):
        return to_fen(self.state)

    def get_turn(self):
        return self.state['turn']

    def get_status(self):
        return get_game_status(self.state)

    def get_legal_moves(self):
        moves = generate_legal_moves(self.state, self.state['turn'])
        result = []
        for m in moves:
            frm = chr(ord('a') + m['from']['file']) + str(m['from']['rank'] + 1)
            to = chr(ord('a') + m['to']['file']) + str(m['to']['rank'] + 1)
            result.append({'from': frm, 'to': to, 'promotion': m['promotion']})
        return result

    def make_move(self, from_sq, to_sq, promotion=None):
        move = find_legal_move(self.state, from_sq, to_sq, promotion)
        if move is None:
            return False
        apply_move(self.state, move)
        return True

    def new_game(self):
        self.state = create_game_state()

    def load_fen(self, fen):
        self.state = parse_fen(fen)
