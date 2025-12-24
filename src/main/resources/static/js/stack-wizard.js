// Wizard State
let wizardState = {
    currentStep: 1,
    totalSteps: 5,
    community: null,
    platform: null,
    appType: null,
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

// Platform Selection
function selectPlatform(platform) {
    wizardState.platform = platform;
    document.getElementById('selectedPlatform').value = platform;
    
    // Update UI
    document.querySelectorAll('#step-2 .choice-card').forEach(card => {
        card.classList.remove('selected');
    });
    document.querySelector(`#step-2 .choice-card[data-value="${platform}"]`).classList.add('selected');
    
    // Enable next button
    document.getElementById('nextBtn').disabled = false;
}

// App Type Selection
function selectAppType(appType) {
    wizardState.appType = appType;
    document.getElementById('selectedAppType').value = appType;
    
    // Update UI
    document.querySelectorAll('#step-3 .choice-card').forEach(card => {
        card.classList.remove('selected');
    });
    document.querySelector(`#step-3 .choice-card[data-value="${appType}"]`).classList.add('selected');
    
    // Enable next button
    document.getElementById('nextBtn').disabled = false;
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
        const version = card.querySelector('input[name$="-version"]').value;
        const archive = card.querySelector('input[name$="-archive"]').value;
        
        wizardState.selectedApps.push({
            name: appName,
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
            if (!wizardState.platform) {
                alert('Veuillez sélectionner une plateforme');
                return false;
            }
            return true;
        case 3:
            if (!wizardState.appType) {
                alert('Veuillez sélectionner un type d\'application');
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
    
    // For FR, show platform choices
    const platforms = Object.keys(menuStructure[wizardState.community]);
    platformChoices.innerHTML = platforms.map(platform => `
        <div class="choice-card" data-value="${platform}" onclick="selectPlatform('${platform}')">
            <div class="choice-icon">📦</div>
            <div class="choice-title">${platform.toUpperCase()}</div>
            <div class="choice-description">Plateforme ${platform}</div>
        </div>
    `).join('');
}

function prepareAppTypeStep() {
    const typeChoices = document.getElementById('typeChoices');
    
    let appTypes;
    if (wizardState.community === 'be') {
        appTypes = Object.keys(menuStructure.be);
    } else {
        appTypes = Object.keys(menuStructure[wizardState.community][wizardState.platform]);
    }
    
    const typeIcons = {
        'ihm': '🖥️',
        'flux': '🔄'
    };
    
    const typeLabels = {
        'ihm': 'Interface Homme-Machine',
        'flux': 'Flux de données'
    };
    
    typeChoices.innerHTML = appTypes.map(type => `
        <div class="choice-card" data-value="${type}" onclick="selectAppType('${type}')">
            <div class="choice-icon">${typeIcons[type] || '📄'}</div>
            <div class="choice-title">${type.toUpperCase()}</div>
            <div class="choice-description">${typeLabels[type] || type}</div>
        </div>
    `).join('');
}

function prepareApplicationsStep() {
    const applicationsList = document.getElementById('applicationsList');
    
    let apps;
    if (wizardState.community === 'be') {
        apps = menuStructure.be[wizardState.appType];
    } else {
        apps = menuStructure[wizardState.community][wizardState.platform][wizardState.appType];
    }
    
    applicationsList.innerHTML = apps.map(app => `
        <div class="app-config-card">
            <div class="app-config-header">
                <input type="checkbox" class="app-checkbox" data-app="${app}" 
                       onchange="toggleApp(this, '${app}')">
                <div class="app-name">${app}</div>
            </div>
            <div class="app-config-fields">
                <div class="form-field">
                    <label for="${app}-version">Version *</label>
                    <input type="text" id="${app}-version" name="${app}-version" 
                           placeholder="ex: 1.2.3" required disabled
                           onchange="updateSelectedApps()">
                    <span class="field-hint">Version de l'application</span>
                </div>
                <div class="form-field">
                    <label for="${app}-archive">Chemin de l'archive (optionnel)</label>
                    <input type="text" id="${app}-archive" name="${app}-archive" 
                           placeholder="ex: /home/user/app.tar.gz" disabled
                           onchange="updateSelectedApps()">
                    <span class="field-hint">Chemin complet vers l'archive</span>
                </div>
            </div>
        </div>
    `).join('');
}

function prepareSummaryStep() {
    const summaryContainer = document.getElementById('summaryContainer');
    
    let platformInfo = '';
    if (wizardState.community === 'fr') {
        platformInfo = `
            <div class="summary-section">
                <div class="summary-label">Plateforme</div>
                <div class="summary-value">${wizardState.platform.toUpperCase()}</div>
            </div>
        `;
    }
    
    const appsHtml = wizardState.selectedApps.map(app => `
        <div class="summary-app">
            <div class="summary-app-name">${app.name}</div>
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
            <div class="summary-value">${wizardState.community === 'fr' ? 'France' : 'Belgique'}</div>
        </div>
        ${platformInfo}
        <div class="summary-section">
            <div class="summary-label">Type d'application</div>
            <div class="summary-value">${wizardState.appType.toUpperCase()}</div>
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
        const platform = wizardState.platform || '';
        const value = `${platform}|${wizardState.appType}|${app.name}|${app.version}:${app.archive || ''}`;
        
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
