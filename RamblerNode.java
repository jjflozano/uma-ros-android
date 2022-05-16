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

public class RamblerNode extends AbstractNodeMain {

    private String nodeName;
    private Publisher<Bool> ramblerPublisher;
    private boolean enableRambler;

    public RamblerNode(String nodeName, boolean enableRambler) {
        this.nodeName = nodeName;
        this.enableRambler= enableRambler;
    }
    public GraphName getDefaultNodeName()
    {
        return GraphName.of(nodeName+"/ramblerNode");
    }

    public void onStart(ConnectedNode connectedNode) {
        ramblerPublisher = connectedNode.newPublisher(nodeName+"/calling_to_Rambler",Bool._TYPE);
        Bool ramblerMsg = ramblerPublisher.newMessage();

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void setup() {
                ramblerMsg.setData(enableRambler);

            }

            @Override
            protected void loop() throws InterruptedException {
                ramblerPublisher.publish(ramblerMsg);
                Thread.sleep(500);
            }
        });

    }
}