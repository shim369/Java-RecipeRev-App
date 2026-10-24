/*
 * RecipeRev: 評価推移グラフ(F-10)。
 * JSON API から版ごとの平均評価を取得し、Chart.js の折れ線グラフで描く。
 * 調理ログがない版は null になり、spanGaps: false で線を切って表示する。決定版の点は金色で大きくする。
 * 画面の文言は言語ごとに canvas の data-label-* 属性で受け取る(JS に日本語・英語を直書きしない)。
 */
(function () {
  'use strict';
  const canvas = document.getElementById('rating-chart');
  if (!canvas || typeof Chart === 'undefined') return;

  const FINAL_COLOR = '#b8860b';
  const LINE_COLOR = '#c2562a';
  const label = canvas.dataset;
  const format = (template, value) => template.replace('{0}', value);

  function showMessage(text) {
    const p = document.createElement('p');
    p.className = 'muted';
    p.textContent = text; // textContent なので HTML として解釈されない(XSS対策)
    canvas.closest('.chart-box').replaceChildren(p);
  }

  fetch(label.url, { headers: { Accept: 'application/json' } })
    .then((res) => {
      if (!res.ok) throw new Error('HTTP ' + res.status);
      return res.json();
    })
    .then((points) => {
      if (!points.some((p) => p.avgRating !== null)) {
        showMessage(label.labelEmpty);
        return;
      }
      const isFinal = points.map((p) => p.status === 'FINAL');
      new Chart(canvas, {
        type: 'line',
        data: {
          labels: points.map((p) => format(label.labelVersion, p.versionNo)),
          datasets: [{
            label: label.labelAvg,
            data: points.map((p) => p.avgRating),
            borderColor: LINE_COLOR,
            backgroundColor: LINE_COLOR,
            pointBackgroundColor: isFinal.map((f) => (f ? FINAL_COLOR : LINE_COLOR)),
            pointRadius: isFinal.map((f) => (f ? 8 : 4)),
            spanGaps: false,
            tension: 0.2,
          }],
        },
        options: {
          maintainAspectRatio: false,
          scales: { y: { min: 1, max: 5, ticks: { stepSize: 1 } } },
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                afterLabel: (ctx) => {
                  const p = points[ctx.dataIndex];
                  return format(label.labelCooked, p.logCount) + (p.status === 'FINAL' ? ' ' + label.labelFinal : '');
                },
              },
            },
          },
        },
      });
    })
    .catch((err) => {
      console.error('[RecipeRev] rating chart failed', err);
      showMessage(label.labelError);
    });
})();
