package io.github.cihadacar.taskhub.tag;

import java.util.List;
import java.util.Optional;

interface TagRepository {

    Tag create(String name, String color);

    Optional<Tag> findById(Long id);

    List<Tag> findAll();
}
