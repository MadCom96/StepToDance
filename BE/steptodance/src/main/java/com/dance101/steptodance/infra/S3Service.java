package com.dance101.steptodance.infra;

import static org.springframework.util.StringUtils.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.dance101.steptodance.global.exception.category.NotFoundException;
import com.dance101.steptodance.global.utils.FileUtil;
import com.dance101.steptodance.guide.domain.GuideBodyModel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class S3Service {
	@Value(value = "${cloud.aws.s3.bucket}")
	private String bucket;

	private final AmazonS3 amazonS3;

	/* 1. 파일 업로드 */
	public String upload(MultipartFile multipartFile, String s3FileName) throws IOException {
		// 메타데이터 생성
		ObjectMetadata objMeta = new ObjectMetadata();
		objMeta.setContentLength(multipartFile.getInputStream().available());
		// putObject(버킷명, 파일명, 파일데이터, 메타데이터)로 S3에 객체 등록
		amazonS3.putObject(bucket, s3FileName, multipartFile.getInputStream(), objMeta);
		// 등록된 객체의 url 반환 (decoder: url 안의 한글or특수문자 깨짐 방지)
		return URLDecoder.decode(amazonS3.getUrl(bucket, s3FileName).toString(), "utf-8");
	}

	/* 2. 파일 삭제 */
	public void delete (String keyName) {
		try {
			ListObjectsV2Request listRequest = new ListObjectsV2Request()
				.withBucketName(bucket)
				.withPrefix(keyName);
			log.info("delete: listRequest=" + listRequest);

			ListObjectsV2Result listObjectsV2Result = amazonS3.listObjectsV2(listRequest);
			// deleteObject(버킷명, 키값)으로 객체 삭제
			for (S3ObjectSummary object : listObjectsV2Result.getObjectSummaries()) {
				amazonS3.deleteObject(bucket, object.getKey());
			}
		} catch (AmazonServiceException e) {
			log.error(e.toString());
		}
	}

	/* 3. 파일의 presigned URL 반환 */
	public String getPresignedURL (String keyName) {
		String preSignedURL = "";
		// presigned URL이 유효하게 동작할 만료기한 설정 (2분)
		Date expiration = new Date();
		Long expTimeMillis = expiration.getTime();
		expTimeMillis += 1000 * 60 * 2;
		expiration.setTime(expTimeMillis);

		try {
			// presigned URL 발급
			GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucket, keyName)
				.withMethod(HttpMethod.GET)
				.withExpiration(expiration);
			URL url = amazonS3.generatePresignedUrl(generatePresignedUrlRequest);
			preSignedURL = url.toString();
		} catch (Exception e) {
			log.error(e.toString());
		}

		return preSignedURL;
	}

	public Path download(String storedFileName) throws IOException {
		ListObjectsV2Request listRequest = new ListObjectsV2Request()
			.withBucketName(bucket)
			.withPrefix(storedFileName);
		log.info("download: listRequest=" + storedFileName);

		ListObjectsV2Result listObjectsV2Result = amazonS3.listObjectsV2(listRequest);
		S3Object o = null;
		for (S3ObjectSummary object : listObjectsV2Result.getObjectSummaries()) {
			o = amazonS3.getObject(new GetObjectRequest(bucket, object.getKey()));
		}
		S3ObjectInputStream objectInputStream = o.getObjectContent();
		String extension = StringUtils.getFilenameExtension(o.getKey());

		Path tempFilePath = Files.createTempFile("temp-", "."+extension);
		Files.copy(objectInputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
		log.info("download: s3에서 영상 다운로드 성공");
		return tempFilePath;
	}

}
