package com.leeson;

import java.net.InetSocketAddress;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connects the remote server, and forward messages to/from the other peer
 * 
 * @author leeson
 * @date 2020/09/07
 */
public class ClientSocketManager extends SocketManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(ClientSocketManager.class);
    
    private static final int RECONNECT_INTERVAL = 10;   //10 seconds to reconnect
    private NioSocketConnector connector;
    
    ClientSocketManager(SocketConfiguration config) {
        super(config);
        connector = new NioSocketConnector();
        connector.setHandler(handler);
        InetSocketAddress remoteAddress = new InetSocketAddress(config.ip1, config.port1);            
        connector.setDefaultRemoteAddress(remoteAddress);
        
        // handle close events from external peer
        handler.addCloseListener(new ConnectionCloseListener() {
            @Override
            public void onConnectionClose() {
                // try reconnecting until successful
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!handler.isConnected() && !stopFlag) {
                            start();
                            try {
                                Thread.sleep(RECONNECT_INTERVAL * 1000);
                            } catch (Exception e) {
                                 // ignore exception
                            }
                        }
                    }
                }).start();
            }
        });            
    }
    /**
     * Starting socket manager to connect to the external peer.
     */
    public boolean start() {
        try {
            ConnectFuture connectFuture = connector.connect();
            connectFuture.await();
            if (!handler.isConnected()) {
                LOGGER.warn("Cannot connected to {}:{} ..", config.ip1, config.port1);
                return false;
            }
            LOGGER.info("Connected to {}:{}", config.ip1, config.port1);
        } catch (InterruptedException e) {
             LOGGER.error(e.getLocalizedMessage(), e);
        }
        return true;
    }
}