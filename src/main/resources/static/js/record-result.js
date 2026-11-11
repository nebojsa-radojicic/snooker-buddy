(() => {
    const form = document.querySelector('#record-result-form');
    const summaryMode = form.querySelector('#summary-mode');
    const attemptMode = form.querySelector('#attempt-mode');
    const summaryCounts = form.querySelector('#summary-counts');
    const attemptCounts = form.querySelector('#attempt-counts');
    const tally = form.querySelector('#attempt-tally');
    const totalInput = form.querySelector('#attempt-total');
    const successfulInput = form.querySelector('#attempt-successful');
    const save = form.querySelector('button[type="submit"]');
    let total = 0;
    let successful = 0;

    function selectMode(byAttempt) {
        summaryCounts.hidden = summaryCounts.disabled = byAttempt;
        attemptCounts.hidden = attemptCounts.disabled = !byAttempt;
        summaryMode.setAttribute('aria-pressed', String(!byAttempt));
        attemptMode.setAttribute('aria-pressed', String(byAttempt));
        summaryMode.classList.toggle('btn-green', !byAttempt);
        summaryMode.classList.toggle('btn-light', byAttempt);
        attemptMode.classList.toggle('btn-green', byAttempt);
        attemptMode.classList.toggle('btn-light', !byAttempt);
        save.disabled = byAttempt && total === 0;
    }

    function registerAttempt(wasSuccessful) {
        total += 1;
        if (wasSuccessful) successful += 1;
        totalInput.value = String(total);
        successfulInput.value = String(successful);
        tally.textContent = `${successful} / ${total}`;
        save.disabled = false;
    }

    summaryMode.addEventListener('click', () => selectMode(false));
    attemptMode.addEventListener('click', () => selectMode(true));
    form.querySelector('#successful-attempt').addEventListener('click', () => registerAttempt(true));
    form.querySelector('#missed-attempt').addEventListener('click', () => registerAttempt(false));
    form.addEventListener('submit', event => {
        if (!attemptCounts.disabled && total === 0) event.preventDefault();
    });
    form.querySelector('#recording-modes').hidden = false;
})();
