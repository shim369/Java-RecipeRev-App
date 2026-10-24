/*
 * RecipeRev: data-confirm 属性を持つフォームは、送信前に確認ダイアログを出す。
 * 画面ごとに onclick を書かず、属性を付けるだけで使えるようにしている。
 */
document.addEventListener('submit', (event) => {
  const form = event.target.closest('form[data-confirm]');
  if (form && !window.confirm(form.dataset.confirm)) {
    event.preventDefault();
  }
});
