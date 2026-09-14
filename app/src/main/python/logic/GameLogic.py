"""
GameLogic.py — Game flow logic for MeshChess.
Manages turns, timers, and game state transitions.
"""
from engine.GameEngine import ChessGame


class GameSession:
    """Manages a single chess game session."""

    def __init__(self):
        self.game = ChessGame()
        self.move_history = []

    def start_new_game(self, fen=None):
        if fen:
            self.game = ChessGame(fen=fen)
        else:
            self.game = ChessGame()
        self.move_history = []

    def play_move(self, from_sq, to_sq, promotion=None):
        success = self.game.make_move(from_sq, to_sq, promotion)
        if success:
            self.move_history.append({
                'from': from_sq,
                'to': to_sq,
                'promotion': promotion,
                'fen_after': self.game.get_fen()
            })
        return success

    def get_board_fen(self):
        return self.game.get_fen()

    def get_status(self):
        return self.game.get_status()

    def get_legal_moves(self):
        return self.game.get_legal_moves()

    def get_turn(self):
        return self.game.get_turn()

    def get_history(self):
        return self.move_history
