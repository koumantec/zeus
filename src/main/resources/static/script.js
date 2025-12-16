document.addEventListener('DOMContentLoaded', () => {
    /* --------------------------
       DASHBOARD LOGIC
       -------------------------- */
    const grid = document.getElementById('container-grid');
    const refreshBtn = document.getElementById('refresh-btn');
    const wizard = document.getElementById('setup-wizard');
    const wizardForm = document.getElementById('wizard-form');
    const settingsBtn = document.getElementById('settings-btn');
    const closeWizardBtn = document.getElementById('close-wizard');

    // Startup Check
    checkConfig();

    async function checkConfig() {
        try {
            const res = await fetch('/api/config/status');
            const data = await res.json();

            if (!data.configured) {
                // Not configured: Show Wizard, Hide Tabs & Main Content for simplicity or overlay them
                // We use overlay, so just remove 'hidden'
                wizard.classList.remove('hidden');
                closeWizardBtn.style.display = 'none'; // Cannot close initial wizard
            } else {
                fetchData();
                initForm();
            }
        } catch (e) {
            console.error("Config check failed", e);
        }
    }

    // Settings Button
    settingsBtn.addEventListener('click', async () => {
        // Load current values
        try {
            const res = await fetch('/api/config');
            const data = await res.json();

            if (data.gitLogin) document.querySelector('[name="gitLogin"]').value = data.gitLogin;
            if (data.gitPassword) document.querySelector('[name="gitPassword"]').value = data.gitPassword;
            if (data.harborLogin) document.querySelector('[name="harborLogin"]').value = data.harborLogin;
            if (data.harborPassword) document.querySelector('[name="harborPassword"]').value = data.harborPassword;

            closeWizardBtn.style.display = 'inline-block';
            wizard.classList.remove('hidden');
        } catch (e) {
            console.error("Failed to load settings", e);
        }
    });

    closeWizardBtn.addEventListener('click', () => {
        wizard.classList.add('hidden');
    });

    // Wizard Submit
    wizardForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const formData = new FormData(wizardForm);
        const data = Object.fromEntries(formData.entries());

        try {
            const res = await fetch('/api/config', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (res.ok) {
                alert('Configuration sauvegardée !');
                wizard.classList.add('hidden');
                // If this was first run, we might need to kickstart things
                if (!document.getElementById('config').classList.contains('active') && !document.getElementById('monitor').classList.contains('active')) {
                    // Just reload to be safe or init
                    window.location.reload();
                } else {
                    // Update done
                }

                // Ensure form and grid are initialized if not already
                if (grid.innerHTML.trim() === '') {
                    fetchData();
                    initForm();
                }
            } else {
                alert('Erreur lors de la sauvegarde.');
            }
        } catch (e) {
            console.error(e);
            alert('Erreur réseau.');
        }
    });

    /* --------------------------
       TAB LOGIC
       -------------------------- */
    const tabs = document.querySelectorAll('.tab-btn');
    const contents = document.querySelectorAll('.tab-content');

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            // Remove active class
            tabs.forEach(t => t.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));

            // Add active class
            tab.classList.add('active');
            document.getElementById(tab.dataset.tab).classList.add('active');
        });
    });

    async function fetchData() {
        try {
            if (grid.children.length === 0) {
                grid.innerHTML = `
                    <div class="loading-state">
                        <div class="spinner"></div>
                        Fetching container status...
                    </div>
                 `;
            } else {
                refreshBtn.classList.add('loading');
                refreshBtn.innerHTML = `
                    <div class="spinner" style="width: 16px; height: 16px; margin: 0; border-width: 2px;"></div>
                    Refreshing...
                `;
            }

            const response = await fetch('/api/containers');
            const data = await response.json();
            renderCards(data);

        } catch (error) {
            console.error('Error fetching data:', error);
            grid.innerHTML = `
                <div class="loading-state" style="color: var(--error-color)">
                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-bottom: 1rem">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="12"></line>
                        <line x1="12" y1="16" x2="12.01" y2="16"></line>
                    </svg>
                    Failed to load data. Use 'DEMO_MODE=true' to test.
                </div>
            `;
        } finally {
            refreshBtn.classList.remove('loading');
            refreshBtn.innerHTML = `
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M23 4v6h-6"></path>
                    <path d="M1 20v-6h6"></path>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 1 8.51 15"></path>
                </svg>
                Refresh
            `;
        }
    }

    function renderCards(containers) {
        if (!containers || containers.length === 0) {
            grid.innerHTML = `
                <div class="loading-state">
                    No containers found on the remote host.
                </div>
            `;
            return;
        }
        grid.innerHTML = containers.map(container => createCard(container)).join('');
    }

    function createCard(container) {
        const isUp = container.state.toLowerCase().includes('up');
        const statusClass = isUp ? 'status-up' : 'status-exit';
        const command = container.command.length > 50 ? container.command.substring(0, 47) + '...' : container.command;

        return `
            <div class="card">
                <div class="card-header">
                    <div>
                        <h3 class="container-name">${container.name}</h3>
                        <div class="container-image">ID/Img: ${container.image || 'N/A'}</div> 
                    </div>
                    <span class="status-badge ${statusClass}">${container.state}</span>
                </div>
                
                <div class="card-body">
                    <div class="info-row">
                        <span class="info-label">Command</span>
                        <span class="info-value" title="${container.command}">${command}</span>
                    </div>
                    
                    <div class="info-row">
                        <span class="info-label">Ports</span>
                        <span class="info-value">${container.ports || 'None'}</span>
                    </div>

                    ${container.status ? `
                    <div class="info-row">
                        <span class="info-label">Uptime/Status</span>
                        <span class="info-value">${container.status}</span>
                    </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    /* --------------------------
       FORM LOGIC
       -------------------------- */
    const formContainer = document.getElementById('configuration-form');

    // Converted YAML Configuration based on User Decision Tree
    const formConfig = {
        titre: "Composition de la stack",
        description: "Veuillez choisir vos préférences.",
        champs: [
            {
                nom: "community",
                type: "select",
                label: "Communauté",
                obligatoire: true,
                options: [
                    { label: "FR", valeur: "FR" },
                    { label: "BE", valeur: "BE" }
                ],
                condition: []
            },
            // FR Branch - Level 2: Plateforme
            {
                nom: "plateforme",
                type: "select",
                label: "Plateforme",
                obligatoire: true,
                condition: [
                    { champ: "community", valeur: "FR", action: "afficher" }
                ],
                options: [
                    { label: "ACore", valeur: "ACore" },
                    { label: "Core", valeur: "Core" },
                    { label: "ACore et Core", valeur: "ACore_et_Core" }
                ]
            },
            // FR Branch - Level 3: Composants (Dependent on Platform)
            {
                nom: "composants_acore",
                type: "select",
                label: "Composants (ACore)",
                obligatoire: true,
                condition: [
                    { champ: "plateforme", valeur: "ACore", action: "afficher" }
                ],
                options: [
                    { label: "IHM", valeur: "IHM" },
                    { label: "FLUX", valeur: "FLUX" },
                    { label: "ACore Complet", valeur: "ACore_Complet" }
                ]
            },
            {
                nom: "composants_core",
                type: "select",
                label: "Composants (Core)",
                obligatoire: true,
                condition: [
                    { champ: "plateforme", valeur: "Core", action: "afficher" }
                ],
                options: [
                    { label: "IHM", valeur: "IHM" },
                    { label: "FLUX", valeur: "FLUX" },
                    { label: "Core Complet", valeur: "Core_Complet" }
                ]
            },
            {
                nom: "composants_both",
                type: "select",
                label: "Composants (ACore + Core)",
                obligatoire: true,
                condition: [
                    { champ: "plateforme", valeur: "ACore_et_Core", action: "afficher" }
                ],
                options: [
                    { label: "IHM", valeur: "IHM" },
                    { label: "FLUX", valeur: "FLUX" },
                    { label: "Stack Complète", valeur: "Stack_Complete" }
                ]
            },
            // BE Branch - Level 2: Composants Direct
            {
                nom: "composants_be",
                type: "select",
                label: "Composants (BE)",
                obligatoire: true,
                condition: [
                    { champ: "community", valeur: "BE", action: "afficher" }
                ],
                options: [
                    { label: "IHM", valeur: "IHM" },
                    { label: "FLUX", valeur: "FLUX" },
                    { label: "Stack Complète", valeur: "Stack_Complete" }
                ]
            }
        ],
        actions: [
            { nom: "soumettre", type: "submit", label: "Créer l'environnement", url: "/soumettre" },
            { nom: "annuler", type: "reset", label: "Annuler", url: "/annuler" }
        ]
    };

    function initForm() {
        // Build basic structure
        let formHTML = `
            <div class="form-header">
                <h2>${formConfig.titre}</h2>
                <p>${formConfig.description}</p>
            </div>
            <form id="dynamic-form" class="form-grid">
        `;

        formConfig.champs.forEach(field => {
            formHTML += `
                <div class="form-group" id="group-${field.nom}" style="display: none;">
                    <label class="form-label" for="${field.nom}">
                        ${field.label} ${field.obligatoire ? '<span style="color:var(--accent-color)">*</span>' : ''}
                    </label>
                    <select class="form-select" id="${field.nom}" name="${field.nom}" ${field.obligatoire ? 'required' : ''}>
                        <option value="">Sélectionner...</option>
                        ${field.options.map(opt => `<option value="${opt.valeur}">${opt.label}</option>`).join('')}
                    </select>
                </div>
            `;
        });

        // Add actions
        const submitBtn = formConfig.actions.find(a => a.type === 'submit');
        const resetBtn = formConfig.actions.find(a => a.type === 'reset');

        formHTML += `
            <div class="form-actions">
                ${resetBtn ? `<button type="button" class="btn-secondary" id="btn-reset">${resetBtn.label}</button>` : ''}
                ${submitBtn ? `<button type="submit" class="btn-primary" id="btn-submit">${submitBtn.label}</button>` : ''}
            </div>
            </form>
        `;

        formContainer.innerHTML = formHTML;

        // Attach inputs
        const form = document.getElementById('dynamic-form');
        const inputs = form.querySelectorAll('select');

        // Reset Logic
        if (resetBtn) {
            document.getElementById('btn-reset').addEventListener('click', () => {
                form.reset();
                updateVisibility();
            });
        }

        // Change Listener
        inputs.forEach(input => {
            input.addEventListener('change', (e) => {
                // If Community changes, reset everything else to force new path
                if (e.target.name === 'community') {
                    inputs.forEach(i => {
                        if (i.name !== 'community') {
                            i.value = "";
                        }
                    });
                }
                updateVisibility();
            });
        });

        // Submit Logic
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(form);

            // Only send visible fields
            const cleanData = {};
            inputs.forEach(input => {
                if (input.closest('.form-group').style.display !== 'none') {
                    cleanData[input.name] = input.value;
                }
            });

            const btn = document.getElementById('btn-submit');
            const originalText = btn.innerText;
            btn.innerText = 'Envoi...';
            btn.disabled = true;

            try {
                const res = await fetch(submitBtn.url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(cleanData)
                });

                if (res.ok) {
                    alert('Configuration soumise avec succès !');
                } else {
                    alert('Erreur lors de la soumission.');
                }
            } catch (err) {
                console.error(err);
                alert('Erreur réseau.');
            } finally {
                btn.innerText = originalText;
                btn.disabled = false;
            }
        });

        updateVisibility();
    }

    function updateVisibility() {
        // Use live DOM values to support sequential dependency clearing
        // (If Parent is hidden/cleared, Child should see empty value immediately)
        const getLiveValue = (name) => {
            const el = document.querySelector(`select[name="${name}"]`);
            return el ? el.value : "";
        };

        formConfig.champs.forEach(field => {
            const group = document.getElementById(`group-${field.nom}`);

            if (!field.condition || field.condition.length === 0) {
                showField(group);
                return;
            }

            let shouldShow = false;

            for (let cond of field.condition) {
                const parentValue = getLiveValue(cond.champ);
                if (parentValue === cond.valeur && cond.action === 'afficher') {
                    shouldShow = true;
                    break;
                }
            }

            if (shouldShow) {
                showField(group);
            } else {
                hideField(group);
            }
        });
    }

    async function updateGraph() {
        // Determine visible fields and values
        const inputs = Array.from(document.querySelectorAll('#dynamic-form select'));
        const visibleInputs = inputs.filter(i => i.closest('.form-group').style.display !== 'none');

        // Check if all visible fields have a value
        const allFilled = visibleInputs.every(i => i.value !== "");
        const graphContainer = document.getElementById('stack-visualization');

        if (!allFilled || visibleInputs.length === 0) {
            graphContainer.style.display = 'none';
            return;
        }

        const values = {};
        visibleInputs.forEach(i => values[i.name] = i.value);

        // Determine Component Type & Prefix
        let componentType = null;
        if (values.composants_acore) componentType = values.composants_acore;
        else if (values.composants_core) componentType = values.composants_core;
        else if (values.composants_both) componentType = values.composants_both;
        else if (values.composants_be) componentType = values.composants_be;

        let prefix = "BE";
        if (values.community === 'FR') {
            if (values.plateforme === 'ACore') prefix = "ACore";
            else if (values.plateforme === 'Core') prefix = "Core";
            else if (values.plateforme === 'ACore_et_Core') prefix = "Hybrid";
        }

        // --- GRAPH TEMPLATES ---

        // 1. IHM (Web Stack)
        const templateIHM = `
            graph TD
            subgraph DMZ [Zone DMZ]
                Proxy((${prefix}-Proxy))
            end
            subgraph AppZone [Zone Application]
                WebSrv[${prefix}-Web-Server]
                APIGw[${prefix}-API-Gateway]
            end
            subgraph DataZone [Zone Données]
                Cache[(Redis Cache)]
            end
            
            Proxy --> WebSrv
            WebSrv --> APIGw
            APIGw --> Cache
            
            style DMZ fill:rgba(255,255,255,0.05),stroke:#8b9bb4,stroke-dasharray: 5 5
            style AppZone fill:rgba(0,168,255,0.1),stroke:#00a8ff
            style DataZone fill:rgba(0,255,157,0.1),stroke:#00ff9d
            
            style Proxy fill:#050511,stroke:#00f2ff,stroke-width:2px,color:#fff
            style WebSrv fill:#050511,stroke:#00a8ff,stroke-width:2px,color:#fff
            style APIGw fill:#050511,stroke:#00a8ff,stroke-width:2px,color:#fff
            style Cache fill:#050511,stroke:#00ff9d,stroke-width:2px,color:#fff
        `;

        // 2. FLUX (Processing Stack)
        const templateFLUX = `
            graph LR
            subgraph InputZone [Zone Entrée]
                Gateway[${prefix}-Gateway]
            end
            subgraph MsgZone [Zone Messagerie]
                Broker((${prefix}-Broker))
            end
            subgraph ProcessZone [Zone Traitement]
                Worker1[Worker-01]
                Worker2[Worker-02]
            end
            subgraph DataZone [Zone Stockage]
                DB[(${prefix}-DB)]
            end
            
            Gateway --> Broker
            Broker --> Worker1
            Broker --> Worker2
            Worker1 --> DB
            Worker2 --> DB
            
            style InputZone fill:rgba(255,255,255,0.05),stroke:#8b9bb4
            style MsgZone fill:rgba(255,0,85,0.1),stroke:#ff0055
            style ProcessZone fill:rgba(0,168,255,0.1),stroke:#00a8ff
            style DataZone fill:rgba(0,255,157,0.1),stroke:#00ff9d
            
            style Gateway fill:#050511,stroke:#fff,stroke-width:2px,color:#fff
            style Broker fill:#050511,stroke:#ff0055,stroke-width:2px,color:#fff
            style Worker1 fill:#050511,stroke:#00a8ff,stroke-width:2px,color:#fff
            style Worker2 fill:#050511,stroke:#00a8ff,stroke-width:2px,color:#fff
            style DB fill:#050511,stroke:#00ff9d,stroke-width:2px,color:#fff
        `;

        // 3. COMPLETE (Full Stack)
        const templateComplete = `
            graph TD
            subgraph FrontZone [Front-End]
                LB((${prefix}-LoadBalancer))
                WebApp[${prefix}-App]
            end
            subgraph BackZone [Back-End]
                API[${prefix}-API]
                Engine[${prefix}-Engine]
            end
            subgraph Infras [Infrastructure]
                DB[(Primary DB)]
                Replica[(Replica DB)]
            end
            
            LB --> WebApp
            WebApp --> API
            API --> Engine
            Engine --> DB
            DB -.-> Replica
            
            style FrontZone fill:rgba(0,242,255,0.05),stroke:#00f2ff
            style BackZone fill:rgba(0,168,255,0.1),stroke:#00a8ff
            style Infras fill:rgba(0,255,157,0.1),stroke:#00ff9d
            
            
            style LB fill:#050511,stroke:#00f2ff,stroke-width:2px,color:#fff
            style WebApp fill:#050511,stroke:#00f2ff,stroke-width:2px,color:#fff
            style API fill:#050511,stroke:#00a8ff,stroke-width:2px,color:#fff
            style Engine fill:#050511,stroke:#00a8ff,stroke-width:2px,color:#fff
            style DB fill:#050511,stroke:#00ff9d,stroke-width:2px,color:#fff
            style Replica fill:#050511,stroke:#00ff9d,stroke-width:2px,color:#fff,stroke-dasharray: 5 5
        `;

        // Select Template
        let graphDef = templateIHM; // Default

        if (componentType) {
            if (componentType.includes('FLUX')) {
                graphDef = templateFLUX;
            } else if (componentType.includes('Complet') || componentType.includes('Stack_Complete')) {
                graphDef = templateComplete;
            }
        }

        graphContainer.style.display = 'block';
        const graphDiv = document.getElementById('stack-graph');

        // Render
        graphDiv.innerHTML = graphDef;
        graphDiv.removeAttribute('data-processed');

        try {
            await mermaid.run({
                nodes: [graphDiv]
            });
        } catch (e) {
            console.error('Mermaid error', e);
        }
    }

    function showField(el) {
        if (el.style.display === 'none') {
            el.style.display = 'flex';
            el.style.opacity = '0';
            setTimeout(() => el.style.opacity = '1', 10);
            const input = el.querySelector('select');
            if (input) input.disabled = false;
        }
    }

    function hideField(el) {
        el.style.display = 'none';
        const input = el.querySelector('select');
        // Disable hidden inputs so they aren't part of FormData/validation checks if we wanted strictness
        // ex: required fields shouldn't block submit if hidden
        // But for formData retrieval we used above, we need to be careful.
        // Let's just reset value when hiding?
        if (input && input.value !== "") {
            input.value = "";
        }
    }

    // Initialize logic
    initForm();
    fetchData();
    refreshBtn.addEventListener('click', fetchData);
    setInterval(fetchData, 30000);
});
