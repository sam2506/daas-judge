package com.docker.sandbox.amazons3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AmazonS3Service {

    @Autowired
    private AmazonS3 s3client;

    @Value("${aws.testcases.bucketName}")
    private String bucketName;

    public byte[] downloadFile(final String keyName) {
        byte[] content = null;
        final S3Object s3Object = s3client.getObject(bucketName, keyName);
        final S3ObjectInputStream stream = s3Object.getObjectContent();
        try {
            content = IOUtils.toByteArray(stream);
            s3Object.close();
        } catch(final IOException ex) {
            ex.printStackTrace();
        }
        return content;
    }
}
