package com.leeson;

import java.util.HashSet;
import java.util.Set;

import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface ConnectionCloseListener{
    /**
     * Callback when connection with the external peer has been closed
     */
    public void onConnectionClose();
}

interface ConnectionOpenListener{
    /**
     * Callback when connection with the external peer has been opened
     */
    public void onConnectionOpen();
}

/**
 * Connection Handler handles the basic socket operations
 * 
 * @author leeson
 */
public class ConnectionHandler implements IoHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger(IoHandlerAdapter.class);
    protected Set<ConnectionCloseListener> closeListeners = new HashSet<ConnectionCloseListener>();
    protected Set<ConnectionOpenListener> openListeners = new HashSet<ConnectionOpenListener>();
    protected Set<MessageListener> messageListeners = new HashSet<MessageListener>();
    private IoSession ioSession;
    
    public void addCloseListener(ConnectionCloseListener listener) {
        closeListeners.add(listener);
    }
    public void addOpenListener(ConnectionOpenListener listener) {
        openListeners.add(listener);
    }
    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }
    public void sessionCreated(IoSession session) throws Exception {
        //LOGGER.debug("Session {} created,  addr: {}", session.getId(), session.getRemoteAddress());
        if (ioSession != null) {
            LOGGER.info("Cannot support multiply session from  {}:{}", session.getId(), session.getRemoteAddress());
            session.close(true);
            return;
        }
        SocketSessionConfig config = (SocketSessionConfig)session.getConfig();
        config.setTcpNoDelay(true);
        ioSession = session;
    }

    public void sessionOpened(IoSession session) throws Exception {
        LOGGER.info("Session {} opened,  addr: {}", session.getId(), session.getRemoteAddress());
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (ConnectionOpenListener listener : openListeners) {
                    listener.onConnectionOpen();
                }  
            }
        }).start();

    }

    public void sessionClosed(IoSession session) throws Exception {
        LOGGER.info("Session {} closed,  addr: {}", session.getId(), session.getRemoteAddress());
        ioSession = null;
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (ConnectionCloseListener listener : closeListeners) {
                    listener.onConnectionClose();
                } 
            }
        }).start();
       
    }

    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        LOGGER.debug("Session {} is idled,  addr: {}", session.getId(), session.getRemoteAddress());
    }

    public void messageSent(IoSession session, Object message) throws Exception {
        LOGGER.debug("Session {} has sent message to {}", session.getId(), session.getRemoteAddress());
    }
    
    public void messageReceived(IoSession session, final Object message) throws Exception {
        LOGGER.info("Session {} receive message from {}", session.getId(), session.getRemoteAddress());
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (MessageListener listener : messageListeners) {
                    listener.onReceive(message, null);
                }
            }
        }).start();
    }    
    
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        LOGGER.error("ExceptionCaught:", cause);
        LOGGER.error("Session {} exception for addr: {}", session.getId(), session.getRemoteAddress());
        LOGGER.error("Closing for restart...");
        session.close(true);
    }
    
    public void stopSockets() {
        if (ioSession != null) {
            ioSession.close(true);
            ioSession = null;
        }
    }
    
    public boolean sendMessage(Object message) {
        if (ioSession == null) {
            LOGGER.warn("IoSession is closed when sending message..");
            return false;
        }
        WriteFuture result = ioSession.write(message);
        try {
            result.await();
        } catch (InterruptedException e) {
            // ignore exception
        }
        return result.isWritten();
    }
    
    public boolean isConnected() {
        return ioSession != null;
    }
}

