package com.artspace.feed.incoming;

import java.time.Instant;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Post Transfer Object
 */
@Data
@NoArgsConstructor
@ToString
class PostDTO {

  private String id;
  private String message;
  private Instant creationTime;
  private String authorUsername;
  private boolean isEnabled;
  private String action;


  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PostDTO postDTO = (PostDTO) obj;
    return Objects.equals(id, postDTO.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
