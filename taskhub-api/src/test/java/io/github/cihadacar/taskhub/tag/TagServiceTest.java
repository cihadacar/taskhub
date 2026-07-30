package io.github.cihadacar.taskhub.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.github.cihadacar.taskhub.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository, new TagMapper());
    }

    @Test
    void createStripsTheTagName() {
        when(tagRepository.create("backend", "#3366FF"))
                .thenReturn(new Tag("backend", "#3366FF"));

        TagResponse response = tagService.create(new TagRequest(" backend ", "#3366FF"));

        assertThat(response.name()).isEqualTo("backend");
        assertThat(response.color()).isEqualTo("#3366FF");
    }

    @Test
    void getAllReturnsTagsInIdentifierOrder() {
        when(tagRepository.findById(1L)).thenReturn(Optional.of(new Tag("backend", "#3366FF")));
        when(tagRepository.findById(2L)).thenReturn(Optional.of(new Tag("urgent", "#FF0000")));

        List<TagResponse> response = tagService.getAll(Set.of(2L, 1L));

        assertThat(response).extracting(TagResponse::name).containsExactly("backend", "urgent");
    }

    @Test
    void getAllReturnsAnEmptyListWithoutRepositoryCallsForNoIds() {
        assertThat(tagService.getAll(null)).isEmpty();
        assertThat(tagService.getAll(Set.of())).isEmpty();
    }

    @Test
    void getAllRejectsAnUnknownTag() {
        when(tagRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.getAll(Set.of(9L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tag");
    }

    @Test
    void listPaginatesMappedTags() {
        when(tagRepository.findAll()).thenReturn(List.of(
                new Tag("backend", "#3366FF"),
                new Tag("urgent", "#FF0000")));

        var response = tagService.list(1, 1);

        assertThat(response.content()).extracting(TagResponse::name).containsExactly("urgent");
        assertThat(response.totalElements()).isEqualTo(2);
    }
}
