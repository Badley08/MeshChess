'use strict';

const path = require('path');
const crypto = require('crypto');
const express = require('express');
const { WebSocketServer, WebSocket } = require('ws');

const PORT = process.env.PORT || 8080;

// =====================================================================
// CHESS ENGINE
// Hand-written rule validation: legal moves, check/checkmate/stalemate,
// castling, en passant, promotion, draw detection. No external library.
// =====================================================================

function createInitialBoard() {
  const board = Array.from({ length: 8 }, () => Array(8).fill(null));
  const backRank = ['R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R'];
  for (let file = 0; file < 8; file++) {
    board[0][file] = 'w' + backRank[file];
    board[1][file] = 'wP';
    board[6][file] = 'bP';
    board[7][file] = 'b' + backRank[file];
  }
  return board;
}

function cloneBoard(board) {
  return board.map((row) => row.slice());
}

function squareToCoord(sq) {
  const file = sq.charCodeAt(0) - 97;
  const rank = parseInt(sq.slice(1), 10) - 1;
  return { file, rank };
}

function coordToSquare(file, rank) {
  return String.fromCharCode(97 + file) + (rank + 1);
}

function inBounds(file, rank) {
  return file >= 0 && file < 8 && rank >= 0 && rank < 8;
}

function pieceColor(piece) {
  return piece ? piece[0] : null;
}

function pieceType(piece) {
  return piece ? piece[1] : null;
}

function getPositionKey(state) {
  const boardStr = state.board.map((row) => row.map((c) => c || '--').join('')).join('/');
  return boardStr + state.turn + JSON.stringify(state.castlingRights) + (state.enPassantTarget || '-');
}

function createGameState() {
  const state = {
    board: createInitialBoard(),
    turn: 'w',
    castlingRights: { wK: true, wQ: true, bK: true, bQ: true },
    enPassantTarget: null,
    halfmoveClock: 0,
    fullmoveNumber: 1,
    moveHistory: [],
    positionCounts: new Map(),
  };
  state.positionCounts.set(getPositionKey(state), 1);
  return state;
}

function isSquareAttacked(board, file, rank, byColor) {
  const pawnSourceRank = byColor === 'w' ? rank - 1 : rank + 1;
  for (const df of [-1, 1]) {
    const sf = file + df;
    if (inBounds(sf, pawnSourceRank)) {
      const p = board[pawnSourceRank][sf];
      if (p && pieceColor(p) === byColor && pieceType(p) === 'P') return true;
    }
  }

  const knightOffsets = [[1, 2], [2, 1], [2, -1], [1, -2], [-1, -2], [-2, -1], [-2, 1], [-1, 2]];
  for (const [df, dr] of knightOffsets) {
    const sf = file + df, sr = rank + dr;
    if (inBounds(sf, sr)) {
      const p = board[sr][sf];
      if (p && pieceColor(p) === byColor && pieceType(p) === 'N') return true;
    }
  }

  for (let df = -1; df <= 1; df++) {
    for (let dr = -1; dr <= 1; dr++) {
      if (df === 0 && dr === 0) continue;
      const sf = file + df, sr = rank + dr;
      if (inBounds(sf, sr)) {
        const p = board[sr][sf];
        if (p && pieceColor(p) === byColor && pieceType(p) === 'K') return true;
      }
    }
  }

  const diagDirs = [[1, 1], [1, -1], [-1, 1], [-1, -1]];
  for (const [df, dr] of diagDirs) {
    let sf = file + df, sr = rank + dr;
    while (inBounds(sf, sr)) {
      const p = board[sr][sf];
      if (p) {
        if (pieceColor(p) === byColor && (pieceType(p) === 'B' || pieceType(p) === 'Q')) return true;
        break;
      }
      sf += df; sr += dr;
    }
  }

  const orthoDirs = [[1, 0], [-1, 0], [0, 1], [0, -1]];
  for (const [df, dr] of orthoDirs) {
    let sf = file + df, sr = rank + dr;
    while (inBounds(sf, sr)) {
      const p = board[sr][sf];
      if (p) {
        if (pieceColor(p) === byColor && (pieceType(p) === 'R' || pieceType(p) === 'Q')) return true;
        break;
      }
      sf += df; sr += dr;
    }
  }

  return false;
}

