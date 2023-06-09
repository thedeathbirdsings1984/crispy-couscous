package jd.controlling.proxy;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jd.http.Request;
import jd.plugins.Plugin;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.net.httpconnection.HTTPProxy.TYPE;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.updatev2.ProxyData;

public class SingleBasicProxySelectorImpl extends AbstractProxySelectorImpl {
    private final SelectedProxy   proxy;
    private final List<HTTPProxy> list;
    private String                username;
    private String                password;
    private String                tempUser;
    private String                tempPass;

    public ProxyData toProxyData() {
        final HTTPProxyStorable storable = HTTPProxy.getStorable(getProxy());
        storable.setUsername(username);
        storable.setPassword(password);
        storable.setPreferNativeImplementation(isPreferNativeImplementation());
        final ProxyData ret = super.toProxyData(storable);
        ret.setProxy(storable);
        ret.setReconnectSupported(isReconnectSupported());
        return ret;
    }

    @Override
    public String toDetailsString() {
        final String ret = proxy.toString();
        if (StringUtils.isNotEmpty(getUser())) {
            return getUser() + "@" + ret;
        } else {
            return ret;
        }
    }

    @Override
    public boolean updateProxy(Request request, int retryCounter) {
        return ProxyController.getInstance().updateProxy(this, request, retryCounter);
    }

    public SingleBasicProxySelectorImpl(ProxyData proxyData) {
        proxy = new SelectedProxy(this, HTTPProxy.getHTTPProxy(proxyData.getProxy()));
        setFilter(proxyData.getFilter());
        proxy.setConnectMethodPrefered(proxyData.getProxy().isConnectMethodPrefered());
        setResumeAllowed(proxyData.isRangeRequestsSupported());
        setEnabled(proxyData.isEnabled());
        username = proxy.getUser();
        password = proxy.getPass();
        ArrayList<HTTPProxy> list = new ArrayList<HTTPProxy>();
        list.add(proxy);
        this.list = Collections.unmodifiableList(list);
        setReconnectSupported(proxyData.isReconnectSupported());
    }

    public SingleBasicProxySelectorImpl(HTTPProxy rawProxy) {
        proxy = new SelectedProxy(this, rawProxy);
        username = proxy.getUser();
        password = proxy.getPass();
        ArrayList<HTTPProxy> list = new ArrayList<HTTPProxy>();
        list.add(proxy);
        this.list = Collections.unmodifiableList(list);
    }

    @Override
    public List<HTTPProxy> getProxiesByURL(URL url) {
        for (final SelectProxyByURLHook hook : selectProxyByURLHooks) {
            hook.onProxyChoosen(url, list);
        }
        return list;
    }

    public SelectedProxy getProxy() {
        return proxy;
    }

    @Override
    public Type getType() {
        final TYPE type = getProxy().getType();
        switch (type) {
        case HTTP:
            return Type.HTTP;
        case HTTPS:
            return Type.HTTPS;
        case SOCKS4:
            return Type.SOCKS4;
        case SOCKS4A:
            return Type.SOCKS4A;
        case SOCKS5:
            return Type.SOCKS5;
        default:
            throw new IllegalStateException(type.name());
        }
    }

    public void setType(Type value) {
        switch (value) {
        case HTTP:
            proxy.setType(TYPE.HTTP);
            break;
        case HTTPS:
            proxy.setType(TYPE.HTTPS);
            break;
        case SOCKS4:
            proxy.setType(TYPE.SOCKS4);
            break;
        case SOCKS4A:
            proxy.setType(TYPE.SOCKS4A);
            break;
        case SOCKS5:
            proxy.setType(TYPE.SOCKS5);
            break;
        default:
            throw new IllegalStateException(value.name());
        }
    }

    public void setUser(String user) {
        final SelectedProxy proxy = getProxy();
        if (!StringUtils.equals(user, proxy.getUser())) {
            proxy.setUser(user);
            username = user;
            tempUser = null;
            tempPass = null;
            clearBanList();
        }
    }

