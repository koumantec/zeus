const express = require('express');
const { exec } = require('child_process');
const fs = require('fs');
const path = require('path');
const app = express();
const PORT = process.env.PORT || 3000;

// Configuration
const ANSIBLE_HOST = process.env.ANSIBLE_HOST || 'local'; // Host to target
const DEMO_MODE = process.env.DEMO_MODE || 'true'; // Default to true for immediate visualization

app.use(express.static('public'));
app.use(express.json()); // Enable JSON body parsing

// --- CONFIGURATION ENDPOINTS ---
const CONFIG_DIR = '/root/.core';
const CONFIG_FILE = path.join(CONFIG_DIR, 'monitor.conf');

// Helper to parse properties file
const parseConfig = (content) => {
    const config = {};
    content.split('\n').forEach(line => {
        if (line && line.includes('=')) {
            const [key, value] = line.split('=');
            config[key.trim()] = value.trim();
        }
    });
    return config;
};

// Check if configured
app.get('/api/config/status', (req, res) => {
    if (fs.existsSync(CONFIG_FILE)) {
        const content = fs.readFileSync(CONFIG_FILE, 'utf8');
        if (content && content.trim().length > 0) {
            return res.json({ configured: true });
        }
    }
    res.json({ configured: false });
});

// Save config
app.post('/api/config', (req, res) => {
    const { gitLogin, gitPassword, harborLogin, harborPassword } = req.body;

    // Ensure dir exists
    if (!fs.existsSync(CONFIG_DIR)) {
        fs.mkdirSync(CONFIG_DIR, { recursive: true });
    }

    const content = `GIT_LOGIN=${gitLogin}\nGIT_PASSWORD=${gitPassword}\nHARBOR_LOGIN=${harborLogin}\nHARBOR_PASSWORD=${harborPassword}\n`;

    try {
        fs.writeFileSync(CONFIG_FILE, content);
        console.log('Configuration saved to ' + CONFIG_FILE);
        res.json({ success: true });
    } catch (err) {
        console.error('Error writing config:', err);
        res.status(500).json({ error: 'Failed to write configuration' });
    }
});

// Read config (for settings mode)
app.get('/api/config', (req, res) => {
    if (!fs.existsSync(CONFIG_FILE)) {
        return res.json({});
    }
    try {
        const content = fs.readFileSync(CONFIG_FILE, 'utf8');
        const config = parseConfig(content);
        // Do not return passwords in plain text ideally, but for this wizard edit mode user expects them (or maybe placeholders)
        // For simplicity we return them as requested by "mettre à jour" logic
        res.json({
            gitLogin: config.GIT_LOGIN,
            gitPassword: config.GIT_PASSWORD,
            harborLogin: config.HARBOR_LOGIN,
            harborPassword: config.HARBOR_PASSWORD
        });
    } catch (err) {
        console.error('Error reading config:', err);
        res.status(500).json({ error: 'Failed to read configuration' });
    }
});

app.post('/soumettre', (req, res) => {
    console.log('Form submission received:', req.body);
    res.json({ success: true, message: 'Configuration received successfully', receivedData: req.body });
});


app.get('/api/containers', (req, res) => {
    if (DEMO_MODE === 'true') {
        console.log('Serving demo data');
        // Return mock data for demonstration
        const mockData = [
            { name: 'zeus_frontend', command: 'npm start', state: 'Up', status: 'Up 2 hours', ports: '0.0.0.0:3000->3000/tcp' },
            { name: 'zeus_backend', command: 'python app.py', state: 'Up', status: 'Up 2 hours', ports: '0.0.0.0:5000->5000/tcp' },
            { name: 'zeus_db', command: 'postgres', state: 'Up', status: 'Up 2 hours', ports: '5432/tcp' },
            { name: 'zeus_redis', command: 'redis-server', state: 'Up', status: 'Up 2 hours', ports: '6379/tcp' },
            { name: 'zeus_worker', command: 'celery -A app worker', state: 'Exit', status: 'Exited (0) 5 minutes ago', ports: '' },
        ];
        return res.json(mockData);
    }

    // Execute Ansible command
    // Note: This assumes the host machine has ansible installed and configured
    // Command: ansible <HOST> -m shell -a "docker-compose ps"
    // We might need to adjust the directory or use 'docker ps' depending on the exact requirement.
    // The user asked for "docker-compose ps". We often need to be in the correct dir for that.
    // For now, I'll assume a theoretical command or allow overriding it.

    const cmd = process.env.ANSIBLE_CMD || `ansible ${ANSIBLE_HOST} -m shell -a "docker p --format '{{.Names}}|{{.Command}}|{{.State}}|{{.Status}}|{{.Ports}}'"`;

    // Better yet, let's try to parse standard docker-compose ps output if that's strictly what's requested
    // But JSON format from docker inspect or formatted docker ps is easier to parse.
    // Let's stick to the user request: "executes docker-compose ps"

    // Changing approach slightly to be robust: simple text parsing of docker-compose ps
    const ansibleCmd = `ansible ${ANSIBLE_HOST} -m shell -a "docker-compose ps"`;

    exec(ansibleCmd, (error, stdout, stderr) => {
        if (error) {
            console.error(`exec error: ${error}`);
            // If it fails, we might want to return an error or empty list
            return res.status(500).json({ error: 'Failed to execute ansible command', details: stderr || error.message });
        }

        // Parse stdout
        // Ansible output usually looks like:
        // host | CHANGED | rc=0 >>
        //       Name                     Command               State           Ports         
        // --------------------------------------------------------------------------------
        // ...

        const lines = stdout.split('\n');
        const containers = [];

        let parsing = false;

        lines.forEach(line => {
            if (line.includes('Name') && line.includes('Command') && line.includes('State')) {
                parsing = true;
                return; // Skip header
            }
            if (line.startsWith('---')) return; // Skip separator
            if (!parsing) return;
            if (line.trim() === '') return;

            // Simple loose parsing based on whitespace
            // This is fragile but works for a basic implementation. 
            // Ideally we'd use 'docker-compose ps --format json' if available (newer versions)

            const parts = line.split(/\s{2,}/); // Split by 2 or more spaces
            if (parts.length >= 3) {
                containers.push({
                    name: parts[0],
                    command: parts[1],
                    state: parts[2],
                    ports: parts.length > 3 ? parts[3] : ''
                });
            }
        });

        res.json(containers);
    });
});

app.listen(PORT, () => {
    console.log(`Server running at http://localhost:${PORT}`);
    console.log(`Mode: ${DEMO_MODE === 'true' ? 'DEMO' : 'LIVE'}`);
});
