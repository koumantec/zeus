// Wizard State
let wizardState = {
    currentStep: 1,
    totalSteps: 5,
    community: null,
    platforms: [],
    appTypes: [],
    selectedApps: []
};

// Menu structure from menu.yaml
const menuStructure = {
    fr: {
        core: {
            ihm: ['webapp-plf', 'webapp-plbinf', 'plf', 'webservice-a2a'],
            flux: ['core-xchg']
        },
        acore: {
            ihm: ['pilad'],
            flux: ['acore']
        }
    },
    be: {
        ihm: ['webapp-plf', 'webapp-plbinf', 'plf', 'webservice-a2a'],
        flux: ['core-xchg']
    }
};

// Initialize wizard
document.addEventListener('DOMContentLoaded', function() {
    updateNavigationButtons();
});

// Community Selection
function selectCommunity(community) {
    wizardState.community = community;
    document.getElementById('selectedCommunity').value = community;
    
    // Update UI
    document.querySelectorAll('#step-1 .choice-card').forEach(card => {
        card.classList.remove('selected');
    });
    document.querySelector(`#step-1 .choice-card[data-value="${community}"]`).classList.add('selected');
    
    // Enable next button
    document.getElementById('nextBtn').disabled = false;
}

// Platform Selection (multiple)
function togglePlatform(platform) {
    const index = wizardState.platforms.indexOf(platform);
    const card = document.querySelector(`#step-2 .choice-card[data-value="${platform}"]`);
    
    if (index > -1) {
        // Deselect
        wizardState.platforms.splice(index, 1);
        card.classList.remove('selected');
    } else {
        // Select
        wizardState.platforms.push(platform);
        card.classList.add('selected');
    }
    
    // Update hidden input
    document.getElementById('selectedPlatform').value = wizardState.platforms.join(',');
    
    // Enable/disable next button
    document.getElementById('nextBtn').disabled = wizardState.platforms.length === 0;
}

// App Type Selection (multiple)
function toggleAppType(appType) {
    const index = wizardState.appTypes.indexOf(appType);
    const card = document.querySelector(`#step-3 .choice-card[data-value="${appType}"]`);
    
    if (index > -1) {
        // Deselect
        wizardState.appTypes.splice(index, 1);
        card.classList.remove('selected');
    } else {
        // Select
        wizardState.appTypes.push(appType);
        card.classList.add('selected');
    }
    
    // Update hidden input
    document.getElementById('selectedAppType').value = wizardState.appTypes.join(',');
    
    // Enable/disable next button
    document.getElementById('nextBtn').disabled = wizardState.appTypes.length === 0;
}

// Toggle App Selection
function toggleApp(checkbox, appName) {
    const card = checkbox.closest('.app-config-card');
    
    if (checkbox.checked) {
        card.classList.add('selected');
        // Make fields required
        card.querySelectorAll('input[required]').forEach(input => {
            input.disabled = false;
        });
    } else {
        card.classList.remove('selected');
        // Clear and disable fields
        card.querySelectorAll('input').forEach(input => {
            if (input.type !== 'checkbox') {
                input.value = '';
                input.disabled = true;
            }
        });
    }
    
    updateSelectedApps();
}

// Update selected apps list
function updateSelectedApps() {
    wizardState.selectedApps = [];
    document.querySelectorAll('.app-checkbox:checked').forEach(checkbox => {
        const card = checkbox.closest('.app-config-card');
        const appName = checkbox.dataset.app;
        const platform = checkbox.dataset.platform;
        const appType = checkbox.dataset.type;
        const version = card.querySelector('input[name$="-version"]').value;
        const archive = card.querySelector('input[name$="-archive"]').value;
        
        wizardState.selectedApps.push({
            name: appName,
            platform: platform,
            type: appType,
            version: version,
            archive: archive
        });
    });
    
    // Enable/disable next button
    document.getElementById('nextBtn').disabled = wizardState.selectedApps.length === 0;
}

// Navigation
function nextStep() {
    if (wizardState.currentStep < wizardState.totalSteps) {
        // Validate current step
        if (!validateStep(wizardState.currentStep)) {
            return;
        }
        
        // Mark current step as completed
        document.querySelector(`.progress-step[data-step="${wizardState.currentStep}"]`).classList.add('completed');
        
        // Move to next step
        wizardState.currentStep++;
        
        // Prepare next step
        prepareStep(wizardState.currentStep);
        
        // Update UI
        updateStepDisplay();
        updateNavigationButtons();
    }
}

function previousStep() {
    if (wizardState.currentStep > 1) {
        wizardState.currentStep--;
        updateStepDisplay();
        updateNavigationButtons();
    }
}

