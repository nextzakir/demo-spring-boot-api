package com.nextzakir.demospringbootapi.controller;

import com.nextzakir.demospringbootapi.dto.TaskDTO;
import com.nextzakir.demospringbootapi.entity.Task;
import com.nextzakir.demospringbootapi.exception.ConflictException;
import com.nextzakir.demospringbootapi.exception.InternalServerErrorException;
import com.nextzakir.demospringbootapi.exception.NotFoundException;
import com.nextzakir.demospringbootapi.exception.UnprocessableEntityException;
import com.nextzakir.demospringbootapi.helper.EntityState;
import com.nextzakir.demospringbootapi.helper.Helper;
import com.nextzakir.demospringbootapi.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

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

    @PutMapping("/update/by/id/{taskId}")
    public ResponseEntity<Task> updateTaskById(@PathVariable("taskId") Long taskId, @RequestBody TaskDTO dto) {

        Optional<Task> optionalTask = taskService.findTaskByTaskRdbmsId(taskId);

        if (optionalTask.isPresent()) {
            Task theTask;
            Task task = optionalTask.get();

            if (dto.getTaskTitle() != null && !dto.getTaskTitle().isBlank()) {
                task.setTaskTitle(dto.getTaskTitle());
                task.setTaskSlug(Helper.toSlug(dto.getTaskTitle()));
            }

            if (dto.getTaskDescription() != null && !dto.getTaskDescription().isBlank()) {
                task.setTaskDescription(dto.getTaskDescription());
            }

            if (dto.getTaskState() != null && !dto.getTaskState().isBlank()) {
                if (!dto.getTaskState().equals(EntityState.Incomplete.toString()) &&
                        !dto.getTaskState().equals(EntityState.Completed.toString())) {
                    throw new UnprocessableEntityException("Task state value is not valid. Only Incomplete and completed values are allowed.");
                }

                task.setTaskState(dto.getTaskState());
            }

            task.setUpdatedAt(Helper.getCurrentTimestamp());

            try {
                theTask = taskService.saveAndReturnTask(task);
            } catch (Exception e) {
                logger.error("Could not update task. ERROR: " + e.getMessage());
                throw new InternalServerErrorException("Something went wrong on the server!");
            }

            return new ResponseEntity<>(theTask, HttpStatus.ACCEPTED); // 202

        } else {
            throw new NotFoundException("No task found by this Id.");
        }

    }

    @DeleteMapping("/delete/by/id/{taskId}")
    public ResponseEntity<Void> deleteTaskById(@PathVariable("taskId") Long taskId) {

        Optional<Task> optionalTask = taskService.findTaskByTaskRdbmsId(taskId);

        if (optionalTask.isPresent()) {

            Task task = optionalTask.get();

            try {
                taskService.delete(task);
            } catch (Exception e) {
                logger.error("Could not delete task. ERROR: " + e.getMessage());
                throw new InternalServerErrorException("Something went wrong on the server!");
            }

            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204

        } else {
            throw new NotFoundException("No task found by this Id.");
        }

    }

    @GetMapping("/find/by/id/{taskId}")
    public ResponseEntity<Task> findTaskById(@PathVariable("taskId") Long taskId) {

        Optional<Task> optionalTask = taskService.findTaskByTaskRdbmsId(taskId);

        if (optionalTask.isPresent()) {

            Task task = optionalTask.get();
            return new ResponseEntity<>(task, HttpStatus.OK); // 200

        } else {
            throw new NotFoundException("No task found by this Id.");
        }

    }

    @GetMapping("/find/all/")
    public ResponseEntity<List<Task>> findAllTasks(@PageableDefault(size = 15) Pageable pageable) {

        Page<Task> contents = taskService.findAllTasks(pageable);

        if (contents.getContent().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            long totalContents = contents.getTotalElements();
            int nbPageContents = contents.getNumberOfElements();

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(totalContents));

            if (nbPageContents < totalContents) {
                headers.add("first", Helper.buildPageUri(PageRequest.of(0, contents.getSize())));
                headers.add("last", Helper.buildPageUri(PageRequest.of(contents.getTotalPages() - 1, contents.getSize())));

                if (contents.hasNext()) {
                    headers.add("next", Helper.buildPageUri(contents.nextPageable()));
                }

                if (contents.hasPrevious()) {
                    headers.add("prev", Helper.buildPageUri(contents.previousPageable()));
                }

                return new ResponseEntity<>(contents.getContent(), headers, HttpStatus.PARTIAL_CONTENT); // 206
            } else {
                return new ResponseEntity<>(contents.getContent(), headers, HttpStatus.OK); // 200
            }
        }

    }

}