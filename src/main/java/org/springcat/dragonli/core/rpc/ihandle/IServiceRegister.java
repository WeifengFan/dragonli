package org.springcat.dragonli.core.rpc.ihandle;

import org.springcat.dragonli.core.rpc.ihandle.impl.RegisterServerInfo;
import org.springcat.dragonli.core.rpc.RpcRequest;

import java.util.List;

public interface IServiceRegister {

    List<RegisterServerInfo> getServiceList(RpcRequest rpcRequest);
}