// Testes das contas do app web (mesmos valores dos testes em Kotlin). Uso: node --test web-tests/
import test from "node:test";
import assert from "node:assert/strict";
import { rankFor, promoteCount, demoteCount, zoneFor, settleDelta, duelDelta, canDuel, seasonOf, analyzeBoard, SEASON_EPOCH_DAY } from "../web/js/lib/rankedRules.js";
import { streak, weekStreak, weeklyCount, weekStart, weekdayOf, weeklyBuckets } from "../web/js/lib/dashboard.js";
import { fmtKg, parseNum, fmtMinutes, fmtClock, ago } from "../web/js/lib/format.js";

test("elos: fronteiras de divisão e Lenda", () => {
  assert.equal(rankFor(0).label, "Ferro III");
  assert.equal(rankFor(99).label, "Ferro III");
  assert.equal(rankFor(100).label, "Ferro II");
  assert.equal(rankFor(299).label, "Ferro I");
  assert.equal(rankFor(300).label, "Bronze III");
  assert.equal(rankFor(1799).label, "Diamante I");
  assert.equal(rankFor(1800).label, "Lenda");
  assert.equal(rankFor(-5).rp, 0);
  assert.equal(rankFor(250).rpToNext, 50);
});

test("zonas: 20% sobem e 20% descem com contas inteiras", () => {
  assert.equal(promoteCount(4), 0);
  assert.equal(promoteCount(5), 1);
  assert.equal(promoteCount(10), 2);
  assert.equal(promoteCount(11), 3);
  assert.equal(demoteCount(7), 2);
  assert.equal(zoneFor(1, 10, 50), "PROMOTE");
  assert.equal(zoneFor(10, 10, 50), "DEMOTE");
  assert.equal(zoneFor(5, 10, 50), "SAFE");
  assert.equal(zoneFor(1, 10, 0), "DEMOTE", "sem pontos sempre cai");
});

test("fechamento da semana", () => {
  assert.equal(settleDelta(1, 10, 300), 40);
  assert.equal(settleDelta(2, 10, 200), 30);
  assert.equal(settleDelta(5, 10, 100), 10);
  assert.equal(settleDelta(7, 10, 100), -10);
  assert.equal(settleDelta(10, 10, 50), -25);
  assert.equal(settleDelta(3, 10, 0), -35);
  assert.equal(settleDelta(1, 3, 120), 10, "turma pequena");
  assert.equal(settleDelta(2, 3, 80), -20);
});

test("x1 estilo Elo", () => {
  assert.equal(duelDelta(500, 500, 100, 50), 12);
  assert.equal(duelDelta(500, 500, 50, 100), -12);
  assert.equal(duelDelta(500, 500, 70, 70), 0);
  assert.equal(duelDelta(500, 500, 0, 0), 0);
  assert.equal(duelDelta(500, 500, 100, 50, false), 0);
  assert.ok(duelDelta(300, 700, 100, 50) > 12, "zebra vale mais");
  assert.ok(Math.abs(duelDelta(0, 2000, 10, 5)) <= 35);
  assert.ok(canDuel(10, 250));
  assert.ok(!canDuel(10, 400));
});

test("temporadas de 8 semanas", () => {
  assert.deepEqual(seasonOf(SEASON_EPOCH_DAY), { number: 1, weekInSeason: 1, isLastWeek: false });
  assert.deepEqual(seasonOf(SEASON_EPOCH_DAY + 7 * 7), { number: 1, weekInSeason: 8, isLastWeek: true });
  assert.deepEqual(seasonOf(SEASON_EPOCH_DAY + 7 * 8), { number: 2, weekInSeason: 1, isLastWeek: false });
  assert.equal(weekdayOf(SEASON_EPOCH_DAY), 0, "26/10/2026 é segunda");
});

test("análise da turma: o que falta para subir/sair da queda", () => {
  const a = analyzeBoard([300, 200, 150, 90, 0], 150);
  assert.equal(a.position, 3);
  assert.equal(a.zone, "SAFE");
  assert.equal(a.toPromotion, 150);
  const b = analyzeBoard([300, 200, 150, 90, 0], 0);
  assert.equal(b.zone, "DEMOTE");
  assert.ok(b.toSafety >= 100);
  assert.equal(b.projectedDelta, -35);
});

test("home: sequência de dias, semanas e contagem", () => {
  const today = SEASON_EPOCH_DAY + 2; // quarta
  assert.equal(weekStart(today), SEASON_EPOCH_DAY);
  assert.equal(streak([today, today - 1, today - 2], today), 3);
  assert.equal(streak([today - 1, today - 2], today), 2, "ontem ainda conta");
  assert.equal(streak([today - 2], today), 0);
  assert.equal(streak([today, today, today - 1], today), 2, "dias repetidos contam uma vez");
  assert.equal(weeklyCount([today, today - 1, today - 3], today), 2);
  const w = SEASON_EPOCH_DAY;
  assert.equal(weekStreak([w, w + 1, w - 7, w - 6, w - 14], today, 2), 2, "semana atual bateu a meta, mais a anterior");
  assert.equal(weekStreak([w, w + 1, w - 7, w - 6], today, 2), 2);
  assert.equal(weeklyBuckets([w], today, 3).map((b) => b.count).join(), "0,0,1");
});

test("formatação em português", () => {
  assert.equal(fmtKg(40), "40");
  assert.equal(fmtKg(42.5), "42,5");
  assert.equal(parseNum("42,5"), 42.5);
  assert.ok(isNaN(parseNum("abc")));
  assert.equal(fmtMinutes(45), "45 min");
  assert.equal(fmtMinutes(62), "1 h 02 min");
  assert.equal(fmtMinutes(120), "2 h");
  assert.equal(fmtClock(83), "01:23");
  assert.equal(ago(new Date(Date.now() - 5 * 60000).toISOString()), "5 min");
});
