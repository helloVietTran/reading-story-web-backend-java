package com.viettran.reading_story_web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.viettran.reading_story_web.entity.mysql.Image;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChapterResponse {
    String id;
    int chap;
    String chapterName;
    int viewCount;
    String slug;

    String createdAt;
    String updatedAt;

    List<CommentResponse> comments;
    List<Image> imgSrcs;
}
