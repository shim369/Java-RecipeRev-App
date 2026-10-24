/*
 * RecipeRev: 材料行の追加・削除(素のJavaScript)。
 * Spring は ingredients[0].name, ingredients[1].name ... という連番の name を List に詰め直すので、
 * 行を消すと番号に「穴」が空いてしまう。そこで送信直前に全行の番号を振り直す。
 */
(function () {
  'use strict';

  const table = document.querySelector('[data-ingredient-table]');
  if (!table) return;

  const tbody = table.querySelector('tbody');
  const template = document.querySelector('[data-ingredient-template]');
  const addButton = document.querySelector('[data-add-row]');

  function renumber() {
    tbody.querySelectorAll('[data-ingredient-row]').forEach((row, index) => {
      row.querySelectorAll('input').forEach((input) => {
        input.name = input.name.replace(/ingredients\[(\d+|__INDEX__)\]/, 'ingredients[' + index + ']');
        input.removeAttribute('id'); // th:field が付けた id は番号がずれるので外す(label とは aria-label で対応)
      });
    });
  }

  addButton.addEventListener('click', () => {
    const row = template.content.firstElementChild.cloneNode(true);
    tbody.appendChild(row);
    renumber();
    row.querySelector('input').focus();
  });

  // 行ごとにイベントを付けず、tbody で受け取る(イベント委譲)。後から追加した行にも効く
  tbody.addEventListener('click', (event) => {
    const button = event.target.closest('[data-remove-row]');
    if (!button) return;
    const rows = tbody.querySelectorAll('[data-ingredient-row]');
    if (rows.length <= 1) {
      // 最後の1行は消さずに中身だけ空にする(材料は1行以上必須)
      rows[0].querySelectorAll('input').forEach((input) => { input.value = ''; });
      return;
    }
    button.closest('[data-ingredient-row]').remove();
    renumber();
  });

  table.closest('form').addEventListener('submit', renumber);
})();
