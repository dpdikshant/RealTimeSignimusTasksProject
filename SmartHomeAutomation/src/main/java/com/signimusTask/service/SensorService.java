package com.signimusTask.service;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.signimusTask.config.SensorDataEvent;
import com.signimusTask.entity.Sensor;
import com.signimusTask.repository.SensorRepository;
import com.signimusTask.service.DeviceService;

import java.util.List;
import java.util.Optional;

@Service
public class SensorService {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private DeviceService deviceService; // Injected DeviceService for device control actions

    @Autowired
    private ApplicationEventPublisher eventPublisher; // For publishing custom events

    private static final double TEMPERATURE_THRESHOLD = 1.0; // Ignore fluctuations within this range

    // Retrieve all sensors
    public List<Sensor> getAllSensors() {
        return sensorRepository.findAll();
    }

    // Save or update a sensor
    public Sensor saveSensor(Sensor sensor) {
        return sensorRepository.save(sensor);
    }

    // Process sensor data received from MQTT and trigger device actions if necessary
    public void processSensorData(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            String type = json.getString("type");
            String location = json.getString("location");
            double value = json.getDouble("value");

            // Find the sensor by type and location, or create a new one
            Optional<Sensor> existingSensorOpt = sensorRepository.findByTypeAndLocation(type, location);
            Sensor sensor = existingSensorOpt.orElse(new Sensor());
            sensor.setType(type);
            sensor.setLocation(location);

            // Check if the new value is meaningfully different from the previous value
            boolean isSignificantChange = existingSensorOpt.map(s -> Math.abs(s.getValue() - value) >= TEMPERATURE_THRESHOLD)
                                                           .orElse(true);

            // Only update and publish event if there is a significant change
            if (isSignificantChange) {
                sensor.setValue(value);
                sensor.setLastUpdated(System.currentTimeMillis());
                sensorRepository.save(sensor);
                eventPublisher.publishEvent(new SensorDataEvent(this, sensor));

                // Trigger actions based on sensor type and value
                handleSensorTriggeredActions(type, location, value);
            }
        } catch (Exception e) {
            System.err.println("Failed to process sensor data: " + e.getMessage());
        }
    }

    // Define actions based on sensor type and data
    private void handleSensorTriggeredActions(String type, String location, double value) {
        switch (type.toLowerCase()) {
            case "motion":
                if (value > 0) { // Motion detected
                    deviceService.executeAction("turn on light in " + location);
                }
                break;

            case "temperature":
                if (value > 25) { // Example condition to turn on cooling if temperature is high
                    deviceService.executeAction("set thermostat in " + location + " to cool");
                }
                break;

            case "humidity":
                if (value > 70) { // Example condition to activate dehumidifier
                    deviceService.executeAction("turn on dehumidifier in " + location);
                }
                break;

            default:
                System.out.println("No action defined for sensor type: " + type);
                break;
        }
    }
}
