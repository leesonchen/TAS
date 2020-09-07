 package com.leeson;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ServerSocketManager extends SocketManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(ServerSocketManager.class);
    IoAcceptor acceptor;
    
    ServerSocketManager(SocketConfiguration config) {
        super(config);
    }
        
    /**
     * Starting server socket manager in order to accept connection from the external peer.
     * 
     * @return whether server socket is listening
     * @see #isReady() to check whether the external peer has been connected
     */
    public boolean start() {
        acceptor = new NioSocketAcceptor();
        acceptor.setHandler(handler);
        try {
            acceptor.bind(new InetSocketAddress(config.port1));
        } catch (IOException e) {
            LOGGER.error("Cannot open server port!", e);
            return false;
        }
        LOGGER.info("Listening to :{}", config.port1);
        return true;
    }
    
    
    /**
     * Actively stop the connection with external peer. 
     * Usually called when disposed.
     * 
     */
    public void stop() {
        super.stop();
        acceptor.unbind();
        acceptor.dispose();
    }
}