function findKing(board, color) {
  for (let r = 0; r < 8; r++) {
    for (let f = 0; f < 8; f++) {
      const p = board[r][f];
      if (p && pieceColor(p) === color && pieceType(p) === 'K') return { file: f, rank: r };
    }
  }
  return null;
}

function isKingInCheck(board, color) {
  const kingPos = findKing(board, color);
  if (!kingPos) return false;
  const enemyColor = color === 'w' ? 'b' : 'w';
  return isSquareAttacked(board, kingPos.file, kingPos.rank, enemyColor);
}

function generatePieceMoves(state, file, rank) {
  const { board } = state;
  const piece = board[rank][file];
  if (!piece) return [];
  const color = pieceColor(piece);
  const type = pieceType(piece);
  const moves = [];

  const addMove = (tf, tr, extra = {}) => {
    moves.push({
      from: { file, rank },
      to: { file: tf, rank: tr },
      promotion: null,
      isEnPassant: false,
      isCastle: null,
      isDoublePush: false,
      ...extra,
    });
  };

  if (type === 'P') {
    const dir = color === 'w' ? 1 : -1;
    const startRank = color === 'w' ? 1 : 6;
    const promoRank = color === 'w' ? 7 : 0;

    if (inBounds(file, rank + dir) && !board[rank + dir][file]) {
      if (rank + dir === promoRank) {
        for (const promo of ['Q', 'R', 'B', 'N']) addMove(file, rank + dir, { promotion: promo });
      } else {
        addMove(file, rank + dir);
        if (rank === startRank && !board[rank + 2 * dir][file]) {
          addMove(file, rank + 2 * dir, { isDoublePush: true });
        }
      }
    }

    for (const df of [-1, 1]) {
      const tf = file + df, tr = rank + dir;
      if (!inBounds(tf, tr)) continue;
      const target = board[tr][tf];
      if (target && pieceColor(target) !== color) {
        if (tr === promoRank) {
          for (const promo of ['Q', 'R', 'B', 'N']) addMove(tf, tr, { promotion: promo });
        } else {
          addMove(tf, tr);
        }
      } else if (!target && state.enPassantTarget) {
        const ep = squareToCoord(state.enPassantTarget);
        if (ep.file === tf && ep.rank === tr) addMove(tf, tr, { isEnPassant: true });
      }
    }
  } else if (type === 'N') {
    const offsets = [[1, 2], [2, 1], [2, -1], [1, -2], [-1, -2], [-2, -1], [-2, 1], [-1, 2]];
    for (const [df, dr] of offsets) {
      const tf = file + df, tr = rank + dr;
      if (!inBounds(tf, tr)) continue;
      const target = board[tr][tf];
      if (!target || pieceColor(target) !== color) addMove(tf, tr);
    }
  } else if (type === 'B' || type === 'R' || type === 'Q') {
    const dirs = [];
    if (type === 'B' || type === 'Q') dirs.push([1, 1], [1, -1], [-1, 1], [-1, -1]);
    if (type === 'R' || type === 'Q') dirs.push([1, 0], [-1, 0], [0, 1], [0, -1]);
    for (const [df, dr] of dirs) {
      let tf = file + df, tr = rank + dr;
      while (inBounds(tf, tr)) {
        const target = board[tr][tf];
        if (!target) {
          addMove(tf, tr);
        } else {
          if (pieceColor(target) !== color) addMove(tf, tr);
          break;
        }
        tf += df; tr += dr;
      }
    }
  } else if (type === 'K') {
    for (let df = -1; df <= 1; df++) {
      for (let dr = -1; dr <= 1; dr++) {
        if (df === 0 && dr === 0) continue;
        const tf = file + df, tr = rank + dr;
        if (!inBounds(tf, tr)) continue;
        const target = board[tr][tf];
        if (!target || pieceColor(target) !== color) addMove(tf, tr);
      }
    }

    const homeRank = color === 'w' ? 0 : 7;
    if (rank === homeRank && file === 4) {
      const enemyColor = color === 'w' ? 'b' : 'w';
      if (!isSquareAttacked(board, 4, homeRank, enemyColor)) {
        const canK = color === 'w' ? state.castlingRights.wK : state.castlingRights.bK;
        if (
          canK && !board[homeRank][5] && !board[homeRank][6] &&
          board[homeRank][7] === color + 'R' &&
          !isSquareAttacked(board, 5, homeRank, enemyColor) &&
          !isSquareAttacked(board, 6, homeRank, enemyColor)
        ) {
          addMove(6, homeRank, { isCastle: 'K' });
        }

        const canQ = color === 'w' ? state.castlingRights.wQ : state.castlingRights.bQ;
        if (
          canQ && !board[homeRank][1] && !board[homeRank][2] && !board[homeRank][3] &&
          board[homeRank][0] === color + 'R' &&
          !isSquareAttacked(board, 3, homeRank, enemyColor) &&
          !isSquareAttacked(board, 2, homeRank, enemyColor)
        ) {
          addMove(2, homeRank, { isCastle: 'Q' });
        }
      }
    }
  }

  return moves;
}

