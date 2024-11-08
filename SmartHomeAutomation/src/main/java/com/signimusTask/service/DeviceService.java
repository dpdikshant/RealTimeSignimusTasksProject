package com.signimusTask.service;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.signimusTask.entity.Device;
import com.signimusTask.repository.DeviceRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    
    @Autowired
    private MessageChannel deviceControlOutputChannel;

    private MqttClient mqttClient;

    // Initialize MQTT client after bean construction
    @PostConstruct
    public void init() {
        try {
            mqttClient = new MqttClient("tcp://localhost:1883", MqttClient.generateClientId());
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("MQTT connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    System.out.println("Message arrived from topic " + topic + ": " + new String(message.getPayload()));
                    // Process incoming messages if needed
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message delivery complete");
                }
            });
            mqttClient.connect();
            subscribeToDeviceDiscovery();
        } catch (MqttException e) {
            System.err.println("Failed to connect to MQTT broker: " + e.getMessage());
        }
    }

    // Retrieve all devices from the database
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    // Retrieve a specific device by ID
    public Optional<Device> getDeviceById(Long id) {
        return deviceRepository.findById(id);
    }

    // Save or update a device in the database
    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }

    // Update a device status by deviceId and save to the repository
    public Device updateDeviceStatus(String deviceId, String status) {
        Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceId);
        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            device.setStatus(status);
            return deviceRepository.save(device);
        } else {
            throw new RuntimeException("Device not found with ID: " + deviceId);
        }
    }

    // Device discovery subscription with error handling
    private void subscribeToDeviceDiscovery() {
        try {
            mqttClient.subscribe("smartHome/deviceDiscovery", (topic, message) -> {
                String deviceInfo = new String(message.getPayload());
                String[] parts = deviceInfo.split(",");
                if (parts.length == 3) {
                    String deviceId = parts[0];
                    String deviceType = parts[1];
                    String location = parts[2];

                    Optional<Device> existingDevice = deviceRepository.findByDeviceId(deviceId);
                    if (existingDevice.isEmpty()) {
                        Device device = new Device();
                        device.setDeviceId(deviceId);
                        device.setDeviceType(deviceType);
                        device.setLocation(location);
                        device.setStatus("OFF");
                        deviceRepository.save(device);
                        System.out.println("New device discovered and saved: " + device);
                    }
                } else {
                    System.err.println("Invalid device info format received: " + deviceInfo);
                }
            });
        } catch (MqttException e) {
            System.err.println("Error subscribing to device discovery: " + e.getMessage());
        }
    }

    // Find device by type and location
    public Optional<Device> findDeviceByTypeAndLocation(String deviceType, String location) {
        return deviceRepository.findByDeviceTypeAndLocation(deviceType, location);
    }
    
    public void discoverDevices() {
        try {
            mqttClient.subscribe("smartHome/deviceDiscovery", (topic, message) -> {
                String deviceInfo = new String(message.getPayload());
                String[] parts = deviceInfo.split(",");
                if (parts.length == 3) {
                    String deviceId = parts[0];
                    String deviceType = parts[1];
                    String location = parts[2];

                    Optional<Device> existingDevice = deviceRepository.findByDeviceId(deviceId);
                    if (existingDevice.isEmpty()) {
                        Device device = new Device();
                        device.setDeviceId(deviceId);
                        device.setDeviceType(deviceType);
                        device.setLocation(location);
                        device.setStatus("OFF");
                        deviceRepository.save(device);
                        System.out.println("New device discovered and saved: " + device);
                    }
                } else {
                    System.err.println("Invalid device info format received: " + deviceInfo);
                }
            });
        } catch (MqttException e) {
            System.err.println("Error subscribing to device discovery: " + e.getMessage());
        }
    }

    // Execute actions based on parsed commands, such as "turn on light"
    public void executeAction(String action) {
        if (action.contains("turn on")) {
            System.out.println("Executing action: Turning on device based on action string");
        } else if (action.contains("set thermostat to")) {
            System.out.println("Executing action: Setting thermostat based on action string");
        } else {
            System.err.println("Action not recognized: " + action);
        }
    }

    // Send command to device over MQTT through a message channel
    public void sendDeviceCommand(String command) {
        deviceControlOutputChannel.send(MessageBuilder.withPayload(command).build());
        System.out.println("Device command sent: " + command);
    }

    // Close the MQTT client connection gracefully
    @PreDestroy
    public void disconnectMqttClient() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (MqttException e) {
            System.err.println("Failed to disconnect MQTT client: " + e.getMessage());
        }
    }
}

