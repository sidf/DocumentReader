package io.github.sidf.documentreader.featuredetection;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Rect;
import org.opencv.core.MatOfRect;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;

import io.github.sidf.documentreader.util.CommandUtil;
import io.github.sidf.documentreader.util.PathUtil;

public class FeatureDetector implements Runnable, AutoCloseable {
	private boolean isStillRunning;
	private VideoCapture captureDevice;
	private static FeatureDetector instance;
	private ScheduledFuture scheduledFuture;
	private static CascadeClassifier faceClassifier;
	private static CascadeClassifier leftEyeClassifier;
	private static CascadeClassifier rightEyeClassifier;
	private ScheduledExecutorService scheduledExecutorService;
	private static Logger logger = Logger.getLogger(FeatureDetector.class.getName());
	private static final String[] autofocusTweakCommands = { "uvcdynctrl --set='Focus, Auto' 0", "uvcdynctrl --set='Focus (absolute)' 5" };
	
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		leftEyeClassifier = new CascadeClassifier(PathUtil.getResourcePath("cascades/left_eye_lbp.xml"));
		rightEyeClassifier = new CascadeClassifier(PathUtil.getResourcePath("cascades/right_eye_lbp.xml"));
		faceClassifier = new CascadeClassifier(PathUtil.getResourcePath("cascades/lbpcascade_frontalface.xml"));
	}
	
	private FeatureDetector() throws IOException {
		captureDevice = new VideoCapture(0);

		if (!captureDevice.isOpened()) {
			String message = "Could not open video capture device";
			logger.warning(message);
			throw new IOException(message);
		}
		
		disableAutofocus();
		
//		captureDevice.set(3, 1280);
//		captureDevice.set(4, 800);
	}
	
	public static FeatureDetector getInstance() throws IOException{
		if (instance == null) {
			instance = new FeatureDetector();
		}
		return instance;
	}
	
	public void run() {
		featureDetectorLoop();
	}
	
	private void featureDetectorLoop() {
		isStillRunning = true;
		scheduledExecutorService = Executors.newScheduledThreadPool(2);
		
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				System.runFinalization();
				System.gc();
			}
			
		}, 20, 20, TimeUnit.SECONDS);
		
		Mat image;
		Mat grayImage;
		
		while (isStillRunning) {
			image = new Mat();
			grayImage = new Mat();
			
			captureDevice.read(image);
			Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
			
			MatOfRect faceDetections = detectFaces(grayImage);
			
			if (faceDetections.elemSize() == 0) {
				continue;
			}
			
			Mat grayFaceImage = grayImage.submat(faceDetections.toArray()[0]);
//			saveImageToDesktop(grayFaceImage, "face");
			
			int x = 0;
			int y = (int)(grayFaceImage.size().height * 0.2);
			int width = (int)grayFaceImage.size().width;
			int height = (int)(y * 1.7);

			Rect eyeRegionRect = new Rect(x, y, width, height);
			Mat eyeRegionImage = grayFaceImage.submat(eyeRegionRect);
			
			MatOfRect eyeDetections = detectClosedEyes(eyeRegionImage);
			managerSchedule(eyeDetections);			
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				stop();
			}
		}

	}
	
	private void managerSchedule(Mat eyeDetections) {
		if (eyeDetections.elemSize() == 0) {
			if (scheduledFuture != null) {
				scheduledFuture.cancel(false);
				scheduledFuture = null;
				logger.info("Disabled schedule");
			}
		} else if (scheduledFuture == null) {
			scheduledFuture = scheduledExecutorService.schedule(new Runnable() {
				public void run() {
					stop();
					logger.info("Enabled schedule");
				}
			}, 30, TimeUnit.SECONDS);
		}
	}
	
	private MatOfRect detectFaces(Mat grayImage) {
		MatOfRect faces = new MatOfRect();
		faceClassifier.detectMultiScale(grayImage, faces);
		return faces;
	}
	
	private MatOfRect detectClosedEyes(Mat eyeRegionImage) {
		int width = (int)eyeRegionImage.size().width / 2;
		int height = (int)eyeRegionImage.size().height;
		
		Mat rightEyeRegion = eyeRegionImage.submat(0, height, 0, width);
		MatOfRect rightEyes = detectClosedEyes(rightEyeClassifier, rightEyeRegion);
		
		if (rightEyes.toArray().length != 0) {
			System.out.println("right detected");
			saveImageToDesktop(rightEyeRegion, "right");
			return rightEyes;
		}
		
		Mat leftEyeRegion = eyeRegionImage.submat(0, height, width, (int)eyeRegionImage.size().width);
		MatOfRect leftEyes = detectClosedEyes(leftEyeClassifier, leftEyeRegion);
		if (leftEyes.toArray().length != 0) {
			System.out.println("left detected");
//			saveImageToDesktop(leftEyeRegion, "left");
		}
		return leftEyes;
	}
	
	private MatOfRect detectClosedEyes(CascadeClassifier classifier, Mat grayFaceImage) {
		MatOfRect mat = new MatOfRect();
		classifier.detectMultiScale(grayFaceImage, mat);
		return mat;
	}
	
	private void saveImageToDesktop(Mat image, String imageName) {
		String path = System.getProperty("user.home") + "/Desktop";;
		
		if (!new File(path).exists()) {
			logger.warning(String.format("Could not save image %s", imageName));
		}
		
		Highgui.imwrite(String.format("%s/%s.jpg", path, imageName), image);
	}
	
	public void stop() {
		isStillRunning = false;
		
		if (scheduledExecutorService != null) {
			scheduledExecutorService.shutdown();
		}
	}

	public void close() throws Exception {
		captureDevice.release();
	}
	
	private void disableAutofocus() {
		logger.info("Trying to disable autofocus");
		for (String command : autofocusTweakCommands) {
			try {
				CommandUtil.launchNonBlockingCommand(command);
			} catch (Exception e) {
				logger.log(Level.WARNING, "Something went wrong while trying to disable autofocus", e);
			}
		}
	}
}
