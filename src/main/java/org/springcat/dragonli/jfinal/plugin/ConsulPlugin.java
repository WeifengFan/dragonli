package org.springcat.dragonli.jfinal.plugin;

import com.jfinal.plugin.IPlugin;
import org.springcat.dragonli.core.config.ConfigUtil;
import org.springcat.dragonli.core.consul.ConsulUtil;
import org.springcat.dragonli.core.registry.AppInfo;
import org.springcat.dragonli.core.registry.ConsulInfo;
import org.springcat.dragonli.core.registry.ConsulRegister;

public class ConsulPlugin implements IPlugin {

    private String ip;
    private int port;
    private AppInfo appInfo;

    public ConsulPlugin(ConsulInfo consulInfo, AppInfo appInfo) {
        this.ip = consulInfo.getIp();
        this.port = consulInfo.getPort();
        this.appInfo = appInfo;
    }

    @Override
    public boolean start() {
        try {
            ConsulUtil.init(ip,port);
            ConfigUtil.fetchSysConf(appInfo);
            ConsulRegister.register(ConsulUtil.client(), appInfo);
            return true;
        }catch (Exception exception){
            exception.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean stop() {
        try {
            ConsulRegister.unregister(ConsulUtil.client(), appInfo);
            return true;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return false;
    }
}
