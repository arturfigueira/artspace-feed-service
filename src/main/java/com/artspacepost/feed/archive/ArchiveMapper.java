package com.artspacepost.feed.archive;

import com.artspacepost.feed.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
interface ArchiveMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(source = "id", target = "postId")
  @Mapping(source = "author", target = "username")
  Archive toArchive(final Post post);
}
