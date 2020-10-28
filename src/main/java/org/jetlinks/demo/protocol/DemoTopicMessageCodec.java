package org.jetlinks.demo.protocol;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.codec.SimpleMqttMessage;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.*;

import java.util.HashMap;


@Slf4j
public class DemoTopicMessageCodec {

    protected DeviceMessage doDecode(String deviceId, String topic, JSONObject payload) {
        DeviceMessage message = null;
        if (topic.startsWith("/fire_alarm")) {
            message = handleFireAlarm(topic, payload);
        } else if (topic.startsWith("/fault_alarm")) {
            message = handleFaultAlarm(topic, payload);
        } else if (topic.startsWith("/register")) {
            message = handleRegister(payload);
        } else if (topic.startsWith("/unregister")) {
            message = handleUnRegister(payload);
        }  else if (topic.startsWith("/dev_msg")) {
            message = handleDeviceMessage(topic, payload);
        } else if (topic.startsWith("/device_online_status")) {
            message = handleDeviceOnlineStatus(topic, payload);
        } else if (topic.startsWith("/read-property")) {
            message = handleReadPropertyReply(payload);
        } else if (topic.startsWith("/report-property")) { //定时上报属性
            message = handleReportProperty(payload);
        } else if (topic.startsWith("/write-property")) {
            message = handleWritePropertyReply(payload);
        } else if (topic.startsWith("/invoke-function")) {
            message = handleFunctionInvokeReply(payload);
        } else if (topic.startsWith("/children")) {
            ChildDeviceMessage childDeviceMessage = new ChildDeviceMessage();
            childDeviceMessage.setDeviceId(deviceId);
            DeviceMessage children = doDecode(deviceId, topic.substring(9), payload);
            childDeviceMessage.setChildDeviceMessage(children);
            childDeviceMessage.setChildDeviceId(children.getDeviceId());
            message = childDeviceMessage;
        }

        log.info("handle demo message:{}:{}", topic, payload);
        return message;
    }


    protected TopicMessage doEncode(DeviceMessage message) {
        if (message instanceof ReadPropertyMessage) {
            String topic = "/read-property";
            JSONObject data = new JSONObject();
            data.put("messageId", message.getMessageId());
            data.put("deviceId", message.getDeviceId());
            data.put("properties", ((ReadPropertyMessage) message).getProperties());
            return new TopicMessage(topic, data);
        } else if (message instanceof WritePropertyMessage) {
            String topic = "/write-property";
            JSONObject data = new JSONObject();
            data.put("messageId", message.getMessageId());
            data.put("deviceId", message.getDeviceId());
            data.put("properties", ((WritePropertyMessage) message).getProperties());
            return new TopicMessage(topic, data);
        } else if (message instanceof FunctionInvokeMessage) {
            String topic = "/invoke-function";
            FunctionInvokeMessage invokeMessage = ((FunctionInvokeMessage) message);
            JSONObject data = new JSONObject();
            data.put("messageId", message.getMessageId());
            data.put("deviceId", message.getDeviceId());
            data.put("function", invokeMessage.getFunctionId());
            data.put("args", invokeMessage.getInputs());
            return new TopicMessage(topic, data);
        } else if (message instanceof ChildDeviceMessage) {
            TopicMessage msg = doEncode((DeviceMessage) ((ChildDeviceMessage) message).getChildDeviceMessage());
            if (msg == null) {
                return null;
            }
            String topic = "/children" + msg.getTopic();
            return new TopicMessage(topic, msg.getMessage());
        }
        return null;
    }

    private FunctionInvokeMessageReply handleFunctionInvokeReply(JSONObject json) {
        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply();
        reply.setFunctionId(json.getString("functionId"));
        reply.setMessageId(json.getString("messageId"));
        reply.setDeviceId(json.getString("deviceId"));
        reply.setOutput(json.get("output"));
        reply.setCode(json.getString("code"));
        reply.setTimestamp(json.getLong("timestamp"));
        reply.setSuccess(json.getBoolean("success"));
        return reply;
    }

    private DeviceRegisterMessage handleRegister(JSONObject json) {
        DeviceRegisterMessage reply = new DeviceRegisterMessage();
        reply.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        reply.setDeviceId(json.getString("deviceId"));
        reply.setTimestamp(System.currentTimeMillis());
        return reply;
    }

    private DeviceUnRegisterMessage handleUnRegister(JSONObject json) {
        DeviceUnRegisterMessage reply = new DeviceUnRegisterMessage();
        reply.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        reply.setDeviceId(json.getString("deviceId"));
        reply.setTimestamp(System.currentTimeMillis());
        return reply;
    }

    private ReportPropertyMessage handleReportProperty(JSONObject json) {
        ReportPropertyMessage reply = new ReportPropertyMessage();
        reply.setProperties(json.getJSONObject("properties"));
        reply.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        reply.setDeviceId(json.getString("deviceId"));
        reply.setTimestamp(json.getLong("timestamp"));
        return reply;
    }

    private ReadPropertyMessageReply handleReadPropertyReply(JSONObject json) {
        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();
        reply.setProperties(json.getJSONObject("properties"));
        reply.setMessageId(json.getString("messageId"));
        reply.setTimestamp(json.getLong("timestamp"));
        reply.setDeviceId(json.getString("deviceId"));
        reply.setSuccess(json.getBoolean("success"));
        return reply;
    }

    private WritePropertyMessageReply handleWritePropertyReply(JSONObject json) {
        WritePropertyMessageReply reply = new WritePropertyMessageReply();
        reply.setProperties(json.getJSONObject("properties"));
        reply.setMessageId(json.getString("messageId"));
        reply.setTimestamp(json.getLong("timestamp"));
        reply.setDeviceId(json.getString("deviceId"));
        reply.setSuccess(json.getBoolean("success"));
        return reply;
    }

    private EventMessage handleFireAlarm(String topic, JSONObject json) {
        EventMessage eventMessage = new EventMessage();

        eventMessage.setDeviceId(json.getString("deviceId"));
        eventMessage.setEvent("fire_alarm");
        eventMessage.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());

        eventMessage.setData(new HashMap<>(json));
        return eventMessage;
    }

    private EventMessage handleFaultAlarm(String topic, JSONObject json) {
        // String[] topics = topic.split("[/]");
        EventMessage eventMessage = new EventMessage();

        eventMessage.setDeviceId(json.getString("deviceId"));
        eventMessage.setEvent("fault_alarm");
        eventMessage.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        eventMessage.setData(new HashMap<>(json));
        return eventMessage;
    }

    private EventMessage handleDeviceMessage(String topic, JSONObject json) {
        EventMessage eventMessage = new EventMessage();

        eventMessage.setDeviceId(json.getString("deviceId"));
        eventMessage.setEvent("dev_msg");
        eventMessage.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        eventMessage.setData(new HashMap<>(json));
        return eventMessage;
    }

    private CommonDeviceMessage handleDeviceOnlineStatus(String topic, JSONObject json) {
        CommonDeviceMessage deviceMessage;

        if ("1".equals(json.getString("status"))) {
            deviceMessage = new DeviceOnlineMessage();
        } else {
            deviceMessage = new DeviceOfflineMessage();
        }
        deviceMessage.setDeviceId(json.getString("deviceId"));

        return deviceMessage;
    }

}