function validateStep(step) {
    switch(step) {
        case 1:
            if (!wizardState.community) {
                alert('Veuillez sélectionner une communauté');
                return false;
            }
            return true;
        case 2:
            // Skip validation for BE (no platform selection)
            if (wizardState.community === 'be') {
                return true;
            }
            if (wizardState.platforms.length === 0) {
                alert('Veuillez sélectionner au moins une plateforme');
                return false;
            }
            return true;
        case 3:
            if (wizardState.appTypes.length === 0) {
                alert('Veuillez sélectionner au moins un type d\'application');
                return false;
            }
            return true;
        case 4:
            if (wizardState.selectedApps.length === 0) {
                alert('Veuillez sélectionner au moins une application');
                return false;
            }
            // Validate that selected apps have versions
            for (let app of wizardState.selectedApps) {
                if (!app.version) {
                    alert(`Veuillez renseigner la version pour ${app.name}`);
                    return false;
                }
            }
            return true;
        default:
            return true;
    }
}

function prepareStep(step) {
    switch(step) {
        case 2:
            preparePlatformStep();
            break;
        case 3:
            prepareAppTypeStep();
            break;
        case 4:
            prepareApplicationsStep();
            break;
        case 5:
            prepareSummaryStep();
            break;
    }
}

function preparePlatformStep() {
    const platformChoices = document.getElementById('platformChoices');
    
    // For BE, skip to app type
    if (wizardState.community === 'be') {
        wizardState.currentStep = 3;
        prepareAppTypeStep();
        updateStepDisplay();
        return;
    }
    
    // For FR, show platform choices (multiple selection)
    const platforms = Object.keys(menuStructure[wizardState.community]);
    platformChoices.innerHTML = platforms.map(platform => `
        <div class="choice-card" data-value="${platform}" onclick="togglePlatform('${platform}')">
            <div class="choice-icon">📦</div>
            <div class="choice-title">${platform.toUpperCase()}</div>
            <div class="choice-description">Plateforme ${platform}</div>
        </div>
    `).join('');
}

function prepareAppTypeStep() {
    const typeChoices = document.getElementById('typeChoices');
    
    // Collect all unique app types from selected platforms
    let appTypesSet = new Set();
    
    if (wizardState.community === 'be') {
        Object.keys(menuStructure.be).forEach(type => appTypesSet.add(type));
    } else {
        wizardState.platforms.forEach(platform => {
            Object.keys(menuStructure[wizardState.community][platform]).forEach(type => {
                appTypesSet.add(type);
            });
        });
    }
    
    const appTypes = Array.from(appTypesSet);
    
    const typeIcons = {
        'ihm': '🖥️',
        'flux': '🔄'
    };
    
    const typeLabels = {
        'ihm': 'Interface Homme-Machine',
        'flux': 'Flux de données'
    };
    
    typeChoices.innerHTML = appTypes.map(type => `
        <div class="choice-card" data-value="${type}" onclick="toggleAppType('${type}')">
            <div class="choice-icon">${typeIcons[type] || '📄'}</div>
            <div class="choice-title">${type.toUpperCase()}</div>
            <div class="choice-description">${typeLabels[type] || type}</div>
        </div>
    `).join('');
}

function prepareApplicationsStep() {
    const applicationsList = document.getElementById('applicationsList');
    let html = '';
    
    if (wizardState.community === 'be') {
        // For BE, group by app type
        wizardState.appTypes.forEach(appType => {
            const apps = menuStructure.be[appType];
            html += `
                <div class="app-group">
                    <h3 class="app-group-title">${appType.toUpperCase()}</h3>
                    <div class="app-group-content">
                        ${apps.map(app => createAppCard(app, 'be', appType)).join('')}
                    </div>
                </div>
            `;
        });
    } else {
        // For FR, group by platform and app type
        wizardState.platforms.forEach(platform => {
            wizardState.appTypes.forEach(appType => {
                if (menuStructure[wizardState.community][platform][appType]) {
                    const apps = menuStructure[wizardState.community][platform][appType];
                    html += `
                        <div class="app-group">
                            <h3 class="app-group-title">${platform.toUpperCase()} - ${appType.toUpperCase()}</h3>
                            <div class="app-group-content">
                                ${apps.map(app => createAppCard(app, platform, appType)).join('')}
                            </div>
                        </div>
                    `;
                }
            });
        });
    }
    
    applicationsList.innerHTML = html;
}

