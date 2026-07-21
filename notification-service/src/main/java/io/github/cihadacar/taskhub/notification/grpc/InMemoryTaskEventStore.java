package io.github.cihadacar.taskhub.notification.grpc;

import java.util.ArrayList;
import java.util.List;

import io.github.cihadacar.taskhub.proto.notification.v1.TaskEvent;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTaskEventStore {

    private final List<TaskEvent> events = new ArrayList<>();

    public synchronized void save(TaskEvent event) {
        events.add(event);
    }

    public synchronized List<TaskEvent> findAll() {
        return List.copyOf(events);
    }
}
