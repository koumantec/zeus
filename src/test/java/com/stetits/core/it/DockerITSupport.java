package com.stetits.core.it;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.Assumptions;

public final class DockerITSupport {
    private DockerITSupport() {}

    public static void requireDocker(DockerClient docker) {
        try {
            docker.pingCmd().exec();
        } catch (Exception e) {
            Assumptions.abort("Docker not available: " + e.getMessage());
        }
    }
}
