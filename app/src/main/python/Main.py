"""
Main.py — Desktop terminal chess game for MeshChess.
Uses the same chess_rules engine as the Android app.
Run with: python3 Main.py
"""
import sys
import os

# Add the engine path so chess_rules can be imported
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'engine', 'chess_engine'))

try:
    import chess_rules
except ImportError:
    # Fallback: try the hyphenated directory name
    sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'engine', 'chess-python-engine'))
    import chess_rules


UNICODE_PIECES = {
    'wK': '♔', 'wQ': '♕', 'wR': '♖', 'wB': '♗', 'wN': '♘', 'wP': '♙',
    'bK': '♚', 'bQ': '♛', 'bR': '♜', 'bB': '♝', 'bN': '♞', 'bP': '♟',
}


def print_board(state):
    print()
    print("    a   b   c   d   e   f   g   h")
    print("  ┌───┬───┬───┬───┬───┬───┬───┬───┐")
    for r in range(7, -1, -1):
        row_str = f"{r+1} │"
        for f in range(8):
            p = state['board'][r][f]
            if p is None:
                # Checkerboard pattern with dots
                if (r + f) % 2 == 0:
                    row_str += " · │"
                else:
                    row_str += "   │"
            else:
                piece_char = UNICODE_PIECES.get(p, p)
                row_str += f" {piece_char} │"
        row_str += f" {r+1}"
        print(row_str)
        if r > 0:
            print("  ├───┼───┼───┼───┼───┼───┼───┼───┤")
    print("  └───┴───┴───┴───┴───┴───┴───┴───┘")
    print("    a   b   c   d   e   f   g   h")
    print()


def main():
    print("=" * 50)
    print("       ♞  MESHCHESS — Terminal Edition  ♞")
    print("         Created by Karlito (v1.0.0)")
    print("=" * 50)
    print()
    print("Enter moves like: e2 e4")
    print("For promotion: e7 e8 Q")
    print("Type 'q' to quit, 'n' for new game")
    print()

    state = chess_rules.create_game_state()

    while True:
        print_board(state)
        status = chess_rules.get_game_status(state)

        if status['gameOver']:
            result = status['result']
            if result == 'checkmate':
                winner = 'White (♔)' if status['winner'] == 'w' else 'Black (♚)'
                print(f"  ★ CHECKMATE! {winner} wins! ★")
            elif result == 'stalemate':
                print("  ⚖ STALEMATE — Draw!")
            elif result == 'insufficient_material':
                print("  ⚖ DRAW — Insufficient material")
            elif result == 'fifty_move_rule':
                print("  ⚖ DRAW — 50-move rule")
            elif result == 'threefold_repetition':
                print("  ⚖ DRAW — Threefold repetition")
            print()
            again = input("Play again? (y/n): ").strip().lower()
            if again == 'y':
                state = chess_rules.create_game_state()
                continue
            break

        if status['inCheck']:
            print("  ⚠ CHECK!")

        turn = "White (♔)" if state['turn'] == 'w' else "Black (♚)"
        move_str = input(f"  {turn}'s turn > ").strip()

        if move_str.lower() == 'q':
            print("Thanks for playing MeshChess!")
            break
        if move_str.lower() == 'n':
            state = chess_rules.create_game_state()
            continue

        parts = move_str.split()
        if len(parts) >= 2:
            from_sq = parts[0].lower()
            to_sq = parts[1].lower()
            promo = parts[2].upper() if len(parts) > 2 else None

            legal_move = chess_rules.find_legal_move(state, from_sq, to_sq, promo)
            if legal_move:
                chess_rules.apply_move(state, legal_move)
            else:
                print("  ✗ Illegal move. Try again.")
        else:
            print("  ✗ Invalid format. Use: e2 e4")


if __name__ == "__main__":
    main()
