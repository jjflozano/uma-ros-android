package com.example.umarosandroid;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import std_msgs.Bool;
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

public class RoverJ8Node extends AbstractNodeMain {

    private String nodeName;
    private Publisher<Bool> roverJ8Publisher;
    private boolean enableRoverJ8;

    public RoverJ8Node(String nodeName, boolean enableRoverJ8) {
        this.nodeName = nodeName;
        this.enableRoverJ8= enableRoverJ8;
    }
    public GraphName getDefaultNodeName()
    {
        return GraphName.of(nodeName+"/roverJ8Node");
    }

    public void onStart(ConnectedNode connectedNode) {
        roverJ8Publisher = connectedNode.newPublisher(nodeName+"/calling_to_RoverJ8",Bool._TYPE);
        Bool roverJ8Msg = roverJ8Publisher.newMessage();

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void setup() {
                roverJ8Msg.setData(enableRoverJ8);

            }

            @Override
            protected void loop() throws InterruptedException {
                roverJ8Publisher.publish(roverJ8Msg);
                Thread.sleep(500);
            }
        });

    }
}