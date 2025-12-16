FROM node:22-alpine

# Install system dependencies including Ansible and SSH
RUN apk add --no-cache \
    ansible \
    openssh-client \
    sshpass \
    python3 \
    py3-pip

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install node dependencies
RUN npm install --production

# Copy application source
COPY . .

# Create directory for ansible inventory/config if needed
RUN mkdir -p /etc/ansible

# Expose the application port
EXPOSE 3000

# Start the application
CMD ["npm", "start"]
