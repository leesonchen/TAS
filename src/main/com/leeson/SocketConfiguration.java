 package com.leeson;

 /**
 * Configuration describes the socket configuration, 
 * including socket mode, listening port (server mode), or server ip:port (for client mode)
 * 
 * @author leeson
 * @date 2020/08/24
 */
public class SocketConfiguration {
    int mode;   //0-server; 1-client; 2-multiServer; 3-multiClient
    int port1;
    String ip1;
    int port2;
    String ip2;
}
