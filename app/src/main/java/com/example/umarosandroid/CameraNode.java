package com.example.umarosandroid;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Size;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.concurrent.CancellableLoop;
import org.ros.internal.message.MessageBuffers;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutionException;

import sensor_msgs.CameraInfo;
import sensor_msgs.CompressedImage;
import sensor_msgs.Image;
import std_msgs.UInt8MultiArray;

/**
 * @author germanruizmudarra@gmail.com (Germán Ruiz-Mudarra)
 */

public class CameraNode extends AbstractNodeMain {
    private final Context context;
    private final ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private final int HEIGHT = 1080;
    private final int WIDTH = 1920;
    //private final PreviewView previewView;

    private Time currentTime;
    private Publisher<CameraInfo> cameraInfoPublisher;
    private Publisher<CompressedImage> compressedImagePublisher;
    private String nodeName;
    private  String frameId;


    public CameraNode(Context context, ListenableFuture<ProcessCameraProvider> cameraProviderFuture, String nodeName) {
        this.context = context;
        this.cameraProviderFuture = cameraProviderFuture;
        //this.previewView = previewView;
        this.nodeName = nodeName;
    }
    @Override
    public GraphName getDefaultNodeName()
    {
        return GraphName.of(nodeName+"/CameraNode");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        frameId = nodeName+"/camera";
        cameraInfoPublisher = connectedNode.newPublisher(nodeName+"/camera/camera_info", CameraInfo._TYPE);
        compressedImagePublisher = connectedNode.newPublisher(nodeName+"/camera/compressed", CompressedImage._TYPE);
        CameraInfo cameraInfo = cameraInfoPublisher.newMessage();
        CompressedImage compressedImage = compressedImagePublisher.newMessage();


        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private final ChannelBufferOutputStream stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
            private int width;
            private int height;
            @Override
            protected void setup() {
                cameraProviderFuture.addListener(() -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        bindPreviewAndAnalysis(cameraProvider);
                    } catch (ExecutionException | InterruptedException e) {
                        // No errors need to be handled for this Future.
                        // This should never be reached.
                    }
                }, ContextCompat.getMainExecutor(context));
            }

            @Override
            protected void loop() {
            }

            void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
                //Preview preview = new Preview.Builder()
                //        .build();
                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(WIDTH, HEIGHT))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                //preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
                    // analyze is called everytime a frame is captured so this is our main loop
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        byte[] nv21 = imageProxyToYuvImage(imageProxy);
                        width = imageProxy.getWidth();
                        height = imageProxy.getHeight();

                        updateCameraInfo();
                        updateCompressedImage(nv21);
                        //updateRawImage(nv21);
                        imageProxy.close();
                    }
                });
                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageAnalysis);
            }

            private byte[] imageProxyToYuvImage(ImageProxy image) {
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[2].getBuffer();

                int ySize = yBuffer.remaining();
                int vSize = vBuffer.remaining();
                int uSize = uBuffer.remaining();

                byte[] nv21 = new byte[ySize + uSize + vSize];
                // U a V channels are swapped
                yBuffer.get(nv21,0,ySize);
                uBuffer.get(nv21,ySize,uSize);
                vBuffer.get(nv21,ySize+uSize,vSize);

                return nv21;
            }

            public void updateCameraInfo() {
                currentTime = connectedNode.getCurrentTime();
                cameraInfo.getHeader().setStamp(currentTime);
                cameraInfo.getHeader().setFrameId(frameId);

                cameraInfo.setWidth(width);
                cameraInfo.setHeight(height);

                cameraInfoPublisher.publish(cameraInfo);
            }

            @SuppressLint("RestrictedApi")
            public void updateCompressedImage(byte[] nv21) {
                Preconditions.checkNotNull(nv21);

                YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
                compressedImage.setFormat("jpeg");

                stream.buffer().writeBytes(out.toByteArray());
                compressedImage.setData(stream.buffer().copy());
                stream.buffer().clear();

                currentTime = connectedNode.getCurrentTime();
                compressedImage.getHeader().setStamp(currentTime);
                compressedImage.getHeader().setFrameId(frameId);

                compressedImagePublisher.publish(compressedImage);
            }
        });
    }
}