package cpw.mods.forge.cursepacklocator;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class FileCacheManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String INFOURL = "https://addons-ecs.forgesvc.net/api/v2/addon/%s/file/%s";

    private final Path infos;
    private final Path files;

    FileCacheManager() {
        this(Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.ASSETSDIR.get()).orElseThrow(() -> new IllegalStateException("MISSING ASSETS DIR?")));
    }

    public FileCacheManager(final Path assetsDir) {
        final Path cheesycursecache = DirHandler.createOrGetDirectory(assetsDir, "cheesycursecache");
        infos = DirHandler.createOrGetDirectory(cheesycursecache, "infos");
        files = DirHandler.createOrGetDirectory(cheesycursecache, "files");
    }

    public Optional<JsonObject> loadInfo(final String projectId, final String fileId) {
        final Path infoPath = resolveInfo(projectId, fileId);
        if (!Files.exists(infoPath)) {
            return Optional.empty();
        }
        return Optional.of(loadJsonFromFile(infoPath));
    }

    public static JsonObject loadJsonFromFile(final Path infoPath) {
        try (JsonReader jsonReader = new Gson().newJsonReader(Files.newBufferedReader(infoPath))) {
            return new JsonParser().parse(jsonReader).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveInfo(final String projectId, final String fileId) {
        return infos.resolve(projectId + "-" + fileId + ".json");
    }

    private Path resolveFile(final String projectId, final String fileId) {
        return files.resolve(projectId + "-" + fileId + ".jar");
    }

    private Optional<Path> loadFile(final String projectId, final String fileId) {
        final Path filePath = resolveFile(projectId, fileId);
        if (!Files.exists(filePath)) {
            return Optional.empty();
        } else {
            return Optional.of(filePath);
        }
    }

    public Path downloadFile(final String projectId, final String fileId) {
        final JsonObject fileInfo = downloadInfo(projectId, fileId);
        final long packageFingerprint = fileInfo.get("packageFingerprint").getAsLong();
        final Path downloadedFile = loadFile(projectId, fileId)
                .filter(file -> validateFile(file, packageFingerprint))
                .orElseGet(() -> fetchFile(fileInfo.get("downloadUrl").getAsString(), projectId, fileId));
        if (!validateFile(downloadedFile, packageFingerprint)) {
            throw new RuntimeException("Unable to validate the downloaded file "+projectId +" - " + fileId);
        }
        return downloadedFile;
    }

    private Path fetchFile(final String downloadUrl, final String projectId, final String fileId) {
        Path downloadTo = resolveFile(projectId, fileId);
        String fname = downloadUrl.substring(downloadUrl.lastIndexOf('/')+1);
        String rest = downloadUrl.substring(0, downloadUrl.lastIndexOf('/'));
        String encname = fname;
        try {
            encname = URLEncoder.encode(fname, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // never going to happen
        }
        encname = encname.replaceAll("\\+","%20");
        return downloadToFile(downloadTo, rest+"/"+encname);
    }

    boolean validateFile(final Path file, final long checksum) {
        return HashChecker.computeHash(file) == checksum;
    }

    public JsonObject downloadInfo(final String projectId, final String fileId) {
        return loadInfo(projectId, fileId).orElseGet(() -> fetchInfo(projectId, fileId));
    }

    private JsonObject fetchInfo(final String projectId, final String fileId) {
        final Path path = downloadToFile(resolveInfo(projectId, fileId), String.format(INFOURL, projectId, fileId));
        return loadJsonFromFile(path);
    }

    private Path downloadToFile(final Path target, final String urlToGet) {
        try {
            URL url = new URL(urlToGet);
            LOGGER.info("Downloading from URL {}", urlToGet);
            Files.copy(url.openStream(), target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Downloaded from URL {}", urlToGet);
            return target;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
