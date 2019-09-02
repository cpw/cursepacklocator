package cpw.mods.forge.cursepacklocator;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PackFile {
    private static final Logger LOGGER = LogManager.getLogger();
    private final String projectId;
    private final String fileId;
    private String fileName;

    public PackFile(final String projectId, final String fileId) {
        this.projectId = projectId;
        this.fileId = fileId;
    }

    public void loadFileIntoPlace(final Path targetPackDir, final FileCacheManager fileCacheManager) {
        LOGGER.info("CursePackDownloader is loading file {} - {}", this.projectId, this.fileId);
        final Path path = fileCacheManager.downloadFile(this.projectId, this.fileId);
        final JsonObject info = fileCacheManager.downloadInfo(this.projectId, this.fileId);
        this.fileName = info.get("fileName").getAsString();
        final Path targetFile = targetPackDir.resolve(this.fileName);
        if (Files.exists(targetFile) && fileCacheManager.validateFile(targetFile, info.get("packageFingerprint").getAsLong())) {
            LOGGER.info("Skipping existing file {}", this.fileName);
            return;
        }
        try {
            Files.copy(path, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        LOGGER.info("CursePackDownloader has loaded file {} - {} ({})", this.projectId, this.fileId, this.fileName);
    }

    public String getFileName() {
        return fileName;
    }
}
