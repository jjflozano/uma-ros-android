package com.example.umarosandroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.core.util.Preconditions;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.concurrent.CancellableLoop;
import org.ros.internal.message.MessageBuffers;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import std_msgs.Bool;
import std_msgs.UInt16MultiArray;
import std_msgs.UInt8;
import std_msgs.UInt8MultiArray;

public class CuadrigaNode extends AbstractNodeMain {

    private String nodeName;
    private Publisher<Bool> cuadrigaPublisher;
    private boolean enableCuadriga;

    public CuadrigaNode(String nodeName, boolean enableCuadriga) {
        this.nodeName = nodeName;
        this.enableCuadriga = enableCuadriga;

    }
    public GraphName getDefaultNodeName()
    {
        return GraphName.of(nodeName+"/cuadrigaNode");
    }

    public void onStart(ConnectedNode connectedNode) {
        cuadrigaPublisher = connectedNode.newPublisher(nodeName+"/calling_to_Cuadriga",Bool._TYPE);
        Bool cuadrigaMsg = cuadrigaPublisher.newMessage();

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void setup() {
                cuadrigaMsg.setData(enableCuadriga);

            }

            @Override
            protected void loop() throws InterruptedException {
                cuadrigaPublisher.publish(cuadrigaMsg);
                Thread.sleep(500);
            }
        });

    }
}