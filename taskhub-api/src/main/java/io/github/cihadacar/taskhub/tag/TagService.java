package io.github.cihadacar.taskhub.tag;

import java.util.List;
import java.util.Set;

import io.github.cihadacar.taskhub.common.PageResponse;
import io.github.cihadacar.taskhub.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagService(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    public TagResponse create(TagRequest request) {
        return tagMapper.toResponse(tagRepository.create(request.name().strip(), request.color()));
    }

    public PageResponse<TagResponse> list(int page, int size) {
        return PageResponse.from(tagRepository.findAll().stream().map(tagMapper::toResponse).toList(), page, size);
    }

    public List<TagResponse> getAll(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().sorted().map(id -> tagRepository.findById(id)
                        .map(tagMapper::toResponse)
                        .orElseThrow(() -> new ResourceNotFoundException("Tag", id)))
                .toList();
    }
}