function simulateMove(board, move) {
  const newBoard = cloneBoard(board);
  const { from, to, promotion, isEnPassant, isCastle } = move;
  const piece = newBoard[from.rank][from.file];
  const color = pieceColor(piece);

  if (isEnPassant) newBoard[from.rank][to.file] = null;

  newBoard[to.rank][to.file] = promotion ? color + promotion : piece;
  newBoard[from.rank][from.file] = null;

  if (isCastle === 'K') {
    const homeRank = from.rank;
    newBoard[homeRank][5] = newBoard[homeRank][7];
    newBoard[homeRank][7] = null;
  } else if (isCastle === 'Q') {
    const homeRank = from.rank;
    newBoard[homeRank][3] = newBoard[homeRank][0];
    newBoard[homeRank][0] = null;
  }

  return newBoard;
}

function isMoveSafe(state, move) {
  const piece = state.board[move.from.rank][move.from.file];
  const color = pieceColor(piece);
  const simulated = simulateMove(state.board, move);
  return !isKingInCheck(simulated, color);
}

function generateLegalMoves(state, color) {
  const legal = [];
  for (let r = 0; r < 8; r++) {
    for (let f = 0; f < 8; f++) {
      const p = state.board[r][f];
      if (p && pieceColor(p) === color) {
        const pseudo = generatePieceMoves(state, f, r);
        for (const m of pseudo) {
          if (isMoveSafe(state, m)) legal.push(m);
        }
      }
    }
  }
  return legal;
}

function findLegalMove(state, fromSq, toSq, promotion) {
  const from = squareToCoord(fromSq);
  const to = squareToCoord(toSq);
  const legalMoves = generateLegalMoves(state, state.turn);
  return legalMoves.find((m) =>
    m.from.file === from.file && m.from.rank === from.rank &&
    m.to.file === to.file && m.to.rank === to.rank &&
    (m.promotion ? m.promotion === (promotion || 'Q') : true)
  ) || null;
}

