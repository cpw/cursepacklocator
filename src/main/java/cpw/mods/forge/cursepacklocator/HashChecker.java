package cpw.mods.forge.cursepacklocator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HashChecker {
    // Credit to https://github.com/comp500/packwiz-installer/blob/580408b92a214384e3764a8fc848775c149f2fef/src/main/java/link/infra/packwiz/installer/metadata/hash/Murmur2Hasher.java#L39-L52
    // Credit to https://github.com/modmuss50/CAV2/blob/master/murmur.go
    private static byte[] computeNormalizedArray(byte[] input) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (byte b : input) {
            if (!(b == 9 || b == 10 || b == 13 || b == 32)) {
                bos.write(b);
            }
        }
        return bos.toByteArray();
    }

    public static long computeHash(final Path file) {
        try {
            final byte[] bytes = Files.readAllBytes(file);
            final byte[] normalizedArray = computeNormalizedArray(bytes);
            int res = Murmur2.hash32(normalizedArray, normalizedArray.length, 1);
            return Integer.toUnsignedLong(res);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
