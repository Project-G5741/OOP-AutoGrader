(async () => {
  window.__gachaTest = {
    status: 'running',
    mascots: [],
    rolls: 0,
    batches: [],
    errors: [],
  };
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const buttons = () => [...document.querySelectorAll('button')];
  const findBtn = (re) =>
    buttons().find((b) => re.test((b.innerText || '').replace(/\s+/g, ' ')));

  for (let i = 0; i < 25; i++) {
    const add = findBtn(/\+10 Vé Roll/);
    if (add) add.click();
    await sleep(40);
  }
  for (let i = 0; i < 25; i++) {
    const add = findBtn(/\+1,000 Points|\+1,000 Points|\+1,000/);
    if (add) add.click();
    await sleep(40);
  }

  const collectMascots = () => {
    const names = [];
    for (const el of document.querySelectorAll('[role="dialog"] span')) {
      if (!(el.innerText || '').includes('ĐÃ MỞ KHÓA')) continue;
      const card = el.closest('div.relative') || el.parentElement?.parentElement;
      const title = card?.querySelector('span.block, span.font-bold');
      if (title && title.innerText) names.push(title.innerText.trim());
    }
    return names;
  };

  const waitForSummary = async () => {
    const start = Date.now();
    while (Date.now() - start < 20000) {
      const skip = findBtn(/Bỏ qua/);
      if (skip) skip.click();
      if (findBtn(/Quay Tiếp/)) return true;
      await sleep(120);
    }
    return false;
  };

  const first = findBtn(/Quay 10 Lần/);
  if (!first) {
    window.__gachaTest.status = 'no-10-button';
    return;
  }
  first.click();
  if (!(await waitForSummary())) {
    window.__gachaTest.status = 'fail-first-summary';
    return;
  }
  let got = collectMascots();
  window.__gachaTest.mascots.push(...got);
  window.__gachaTest.batches.push(got);
  window.__gachaTest.rolls += 10;

  for (let i = 1; i < 20; i++) {
    const again = findBtn(/Quay Tiếp/);
    if (!again) {
      window.__gachaTest.status = 'no-again-' + i;
      return;
    }
    again.click();
    if (!(await waitForSummary())) {
      window.__gachaTest.status = 'fail-summary-' + i;
      return;
    }
    got = collectMascots();
    window.__gachaTest.mascots.push(...got);
    window.__gachaTest.batches.push(got);
    window.__gachaTest.rolls += 10;
    window.__gachaTest.progress = i + 1;
  }
  window.__gachaTest.status = 'done';
})();
'started';
