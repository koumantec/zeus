package com.stetits.core.docker.service;

import com.stetits.core.docker.model.ContainerInfo;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MockDockerService {

    public List<ContainerInfo> listContainers() {
        return Arrays.asList(
                new ContainerInfo(
                        "a1b2c3d4e5f6",
                        "nginx-frontend",
                        "nginx:latest",
                        "Up 2 hours",
                        "running",
                        Arrays.asList("80:80/tcp", "443:443/tcp"),
                        Arrays.asList("/etc/nginx/conf.d -> /host/conf", "/var/log/nginx -> /host/logs")),
                new ContainerInfo(
                        "f6e5d4c3b2a1",
                        "postgres-db",
                        "postgres:15-alpine",
                        "Up 2 hours",
                        "running",
                        Arrays.asList("5432:5432/tcp"),
                        Arrays.asList("/var/lib/postgresql/data -> /host/data")),
                new ContainerInfo(
                        "9876543210ab",
                        "redis-cache",
                        "redis:7",
                        "Up 45 minutes",
                        "running",
                        Arrays.asList("6379:6379/tcp"),
                        Arrays.asList()),
                new ContainerInfo(
                        "c3d4e5f6a1b2",
                        "worker-job",
                        "my-app/worker:v2",
                        "Exited (0) 10 minutes ago",
                        "exited",
                        Arrays.asList(),
                        Arrays.asList()));
    }
}
