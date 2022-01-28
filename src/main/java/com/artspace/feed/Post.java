package com.artspace.feed;

import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.With;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "An user's post")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Getter
@Builder
@With
public class Post {
  private final String id;

  @NotEmpty
  private final String message;

  @NotNull
  private final Instant creationTime;

  @NotBlank
  private final String author;

  private final boolean isEnabled;

  public Instant getCreationTime() {
    return Instant.ofEpochSecond(creationTime.getEpochSecond());
  }
}
