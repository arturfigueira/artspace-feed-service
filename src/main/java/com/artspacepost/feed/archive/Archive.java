package com.artspacepost.feed.archive;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@ToString
@Data
@Entity
class Archive {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_archive_id")
  @SequenceGenerator(name = "seq_archive_id", sequenceName = "seq_archive_id")
  private long id;

  @NotBlank
  @Size(min = 1, max = 100)
  private String postId;

  @NotBlank
  @Size(min = 3, max = 50)
  private String username;

  @NotNull
  private Instant creationTime;
}
