(() => {
    const path = location.pathname.startsWith('/players') ? '/players' : '/training';
    document.querySelector(`nav a[href="${path}"]`)?.setAttribute('aria-current', 'page');
    const custom = document.querySelector('#customTargets');
    const targets = document.querySelector('#custom-targets');
    if (custom && targets) {
        const update = () => {
            targets.hidden = !custom.checked;
            custom.setAttribute('aria-expanded', String(custom.checked));
            targets.querySelectorAll('input').forEach(input => { input.required = custom.checked; });
        };
        custom.addEventListener('change', update);
        update();
    }

    document.querySelectorAll('[data-assignment-exercise]').forEach(exercise => {
        const selected = exercise.querySelector('input[type="checkbox"]');
        const criterion = exercise.querySelector('details');
        const input = criterion.querySelector('textarea');
        const update = () => {
            criterion.hidden = !selected.checked;
            input.disabled = !selected.checked;
        };
        selected.addEventListener('change', update);
        update();
    });
})();