function applyMove(state, move) {
  const { from, to, promotion, isEnPassant, isCastle } = move;
  const piece = state.board[from.rank][from.file];
  const color = pieceColor(piece);
  const type = pieceType(piece);
  const capturedPiece = state.board[to.rank][to.file];
  const isPawnMove = type === 'P';
  const isCapture = !!capturedPiece || isEnPassant;

  if (isEnPassant) state.board[from.rank][to.file] = null;

  state.board[to.rank][to.file] = promotion ? color + promotion : piece;
  state.board[from.rank][from.file] = null;

  if (isCastle === 'K') {
    const homeRank = from.rank;
    state.board[homeRank][5] = state.board[homeRank][7];
    state.board[homeRank][7] = null;
  } else if (isCastle === 'Q') {
    const homeRank = from.rank;
    state.board[homeRank][3] = state.board[homeRank][0];
    state.board[homeRank][0] = null;
  }

  if (type === 'K') {
    if (color === 'w') { state.castlingRights.wK = false; state.castlingRights.wQ = false; }
    else { state.castlingRights.bK = false; state.castlingRights.bQ = false; }
  }
  if (type === 'R') {
    if (color === 'w' && from.rank === 0 && from.file === 0) state.castlingRights.wQ = false;
    if (color === 'w' && from.rank === 0 && from.file === 7) state.castlingRights.wK = false;
    if (color === 'b' && from.rank === 7 && from.file === 0) state.castlingRights.bQ = false;
    if (color === 'b' && from.rank === 7 && from.file === 7) state.castlingRights.bK = false;
  }
  if (capturedPiece && pieceType(capturedPiece) === 'R') {
    if (to.rank === 0 && to.file === 0) state.castlingRights.wQ = false;
    if (to.rank === 0 && to.file === 7) state.castlingRights.wK = false;
    if (to.rank === 7 && to.file === 0) state.castlingRights.bQ = false;
    if (to.rank === 7 && to.file === 7) state.castlingRights.bK = false;
  }

  if (move.isDoublePush) {
    const epRank = (from.rank + to.rank) / 2;
    state.enPassantTarget = coordToSquare(from.file, epRank);
  } else {
    state.enPassantTarget = null;
  }

  if (isPawnMove || isCapture) state.halfmoveClock = 0;
  else state.halfmoveClock++;

  if (color === 'b') state.fullmoveNumber++;

  state.turn = color === 'w' ? 'b' : 'w';

  state.moveHistory.push({
    from: coordToSquare(from.file, from.rank),
    to: coordToSquare(to.file, to.rank),
    piece: type,
    color,
    captured: capturedPiece ? pieceType(capturedPiece) : (isEnPassant ? 'P' : null),
    promotion: promotion || null,
    isCastle: isCastle || null,
    isEnPassant: !!isEnPassant,
  });

  const key = getPositionKey(state);
  state.positionCounts.set(key, (state.positionCounts.get(key) || 0) + 1);

  return { capturedPiece: capturedPiece ? pieceType(capturedPiece) : (isEnPassant ? 'P' : null) };
}

function isInsufficientMaterial(board) {
  const pieces = [];
  for (let r = 0; r < 8; r++) {
    for (let f = 0; f < 8; f++) {
      const p = board[r][f];
      if (p && pieceType(p) !== 'K') pieces.push(p);
    }
  }
  if (pieces.length === 0) return true;
  if (pieces.length === 1 && (pieceType(pieces[0]) === 'B' || pieceType(pieces[0]) === 'N')) return true;
  if (pieces.length === 2 && pieceType(pieces[0]) === 'B' && pieceType(pieces[1]) === 'B') {
    const squareColors = [];
    for (let r = 0; r < 8; r++) {
      for (let f = 0; f < 8; f++) {
        const p = board[r][f];
        if (p && pieceType(p) === 'B') squareColors.push((r + f) % 2);
      }
    }
    if (squareColors.length === 2 && squareColors[0] === squareColors[1]) return true;
  }
  return false;
}

function getGameStatus(state) {
  const color = state.turn;
  const inCheck = isKingInCheck(state.board, color);
  const legalMoves = generateLegalMoves(state, color);
  const noMoves = legalMoves.length === 0;
  const checkmate = inCheck && noMoves;
  const stalemate = !inCheck && noMoves;
  const insufficientMaterial = isInsufficientMaterial(state.board);
  const fiftyMoveDraw = state.halfmoveClock >= 100;
  const key = getPositionKey(state);
  const threefoldDraw = (state.positionCounts.get(key) || 0) >= 3;

  let result = null;
  let winner = null;
  if (checkmate) { result = 'checkmate'; winner = color === 'w' ? 'b' : 'w'; }
  else if (stalemate) result = 'stalemate';
  else if (insufficientMaterial) result = 'insufficient_material';
  else if (fiftyMoveDraw) result = 'fifty_move_rule';
  else if (threefoldDraw) result = 'threefold_repetition';

  return { inCheck, checkmate, stalemate, gameOver: !!result, result, winner, turn: color };
}

