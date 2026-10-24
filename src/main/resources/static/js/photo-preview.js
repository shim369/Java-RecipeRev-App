/*
 * RecipeRev: 写真を選んだら、送信前にプレビューを表示する。
 * URL.createObjectURL はファイルをサーバに送らずにブラウザ内で表示するための一時URLを作る。
 * サイズ超過もここで先に知らせる(最終的な検査はサーバ側で行う)。警告文は言語ごとに data-too-large 属性で受け取る。
 */
(function () {
  'use strict';
  const MAX_BYTES = 5 * 1024 * 1024;
  const input = document.querySelector('[data-photo-input]');
  const preview = document.querySelector('[data-photo-preview]');
  if (!input || !preview) return;

  input.addEventListener('change', () => {
    const file = input.files[0];
    if (preview.src) URL.revokeObjectURL(preview.src);
    if (!file) {
      preview.style.display = 'none';
      return;
    }
    if (file.size > MAX_BYTES) {
      window.alert(input.dataset.tooLarge);
      input.value = '';
      preview.style.display = 'none';
      return;
    }
    preview.src = URL.createObjectURL(file);
    preview.style.display = 'block';
  });
})();
