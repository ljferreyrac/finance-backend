package com.finanzasia.application.service;

import com.finanzasia.domain.exceptions.DuplicateTagException;
import com.finanzasia.domain.exceptions.TagNotFoundException;
import com.finanzasia.domain.model.Tag;
import com.finanzasia.domain.port.out.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private UUID userId;
    private UUID tagId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tagId = UUID.randomUUID();
    }

    // ------------------------------------------------------------------
    // listTags
    // ------------------------------------------------------------------

    @Test
    void listTagsReturnsTagsForUser() {
        Tag tag = new Tag(tagId, userId, "deducible", "#FF5733");
        when(tagRepository.findByUserId(userId)).thenReturn(List.of(tag));

        List<Tag> result = tagService.listTags(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(tagId);
        assertThat(result.get(0).name()).isEqualTo("deducible");
        verify(tagRepository).findByUserId(userId);
    }

    // ------------------------------------------------------------------
    // createTag
    // ------------------------------------------------------------------

    @Test
    void createTagSuccess() {
        String name = "ocio";
        String color = "#336699";

        when(tagRepository.existsByUserIdAndName(userId, name)).thenReturn(false);

        Tag savedTag = new Tag(UUID.randomUUID(), userId, name, color);
        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

        Tag result = tagService.createTag(userId, name, color);

        assertThat(result.name()).isEqualTo(name);
        assertThat(result.color()).isEqualTo(color);
        assertThat(result.userId()).isEqualTo(userId);

        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo(name);
    }

    @Test
    void createTagThrowsDuplicateTagExceptionWhenNameExists() {
        String name = "ocio";
        when(tagRepository.existsByUserIdAndName(userId, name)).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(userId, name, "#336699"))
                .isInstanceOf(DuplicateTagException.class)
                .hasMessageContaining("ocio");

        verify(tagRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // updateTag
    // ------------------------------------------------------------------

    @Test
    void updateTagSuccess() {
        Tag existing = new Tag(tagId, userId, "viaje", "#AABBCC");
        String newName = "viaje_trabajo";
        String newColor = "#112233";

        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(existing));
        when(tagRepository.existsByUserIdAndName(userId, newName)).thenReturn(false);

        Tag updated = new Tag(tagId, userId, newName, newColor);
        when(tagRepository.save(any(Tag.class))).thenReturn(updated);

        Tag result = tagService.updateTag(userId, tagId, newName, newColor);

        assertThat(result.name()).isEqualTo(newName);
        assertThat(result.color()).isEqualTo(newColor);

        ArgumentCaptor<Tag> captor = ArgumentCaptor.forClass(Tag.class);
        verify(tagRepository).save(captor.capture());
        assertThat(captor.getValue().id()).isEqualTo(tagId);
        assertThat(captor.getValue().name()).isEqualTo(newName);
    }

    @Test
    void updateTagThrowsTagNotFoundExceptionWhenNotOwner() {
        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.updateTag(userId, tagId, "new-name", null))
                .isInstanceOf(TagNotFoundException.class);

        verify(tagRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // deleteTag
    // ------------------------------------------------------------------

    @Test
    void deleteTagSuccess() {
        when(tagRepository.deleteByIdAndUserId(tagId, userId)).thenReturn(true);

        tagService.deleteTag(userId, tagId);

        verify(tagRepository).deleteByIdAndUserId(tagId, userId);
    }

    @Test
    void deleteTagThrowsTagNotFoundExceptionWhenNotOwner() {
        when(tagRepository.deleteByIdAndUserId(tagId, userId)).thenReturn(false);

        assertThatThrownBy(() -> tagService.deleteTag(userId, tagId))
                .isInstanceOf(TagNotFoundException.class);
    }

    @Test
    void updateTagRejectsANameAlreadyUsedByAnotherTag() {
        Tag existing = new Tag(tagId, userId, "viaje", "#FFF");
        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(existing));
        when(tagRepository.existsByUserIdAndName(userId, "deducible")).thenReturn(true);

        assertThatThrownBy(() -> tagService.updateTag(userId, tagId, "Deducible", null))
                .isInstanceOf(DuplicateTagException.class);

        verify(tagRepository, never()).save(any());
    }

    @Test
    void updateTagKeepsTheCurrentNameWhenNoneIsSupplied() {
        Tag existing = new Tag(tagId, userId, "viaje", "#FFF");
        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(existing));
        when(tagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tag result = tagService.updateTag(userId, tagId, null, "#000");

        // Name unchanged means no duplicate lookup is warranted.
        assertThat(result.name()).isEqualTo("viaje");
        assertThat(result.color()).isEqualTo("#000");
        verify(tagRepository, never()).existsByUserIdAndName(any(), any());
    }

    @Test
    void updateTagKeepsTheCurrentColourWhenNoneIsSupplied() {
        Tag existing = new Tag(tagId, userId, "viaje", "#FFF");
        when(tagRepository.findByIdAndUserId(tagId, userId)).thenReturn(Optional.of(existing));
        when(tagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Tag result = tagService.updateTag(userId, tagId, null, null);

        assertThat(result.color()).isEqualTo("#FFF");
    }
}
