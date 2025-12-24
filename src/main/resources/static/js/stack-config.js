// Update selection counter
function updateSelectionCounter() {
    const selectedApps = document.querySelectorAll('.application-checkbox:checked');
    const counter = document.getElementById('selectionCount');
    if (counter) {
        counter.textContent = selectedApps.length;
        // Add animation
        counter.style.transform = 'scale(1.2)';
        setTimeout(() => {
            counter.style.transform = 'scale(1)';
        }, 200);
    }
}

// Toggle node expansion
function toggleNode(element) {
    // Don't toggle if clicking on checkbox or label
    if (event.target.type === 'checkbox' || event.target.tagName === 'LABEL') {
        return;
    }
    
    const expandIcon = element.querySelector('.expand-icon');
    const children = element.parentElement.querySelector('.node-children');
    
    if (expandIcon && children) {
        element.classList.toggle('expanded');
        children.classList.toggle('open');
    }
}

// Toggle children checkboxes when parent is checked/unchecked
function toggleChildren(checkbox) {
    const nodeHeader = checkbox.closest('.node-header');
    const nodeElement = nodeHeader.parentElement;
    const childrenContainer = nodeElement.querySelector('.node-children');
    
    if (childrenContainer) {
        // Expand children when checking parent
        if (checkbox.checked) {
            childrenContainer.classList.add('expanded');
            const expandIcon = nodeHeader.querySelector('.expand-icon');
            if (expandIcon) {
                expandIcon.classList.add('expanded');
            }
        }
        
        // Check/uncheck all child checkboxes
        const childCheckboxes = childrenContainer.querySelectorAll('.node-checkbox');
        childCheckboxes.forEach(child => {
            child.checked = checkbox.checked;
            // Recursively toggle children
            if (child.classList.contains('platform-checkbox') || 
                child.classList.contains('component-checkbox')) {
                toggleChildren(child);
            }
        });
    }
    
    // Update parent checkbox state
    updateParentCheckbox(checkbox);
}

// Update parent checkbox based on children state
function updateParentCheckbox(checkbox) {
    const parentId = checkbox.dataset.parent;
    if (parentId) {
        const parentCheckbox = document.getElementById(parentId);
        if (parentCheckbox) {
            const parentNodeElement = parentCheckbox.closest('.node-header').parentElement;
            const childrenContainer = parentNodeElement.querySelector('.node-children');
            
            if (childrenContainer) {
                const childCheckboxes = Array.from(
                    childrenContainer.querySelectorAll(':scope > .tree-node > .node-header > .node-checkbox')
                );
                
                const allChecked = childCheckboxes.every(cb => cb.checked);
                const someChecked = childCheckboxes.some(cb => cb.checked);
                
                parentCheckbox.checked = allChecked;
                parentCheckbox.indeterminate = someChecked && !allChecked;
            }
        }
    }
}

// Switch community (FR/BE) with animation
function switchCommunity(community) {
    if (!community) {
        // Hide all trees if no community selected
        document.getElementById('tree-fr').style.display = 'none';
        document.getElementById('tree-be').style.display = 'none';
        return;
    }
    
    // Hide all trees first
    const allTrees = document.querySelectorAll('.tree-container');
    allTrees.forEach(tree => {
        tree.style.display = 'none';
    });
    
    // Show selected tree with animation
    const selectedTree = document.getElementById('tree-' + community);
    if (selectedTree) {
        selectedTree.style.display = 'block';
        // Trigger reflow to restart animation
        selectedTree.style.animation = 'none';
        setTimeout(() => {
            selectedTree.style.animation = '';
        }, 10);
    }
    
    // Uncheck all checkboxes in hidden tree
    const hiddenTree = community === 'fr' ? 'tree-be' : 'tree-fr';
    const hiddenCheckboxes = document.querySelectorAll('#' + hiddenTree + ' .node-checkbox');
    hiddenCheckboxes.forEach(cb => {
        cb.checked = false;
    });
    
    // Update counter
    updateSelectionCounter();
}

// Reset form
function resetForm() {
    // Reset community dropdown
    const communitySelect = document.getElementById('communitySelect');
    if (communitySelect) {
        communitySelect.value = '';
    }
    
    // Hide all trees
    switchCommunity('');
    
    // Uncheck all checkboxes
    document.querySelectorAll('.node-checkbox').forEach(cb => {
        cb.checked = false;
        cb.indeterminate = false;
    });
    
    // Collapse all nodes
    document.querySelectorAll('.node-children').forEach(children => {
        children.classList.remove('expanded');
        children.classList.remove('open');
    });
    document.querySelectorAll('.node-header').forEach(header => {
        header.classList.remove('expanded');
    });
    
    // Update counter
    updateSelectionCounter();
}

// Form validation
document.getElementById('stackForm').addEventListener('submit', function(e) {
    const selectedApps = document.querySelectorAll('.application-checkbox:checked');
    
    if (selectedApps.length === 0) {
        e.preventDefault();
        alert('Veuillez sélectionner au moins une application');
        return false;
    }
});

// Load existing selections
function loadExistingSelections(community, selections) {
    if (!community || !selections || selections.length === 0) {
        return;
    }
    
    // Set community dropdown
    const communitySelect = document.getElementById('communitySelect');
    if (communitySelect) {
        communitySelect.value = community;
        switchCommunity(community);
    }
    
    // Check the corresponding checkboxes
    selections.forEach(selection => {
        const checkbox = document.querySelector(`input[name="selections"][value="${selection}"]`);
        if (checkbox) {
            checkbox.checked = true;
            
            // Expand parent nodes and update their state
            let currentElement = checkbox.closest('.tree-node');
            while (currentElement) {
                const nodeChildren = currentElement.querySelector('.node-children');
                if (nodeChildren) {
                    nodeChildren.classList.add('expanded');
                    const expandIcon = currentElement.querySelector('.expand-icon');
                    if (expandIcon) {
                        expandIcon.classList.add('expanded');
                    }
                }
                
                // Update parent checkbox
                const parentCheckbox = currentElement.querySelector(':scope > .node-header > .node-checkbox');
                if (parentCheckbox && parentCheckbox.dataset.parent) {
                    updateParentCheckbox(parentCheckbox);
                }
                
                // Move to parent tree node
                currentElement = currentElement.parentElement.closest('.tree-node');
            }
        }
    });
}

// Add change listeners to all application checkboxes to update parents and counter
document.addEventListener('DOMContentLoaded', function() {
    // Update counter on page load
    updateSelectionCounter();
    
    document.querySelectorAll('.application-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateParentCheckbox(this);
            updateSelectionCounter();
        });
    });
    
    document.querySelectorAll('.component-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateParentCheckbox(this);
            updateSelectionCounter();
        });
    });
    
    document.querySelectorAll('.platform-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateSelectionCounter();
        });
    });
    
    // Load existing selections if available
    const existingCommunity = document.body.dataset.existingCommunity;
    const existingSelectionsStr = document.body.dataset.existingSelections;
    if (existingCommunity && existingSelectionsStr && existingSelectionsStr.trim() !== '') {
        try {
            // Split by comma to get array of selections
            const existingSelections = existingSelectionsStr.split(',').map(s => s.trim()).filter(s => s !== '');
            if (existingSelections.length > 0) {
                loadExistingSelections(existingCommunity, existingSelections);
            }
        } catch (e) {
            console.error('Error loading existing selections:', e);
        }
    }
});
