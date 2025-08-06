package org.lukasz.filmcategorizationsystem;

import jakarta.validation.ConstraintViolation;
import org.lukasz.filmcategorizationsystem.exceptions.CustomValidationException;
import org.lukasz.filmcategorizationsystem.exceptions.FileException;
import org.lukasz.filmcategorizationsystem.exceptions.MediaFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Validation {
    private final LocalValidatorFactoryBean validatorFactoryBean;
    private final Logger logger = LoggerFactory.getLogger(Validation.class);

    public Validation(LocalValidatorFactoryBean validatorFactoryBean) {
        this.validatorFactoryBean = validatorFactoryBean;
    }

    <T> void validation(T t) {

        Set<ConstraintViolation<T>> violations = validatorFactoryBean.validate(t);


        if (!violations.isEmpty()) {
            String errorMessage = violations.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(" , "));

            logger.error("inside validation  errors: {}", errorMessage);

            throw new CustomValidationException(errorMessage);
        }


    }

    void validateVideoFile(MultipartFile file) {

        long fileOver1GB = 1_073_741_824L;
        if (file.getSize() > fileOver1GB) {
            logger.error("File {} exceeds the maximum allowed size of 1GB  {}", file.getOriginalFilename(), file.getSize());
            throw new FileException("File size exceeds the maximum allowed size of 1GB");
        }
        Set<String> supportedVideoTypes = Set.of("video/mp4", "video/x-matroska", "video/x-msvideo");

        String contentType = file.getContentType();

        if (!supportedVideoTypes.contains(contentType)) {
            throw new MediaFileException("Only video files (.mp4, .avi, .mkv) are allowed");
        }
    }
}
