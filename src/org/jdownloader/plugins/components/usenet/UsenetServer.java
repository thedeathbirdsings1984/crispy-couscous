package org.jdownloader.plugins.components.usenet;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;

public class UsenetServer implements Storable {
    private String host = null;

    public String getHost() {
        return host;
    }

    public int getPort() {
        if (port > 0) {
            return port;
        } else {
            return isSSL() ? 563 : 119;
        }
    }

    public boolean isSSL() {
        return ssl;
    }

    private int port = -1;

    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int hashCode() {
        return (getHost() + ":" + getPort() + ":" + isSSL()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof UsenetServer) {
            final UsenetServer other = (UsenetServer) obj;
            return isSSL() == other.isSSL() && getPort() == other.getPort() && StringUtils.equalsIgnoreCase(getHost(), other.getHost());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Host:" + getHost() + "|Port:" + getPort() + "|SSL:" + isSSL();
    }

    public void setPort(int port) {
        this.port = port;
    }

    private boolean ssl = false;

    public UsenetServer(/* Storable */) {
    }

    public static List<UsenetServer> createServerList(final String host, final boolean ssl, final int... ports) {
        if (ssl) {
            return createServerList(host, new int[0], ports);
        } else {
            return createServerList(host, ports, new int[0]);
        }
    }

    public static List<UsenetServer> createServerList(final String host, final int[] nonSSLPorts, final int[] SSLPorts) {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        for (int port : nonSSLPorts) {
            ret.add(new UsenetServer(host, port, false));
        }
        for (int port : SSLPorts) {
            ret.add(new UsenetServer(host, port, true));
        }
        return ret;
    }

    public UsenetServer(final String host, final int port) {
        this(host, port, false);
    }

    public boolean validate() {
        return getPort() > 0 && StringUtils.isNotEmpty(getHost());
    }

    public UsenetServer(final String host, final int port, final boolean ssl) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }
}
