package com.example.jobtracker.service.avatar;

import com.example.jobtracker.config.AvatarProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AvatarImageProcessor {

    private final int maxDimension;
    private final int thumbnailSize;

    public AvatarImageProcessor(AvatarProperties properties) {
        this.maxDimension = properties.maxDimension();
        this.thumbnailSize = properties.thumbnailSize();
        ImageIO.scanForPlugins();
    }

    public AvatarImage process(byte[] content) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw unsupportedType();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase();
                String contentType;
                if (format.equals("jpeg") || format.equals("jpg")) {
                    contentType = "image/jpeg";
                } else if (format.equals("png")) {
                    contentType = "image/png";
                } else if (format.equals("webp")) {
                    contentType = "image/webp";
                } else {
                    throw unsupportedType();
                }

                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > maxDimension || height > maxDimension) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Image must not be larger than " + maxDimension + "x" + maxDimension + " pixels");
                }

                boolean transparent = reader.getImageTypes(0).next().getColorModel().hasAlpha();
                String thumbnailFormat = transparent ? "png" : "jpg";
                String thumbnailContentType = transparent ? "image/png" : "image/jpeg";
                byte[] thumbnail = makeThumbnail(content, thumbnailFormat);

                return new AvatarImage(contentType, width, height, thumbnail, thumbnailContentType);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is not a valid image");
        }
    }

    private byte[] makeThumbnail(byte[] content, String format) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(content))
                .size(thumbnailSize, thumbnailSize)
                .crop(Positions.CENTER)
                .outputFormat(format)
                .toOutputStream(output);
        return output.toByteArray();
    }

    private static ResponseStatusException unsupportedType() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG and WebP images are allowed");
    }
}
