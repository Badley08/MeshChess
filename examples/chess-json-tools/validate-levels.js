#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const engine = require('./chess-rules.js');

const SQUARE_RE = /^[a-h][1-8]$/;
const PROMO_VALUES = ['Q', 'R', 'B', 'N', null, undefined];

function fail(errors, msg) {
  errors.push(msg);
}

// Structural check for a level file — no engine involved yet, just shape.
function checkLevelShape(data, errors) {
  if (typeof data !== 'object' || data === null) {
    fail(errors, 'root value must be a JSON object');
    return;
  }
  for (const field of ['id', 'title', 'startingFen', 'solution']) {
    if (!(field in data)) fail(errors, `missing required field "${field}"`);
  }
  if (data.startingFen !== undefined && typeof data.startingFen !== 'string') {
    fail(errors, '"startingFen" must be a string');
  }
  if (data.solution !== undefined) {
    if (!Array.isArray(data.solution) || data.solution.length < 1) {
      fail(errors, '"solution" must be a non-empty array');
    } else {
      data.solution.forEach((m, i) => {
        if (typeof m !== 'object' || m === null) {
          fail(errors, `solution[${i}] must be an object`);
          return;
        }
        if (!SQUARE_RE.test(m.from)) fail(errors, `solution[${i}].from is not a valid square: ${JSON.stringify(m.from)}`);
        if (!SQUARE_RE.test(m.to)) fail(errors, `solution[${i}].to is not a valid square: ${JSON.stringify(m.to)}`);
        if (!PROMO_VALUES.includes(m.promotion)) fail(errors, `solution[${i}].promotion is invalid: ${JSON.stringify(m.promotion)}`);
      });
    }
  }
  if (data.difficulty !== undefined) {
    if (!Number.isInteger(data.difficulty) || data.difficulty < 1 || data.difficulty > 10) {
      fail(errors, '"difficulty" must be an integer from 1 to 10');
    }
  }
}

// Chess-legality check for a level file — replays startingFen + solution
// through the real engine, so a typo'd square or an illegal move fails
// loudly instead of shipping silently inside the app.
function checkLevelLegality(data, errors) {
  let state;
  try {
    state = engine.parseFEN(data.startingFen);
  } catch (e) {
    fail(errors, `startingFen is not a valid position: ${e.message}`);
    return;
  }

  if (!Array.isArray(data.solution)) return; // shape check already reported this

  data.solution.forEach((step, i) => {
    const legal = engine.findLegalMove(state, step.from, step.to, step.promotion);
    if (!legal) {
      fail(errors, `solution[${i}] (${step.from}-${step.to}) is not a legal move in the position at that point`);
      return; // stop replaying this level, later steps depend on this one
    }
    engine.applyMove(state, legal);
  });
}

// Structural + legality check for a game-export file.
function checkExport(data, errors) {
  if (typeof data !== 'object' || data === null) {
    fail(errors, 'root value must be a JSON object');
    return;
  }
  for (const field of ['exportDate', 'version', 'mode', 'players', 'startingFen', 'moves']) {
    if (!(field in data)) fail(errors, `missing required field "${field}"`);
  }
  if (data.mode !== undefined && !['wifi', 'bluetooth', 'ai', 'online'].includes(data.mode)) {
    fail(errors, `"mode" must be one of wifi/bluetooth/ai/online, got ${JSON.stringify(data.mode)}`);
  }
  if (!Array.isArray(data.moves)) {
    fail(errors, '"moves" must be an array');
    return;
  }

  let state;
  try {
    state = engine.parseFEN(data.startingFen);
  } catch (e) {
    fail(errors, `startingFen is not a valid position: ${e.message}`);
    return;
  }

  data.moves.forEach((mv, i) => {
    if (!SQUARE_RE.test(mv.from) || !SQUARE_RE.test(mv.to)) {
      fail(errors, `moves[${i}] has an invalid from/to square`);
      return;
    }
    const legal = engine.findLegalMove(state, mv.from, mv.to, mv.promotion);
    if (!legal) {
      fail(errors, `moves[${i}] (${mv.from}-${mv.to}) is not a legal move at that point in the game`);
      return;
    }
    engine.applyMove(state, legal);
  });
}

function detectKind(data) {
  if (data && Array.isArray(data.moves)) return 'export';
  if (data && Array.isArray(data.solution)) return 'level';
  return 'unknown';
}

function validateFile(filePath) {
  const raw = fs.readFileSync(filePath, 'utf8');
  let data;
  try {
    data = JSON.parse(raw);
  } catch (e) {
    return { file: filePath, ok: false, errors: [`invalid JSON: ${e.message}`] };
  }

  const errors = [];
  const kind = detectKind(data);
  if (kind === 'level') {
    checkLevelShape(data, errors);
    if (errors.length === 0) checkLevelLegality(data, errors);
  } else if (kind === 'export') {
    checkExport(data, errors);
  } else {
    errors.push('could not tell if this is a level (needs "solution") or an export (needs "moves")');
  }

  return { file: filePath, ok: errors.length === 0, errors };
}

function collectJsonFiles(inputPaths) {
  const files = [];
  for (const p of inputPaths) {
    const stat = fs.statSync(p);
    if (stat.isDirectory()) {
      for (const entry of fs.readdirSync(p)) {
        if (entry.endsWith('.json') && !entry.endsWith('.schema.json')) {
          files.push(path.join(p, entry));
        }
      }
    } else {
      files.push(p);
    }
  }
  return files;
}

function main() {
  const inputPaths = process.argv.slice(2);
  if (inputPaths.length === 0) {
    console.error('Usage: node validate-levels.js <file-or-directory> [...]');
    process.exit(1);
  }

  const files = collectJsonFiles(inputPaths);
  let anyFailed = false;

  for (const file of files) {
    const result = validateFile(file);
    if (result.ok) {
      console.log(`OK   ${file}`);
    } else {
      anyFailed = true;
      console.log(`FAIL ${file}`);
      for (const err of result.errors) console.log(`     - ${err}`);
    }
  }

  process.exit(anyFailed ? 1 : 0);
}

main();
