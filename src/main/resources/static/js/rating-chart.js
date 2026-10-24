/*
 * RecipeRev: 評価推移グラフ(F-10)。
 * JSON API から版ごとの平均評価を取得し、Chart.js の折れ線グラフで描く。
 * 調理ログがない版は null になり、spanGaps: false で線を切って表示する。決定版の点は金色で大きくする。
 */
(function () {
  'use strict';
  const canvas = document.getElementById('rating-chart');
  if (!canvas || typeof Chart === 'undefined') return;

  const FINAL_COLOR = '#b8860b';
  const LINE_COLOR = '#c2562a';

  fetch(canvas.dataset.url, { headers: { Accept: 'application/json' } })
    .then((res) => {
      if (!res.ok) throw new Error('HTTP ' + res.status);
      return res.json();
    })
    .then((points) => {
      if (!points.some((p) => p.avgRating !== null)) {
        canvas.closest('.chart-box').innerHTML = '<p class="muted">まだ調理ログがありません。版詳細から調理ログを登録するとグラフが表示されます。</p>';
        return;
      }
      const isFinal = points.map((p) => p.status === 'FINAL');
      new Chart(canvas, {
        type: 'line',
        data: {
          labels: points.map((p) => '第' + p.versionNo + '版'),
          datasets: [{
            label: '平均評価',
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
                  return '調理回数: ' + p.logCount + (p.status === 'FINAL' ? '(決定版)' : '');
                },
              },
            },
          },
        },
      });
    })
    .catch((err) => {
      console.error('[RecipeRev] rating chart failed', err);
      canvas.closest('.chart-box').innerHTML = '<p class="muted">グラフを読み込めませんでした。</p>';
    });
})();
