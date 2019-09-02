package cpw.mods.forge.cursepacklocator;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class CurseLocator implements IModLocator {
    private final FileCacheManager fileCacheManager;
    private IModLocator wrappedModFolderLocator;
    private final CursePack pack;

    public CurseLocator() {
        final Path gameDir = Launcher.INSTANCE.environment()
                .getProperty(IEnvironment.Keys.GAMEDIR.get())
                .orElseThrow(()->new IllegalStateException("MISSING GAMEDIR?!"));
        fileCacheManager = new FileCacheManager();
        pack = new CursePack(gameDir, fileCacheManager);
    }

    public CurseLocator(Path dir) throws IOException {
        fileCacheManager = new FileCacheManager(DirHandler.createOrGetDirectory(dir, "dummycache"));
        pack = new CursePack(dir, fileCacheManager);
    }

    @Override
    public List<IModFile> scanMods() {
        if (!pack.isValidPack()) return Collections.emptyList();
        pack.waitForPackDownload();
        final List<IModFile> modFiles = wrappedModFolderLocator.scanMods();
        return modFiles.stream().filter(pack::fileInPack).collect(Collectors.toList());
    }

    @Override
    public String name() {
        return "cursepack downloader";
    }

    @Override
    public Path findPath(final IModFile modFile, final String... path) {
        return wrappedModFolderLocator.findPath(modFile, path);
    }

    @Override
    public void scanFile(final IModFile modFile, final Consumer<Path> pathConsumer) {
        wrappedModFolderLocator.scanFile(modFile, pathConsumer);
    }

    @Override
    public Optional<Manifest> findManifest(final Path file) {
        return wrappedModFolderLocator.findManifest(file);
    }

    @Override
    public void initArguments(final Map<String, ?> arguments) {
        if (!pack.isValidPack()) return;
        final Function<Path, IModLocator> modFolderLocatorFactory = Launcher.INSTANCE.environment()
                .getProperty(Environment.Keys.MODFOLDERFACTORY.get())
                .orElseThrow(()->new RuntimeException("Unable to locate ModsFolder locator factory"));
        final Path cursemods = pack.getCurseModPath();
        wrappedModFolderLocator = modFolderLocatorFactory.apply(cursemods);
        pack.startPackDownload();
    }

    @Override
    public boolean isValid(final IModFile modFile) {
        return wrappedModFolderLocator.isValid(modFile);
    }
}
