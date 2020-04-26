package org.springcat.dragonli.core.rpc.ihandle;

import io.vavr.control.Try;
import org.springcat.dragonli.core.rpc.RpcRequest;

import java.util.function.Function;
import java.util.function.Supplier;

public interface IErrorHandle {

    default  <T> T execute(RpcRequest rpcRequest,Supplier<T> rpcSupplier, Function<? super Throwable, ? extends T> errorHandler){
        Supplier<T> transformSupplier = decorateCircuitBreaker(rpcRequest,rpcSupplier);
        Try<T> retry = decorateRetry(rpcRequest, transformSupplier, errorHandler);
        return retry.get();
    }

    void init(String key);
    /**
     * 熔断实现类
     * @param rpcRequest
     * @param supplier
     * @param <T>
     * @return
     */
    <T> Supplier<T> decorateCircuitBreaker(RpcRequest rpcRequest,Supplier<T> supplier);

    /**
     * 失败重试实现类
     * @param rpcRequest
     * @param supplier
     * @param errorHandler
     * @param <T>
     * @return
     */
    <T> Try<T> decorateRetry(RpcRequest rpcRequest, Supplier<T> supplier, Function<? super Throwable, ? extends T> errorHandler);

}
