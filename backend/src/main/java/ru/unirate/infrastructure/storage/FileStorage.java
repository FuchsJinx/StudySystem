package ru.unirate.infrastructure.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.unirate.api.error.ApiException;
import ru.unirate.api.error.ErrorCodes;
import ru.unirate.infrastructure.config.UnirateProperties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStorage {

    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "zip", "doc", "docx", "png");

    private final Path root;
    private final long maxBytes;

    public FileStorage(UnirateProperties properties) {
        this.root = Path.of(properties.getUploads().getDir()).toAbsolutePath().normalize();
        this.maxBytes = properties.getUploads().getMaxBytes();
        try {
            Files.createDirectories(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Не удалось создать каталог загрузок: " + root, ex);
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.FILE_INVALID_TYPE, "Файл пустой");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest(ErrorCodes.FILE_TOO_LARGE,
                    "Файл превышает допустимый размер 10 МБ");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw ApiException.badRequest(ErrorCodes.FILE_INVALID_TYPE,
                    "Допустимые форматы файлов: PDF, ZIP, DOC, DOCX, PNG");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw ApiException.badRequest(ErrorCodes.FILE_INVALID_TYPE, "Некорректное имя файла");
        }
        try (InputStream input = file.getInputStream()) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Не удалось сохранить файл загрузки", ex);
            throw ApiException.badRequest(ErrorCodes.FILE_INVALID_TYPE, "Не удалось сохранить файл");
        }
        return new StoredFile(storedName, original);
    }

    public Resource load(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            throw ApiException.notFound(ErrorCodes.NOT_FOUND, "Файл не найден");
        }
        try {
            Path path = root.resolve(storedName).normalize();
            if (!path.startsWith(root)) {
                throw ApiException.notFound(ErrorCodes.NOT_FOUND, "Файл не найден");
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound(ErrorCodes.NOT_FOUND, "Файл не найден");
            }
            return resource;
        } catch (java.net.MalformedURLException ex) {
            throw ApiException.notFound(ErrorCodes.NOT_FOUND, "Файл не найден");
        }
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String storedName, String originalName) {
    }
}
