# Core Monitor - Spring Boot Application

A Spring Boot application with Thymeleaf template engine, featuring a dark Kibana-inspired UI.

## Technologies

- **Java**: 21
- **Framework**: Spring Boot 3.2.0
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## Features

- Dark theme UI inspired by Kibana
- Responsive design
- Fully containerized build and deployment
- Health check monitoring

## Prerequisites

- Docker
- Docker Compose

## Quick Start

### Build and Run with Docker Compose

```bash
docker-compose up --build
```

The application will be available at: **http://localhost:3000**

### Stop the Application

```bash
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

## License

MIT
