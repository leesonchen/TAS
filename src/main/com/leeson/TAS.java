package com.leeson;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


interface MessageListener{
    /**
     * Callback when the other side received messages
     * 
     * @param message   MINA message to be notified
     * @param arg       Reserved
     * @return
     */
    public boolean onReceive(Object message, Object arg);
}

/**
 * Main-class for TAS
 * @author leeson
 *
 */
public class TAS {
    public static final Logger LOGGER = LoggerFactory.getLogger(TAS.class);

    //Socket configurations
    private SocketConfiguration leftConfig;
    private SocketConfiguration rightConfig;
    
    //Socket managers
    SocketManager leftManager;
    SocketManager rightManager;

    public TAS(){}
    
    private int propParseInt(String propKey, Properties prop) {
        return Integer.parseInt(propParseString(propKey, prop));
    }
    private String propParseString(String propKey, Properties prop) {
        Object propValue = prop.get(propKey);
        if (null == propValue){
            LOGGER.error("Cannot parse {} ！", propKey);
            throw new RuntimeException("Error while parsing configuration");
        }
        return (String)propValue;
    }    
    private SocketConfiguration parseSocketConfiguration(String prefix, Properties prop) {
        SocketConfiguration config = new SocketConfiguration();
        
        String propKey = prefix + ".mode";
        Object propValue = prop.get(propKey);
        if (null != propValue){
            config.mode = Integer.parseInt((String)propValue);
        }else{
            LOGGER.error("Cannot parse {} ！", propKey);
            return null;
        }


        if (config.mode == 1 || config.mode == 3) {
            // client mode
            config.port1 = propParseInt(prefix + ".port", prop);
            config.ip1 = propParseString(prefix + ".ip", prop);
            
            if (config.mode == 3) {
                LOGGER.error("Do not support multi-client mode yet!");
                return null;
                //  multi-client
                // config.port2 = propParseInt(prefix + ".port2", prop);
                // config.ip2 = propParseString(prefix + ".ip2", prop);
            }
        } else {
            // server mode
            config.port1 = propParseInt(prefix + ".port", prop);
            if (config.mode == 2) {
                LOGGER.error("Do not support multi-server mode yet!");
                return null;

                // multi-server
                // config.port2 = propParseInt(prefix + ".port2", prop);
            }
        }

        return config;
    }
    public void init(){
        Properties prop = new Properties();
        try {
            FileInputStream input = new FileInputStream("conf/tas.properties");                    
            prop.load(input);
            input.close();
        } catch (Exception e) {
            LOGGER.error("Cannot parse conf/tas.properties!", e);
            System.exit(0);
        }
        
        // read configuration
        leftConfig = parseSocketConfiguration("left", prop);
        rightConfig = parseSocketConfiguration("right", prop);
        if (leftConfig == null || rightConfig == null) {
            LOGGER.error("Configuration conf/tas.properties is invalid!");
            System.exit(0);
        }
        if (leftConfig.mode > 1) {
            LOGGER.error("Cannot support multi-sockets for left side!");
            System.exit(0);
        }
    }

    public void start()  {
        // Init socket managers
        leftManager = SocketManager.getSocketManager(leftConfig);
        rightManager = SocketManager.getSocketManager(rightConfig);
        
        // Forward messages to the other side transparently
        leftManager.addMessageListener(new MessageListener() {
            @Override
            public boolean onReceive(Object message, Object arg) {
                 return rightManager.sendMessage(message);
            }
        });
        rightManager.addMessageListener(new MessageListener() {
            @Override
            public boolean onReceive(Object message, Object arg) {
                 return leftManager.sendMessage(message);
            }
        });        
        
        // Start and run the two managers
        if (!leftManager.start()) {
            LOGGER.error("Failed to start left side socket manager!");
            System.exit(0);
        }
        if (!rightManager.start()) {
            LOGGER.error("Failed to start right side socket manager!");
            System.exit(0);
        }
    }
    
    public static void main(String[] args) {       
        PropertyConfigurator.configure("conf/log4j.properties");
        
        TAS socketAdapter = new TAS();
        socketAdapter.init();
        socketAdapter.start();
    }

}
