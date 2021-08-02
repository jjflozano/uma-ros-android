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

public class NLPNode extends AbstractNodeMain {

    private String nodeName;
    private Publisher<Bool> nlpPubliser;
    private boolean enableNlp;

    public NLPNode(String nodeName, boolean enableNlp) {
        this.nodeName = nodeName;
        this.enableNlp = enableNlp;
    }

    @Override
    public GraphName getDefaultNodeName()
    {
        return GraphName.of(nodeName+"/NLPNode");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        nlpPubliser = connectedNode.newPublisher(nodeName+"/nlp_activated",Bool._TYPE);
        Bool nlpMsg = nlpPubliser.newMessage();

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            @Override
            protected void setup() {
                nlpMsg.setData(enableNlp);

            }

            @Override
            protected void loop() throws InterruptedException {
                nlpPubliser.publish(nlpMsg);
                Thread.sleep(1000);
            }
        });

    }
}
