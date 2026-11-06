package com.eiu.capstone.backend.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.feedback.ConvertedAttachment;

@Component
public class FeedbackImageConverter {

    public static final int MAX_SIDE = 1920;
    public static final float QUALITY = 0.8f;

    public ConvertedAttachment convertRasterToWebp(String originalFilename, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw unprocessable("Image file is empty.");
        }
        if (!isPng(bytes) && !isJpeg(bytes)) {
            throw unprocessable("PNG and JPG files must be valid images.");
        }
        ImageIO.scanForPlugins();
        BufferedImage image;
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            image = ImageIO.read(in);
        } catch (IOException e) {
            throw unprocessable("Could not read the image.");
        }
        if (image == null) {
            throw unprocessable("PNG and JPG files must be valid images.");
        }
        BufferedImage scaled = scaleIfNeeded(image);
        byte[] webp = encodeWebp(scaled);
        return new ConvertedAttachment(originalFilename, "image/webp", webp);
    }

    public ConvertedAttachment passthroughSource(String originalFilename, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw unprocessable("File is empty.");
        }
        return new ConvertedAttachment(originalFilename, contentType, bytes);
    }

    static boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && bytes[0] == (byte) 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47;
    }

    static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && bytes[0] == (byte) 0xFF
                && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF;
    }

    static String suffixOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static BufferedImage scaleIfNeeded(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longest = Math.max(width, height);
        if (longest <= MAX_SIDE) {
            return source;
        }
        double scale = MAX_SIDE / (double) longest;
        int nextWidth = Math.max(1, (int) Math.round(width * scale));
        int nextHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage scaled = new BufferedImage(nextWidth, nextHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, nextWidth, nextHeight, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    private static byte[] encodeWebp(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
        if (!writers.hasNext()) {
            throw unprocessable("Could not convert the image to WebP.");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                String[] types = param.getCompressionTypes();
                if (types != null && types.length > 0) {
                    String lossy = null;
                    for (String type : types) {
                        if (type != null && type.toLowerCase(Locale.ROOT).contains("lossy")) {
                            lossy = type;
                            break;
                        }
                    }
                    param.setCompressionType(lossy != null ? lossy : types[0]);
                }
                param.setCompressionQuality(QUALITY);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw unprocessable("Could not convert the image to WebP.");
        } finally {
            writer.dispose();
        }
        byte[] encoded = out.toByteArray();
        if (encoded.length == 0) {
            throw unprocessable("Could not convert the image to WebP.");
        }
        return encoded;
    }

    private static ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
