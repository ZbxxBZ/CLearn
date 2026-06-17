package com.clearn.worker.sandbox;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.Set;

@Component
public class WorkspaceFactory {
    private static final Set<PosixFilePermission> WORKSPACE_PERMISSIONS =
            PosixFilePermissions.fromString("rwxr-xr-x");

    public Path create() {
        try {
            Path workspace = Files.createTempDirectory("clearn-judge-");
            applyPermissionsIfSupported(workspace, WORKSPACE_PERMISSIONS);
            return workspace;
        } catch (IOException ex) {
            throw new IllegalStateException("failed to create judge workspace", ex);
        }
    }

    public void delete(Path workspace) {
        if (workspace == null || !Files.exists(workspace)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(workspace)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(this::deletePath);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to delete judge workspace", ex);
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to delete judge workspace path: " + path, ex);
        }
    }

    private void applyPermissionsIfSupported(Path path, Set<PosixFilePermission> permissions) {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to set permissions on " + path, ex);
        }
    }
}
