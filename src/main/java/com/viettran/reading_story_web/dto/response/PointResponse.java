package com.viettran.reading_story_web.dto.response;

import com.viettran.reading_story_web.entity.mysql.PointHistory;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PointResponse {
    String id;

    Integer total;
    Integer consecutiveAttendanceCount;

    Instant lastAttendanceDate;

    List<PointHistory> pointHistories;
}
