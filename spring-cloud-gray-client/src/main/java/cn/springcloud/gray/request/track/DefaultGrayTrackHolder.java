package cn.springcloud.gray.request.track;

import cn.springcloud.gray.GrayClientHolder;
import cn.springcloud.gray.client.config.properties.GrayTrackProperties;
import cn.springcloud.gray.communication.InformationClient;
import cn.springcloud.gray.local.InstanceLocalInfo;
import cn.springcloud.gray.local.InstanceLocalInfoAware;
import cn.springcloud.gray.model.GrayTrackDefinition;
import cn.springcloud.gray.refresh.Refresher;
import cn.springcloud.gray.request.GrayInfoTracker;
import cn.springcloud.gray.request.GrayTrackInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultGrayTrackHolder extends AbstractCommunicableGrayTrackHolder implements InstanceLocalInfoAware, Refresher {

    public static final String GRAY_TRACK_REFRESH_TRIGGER_NAME = "refresh_gray_track";

    private Timer updateTimer = new Timer("Gray-Track-Update-Timer", true);
    private GrayTrackProperties grayTrackProperties;
    private InstanceLocalInfo instanceLocalInfo;
    private int scheduleOpenForWorkCount = 0;
    private int scheduleOpenForWorkLimit = 5;


    public DefaultGrayTrackHolder(
            GrayTrackProperties grayTrackProperties, InformationClient informationClient,
            List<GrayInfoTracker<? extends GrayTrackInfo, ?>> trackers) {
        this(grayTrackProperties, informationClient, trackers, null);
    }

    public DefaultGrayTrackHolder(
            GrayTrackProperties grayTrackProperties, InformationClient informationClient,
            List<GrayInfoTracker<? extends GrayTrackInfo, ?>> trackers,
            List<GrayTrackDefinition> trackDefinitions) {
        super(informationClient, trackers, trackDefinitions);
        this.grayTrackProperties = grayTrackProperties;
//        openForWork();
    }

    public void setup() {
        scheduleOpenForWork();
    }


    public void openForWork() {
        log.info("拉取灰度追踪列表");

        if (getGrayInformationClient() != null) {
            boolean t = doUpdate();
            int timerMs = grayTrackProperties.getDefinitionsUpdateIntervalTimerInMs();
            if (timerMs > 0) {
                updateTimer.schedule(new DefaultGrayTrackHolder.UpdateTask(), timerMs, timerMs);
            } else if (!t) {
                scheduleOpenForWork();
            }
        } else {
            loadPropertiesTrackDefinitions();
        }
    }


    private void scheduleOpenForWork() {
        if (scheduleOpenForWorkCount > scheduleOpenForWorkLimit) {
            return;
        }
        scheduleOpenForWorkCount++;
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                openForWork();
            }
        }, grayTrackProperties.getDefinitionsInitializeDelayTimeInMs());
    }

    private boolean doUpdate() {
        Map<String, GrayTrackDefinition> trackDefinitionMap = new ConcurrentHashMap<>();
        try {
            log.debug("更新灰度追踪列表...");

            InstanceLocalInfo instanceLocalInfo = getInstanceLocalInfo();
            if (instanceLocalInfo == null) {
                log.warn("本地实例信息为null,跳过更新");
                return false;
            }
            List<GrayTrackDefinition> trackDefinitions = getGrayInformationClient()
                    .getTrackDefinitions(instanceLocalInfo.getServiceId(), instanceLocalInfo.getInstanceId());
            trackDefinitions.forEach(definition -> {
                updateTrackDefinition(trackDefinitionMap, definition);
            });
        } catch (Exception e) {
            log.error("更新灰度追踪列表失败", e);
            return false;
        }
        joinLoadedTrackDefinitions(trackDefinitionMap);
        setTrackDefinitions(trackDefinitionMap);
        return true;
    }

    private void loadPropertiesTrackDefinitions() {
        Map<String, GrayTrackDefinition> trackDefinitionMap = new ConcurrentHashMap<>();
        joinLoadedTrackDefinitions(trackDefinitionMap);
        setTrackDefinitions(trackDefinitionMap);
    }


    private void joinLoadedTrackDefinitions(Map<String, GrayTrackDefinition> definitionMap) {
        grayTrackProperties.getWeb().getTrackDefinitions().forEach(definition -> {
            if (!definitionMap.containsKey(definition.getName())) {
                updateTrackDefinition(definitionMap, definition);
            }
        });
    }

    @Override
    public void setInstanceLocalInfo(InstanceLocalInfo instanceLocalInfo) {
        this.instanceLocalInfo = instanceLocalInfo;
    }

    public InstanceLocalInfo getInstanceLocalInfo() {
        if (instanceLocalInfo == null) {
            instanceLocalInfo = GrayClientHolder.getInstanceLocalInfo();
        }
        return instanceLocalInfo;
    }

    @Override
    public void refresh() {
        if (getGrayInformationClient() != null) {
            doUpdate();
        } else {
            loadPropertiesTrackDefinitions();
        }
    }

    @Override
    public String triggerName() {
        return GRAY_TRACK_REFRESH_TRIGGER_NAME;
    }

    class UpdateTask extends TimerTask {

        @Override
        public void run() {
            doUpdate();
        }
    }


}
