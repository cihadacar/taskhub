package io.github.cihadacar.taskhub.tag;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.github.cihadacar.taskhub.common.error.ConflictException;
import org.springframework.stereotype.Repository;

@Repository
class InMemoryTagRepository implements TagRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, Tag> tags = new ConcurrentHashMap<>();

    @Override
    public synchronized Tag create(String name, String color) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        if (tags.values().stream().anyMatch(tag -> tag.name().equals(normalizedName))) {
            throw new ConflictException("A tag with that name already exists.");
        }
        Tag tag = new Tag(sequence.incrementAndGet(), normalizedName, color.toUpperCase(Locale.ROOT));
        tags.put(tag.id(), tag);
        return tag;
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return Optional.ofNullable(tags.get(id));
    }

    @Override
    public List<Tag> findAll() {
        return tags.values().stream().sorted(Comparator.comparing(Tag::id)).toList();
    }
}
