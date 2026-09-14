import sys
import os

# Add the engine path so chess_rules can be imported
sys.path.append(os.path.join(os.path.dirname(__file__), 'engine', 'chess-python-engine'))
import chess_rules

def print_board(state):
    print("\n   a b c d e f g h")
    print("  -----------------")
    for r in range(7, -1, -1):
        row_str = f"{r+1} |"
        for f in range(8):
            p = state['board'][r][f]
            if p is None:
                row_str += ". "
            else:
                color, ptype = p[0], p[1]
                char = ptype if color == 'w' else ptype.lower()
                row_str += char + " "
        row_str += f"| {r+1}"
        print(row_str)
    print("  -----------------")
    print("   a b c d e f g h\n")

def main():
    print("Welcome to MeshChess (Terminal Version)!")
    state = chess_rules.create_game_state()
    
    while True:
        print_board(state)
        status = chess_rules.get_game_status(state)
        
        if status['gameOver']:
            print("Game Over!")
            print(f"Result: {status['result']}")
            if status['winner']:
                print(f"Winner: {'White' if status['winner'] == 'w' else 'Black'}")
            break
            
        if status['inCheck']:
            print("Check!")
            
        turn = "White" if state['turn'] == 'w' else "Black"
        move_str = input(f"{turn}'s turn. Enter move (e.g. e2 e4) or 'q' to quit: ").strip()
        
        if move_str.lower() == 'q':
            break
            
        parts = move_str.split()
        if len(parts) >= 2:
            from_sq = parts[0]
            to_sq = parts[1]
            promo = parts[2].upper() if len(parts) > 2 else None
            
            legal_move = chess_rules.find_legal_move(state, from_sq, to_sq, promo)
            if legal_move:
                chess_rules.apply_move(state, legal_move)
            else:
                print("Invalid or illegal move. Try again.")
        else:
            print("Invalid format. Use 'e2 e4'.")

if __name__ == "__main__":
    main()
