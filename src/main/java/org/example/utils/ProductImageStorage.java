package org.example.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Copie les images uploadées dans le dossier {@code product_images} (sous le répertoire de travail JVM)
 * et charge les visuels produits (fichier local ou ressource classpath {@code /images/...}).
 */
public final class ProductImageStorage {

    private static final String DIR_NAME = "product_images";

    private ProductImageStorage() {
    }

    public static Path getStorageDirectory() {
        Path dir = Paths.get(System.getProperty("user.dir", ".")).resolve(DIR_NAME);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dir;
    }

    /**
     * Copie le fichier choisi vers le dossier de stockage avec un nom unique.
     *
     * @return nom de fichier seul à enregistrer en base (colonne {@code image_produit})
     */
    public static String copyUploadedFile(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        if (!Files.isRegularFile(source)) {
            throw new IOException("Fichier introuvable : " + source);
        }
        String original = source.getFileName().toString();
        String ext = extensionOf(original);
        int dot = original.lastIndexOf('.');
        String stem = dot > 0 ? original.substring(0, dot) : original;
        String safeStem = stem.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeStem.isEmpty()) {
            safeStem = "image";
        }
        if (safeStem.length() > 80) {
            safeStem = safeStem.substring(0, 80);
        }
        String unique = System.currentTimeMillis() + "_" + safeStem + ext;
        Path dest = getStorageDirectory().resolve(unique);
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest.getFileName().toString();
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return ".jpg";
        }
        String ext = filename.substring(dot).toLowerCase();
        if (ext.matches("\\.(png|jpe?g|gif|webp|bmp)")) {
            return ext;
        }
        return ".jpg";
    }

    /**
     * Affiche l’image du produit : d’abord {@code product_images/nom}, sinon ressource {@code /images/nom}.
     */
    public static void applyToImageView(ImageView imageView, String storedFileName) {
        if (imageView == null || storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        String name = storedFileName.trim();
        if (name.contains("..") || name.contains("/") || name.contains("\\")) {
            return;
        }
        Path local = getStorageDirectory().resolve(name);
        if (Files.isRegularFile(local)) {
            imageView.setImage(new Image(local.toUri().toString(), true));
            return;
        }
        var url = ProductImageStorage.class.getResource("/images/" + name);
        if (url != null) {
            imageView.setImage(new Image(url.toExternalForm(), true));
        }
    }
}
