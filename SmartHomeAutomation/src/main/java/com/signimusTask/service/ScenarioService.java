package com.signimusTask.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import com.signimusTask.entity.Scenario;
import com.signimusTask.repository.ScenarioRepository;

import java.util.List;

@Service
public class ScenarioService {

 @Autowired
 private ScenarioRepository scenarioRepository;

 @Autowired
 private DeviceService deviceService; // Assume DeviceService has methods to execute actions

 private final ExpressionParser parser = new SpelExpressionParser();

 public List<Scenario> getAllScenarios() {
     return scenarioRepository.findAll();
 }

 public Scenario saveScenario(Scenario scenario) {
     return scenarioRepository.save(scenario);
 }

 public void executeScenario(Long scenarioId, Object contextData) {
     Scenario scenario = scenarioRepository.findById(scenarioId).orElseThrow(() -> new RuntimeException("Scenario not found"));

     // Evaluate condition using SpEL
     if (evaluateCondition(scenario.getConditionDescription(), contextData)) {
         scenario.getActions().forEach(action -> {
             // Assuming action format like "turn on light in living room" or "set thermostat to 22"
             deviceService.executeAction(action);
         });
     }
 }

 private boolean evaluateCondition(String condition, Object contextData) {
     if (condition == null || condition.isEmpty()) return true;

     StandardEvaluationContext context = new StandardEvaluationContext();
     context.setRootObject(contextData);

     return parser.parseExpression(condition).getValue(context, Boolean.class);
 }
}
