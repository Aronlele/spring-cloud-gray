package cn.springcloud.gray.event.sourcehander;

import cn.springcloud.gray.event.EventType;
import cn.springcloud.gray.event.GrayEventMsg;
import cn.springcloud.gray.event.SourceType;
import cn.springcloud.gray.local.InstanceLocalInfo;
import cn.springcloud.gray.local.InstanceLocalInfoObtainer;
import cn.springcloud.gray.model.GrayTrackDefinition;
import cn.springcloud.gray.request.track.CommunicableGrayTrackHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class GrayTrackEventHandler implements GraySourceEventHandler {

    private static final Logger log = LoggerFactory.getLogger(GrayTrackEventHandler.class);

    private InstanceLocalInfoObtainer instanceLocalInfoObtainer;
    private CommunicableGrayTrackHolder grayTrackHolder;


    public GrayTrackEventHandler(
            InstanceLocalInfoObtainer instanceLocalInfoObtainer,
            CommunicableGrayTrackHolder grayTrackHolder) {
        this.instanceLocalInfoObtainer = instanceLocalInfoObtainer;
        this.grayTrackHolder = grayTrackHolder;
    }

    @Override
    public void handle(GrayEventMsg eventMsg) {
        if (!Objects.equals(eventMsg.getSourceType(), SourceType.GRAY_TRACK)) {
            return;
        }

        if (eventMsg.getSource() == null) {
            throw new NullPointerException("event source is null");
        }


        InstanceLocalInfo instanceLocalInfo = instanceLocalInfoObtainer.getInstanceLocalInfo();

        if (!StringUtils.equals(eventMsg.getServiceId(), instanceLocalInfo.getServiceId())) {
            return;
        }
        if (StringUtils.isNotEmpty(eventMsg.getInstanceId())
                && !StringUtils.equals(eventMsg.getInstanceId(), instanceLocalInfo.getInstanceId())) {
            return;
        }

        GrayTrackDefinition grayTrackDefinition = (GrayTrackDefinition) eventMsg.getSource();

        if (Objects.equals(eventMsg.getEventType(), EventType.UPDATE)) {
            grayTrackHolder.updateTrackDefinition(grayTrackDefinition);
        } else {
            grayTrackHolder.deleteTrackDefinition(grayTrackDefinition);
        }
    }
}
