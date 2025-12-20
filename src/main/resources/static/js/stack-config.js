// Toggle node expansion
function toggleNode(element) {
    // Don't toggle if clicking on checkbox or label
    if (event.target.type === 'checkbox' || event.target.tagName === 'LABEL') {
        return;
    }
    
    const expandIcon = element.querySelector('.expand-icon');
    const children = element.parentElement.querySelector('.node-children');
    
    if (expandIcon && children) {
        expandIcon.classList.toggle('expanded');
        children.classList.toggle('expanded');
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

// Switch community (FR/BE)
function switchCommunity(community) {
    // Hide all trees
    document.getElementById('tree-fr').style.display = 'none';
    document.getElementById('tree-be').style.display = 'none';
    
    // Show selected tree
    document.getElementById('tree-' + community).style.display = 'block';
    
    // Uncheck all checkboxes in hidden tree
    const hiddenTree = community === 'fr' ? 'tree-be' : 'tree-fr';
    const hiddenCheckboxes = document.querySelectorAll('#' + hiddenTree + ' .node-checkbox');
    hiddenCheckboxes.forEach(cb => {
        cb.checked = false;
    });
}

// Reset form
function resetForm() {
    // Reset community to FR
    document.querySelector('input[name="community"][value="fr"]').checked = true;
    switchCommunity('fr');
    
    // Uncheck all checkboxes
    document.querySelectorAll('.node-checkbox').forEach(cb => {
        cb.checked = false;
        cb.indeterminate = false;
    });
    
    // Collapse all nodes
    document.querySelectorAll('.node-children').forEach(children => {
        children.classList.remove('expanded');
    });
    document.querySelectorAll('.expand-icon').forEach(icon => {
        icon.classList.remove('expanded');
    });
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

// Add change listeners to all application checkboxes to update parents
document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.application-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateParentCheckbox(this);
        });
    });
    
    document.querySelectorAll('.component-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateParentCheckbox(this);
        });
    });
});
