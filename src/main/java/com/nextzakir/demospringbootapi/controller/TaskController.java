package com.nextzakir.demospringbootapi.controller;

import com.nextzakir.demospringbootapi.dto.TaskDTO;
import com.nextzakir.demospringbootapi.entity.Task;
import com.nextzakir.demospringbootapi.exception.ConflictException;
import com.nextzakir.demospringbootapi.exception.InternalServerErrorException;
import com.nextzakir.demospringbootapi.exception.UnprocessableEntityException;
import com.nextzakir.demospringbootapi.helper.EntityState;
import com.nextzakir.demospringbootapi.helper.Helper;
import com.nextzakir.demospringbootapi.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @PostMapping("/save/")
    public ResponseEntity<Task> storeTask(@RequestBody TaskDTO dto) {

        if (dto.getTaskTitle() != null && dto.getTaskDescription() != null) {

            if (dto.getTaskTitle().isBlank() || dto.getTaskDescription().isBlank()) {
                throw new UnprocessableEntityException("Invalid task title and task description values.");
            }

            if (dto.getTaskTitle().length() > 50 || dto.getTaskDescription().length() > 150) {
                throw new UnprocessableEntityException("Task title should not be more than 50 characters and task description should not be more than 150 chars.");
            }

            if (taskService.taskTitleExists(dto.getTaskTitle())) {
                throw new ConflictException("Task title already exists.");
            }

            Task theTask;
            Task task = new Task();
            task.setTaskTitle(dto.getTaskTitle());
            task.setTaskSlug(Helper.toSlug(dto.getTaskTitle()));
            task.setTaskDescription(dto.getTaskDescription());
            task.setTaskState(EntityState.Incomplete.toString());
            task.setCreatedAt(Helper.getCurrentTimestamp());
            task.setUpdatedAt(Helper.getCurrentTimestamp());

            try {
                theTask = taskService.saveAndReturnTask(task);
            } catch (Exception e) {
                logger.error("Could not save new task. ERROR: " + e.getMessage());
                throw new InternalServerErrorException("Something went wrong on the server!");
            }

            return new ResponseEntity<>(theTask, HttpStatus.CREATED);
        } else {
            throw new UnprocessableEntityException("Task title and task description fields are required!");
        }

    }

}