// Export the board as a standard FEN string, the common interchange format
// most chess UI libraries (including on Android) can consume directly.
function toFEN(state) {
  const rows = [];
  for (let r = 7; r >= 0; r--) {
    let row = '';
    let empty = 0;
    for (let f = 0; f < 8; f++) {
      const p = state.board[r][f];
      if (!p) {
        empty++;
      } else {
        if (empty > 0) { row += empty; empty = 0; }
        const letter = pieceType(p);
        row += pieceColor(p) === 'w' ? letter : letter.toLowerCase();
      }
    }
    if (empty > 0) row += empty;
    rows.push(row);
  }
  const placement = rows.join('/');

  let castling = '';
  if (state.castlingRights.wK) castling += 'K';
  if (state.castlingRights.wQ) castling += 'Q';
  if (state.castlingRights.bK) castling += 'k';
  if (state.castlingRights.bQ) castling += 'q';
  if (!castling) castling = '-';

  const ep = state.enPassantTarget || '-';

  return `${placement} ${state.turn} ${castling} ${ep} ${state.halfmoveClock} ${state.fullmoveNumber}`;
}
// Parse a FEN string into the same game-state shape used internally
// (the reverse of toFEN). Used to load a custom starting position,
// e.g. for a puzzle/level that does not start from the normal setup.
function parseFEN(fen) {
  const parts = String(fen).trim().split(/\s+/);
  const [placement, turnPart, castlingPart, epPart, halfmovePart, fullmovePart] = parts;

  if (!placement) throw new Error('FEN is missing the piece placement field');

  const board = Array.from({ length: 8 }, () => Array(8).fill(null));
  const ranks = placement.split('/');
  if (ranks.length !== 8) throw new Error(`FEN placement must have 8 ranks, got ${ranks.length}`);

  for (let i = 0; i < 8; i++) {
    const rank = 7 - i; // ranks[0] is rank 8 (board index 7), ranks[7] is rank 1 (board index 0)
    let file = 0;
    for (const ch of ranks[i]) {
      if (/[1-8]/.test(ch)) {
        file += parseInt(ch, 10);
      } else if (/[pnbrqkPNBRQK]/.test(ch)) {
        if (file > 7) throw new Error(`FEN rank ${8 - i} has too many squares`);
        const color = ch === ch.toUpperCase() ? 'w' : 'b';
        board[rank][file] = color + ch.toUpperCase();
        file++;
      } else {
        throw new Error(`FEN contains an invalid character: "${ch}"`);
      }
    }
    if (file !== 8) throw new Error(`FEN rank ${8 - i} does not add up to 8 squares`);
  }

  const castlingRights = { wK: false, wQ: false, bK: false, bQ: false };
  if (castlingPart && castlingPart !== '-') {
    for (const ch of castlingPart) {
      if (ch === 'K') castlingRights.wK = true;
      else if (ch === 'Q') castlingRights.wQ = true;
      else if (ch === 'k') castlingRights.bK = true;
      else if (ch === 'q') castlingRights.bQ = true;
      else throw new Error(`FEN contains an invalid castling flag: "${ch}"`);
    }
  }

  const state = {
    board,
    turn: turnPart === 'b' ? 'b' : 'w',
    castlingRights,
    enPassantTarget: epPart && epPart !== '-' ? epPart : null,
    halfmoveClock: halfmovePart !== undefined ? parseInt(halfmovePart, 10) || 0 : 0,
    fullmoveNumber: fullmovePart !== undefined ? parseInt(fullmovePart, 10) || 1 : 1,
    moveHistory: [],
    positionCounts: new Map(),
  };
  state.positionCounts.set(getPositionKey(state), 1);
  return state;
}

module.exports = {
  createGameState,
  createInitialBoard,
  squareToCoord,
  coordToSquare,
  pieceColor,
  pieceType,
  isKingInCheck,
  generateLegalMoves,
  findLegalMove,
  applyMove,
  getGameStatus,
  getPositionKey,
  toFEN,
  parseFEN,
};
