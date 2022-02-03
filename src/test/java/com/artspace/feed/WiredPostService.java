package com.artspace.feed;

import com.github.javafaker.Faker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

class WiredPostService {

  private final Faker faker = new Faker(Locale.ENGLISH);

  private final List<Post> samplePosts = new ArrayList<>();

  public static final long PAGE_SIZE = 5L;
  public static final int MAX_PAGES = 6;

  public WiredPostService() {
    final var maxPosts = PAGE_SIZE * MAX_PAGES;
    for (int i = 0; i < maxPosts; i++) {
      samplePosts.add(samplePost());
    }
  }

  public List<Post> getPage(int index) {
    return samplePosts.stream().skip(PAGE_SIZE * index).limit(PAGE_SIZE).collect(Collectors.toUnmodifiableList());
  }

  private Post samplePost() {
    return Post.builder()
        .id(UUID.randomUUID().toString())
        .author(faker.name().username())
        .message(faker.lorem().sentence())
        .isEnabled(true)
        .creationTime(Instant.now()).build();
  }
}
