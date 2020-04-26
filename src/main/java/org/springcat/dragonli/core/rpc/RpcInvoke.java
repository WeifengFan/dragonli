package org.springcat.dragonli.core.rpc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import lombok.Data;
import lombok.SneakyThrows;
import org.springcat.dragonli.core.rpc.exception.RpcException;
import org.springcat.dragonli.core.rpc.exception.RpcExceptionCodes;
import org.springcat.dragonli.core.rpc.ihandle.*;
import org.springcat.dragonli.core.rpc.ihandle.impl.RegisterServiceInfo;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 整个框架的核心类
 */
@Data
public class RpcInvoke {

    private final static Log log = LogFactory.get();

    private RpcConf rpcConf;

    private ILoadBalanceRule loadBalanceRule;

    private ISerialize serialize;

    private IHttpTransform httpTransform;

    private IServiceRegister serviceRegister;

    private IValidation validation;

    private Map<Method,RpcMethodInfo> apiMap;

    /**
     *
     *  method -> buildRpcRequest -> serialize  -> loaderBalance  -> transform  -> deserialize -> return
     *                                                                 | |
     *                                                              errorHandle
     * @param rpcRequest
     * @return
     * @throws RpcException
     */
    @SneakyThrows
    public RpcResponse invoke(RpcRequest rpcRequest) throws RpcException {
        try {
            RpcMethodInfo rpcMethodInfo = getApiMap().get(rpcRequest.getMethod());
            rpcRequest.setRpcMethodInfo(rpcMethodInfo);

            //1 校验参数,异常会中止流程
            Optional<RpcResponse> response = validation.validateWithRpcResponse(rpcRequest);
            if(response.isPresent()){
                log.debug("RpcInvoke validate rpcRequest{},failed error code:{}" ,rpcRequest,response.get().getCode());
                Optional<RpcResponse> recoverResponse = rpcRequest.recoverResult();
                if(recoverResponse.isPresent()){
                    return recoverResponse.get();
                }
                return response.get();
            }

            //2 serviceGetter
            List<RegisterServiceInfo> serviceList = serviceRegister.getServiceList(rpcRequest);

            //3 loaderBalance
            RegisterServiceInfo choose = loadBalanceRule.choose(serviceList, rpcRequest);

            //4 serialize encode
            String body = serialize.encode(rpcRequest.getRequestObj());

            //5 decorate error handle
            String url = httpTransform.genUrl(rpcRequest, choose);


            AtomicReference<RpcException> httpTransformRpcException = new AtomicReference<RpcException>();
            Supplier<String> transformSupplier = () -> {
                try {
                    return httpTransform.post(url, rpcRequest.getRpcHeader(), body);
                } catch (RpcException e) {
                    httpTransformRpcException.set(e);
                }
                return null;
            };

            //错误处理装饰类
            Supplier<String> supplier = rpcRequest.getRpcMethodInfo().getIErrorHandle().transformErrorHandle(transformSupplier, rpcRequest, choose);

            //6 Transform invoke
            String resp = supplier.get();

            RpcException rpcException = httpTransformRpcException.get();
            if( rpcException != null){
                throw rpcException;
            }

            //7 serialize decode
            if (StrUtil.isNotBlank(resp)) {
                RpcResponse decodeResp = serialize.decode(resp, rpcRequest.getRpcMethodInfo().getReturnType());
                decodeResp.setCode(RpcExceptionCodes.SUCCESS.getCode());
                return decodeResp;
            }

        }catch (RpcException rpcException){
            Optional<RpcResponse> recoverResponse = rpcRequest.recoverResult();
            if(recoverResponse.isPresent()){
                return recoverResponse.get();
            }else {
                return RpcUtil.buildRpcResponse(rpcException.getMessage(), rpcRequest.getRpcMethodInfo().getReturnType()).get();
            }
        }catch (Exception error){
            log.error("RpcInvoke invoke error rpcRequest{},error:{}", rpcRequest, error);

            Optional<RpcResponse> recoverResponse = rpcRequest.recoverResult();
            if(recoverResponse.isPresent()){
                return recoverResponse.get();
            }else {
                return RpcUtil.buildRpcResponse(RpcExceptionCodes.ERR_OTHER.getCode(), rpcRequest.getRpcMethodInfo().getReturnType()).get();
            }
        }

        return RpcUtil.newInstance(rpcRequest.getRpcMethodInfo().getReturnType());
    }
}
