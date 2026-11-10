package com.finanzapp.lifehub.modules.home.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class HomeStorageService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final Path root;

    public HomeStorageService(@Value("${lifehub.home.storage-root:./.local/home-attachments}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("El documento no puede estar vacío");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Cada documento puede ocupar como máximo 10 MB");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png") &&
                !contentType.equals("image/webp") && !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG, WEBP o PDF");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "documento" : file.getOriginalFilename());
        if (original.contains("..")) throw new IllegalArgumentException("Nombre de fichero no válido");
        String key = UUID.randomUUID() + "-" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        try {
            Files.createDirectories(root);
            Path destination = root.resolve(key).normalize();
            if (!destination.startsWith(root)) throw new IllegalArgumentException("Ruta de fichero no válida");
            file.transferTo(destination);
            return new StoredFile(key, original, contentType, file.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el documento", e);
        }
    }

    public Resource load(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root) || !Files.exists(file)) throw new IllegalArgumentException("Documento no encontrado");
        return new FileSystemResource(file);
    }

    public void delete(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) throw new IllegalArgumentException("Ruta de fichero no válida");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar el documento almacenado", e);
        }
    }

    public record StoredFile(String key, String originalFilename, String contentType, long size) { }
}
