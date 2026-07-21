package io.github.cihadacar.taskhub.tag;

import java.net.URI;

import io.github.cihadacar.taskhub.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/tags")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    @Operation(summary = "Create a tag")
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
        TagResponse response = tagService.create(request);
        return ResponseEntity.created(URI.create("/api/tags/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "List tags")
    public PageResponse<TagResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return tagService.list(page, size);
    }
}
