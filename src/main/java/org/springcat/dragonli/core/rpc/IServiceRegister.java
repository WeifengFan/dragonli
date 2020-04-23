package org.springcat.dragonli.core.rpc;

import com.ecwid.consul.v1.health.model.HealthService;
import java.util.List;

public interface IServiceRegister {

    List<HealthService> getServiceList(RpcRequest rpcRequest);
}
