const Help = (() => {
    let sections = [];
    let activeSlug = null;
    let searchDebounce = null;
    let searchQuery = '';

    async function init() {
        await loadSections();
        bindSearch();

        const hash = window.location.hash.replace('#', '');
        if (hash) {
            loadSection(hash);
        } else if (sections.length > 0) {
            loadSection(sections[0].slug);
        }
    }

    async function loadSections() {
        try {
            sections = await API.get('/help');
            renderSidebar(sections);
        } catch (e) {
            document.getElementById('sectionList').innerHTML =
                '<div class="text-danger p-3 small">Failed to load documentation index.</div>';
        }
    }

    function renderSidebar(items, isSearchResults = false) {
        const list = document.getElementById('sectionList');

        if (items.length === 0) {
            list.innerHTML = '<div class="no-results">No sections found.</div>';
            return;
        }

        list.innerHTML = items.map(item => {
            const isActive = item.slug === activeSlug;
            const excerpt = item.excerpt
                ? `<div class="search-result-excerpt">${escapeHtml(item.excerpt)}</div>`
                : '';
            const badge = item.matchCount
                ? `<span class="badge bg-secondary match-badge">${item.matchCount}</span>`
                : '';
            return `
                <a class="section-item${isActive ? ' active' : ''}"
                   href="#${item.slug}"
                   onclick="Help.selectSection('${item.slug}'); return false;">
                    ${!isSearchResults ? `<span class="section-num">${slugToNum(item.slug)}</span>` : ''}
                    <div style="flex:1; min-width:0;">
                        <div class="section-title">${escapeHtml(item.title)}</div>
                        ${excerpt}
                    </div>
                    ${badge}
                </a>`;
        }).join('');
    }

    function slugToNum(slug) {
        const match = slug.match(/^(\d+)/);
        return match ? match[1] : '';
    }

    function bindSearch() {
        const input = document.getElementById('helpSearch');
        const clearBtn = document.getElementById('clearSearch');

        input.addEventListener('input', () => {
            const q = input.value.trim();
            clearBtn.style.display = q ? 'block' : 'none';
            clearTimeout(searchDebounce);

            if (!q) {
                searchQuery = '';
                document.getElementById('searchStatus').textContent = '';
                renderSidebar(sections);
                if (activeSlug) {
                    highlightInContent('');
                }
                return;
            }

            // Instant title filter
            const filtered = sections.filter(s =>
                s.title.toLowerCase().includes(q.toLowerCase())
            );
            renderSidebar(filtered);

            searchDebounce = setTimeout(() => doSearch(q), 300);
        });

        clearBtn.addEventListener('click', () => {
            input.value = '';
            clearBtn.style.display = 'none';
            searchQuery = '';
            document.getElementById('searchStatus').textContent = '';
            renderSidebar(sections);
            if (activeSlug) highlightInContent('');
        });
    }

    async function doSearch(q) {
        searchQuery = q;
        document.getElementById('searchStatus').textContent = 'Searching...';
        try {
            const results = await API.get(`/help/search?q=${encodeURIComponent(q)}`);
            if (document.getElementById('helpSearch').value.trim() !== q) return;

            const status = document.getElementById('searchStatus');
            if (results.length === 0) {
                status.textContent = 'No results found.';
                renderSidebar([]);
            } else {
                status.textContent = `${results.length} section${results.length !== 1 ? 's' : ''} found`;
                renderSidebar(results, true);
                if (activeSlug) highlightInContent(q);
            }
        } catch (e) {
            document.getElementById('searchStatus').textContent = 'Search failed.';
        }
    }

    async function selectSection(slug) {
        window.location.hash = slug;
        await loadSection(slug);
    }

    async function loadSection(slug) {
        activeSlug = slug;
        updateActiveSidebarItem(slug);

        const area = document.getElementById('contentArea');
        area.innerHTML = `
            <div class="text-center py-5 text-muted">
                <div class="spinner-border text-secondary" role="status"></div>
                <p class="mt-2">Loading...</p>
            </div>`;

        try {
            const data = await API.get(`/help/${slug}`);
            area.innerHTML = `<div class="markdown-body">${data.html}</div>`;
            area.scrollTop = 0;

            if (searchQuery) {
                highlightInContent(searchQuery);
            }
        } catch (e) {
            area.innerHTML = `<div class="alert alert-danger m-3">Failed to load section.</div>`;
        }
    }

    function updateActiveSidebarItem(slug) {
        document.querySelectorAll('.section-item').forEach(el => {
            el.classList.toggle('active', el.getAttribute('href') === `#${slug}`);
        });
    }

    function highlightInContent(query) {
        const area = document.getElementById('contentArea');
        if (!query) {
            // Remove existing highlights
            area.querySelectorAll('mark[data-search]').forEach(mark => {
                mark.replaceWith(document.createTextNode(mark.textContent));
            });
            return;
        }
        highlightTextNodes(area, query);
    }

    function highlightTextNodes(node, query) {
        if (node.nodeType === Node.TEXT_NODE) {
            const text = node.textContent;
            const lower = text.toLowerCase();
            const qLower = query.toLowerCase();
            const idx = lower.indexOf(qLower);
            if (idx === -1) return;

            const fragment = document.createDocumentFragment();
            let last = 0;
            let pos = idx;
            while (pos !== -1) {
                fragment.appendChild(document.createTextNode(text.substring(last, pos)));
                const mark = document.createElement('mark');
                mark.setAttribute('data-search', '1');
                mark.textContent = text.substring(pos, pos + query.length);
                fragment.appendChild(mark);
                last = pos + query.length;
                pos = lower.indexOf(qLower, last);
            }
            fragment.appendChild(document.createTextNode(text.substring(last)));
            node.parentNode.replaceChild(fragment, node);
            return;
        }
        if (node.nodeType === Node.ELEMENT_NODE && !['SCRIPT', 'STYLE', 'CODE', 'PRE'].includes(node.tagName)) {
            Array.from(node.childNodes).forEach(child => highlightTextNodes(child, query));
        }
    }

    function escapeHtml(str) {
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    return { init, selectSection };
})();

document.addEventListener('DOMContentLoaded', () => Help.init());
