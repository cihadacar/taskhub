package io.github.cihadacar.taskhub.notification;

public interface TaskEventPublisher {

    void publish(TaskNotification notification);
}
