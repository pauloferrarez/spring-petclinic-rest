// Calcula as estatísticas de desempenho (NF-01 e NF-02) a partir do relatório JSON do Newman.
// Uso: node desempenho_estatisticas.js <newman.json> [limiteMs]
// Descarta a 1ª iteração (aquecimento – OBS-03) e calcula mínimo, média, mediana, p95 e máximo das demais.
const fs = require('fs');

const [file, limitArg] = process.argv.slice(2);
const limit = Number(limitArg || 500);
const run = JSON.parse(fs.readFileSync(file, 'utf8')).run;

const byRequest = {};
for (const execution of run.executions) {
  const name = execution.item.name;
  if (!name.startsWith('NF-')) continue;
  (byRequest[name] ||= []).push({ iteration: execution.cursor.iteration, ms: execution.response.responseTime, code: execution.response.code });
}

// percentil pelo método do posto mais próximo (nearest rank)
const percentile = (sorted, p) => sorted[Math.ceil((p / 100) * sorted.length) - 1];

let ok = true;
for (const [name, samples] of Object.entries(byRequest)) {
  const warmup = samples.filter(s => s.iteration === 0);
  const measured = samples.filter(s => s.iteration > 0);
  const times = measured.map(s => s.ms).sort((a, b) => a - b);
  const mean = times.reduce((a, b) => a + b, 0) / times.length;
  const p95 = percentile(times, 95);
  const pass = p95 <= limit;
  ok &&= pass;
  console.log(name);
  console.log(`  aquecimento (descartado): ${warmup.map(s => s.ms + ' ms').join(', ')}`);
  console.log(`  amostras: ${times.length} | mín ${times[0]} ms | média ${mean.toFixed(1)} ms | mediana ${percentile(times, 50)} ms | p95 ${p95} ms | máx ${times[times.length - 1]} ms`);
  console.log(`  status HTTP: ${[...new Set(measured.map(s => s.code))].join(', ')}`);
  console.log(`  critério p95 <= ${limit} ms: ${pass ? 'APROVADO' : 'REPROVADO'}`);
  console.log(`  tempos (ms, em ordem de execução): ${measured.map(s => s.ms).join(' ')}`);
}
process.exit(ok ? 0 : 1);
