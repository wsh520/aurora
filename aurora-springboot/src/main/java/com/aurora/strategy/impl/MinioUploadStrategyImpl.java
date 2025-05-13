package com.aurora.strategy.impl;

import com.aurora.config.properties.MinioProperties;
import com.aurora.exception.BizException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.StatObjectArgs;
import io.minio.messages.DeleteObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("minioUploadStrategyImpl")
public class MinioUploadStrategyImpl extends AbstractUploadStrategyImpl {

    @Autowired
    private MinioProperties minioProperties;

    @Override
    public Boolean exists(String filePath) {
        boolean exist = true;
        try {
            getMinioClient()
                    .statObject(StatObjectArgs.builder().bucket(minioProperties.getBucketName()).object(filePath).build());
        } catch (Exception e) {
            exist = false;
        }
        return exist;
    }

    @SneakyThrows
    @Override
    public void upload(String path, String fileName, InputStream inputStream) {
        getMinioClient().putObject(
                PutObjectArgs.builder().bucket(minioProperties.getBucketName()).object(path + fileName).stream(
                                inputStream, inputStream.available(), -1)
                        .build());
    }

    @Override
    public String getFileAccessUrl(String filePath) {
        return minioProperties.getUrl() + filePath;
    }

    @Override
    public Boolean createFilePath(String path) {
        // 确保路径以 / 结尾
        if (!path.endsWith("/")) {
            path += "/";
        }

        try (InputStream emptyStream = new ByteArrayInputStream(new byte[0])) {
            getMinioClient().putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(path)
                            .stream(emptyStream, 0, -1)
                            .build()
            );
        } catch (Exception e) {
            log.error("创建虚拟目录失败: " + path, e);
            throw new BizException("创建虚拟目录失败");
        }
        System.out.println("虚拟目录已创建: " + path);
        return true;
    }

    @Override
    public void deleteFiles(List<String> filePathList) {
        try {
            List<DeleteObject> deleteObjects = filePathList.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            RemoveObjectsArgs args = RemoveObjectsArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .objects(deleteObjects)
                    .build();

            getMinioClient().removeObjects(args);

            log.info("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除文件失败", e);
            throw new BizException("批量删除文件失败");
        }
    }

    private MinioClient getMinioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

}