function createAppCard(app, platform, appType) {
    const uniqueId = `${platform}-${appType}-${app}`;
    return `
        <div class="app-config-card">
            <div class="app-config-header">
                <input type="checkbox" class="app-checkbox" data-app="${app}" data-platform="${platform}" data-type="${appType}"
                       onchange="toggleApp(this, '${app}')">
                <div class="app-name">${app}</div>
            </div>
            <div class="app-config-fields">
                <div class="form-field">
                    <label for="${uniqueId}-version">Version *</label>
                    <input type="text" id="${uniqueId}-version" name="${uniqueId}-version" 
                           placeholder="ex: 1.2.3" required disabled
                           onchange="updateSelectedApps()">
                    <span class="field-hint">Version de l'application</span>
                </div>
                <div class="form-field">
                    <label for="${uniqueId}-archive">Chemin de l'archive (optionnel)</label>
                    <input type="text" id="${uniqueId}-archive" name="${uniqueId}-archive" 
                           placeholder="ex: /home/user/app.tar.gz" disabled
                           onchange="updateSelectedApps()">
                    <span class="field-hint">Chemin complet vers l'archive</span>
                </div>
            </div>
        </div>
    `;
}

function prepareSummaryStep() {
    const summaryContainer = document.getElementById('summaryContainer');
    
    let platformInfo = '';
    if (wizardState.community === 'fr' && wizardState.platforms.length > 0) {
        platformInfo = `
            <div class="summary-section">
                <div class="summary-label">Plateformes</div>
                <div class="summary-value">${wizardState.platforms.map(p => p.toUpperCase()).join(', ')}</div>
            </div>
        `;
    }
    
    const appsHtml = wizardState.selectedApps.map(app => `
        <div class="summary-app">
            <div class="summary-app-name">${app.name}</div>
            <div class="summary-app-badge">${app.platform.toUpperCase()} - ${app.type.toUpperCase()}</div>
            <div class="summary-app-details">
                <div class="summary-app-detail">
                    <strong>Version:</strong>
                    <span>${app.version}</span>
                </div>
                ${app.archive ? `
                    <div class="summary-app-detail">
                        <strong>Archive:</strong>
                        <span>${app.archive}</span>
                    </div>
                ` : ''}
            </div>
        </div>
    `).join('');
    
    summaryContainer.innerHTML = `
        <div class="summary-section">
            <div class="summary-label">Communauté</div>
            <div class="summary-value">${wizardState.community.toUpperCase()}</div>
        </div>
        ${platformInfo}
        <div class="summary-section">
            <div class="summary-label">Types d'application</div>
            <div class="summary-value">${wizardState.appTypes.map(t => t.toUpperCase()).join(', ')}</div>
        </div>
        <div class="summary-section">
            <div class="summary-label">Applications sélectionnées (${wizardState.selectedApps.length})</div>
            <div class="summary-apps">
                ${appsHtml}
            </div>
        </div>
    `;
    
    // Prepare hidden inputs for form submission
    prepareFormSubmission();
}

function prepareFormSubmission() {
    const hiddenSelections = document.getElementById('hiddenSelections');
    hiddenSelections.innerHTML = '';
    
    wizardState.selectedApps.forEach((app, index) => {
        const value = `${app.platform}|${app.type}|${app.name}|${app.version}:${app.archive || ''}`;
        
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = 'selections';
        input.value = value;
        hiddenSelections.appendChild(input);
    });
}

function updateStepDisplay() {
    // Hide all steps
    document.querySelectorAll('.wizard-step').forEach(step => {
        step.classList.remove('active');
    });
    
    // Show current step
    document.getElementById(`step-${wizardState.currentStep}`).classList.add('active');
    
    // Update progress bar
    document.querySelectorAll('.progress-step').forEach(step => {
        step.classList.remove('active');
        const stepNum = parseInt(step.dataset.step);
        if (stepNum === wizardState.currentStep) {
            step.classList.add('active');
        }
    });
}

function updateNavigationButtons() {
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const submitBtn = document.getElementById('submitBtn');
    
    // Previous button
    if (wizardState.currentStep === 1) {
        prevBtn.style.display = 'none';
    } else {
        prevBtn.style.display = 'inline-flex';
    }
    
    // Next/Submit buttons
    if (wizardState.currentStep === wizardState.totalSteps) {
        nextBtn.style.display = 'none';
        submitBtn.style.display = 'inline-flex';
    } else {
        nextBtn.style.display = 'inline-flex';
        submitBtn.style.display = 'none';
        
        // Disable next button initially for steps that require selection
        if ([1, 2, 3, 4].includes(wizardState.currentStep)) {
            nextBtn.disabled = true;
        }
    }
}

// Form submission
document.getElementById('wizardForm').addEventListener('submit', function(e) {
    if (wizardState.selectedApps.length === 0) {
        e.preventDefault();
        alert('Veuillez sélectionner au moins une application');
        return false;
    }
});
