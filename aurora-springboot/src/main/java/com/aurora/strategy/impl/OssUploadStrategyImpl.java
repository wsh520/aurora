package com.aurora.strategy.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aurora.config.properties.OssConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

@Service("ossUploadStrategyImpl")
public class OssUploadStrategyImpl extends AbstractUploadStrategyImpl {

    @Autowired
    private OssConfigProperties ossConfigProperties;

    @Override
    public Boolean exists(String filePath) {
        return getOssClient().doesObjectExist(ossConfigProperties.getBucketName(), filePath);
    }

    @Override
    public void upload(String path, String fileName, InputStream inputStream) {
        getOssClient().putObject(ossConfigProperties.getBucketName(), path + fileName, inputStream);
    }

    @Override
    public String getFileAccessUrl(String filePath) {
        return ossConfigProperties.getUrl() + filePath;
    }


    @Override
    public Boolean createFilePath(String path) {
        OSS ossClient = getOssClient();
        // 确保路径以 / 结尾
        if (!path.endsWith("/")) {
            path += "/";
        }

        // 创建空对象表示目录
        ossClient.putObject(ossConfigProperties.getBucketName(), path, new ByteArrayInputStream(new byte[0]));

        System.out.println("虚拟目录已创建: " + path);
        ossClient.shutdown();
        return true;
    }

    @Override
    public void deleteFiles(List<String> filePathList) {

        try {
            DeleteObjectsRequest request = new DeleteObjectsRequest(ossConfigProperties.getBucketName());
            request.setKeys(filePathList);

            getOssClient().deleteObjects(request);
        } finally {
            getOssClient().shutdown();
        }
    }

    private OSS getOssClient() {
        return new OSSClientBuilder().build(ossConfigProperties.getEndpoint(), ossConfigProperties.getAccessKeyId(), ossConfigProperties.getAccessKeySecret());
    }

}
