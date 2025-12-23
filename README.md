# Core Monitor - Spring Boot Application

A Spring Boot application with Thymeleaf template engine, featuring a dark Kibana-inspired UI.

## Technologies

- **Java**: 21
- **Framework**: Spring Boot 3.2.0
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## Features

- **Dark theme UI** inspired by Kibana
- **Responsive design**
- **Fully containerized** build and deployment
- **Health check** monitoring
- **Stack Configuration System**: Intelligent form system for selecting application stack components
  - 4-level hierarchy: Community → Platform → Component → Application
  - Expandable tree view with checkboxes
  - Single selection for community (FR/BE)
  - Multiple selection for platforms, components, and applications
  - Confirmation page with detailed summary
- **Settings Management**: Secure storage and management of GitHub STET credentials
  - Automatic redirection to settings page if not configured
  - Persistent storage in `~/.core/settings.conf`
  - Easy editing via Settings tab in the dashboard
  - Docker volume mounting for data persistence

## Prerequisites

- Docker
- Docker Compose

## Quick Start

### Option 1: Using the rebuild script (Recommended)

```bash
./rebuild.sh
```

### Option 2: Using Docker Compose

```bash
docker-compose up --build
```

### Option 3: Manual Docker commands

```bash
docker build -t core-monitor .
docker run -d -p 3000:3000 --name core-monitor-app core-monitor
```

The application will be available at:
- **Dashboard**: http://localhost:3000
- **Stack Configuration**: http://localhost:3000/stack-config
- **Settings**: http://localhost:3000/settings

> **Note**: On first launch, you will be automatically redirected to the Settings page to configure your GitHub STET credentials.

### Stop the Application

```bash
docker stop core-monitor-app
# or with docker-compose
docker-compose down
```

## Manual Docker Commands

### Build the Docker image

```bash
docker build -t core-monitor .
```

### Run the container

```bash
docker run -p 3000:3000 core-monitor
```

## Development

### Local Development (without Docker)

If you want to run the application locally without Docker:

```bash
mvn spring-boot:run
```

### Build the application

```bash
mvn clean package
```

## Project Structure

```
core-monitor/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/coremonitor/
│       │       ├── CoreMonitorApplication.java
│       │       └── controller/
│       │           └── HomeController.java
│       └── resources/
│           ├── application.properties
│           ├── templates/
│           │   └── index.html
│           └── static/
│               └── css/
│                   └── style.css
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## Configuration

The application runs on port **3000** by default. You can modify this in `src/main/resources/application.properties`.

### Settings Configuration

The application stores GitHub STET credentials in `~/.core/settings.conf`. This file is automatically created on first configuration and persists between container restarts thanks to Docker volume mounting.

For more information about the settings feature, see:
- **[START_HERE.md](START_HERE.md)** - Quick start guide
- **[SUMMARY.md](SUMMARY.md)** - Feature overview
- **[DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md)** - Complete documentation index

## Documentation

Comprehensive documentation is available:

| Document | Description |
|----------|-------------|
| [START_HERE.md](START_HERE.md) | 🚀 Quick start guide - **Read this first!** |
| [SUMMARY.md](SUMMARY.md) | 📋 Feature overview and quick reference |
| [QUICK_START.md](QUICK_START.md) | ⚡ Detailed startup instructions |
| [README_SETTINGS.md](README_SETTINGS.md) | 🎯 Settings feature guide |
| [DOCUMENTATION_INDEX.md](DOCUMENTATION_INDEX.md) | 🗺️ Complete documentation index |

## License

MIT
