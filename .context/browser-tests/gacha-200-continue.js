(async () => {
  const prev = window.__gachaTest || {
    mascots: [],
    rolls: 0,
    batches: [],
    errors: [],
  };
  window.__gachaTest = {
    ...prev,
    status: 'running',
    errors: prev.errors || [],
  };
  const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
  const buttons = () => [...document.querySelectorAll('button')];
  const findBtn = (re) =>
    buttons().find((b) => re.test((b.innerText || '').replace(/\s+/g, ' ')));

  const collectMascots = () => {
    const names = [];
    const dialog = document.querySelector('[role="dialog"]');
    if (!dialog) return names;
    for (const badge of dialog.querySelectorAll('span')) {
      const t = (badge.innerText || '').trim();
      if (!/MASCOT/i.test(t) && t !== 'Legendary Mascot' && t !== 'Đá Tinh Thể') continue;
      if (!/MASCOT/i.test(t)) continue;
      const card = badge.closest('div.relative') || badge.parentElement;
      const title = card?.querySelector('span.block');
      if (title) names.push(title.innerText.trim());
    }
    return names;
  };

  const waitForNextSummary = async () => {
    const start = Date.now();
    let sawCharging = false;
    while (Date.now() - start < 20000) {
      const skip = findBtn(/Bỏ qua/);
      if (skip) skip.click();
      const again = findBtn(/Quay Tiếp/);
      const charging = /hội tụ năng lượng|HÀO QUANG|PHẦN THƯỞNG|ÁNH SÁNG/i.test(
        document.body.innerText
      );
      if (charging || !again) sawCharging = true;
      if (sawCharging && again) return true;
      await sleep(100);
    }
    return !!findBtn(/Quay Tiếp/);
  };

  const remaining = Math.max(0, 20 - Math.floor((window.__gachaTest.rolls || 0) / 10));
  for (let i = 0; i < remaining; i++) {
    const add = findBtn(/\+10 Vé Roll/);
    if (add) add.click();
    await sleep(80);
    const again = findBtn(/Quay Tiếp/) || findBtn(/Quay 10 Lần/);
    if (!again) {
      window.__gachaTest.status = 'no-button-' + i;
      return;
    }
    again.click();
    if (!(await waitForNextSummary())) {
      window.__gachaTest.status = 'timeout-' + i;
      return;
    }
    const got = collectMascots();
    window.__gachaTest.mascots.push(...got);
    window.__gachaTest.batches.push(got);
    window.__gachaTest.rolls += 10;
    window.__gachaTest.progress = window.__gachaTest.rolls;
  }
  window.__gachaTest.status = 'done';
})();
'started';
