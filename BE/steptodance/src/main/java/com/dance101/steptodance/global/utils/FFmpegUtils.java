package com.dance101.steptodance.global.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import com.dance101.steptodance.guide.data.request.GuideMessageRequest;
import com.dance101.steptodance.guide.service.AIServerService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class FFmpegUtils {
	private final String ffmpegPath;
	private final String ffprobePath;
	private FFmpeg ffmpeg;
	private FFprobe ffprobe;
	private final String outputDirPath = "data/vod/output";
	private final AIServerService aiServerService;

	public FFmpegUtils(
		@Value("${ffmpeg.location}")String ffmpegPath,
		@Value("${ffprobe.location}")String ffprobePath,
		AIServerService aiServerService) throws IOException
	{
		this.ffmpegPath = ffmpegPath;
		this.ffprobePath = ffprobePath;
		this.aiServerService = aiServerService;
		ffmpeg = new FFmpeg(this.ffmpegPath);
		ffprobe = new FFprobe(this.ffprobePath);
	}

	public void sendGuideVodToKafka(long id, MultipartFile video) throws IOException {
		Path tempFilePath = Files.createTempFile("temp-", ".mp4");
		video.transferTo(tempFilePath);

		Files.createDirectories(Path.of(outputDirPath + id));

		FFmpegBuilder builder = new FFmpegBuilder()
			.setInput(tempFilePath.toString())
			.addOutput(outputDirPath+id+"/frame_%04d.png")
			.setVideoFrameRate(2, 1) // 1초에 2프레임 추출
			.done();

		FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
		executor.createJob(builder).run();

		System.out.println();
		try (Stream<Path> paths = Files.walk(Path.of(outputDirPath+id))) {
			paths.filter(Files::isRegularFile)
				.forEach(path -> {
					try {
						aiServerService.publish(
							GuideMessageRequest.builder()
								.guideId(id)
								.name(String.valueOf(path.getFileName()))
								.image(Files.readAllBytes(path))
								.size(300)
								.build()
						);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
			});
		} catch(IOException e) {
			e.printStackTrace();
		}

		// 이미지파일 삭제
		// TODO: 이미지들이 잘 지워지는가 확인
		Files.walk(Path.of(outputDirPath + id))
			.map(Path::toFile)
			.forEach(File::delete);
		Files.delete(Path.of(outputDirPath + id));
		// 영상파일 삭제
		Files.delete(tempFilePath);
		log.info("============== Sending Guide Vod Done ==============");
	}
}
