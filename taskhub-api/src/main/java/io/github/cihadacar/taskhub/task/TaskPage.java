package io.github.cihadacar.taskhub.task;

import java.util.List;

record TaskPage(List<Task> content, long totalElements) {

    TaskPage {
        content = List.copyOf(content);
    }
}