    public void setPassword(String pass) {
        final SelectedProxy proxy = getProxy();
        if (!StringUtils.equals(pass, proxy.getPass())) {
            proxy.setPass(pass);
            password = pass;
            tempUser = null;
            tempPass = null;
            clearBanList();
        }
    }

    public String getPassword() {
        final String ret = tempPass;
        if (ret != null) {
            return "(Temp)" + ret;
        } else {
            return password;
        }
    }

    public String _getPassword() {
        final String ret = tempPass;
        if (ret != null) {
            return ret;
        } else {
            return password;
        }
    }

    public String _getUsername() {
        final String ret = tempUser;
        if (ret != null) {
            return ret;
        } else {
            return username;
        }
    }

    public String getUser() {
        final String ret = tempUser;
        if (ret != null) {
            return "(Temp)" + ret;
        } else {
            return username;
        }
    }

    @Override
    public boolean isPreferNativeImplementation() {
        return getProxy().isPreferNativeImplementation();
    }

    @Override
    public void setPreferNativeImplementation(boolean preferNativeImplementation) {
        if (isPreferNativeImplementation() != preferNativeImplementation) {
            getProxy().setPreferNativeImplementation(preferNativeImplementation);
            clearBanList();
        }
    }

    public int getPort() {
        return getProxy().getPort();
    }

    public void setPort(int port) {
        final SelectedProxy proxy = getProxy();
        if (proxy.getPort() != port) {
            proxy.setPort(port);
            clearBanList();
        }
    }

    public void setHost(String value) {
        final SelectedProxy proxy = getProxy();
        if (!StringUtils.equals(value, proxy.getHost())) {
            proxy.setHost(value);
            clearBanList();
        }
    }

    public String getHost() {
        return getProxy().getHost();
    }

    @Override
    public String toExportString() {
        final StringBuilder sb = new StringBuilder();
        boolean hasUSerInfo = false;
        final SelectedProxy proxy = getProxy();
        switch (proxy.getType()) {
        case HTTP:
            sb.append("http://");
            break;
        case HTTPS:
            sb.append("https://");
            break;
        case SOCKS4:
            sb.append("socks4://");
            break;
        case SOCKS4A:
            sb.append("socks4a://");
            break;
        case SOCKS5:
            sb.append("socks5://");
            break;
        case DIRECT:
            if (proxy.getLocal() != null) {
                sb.append("direct://");
                sb.append(proxy.getLocal());
                return sb.toString();
            } else {
                return null;
            }
        default:
            return null;
        }
        final String username = _getUsername();
        if (!StringUtils.isEmpty(username)) {
            sb.append(username);
            hasUSerInfo = true;
        }
        final String password = _getPassword();
        if (!StringUtils.isEmpty(password)) {
            if (hasUSerInfo) {
                sb.append(":");
            }
            hasUSerInfo = true;
            sb.append(password);
        }
        if (hasUSerInfo) {
            sb.append("@");
        }
        sb.append(getHost());
        if (getPort() > 0) {
            sb.append(":");
            sb.append(getPort());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass().equals(SingleBasicProxySelectorImpl.class) && getProxy().equals(((SingleBasicProxySelectorImpl) obj).getProxy());
    }

    @Override
    public int hashCode() {
        return SingleBasicProxySelectorImpl.class.hashCode();
    }

    @Override
    protected boolean isLocal() {
        return false;
    }

    @Override
    public boolean isProxyBannedFor(HTTPProxy orgReference, URL url, Plugin pluginFromThread, boolean ignoreConnectBans) {
        // can orgRef be null? I doubt that. TODO:ensure
        if (!getProxy().equals(orgReference)) {
            return false;
        } else {
            return super.isProxyBannedFor(orgReference, url, pluginFromThread, ignoreConnectBans);
        }
    }

    public void setTempAuth(String user, String pass) {
        final SelectedProxy proxy = getProxy();
        proxy.setUser(user == null ? username : user);
        proxy.setPass(pass == null ? password : pass);
        this.tempUser = user;
        this.tempPass = pass;
        if (user != null || pass != null) {
            clearBanList();
        }
    }
}
