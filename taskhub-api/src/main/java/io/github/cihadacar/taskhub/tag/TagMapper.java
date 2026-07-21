package io.github.cihadacar.taskhub.tag;

import org.springframework.stereotype.Component;

@Component
class TagMapper {

    TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.id(), tag.name(), tag.color());
    }
}
