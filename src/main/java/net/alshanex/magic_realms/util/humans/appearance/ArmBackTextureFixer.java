package net.alshanex.magic_realms.util.humans.appearance;

import net.alshanex.magic_realms.MagicRealms;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ArmBackTextureFixer {
    public static BufferedImage fixArmBackStripes(BufferedImage originalTexture) {
        if (originalTexture == null) return null;

        BufferedImage fixedTexture = new BufferedImage(
                originalTexture.getWidth(),
                originalTexture.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = fixedTexture.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setComposite(AlphaComposite.Src);

        // Copiar toda la textura original
        g2d.drawImage(originalTexture, 0, 0, null);

        // Corregir específicamente la parte trasera de los brazos
        fixRightArmBack(g2d, originalTexture);
        fixLeftArmBack(g2d, originalTexture);

        g2d.dispose();

        //MagicRealms.LOGGER.debug("Fixed arm back stripes in texture");
        return fixedTexture;
    }

    private static void fixRightArmBack(Graphics2D g2d, BufferedImage source) {
        try {
            // Coordenadas de la parte trasera del brazo derecho en layout estándar de Minecraft
            int backX = 52, backY = 20, backWidth = 4, backHeight = 12;

            // Verificar si esta área tiene problemas (píxeles transparentes)
            if (hasTransparentPixels(source, backX, backY, backWidth, backHeight)) {
                //MagicRealms.LOGGER.debug("Fixing right arm back at {}x{} {}x{}", backX, backY, backWidth, backHeight);

                //Copiar desde el frente del brazo (espejo)
                copyAndMirrorRegion(g2d, source, 44, 20, 4, 12, backX, backY);
            }

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error fixing right arm back", e);
        }
    }

    private static void fixLeftArmBack(Graphics2D g2d, BufferedImage source) {
        try {
            // Coordenadas de la parte trasera del brazo izquierdo
            int backX = 44, backY = 52, backWidth = 4, backHeight = 12;

            if (hasTransparentPixels(source, backX, backY, backWidth, backHeight)) {
                //MagicRealms.LOGGER.debug("Fixing left arm back at {}x{} {}x{}", backX, backY, backWidth, backHeight);

                // Copiar desde el frente del brazo izquierdo (espejo)
                copyAndMirrorRegion(g2d, source, 36, 52, 4, 12, backX, backY);
            }

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error fixing left arm back", e);
        }
    }

    private static boolean hasTransparentPixels(BufferedImage source, int x, int y, int width, int height) {
        try {
            for (int dy = 0; dy < height && (y + dy) < source.getHeight(); dy++) {
                for (int dx = 0; dx < width && (x + dx) < source.getWidth(); dx++) {
                    int pixel = source.getRGB(x + dx, y + dy);
                    int alpha = (pixel >> 24) & 0xFF;

                    if (alpha < 200) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void copyAndMirrorRegion(Graphics2D g2d, BufferedImage source,
                                            int srcX, int srcY, int srcWidth, int srcHeight,
                                            int destX, int destY) {
        try {
            // Extraer la región fuente
            BufferedImage sourceRegion = source.getSubimage(srcX, srcY, srcWidth, srcHeight);

            // Crear imagen espejo (flip horizontal)
            BufferedImage mirroredRegion = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D mirrorG2d = mirroredRegion.createGraphics();

            // Voltear horizontalmente
            mirrorG2d.drawImage(sourceRegion, srcWidth, 0, 0, srcHeight, 0, 0, srcWidth, srcHeight, null);
            mirrorG2d.dispose();

            // Dibujar en la posición destino
            g2d.drawImage(mirroredRegion, destX, destY, null);

            //MagicRealms.LOGGER.debug("Copied and mirrored region from {}x{} to {}x{}", srcX, srcY, destX, destY);

        } catch (Exception e) {
            MagicRealms.LOGGER.error("Error copying and mirroring region", e);

            // Fallback: copiar sin espejo
            copyRegion(g2d, source, srcX, srcY, srcWidth, srcHeight, destX, destY);
        }
    }

    private static void copyRegion(Graphics2D g2d, BufferedImage source,
                                   int srcX, int srcY, int srcWidth, int srcHeight,
                                   int destX, int destY) {
        try {
            BufferedImage region = source.getSubimage(srcX, srcY, srcWidth, srcHeight);
            g2d.drawImage(region, destX, destY, null);
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error copying region from {}x{} to {}x{}", srcX, srcY, destX, destY);
        }
    }

    private static void fillRegionWithArmColor(Graphics2D g2d, BufferedImage source,
                                               int destX, int destY, int width, int height,
                                               int sampleX, int sampleY) {
        try {
            // Obtener color promedio del área de muestra
            Color armColor = getAverageColorFromRegion(source, sampleX, sampleY, 4, 12);

            if (armColor != null) {
                g2d.setColor(armColor);
                g2d.fillRect(destX, destY, width, height);

                //MagicRealms.LOGGER.debug("Filled arm back region with average color: {}", armColor);
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error filling region with arm color");
        }
    }

    private static Color getAverageColorFromRegion(BufferedImage source, int x, int y, int width, int height) {
        try {
            int totalRed = 0, totalGreen = 0, totalBlue = 0, totalAlpha = 0;
            int pixelCount = 0;

            for (int dy = 0; dy < height && (y + dy) < source.getHeight(); dy++) {
                for (int dx = 0; dx < width && (x + dx) < source.getWidth(); dx++) {
                    int rgb = source.getRGB(x + dx, y + dy);
                    int alpha = (rgb >> 24) & 0xFF;

                    if (alpha > 100) {
                        totalRed += (rgb >> 16) & 0xFF;
                        totalGreen += (rgb >> 8) & 0xFF;
                        totalBlue += rgb & 0xFF;
                        totalAlpha += alpha;
                        pixelCount++;
                    }
                }
            }

            if (pixelCount > 0) {
                return new Color(
                        totalRed / pixelCount,
                        totalGreen / pixelCount,
                        totalBlue / pixelCount,
                        Math.min(255, totalAlpha / pixelCount)
                );
            }

        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Error calculating average color");
        }

        return null;
    }
}
