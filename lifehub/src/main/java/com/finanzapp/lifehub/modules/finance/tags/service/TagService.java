package com.finanzapp.tags.service;

import com.finanzapp.tags.service.domain.CreateTagRequest;
import com.finanzapp.tags.service.domain.Tag;

import java.util.List;

public interface TagService {

    Tag createTag(CreateTagRequest createTagRequest, String userEmail);

    List<Tag> getTagsByUserEmail(String userEmail);

    Tag getTagByIdAndUserEmail(Long tagId, String userEmail);

    void deleteTag(Long tagId, String userEmail);

    Boolean existsByIdAndUserEmail(Long tagId, String userEmail);

    List<Tag> getTagsByIdsAndUserEmail(List<Long> ids, String email);

    Tag createTagForOwner(CreateTagRequest createTagRequest, String ownerEmail, String actorEmail);

    List<Tag> getTagsForOwner(String ownerEmail, String actorEmail);

    void deleteTagForOwner(Long tagId, String ownerEmail, String actorEmail);
}
