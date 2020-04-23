package org.springcat.dragonli.core.rpc;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.ecwid.consul.v1.health.model.HealthService;
import org.springcat.dragonli.core.registry.RegisterServerInfo;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RpcInvoke {

    private static final Log log = LogFactory.get(RpcInvoke.class);

    private static ILoadBalanceRule loadBalanceRule;
    private static ISerialize serialize;
    private static IHttpTransform httpTransform;
    private static RpcInvoke invoke;
    private static RpcInfo rpcInfo;
    private static IErrorHandle errorHandle;
    private static IServiceRegister serviceRegister;

    public static void init(RpcInfo rpcInfo1,Consumer<Map<Class<?>, Object>> consumer) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        rpcInfo = rpcInfo1;

        //初始化负载均衡
        loadBalanceRule = (ILoadBalanceRule) Class.forName(rpcInfo.getLoadBalanceRuleImplClass()).newInstance();
        //初始化序列化
        serialize = (ISerialize) Class.forName(rpcInfo.getSerializeImplClass()).newInstance();
        //初始化http请求客户端
        httpTransform = (IHttpTransform) Class.forName(rpcInfo.getHttpTransformImplClass()).newInstance();
        //初始化错误处理
        errorHandle = (IErrorHandle) Class.forName(rpcInfo.getErrorHandleImplClass()).newInstance();
        //初始化服务列表获取
        serviceRegister = (IServiceRegister) Class.forName(rpcInfo.getServiceRegisterImplClass()).newInstance();


        //初始化接口代理类
        List<Class<?>> services = RpcUtil.scanRpcService(rpcInfo.getScanPackages());
        Map<Class<?>, Object> implMap = RpcUtil.convert2RpcServiceImpl(services);
        consumer.accept(implMap);
    }




    /**
     *
     *
     *  method -> buildRpcRequest -> serialize  -> loaderBalance  -> transform  -> deserialize -> return
     *                                              |                   |                           |
     *                                              ------------->   errorHandle   ---------------->
     *
     * @param rpcRequest
     * @return
     * @throws RpcException
     */
    public static Object invoke(RpcRequest rpcRequest) throws RpcException {
        //serviceGetter
        List<RegisterServerInfo> serviceList = serviceRegister.getServiceList(rpcRequest);

        //loaderBalance
        RegisterServerInfo choose = loadBalanceRule.choose(serviceList,rpcRequest);
        if (choose == null) {
            log.error("can not find healthService");
            return null;
        }

        rpcRequest.setSerialize(serialize);

        //transform
        try {
            Supplier<Object> supplier = errorHandle.transformErrorHandle(httpTransform,rpcRequest, choose);
            return supplier.get();
        } catch (Exception e) {
            throw new RpcException(e.getMessage());
        }
    }


}
