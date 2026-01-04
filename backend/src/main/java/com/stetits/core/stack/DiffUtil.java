package com.stetits.core.stack;

import com.stetits.core.docker.DockerClientFacade;

import java.util.*;

public final class DiffUtil {
    private DiffUtil() {}

    public static boolean equalsSpec(DockerClientFacade.InspectContainer actual, DockerClientFacade.ContainerSpec desired) {
        if (!Objects.equals(actual.image(), desired.image())) return false;
        if (!Objects.equals(normalizeEnv(actual.env()), normalizeEnv(desired.env()))) return false;
        if (!Objects.equals(normalizePorts(actual.portsTcp()), normalizePorts(desired.portsTcp()))) return false;
        if (!Objects.equals(actual.hostname(), desired.hostname())) return false;
        if (!Objects.equals(normalizeCmd(actual.command()), normalizeCmd(desired.command()))) return false;

        // network name (docker returns network name)
        if (desired.network() != null && actual.networkName() != null) {
            if (!Objects.equals(actual.networkName(), desired.network().name())) return false;
        }

        // aliases should contain service name (at least)
        if (desired.networkAliases() != null && !desired.networkAliases().isEmpty()) {
            var a = new HashSet<>(actual.aliases() == null ? List.of() : actual.aliases());
            for (String al : desired.networkAliases()) if (!a.contains(al)) return false;
        }

        // mounts: compare set of (volumeName -> containerPath, ro)
        return Objects.equals(normalizeMounts(actual.mounts()), normalizeMounts(desired.mounts()));
    }

    private static List<String> normalizeCmd(List<String> cmd) {
        if (cmd == null) return List.of();
        // Certains containers peuvent avoir cmd vide vs null
        return cmd.stream().filter(s -> s != null && !s.isBlank()).toList();
    }

    private static Map<String,String> normalizeEnv(Map<String,String> env) {
        return env == null ? Map.of() : new TreeMap<>(env);
    }

    private static Map<Integer,Integer> normalizePorts(Map<Integer,Integer> p) {
        return p == null ? Map.of() : new TreeMap<>(p);
    }

    private static List<String> normalizeMounts(List<DockerClientFacade.MountSpec> mounts) {
        if (mounts == null) return List.of();
        List<String> out = new ArrayList<>();
        for (var m : mounts) out.add(m.volumeName() + "|" + m.containerPath() + "|" + m.readOnly());
        out.sort(String::compareTo);
        return out;
    }
}
