package com.example.workflow.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/task")
public class TaskController {
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @PostMapping("/create")
    public ResponseEntity<String> createTask(@RequestBody final Map<String, Object> payload) {
        String initiatorName = (String) payload.get("initiatorName");
        Map<String, Object> variables = new HashMap<>();
        variables.put("initiatorName", initiatorName);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("approval-process", variables);

        return ResponseEntity.ok("Process started with ID:" + processInstance.getProcessInstanceId());
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllTasks() {
        List<Task> tasks = taskService.createTaskQuery().list();

        List<Map<String, Object>> taskList = tasks.stream().map(task -> {
            Map<String, Object> taskDetails = new HashMap<>();
            taskDetails.put("taskId", task.getId());
            taskDetails.put("taskName", task.getName());
            taskDetails.put("assignee", task.getAssignee());
            return taskDetails;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(taskList);
    }

    @PostMapping("/{taskId}/claim")
    public ResponseEntity<String> claimTask(@PathVariable(name = "taskId") final String taskId,
            @RequestBody final Map<String, Object> payload) {
        String assignee = (String) payload.get("assignee");
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
        }
        taskService.setAssignee(taskId, assignee);
        taskService.setVariable(taskId, "status", "PENDING");

        return ResponseEntity.ok("Task " + taskId + " assigned to " + assignee);
    }

    @PostMapping("/{taskId}/complete/approve")
    public ResponseEntity<String> completeTask(@PathVariable(name = "taskId") final String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task not found");
        }
        taskService.setVariable(taskId, "status", "APPROVED");
        taskService.complete(taskId);
        return ResponseEntity.ok("Task " + taskId + " completed");
    }
}
