package cn.springcloud.gray.client.netflix;

import cn.springcloud.gray.GrayClientHolder;
import cn.springcloud.gray.GrayManager;
import cn.springcloud.gray.ServerChooser;
import cn.springcloud.gray.ServerListResult;
import cn.springcloud.gray.choose.GrayPredicate;
import cn.springcloud.gray.model.GrayService;
import cn.springcloud.gray.request.GrayRequest;
import cn.springcloud.gray.request.RequestLocalStorage;
import cn.springcloud.gray.servernode.ServerExplainer;
import cn.springcloud.gray.servernode.ServerListProcessor;
import cn.springcloud.gray.servernode.ServerSpec;
import com.netflix.loadbalancer.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class RibbonServerChooser implements ServerChooser<Server> {

    private GrayManager grayManager;

    private RequestLocalStorage requestLocalStorage;

    private GrayPredicate grayPredicate;

    private ServerExplainer<Server> serverExplainer;

    protected ServerListProcessor serverListProcessor;


    public RibbonServerChooser(
            GrayManager grayManager,
            RequestLocalStorage requestLocalStorage,
            GrayPredicate grayPredicate,
            ServerExplainer<Server> serverExplainer,
            ServerListProcessor serverListProcessor) {
        this.grayManager = grayManager;
        this.requestLocalStorage = requestLocalStorage;
        this.grayPredicate = grayPredicate;
        this.serverExplainer = serverExplainer;
        this.serverListProcessor = serverListProcessor;
    }

    @Override
    public boolean matchGrayDecisions(ServerSpec serverSpec) {
        return grayPredicate.apply(serverSpec);
    }

    @Override
    public boolean matchGrayDecisions(Server server) {
        return matchGrayDecisions(serverExplainer.apply(server));
    }

    @Override
    public ServerListResult<Server> distinguishServerList(List<Server> servers) {
        String serviceId = getServiceId(servers);
        if (StringUtils.isEmpty(serviceId)) {
            log.warn("没有获取到当前remote request的service id, 返回null");
            return null;
        }
        return distinguishServerList(serviceId, servers);
    }


    @Override
    public ServerListResult<Server> distinguishAndMatchGrayServerList(List<Server> servers) {
        ServerListResult<Server> serverListResult = distinguishServerList(servers);
        if (serverListResult == null) {
            return null;
        }

        if (GrayClientHolder.getGraySwitcher().isEanbleGrayRouting()) {
            log.debug("开始匹配{}服务的灰度实例", serverListResult.getServiceId());
            List<Server> matchedGrayServers = serverListResult.getGrayServers().stream()
                    .filter(this::matchGrayDecisions)
                    .collect(Collectors.toList());
            log.debug("{} 服务共有{}个灰度实例，本次匹配到{}个",
                    serverListResult.getServiceId(), serverListResult.getGrayServers().size(), matchedGrayServers.size());
            serverListResult.setGrayServers(matchedGrayServers);
        } else {
            log.debug("grayRouting未打开,将{}服务的灰度实例清空,使之路由到正常实例", serverListResult.getServiceId());
            serverListResult.setGrayServers(ListUtils.EMPTY_LIST);
        }

        return serverListResult;
    }

    private String getServiceId(List<Server> servers) {
        GrayRequest grayRequest = requestLocalStorage.getGrayRequest();
        if (grayRequest != null && StringUtils.isNotEmpty(grayRequest.getServiceId())) {
            return grayRequest.getServiceId();
        }
        if (CollectionUtils.isNotEmpty(servers)) {
            Server server = servers.get(0);
            if (!Objects.isNull(server)) {
                return server.getMetaInfo().getServiceIdForDiscovery();
            }
        }
        return null;
    }


    private ServerListResult<Server> distinguishServerList(String serviceId, List<Server> servers) {
        if (!grayManager.hasGray(serviceId)) {
            log.debug("当前灰度开关未打开，或服务 '{}'没有相关灰度策略", serviceId);
            return null;
        }

        GrayService grayService = grayManager.getGrayService(serviceId);
        List<Server> serverList = serverListProcessor.process(serviceId, servers);
        List<Server> grayServers = new ArrayList<>(grayService.getGrayInstances().size());
        List<Server> normalServers = new ArrayList<>(Math.min(servers.size(), grayService.getGrayInstances().size()));

        for (Server server : serverList) {
            if (grayService.getGrayInstance(server.getMetaInfo().getInstanceId()) != null) {
                grayServers.add(server);
            } else {
                normalServers.add(server);
            }
        }

        log.debug("区分服务实例灰度状态: 服务 {} 实例数:{total: {}, gray:{}, normal:{}}", serviceId, servers.size(), grayServers.size(), normalServers.size());
        return new ServerListResult<>(serviceId, grayServers, normalServers);
    }
}
