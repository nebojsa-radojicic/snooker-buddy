(() => {
    // HTMX can execute page scripts again when restoring a history snapshot.
    if (window.snookerBuddyExercisesInitialized) return;
    window.snookerBuddyExercisesInitialized = true;
    let exerciseView = 'grid';
    function applyExerciseView() {
        document.querySelector('.exercise-grid')?.classList.toggle('list-mode', exerciseView === 'list');
        document.querySelectorAll('[data-exercise-view]').forEach(button => {
            const active = button.dataset.exerciseView === exerciseView;
            button.classList.toggle('active', active);
            button.setAttribute('aria-pressed', String(active));
        });
    }
    document.addEventListener('click', event => {
        const button = event.target.closest('[data-exercise-view]');
        if (!button) return;
        exerciseView = button.dataset.exerciseView;
        applyExerciseView();
    });
    document.addEventListener('htmx:afterSwap', applyExerciseView);
    document.addEventListener('htmx:historyRestore', applyExerciseView);
    document.querySelector('nav a[href="/exercises"]')?.setAttribute('aria-current', 'page');
    const form = document.querySelector('#exercise-form');
    if (!form) return;
    const input = form.querySelector('#drawing');
    const preview = form.querySelector('#drawing-preview');
    const status = form.querySelector('#drawing-status');
    const save = form.querySelector('#save-exercise');
    let request = 0;
    let previewUrl;
    input.addEventListener('change', async () => {
        const current = ++request;
        const file = input.files[0];
        input.setCustomValidity('');
        preview.hidden = true;
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        status.textContent = '';
        if (!file) { save.disabled = false; return; }
        save.disabled = true;
        try {
            if (file.size > 20 * 1024 * 1024) throw Error('Choose a photo up to 20 MB.');
            if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) throw Error('Choose a JPG, PNG or WebP photo.');
            status.textContent = 'Preparing your drawing…';
            const bitmap = await createImageBitmap(file);
            let blob;
            try {
                if (bitmap.width * bitmap.height > 60000000) throw Error('Choose an image under 60 megapixels.');
                const scale = Math.min(1, 1600 / Math.max(bitmap.width, bitmap.height));
                const canvas = document.createElement('canvas');
                canvas.width = Math.max(1, Math.round(bitmap.width * scale));
                canvas.height = Math.max(1, Math.round(bitmap.height * scale));
                const context = canvas.getContext('2d');
                context.fillStyle = '#fff';
                context.fillRect(0, 0, canvas.width, canvas.height);
                context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
                blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9));
                if (!blob) throw Error('This drawing could not be prepared. Choose another photo.');
            } finally { bitmap.close(); }
            if (current !== request) return;
            const transfer = new DataTransfer();
            transfer.items.add(new File([blob], 'drawing.jpg', {type: 'image/jpeg'}));
            input.files = transfer.files;
            previewUrl = URL.createObjectURL(blob);
            preview.src = previewUrl;
            preview.hidden = false;
            status.textContent = 'Drawing ready to save.';
        } catch (error) {
            if (current !== request) return;
            input.value = '';
            input.setCustomValidity(error.message || 'This photo could not be read. Choose another.');
            status.textContent = input.validationMessage;
        } finally { if (current === request) save.disabled = false; }
    });
})();
