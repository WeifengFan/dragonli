package org.springcat.dragonli.jfinal;

import com.jfinal.config.*;
import com.jfinal.json.MixedJsonFactory;
import com.jfinal.template.Engine;
import org.springcat.dragonli.core.rpc.RpcConf;
import org.springcat.dragonli.core.Context;
import org.springcat.dragonli.core.rpc.RpcUtil;
import org.springcat.dragonli.core.rpc.ihandle.impl.ConsistentHashRule;
import org.springcat.dragonli.jfinal.plugin.ConsulPlugin;
import org.springcat.dragonli.jfinal.plugin.RpcPlugin;
import org.springcat.dragonli.core.registry.AppConf;
import org.springcat.dragonli.core.consul.ConsulConf;
import org.springcat.dragonli.core.config.SettingGroup;
import org.springcat.dragonli.core.config.SettingUtil;


/**
 * 仅仅为了简化初始化配置
 */
public abstract class DragonLiConfig extends JFinalConfig {

    private ConsulConf consulConf = SettingUtil.getConfBean(SettingGroup.consul);
    private AppConf appConf = SettingUtil.getConfBean(SettingGroup.application);
    private RpcConf rpcConf = SettingUtil.getConfBean(SettingGroup.rpc);

    @Override
    public void configConstant(Constants me) {
        configConstantPlus(me);
        me.setConfigPluginOrder(1);
        me.setInjectDependency(true);
        // 配置对超类中的属性进行注入
        me.setInjectSuperClass(true);
        me.setJsonFactory(new MixedJsonFactory());
    }

    public abstract void configConstantPlus(Constants me);

    @Override
    public void configRoute(Routes me) {
        configRoutePlus(me);
        JFinalStatusController.init(me,appConf);
    }

    public abstract void configRoutePlus(Routes me);

    @Override
    public void configPlugin(Plugins me) {

        //为了先从配置中心拉取配置
        me.add(new ConsulPlugin(consulConf, appConf));

        configPluginPlus(me);

        //init rpc client
        RpcPlugin rpcPlugin = new RpcPlugin(rpcConf);
        me.add(rpcPlugin);
}

    public abstract void configPluginPlus(Plugins me);

    @Override
    public void configInterceptor(Interceptors me) {
        configInterceptorPlus(me);
        me.add(inv -> {
            Context.init();
            //传递rpc调用间的参数
            Context.setRpcParam(ConsistentHashRule.LOADER_BALANCE_FLAG, RpcUtil.getClientIp(inv.getController().getRequest()));
            inv.invoke();
            Context.clear();
        });
    }

    public abstract void configInterceptorPlus(Interceptors me);

    @Override
    public void configEngine(Engine me) {
        configEnginePlus(me);
    }
    /**
     * Config engine
     */
    public abstract void configEnginePlus(Engine me);

    @Override
    public void configHandler(Handlers me){
        configHandlerPlus(me);
    }
    /**
     * Config handler
     */
    public abstract void configHandlerPlus(Handlers me);

}
