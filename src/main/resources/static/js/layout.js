(() => {
    const sidebar = document.getElementById('sidebar');
    const backdrop = document.getElementById('menu-backdrop');
    const navigationButton = document.getElementById('navigation-button');
    const profile = document.getElementById('user-profile');
    const profileButton = document.getElementById('user-menu-button');
    const dropdown = document.getElementById('user-dropdown');
    const mobile = window.matchMedia('(max-width: 767px)');
    let navigationOpen = false;

    function setNavigation(open) {
        navigationOpen = mobile.matches && open;
        sidebar.classList.toggle('open', navigationOpen);
        sidebar.inert = mobile.matches && !navigationOpen;
        backdrop.hidden = !navigationOpen;
        navigationButton.setAttribute('aria-expanded', String(navigationOpen));
        document.body.style.overflow = navigationOpen ? 'hidden' : '';
    }

    function setProfile(open) {
        if (!dropdown || !profileButton) return;
        dropdown.hidden = !open;
        profileButton.setAttribute('aria-expanded', String(open));
    }

    navigationButton.addEventListener('click', () => setNavigation(!navigationOpen));
    backdrop.addEventListener('click', () => setNavigation(false));
    sidebar.addEventListener('click', event => {
        if (event.target.closest('a')) setNavigation(false);
    });
    mobile.addEventListener('change', () => setNavigation(false));
    profileButton?.addEventListener('click', () => setProfile(dropdown.hidden));
    document.addEventListener('click', event => {
        if (profile && !profile.contains(event.target)) setProfile(false);
        const dismiss = event.target.closest('[data-dismiss-notice]');
        if (dismiss) dismiss.closest('.notice')?.remove();
    });
    document.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;
        if (navigationOpen) {
            setNavigation(false);
            navigationButton.focus();
        }
        if (dropdown && !dropdown.hidden) {
            setProfile(false);
            profileButton.focus();
        }
    });
    profile?.addEventListener('focusout', event => {
        if (event.relatedTarget && !profile.contains(event.relatedTarget)) setProfile(false);
    });
    setNavigation(false);
})();
