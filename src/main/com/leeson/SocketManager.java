package com.leeson;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socket manager maintains a connection with external peer (client or server) until stop() is called.
 * For client-mode socket manager, reconnecting will be performed when disconnected.
 * 
 * @author leeson
 * @date 2020/08/24
 */
public abstract class SocketManager {

     public static final Logger LOGGER = LoggerFactory.getLogger(SocketManager.class);
     
     protected SocketConfiguration config;
     protected ConnectionHandler handler;
     protected boolean stopFlag = false;
     
     static final int MAX_CACHED_MESSAGE_SIZE = 10;
     protected Queue<Object> cacheMessageQueue = new java.util.concurrent.ArrayBlockingQueue<Object>(MAX_CACHED_MESSAGE_SIZE);
     
     static SocketManager getSocketManager(SocketConfiguration config) {
         if (config.mode == 0 || config.mode == 2) {
             return new ServerSocketManager(config);
         } else {
             return new ClientSocketManager(config);
         }
     }
     
     SocketManager(SocketConfiguration config) {
         this.config = config;
         handler = new ConnectionHandler();  // handler for receiving and forwarding messages
         handler.addOpenListener(new ConnectionOpenListener() {
            @Override
            public void onConnectionOpen() {
                // send cached messages when connection recovered
                while (!cacheMessageQueue.isEmpty()) {
                    Object message = cacheMessageQueue.peek();
                    if (handler.sendMessage(message)) {
                        cacheMessageQueue.remove();
                    } else {
                        LOGGER.warn("Failed when sending cached message!");
                        handler.stopSockets();
                        return;
                    }
                }
            }
         });
     }
         
     public boolean isReady() {
         return handler.isConnected() && !stopFlag;
     }
     
     /**
      * Starting socket manager to accept connection from or connect to the external peer.
      */
     abstract public boolean start();

     
     /**
      * Actively stop the connection with external peer. 
      * Usually called when disposed.
      * 
      */
     public void stop() {
         stopFlag = true;
         handler.stopSockets();
     }
     
     /**
      * Adding listeners to be notified when receiving messages from the external peer
      * 
      * @param listener  MessageListener
      */
     public void addMessageListener(MessageListener listener) {
         handler.addMessageListener(listener);
     }

     /**
      * Send message to the external peer
      * 
      * @param message  MINA stream message to be sent
      * @return whether successful
      */
     public boolean sendMessage(Object message) {
         // received message from other side
         if (!isReady()) {
             LOGGER.info("Cached message {} when not ready. Currently cached {} messages", 
                 message, cacheMessageQueue.size());
             
             if (cacheMessageQueue.size() == MAX_CACHED_MESSAGE_SIZE) {
                 cacheMessageQueue.remove();
             }
             cacheMessageQueue.add(message);
             return false;
         }
         return handler.sendMessage(message);
     }
 }

