package com.viettran.reading_story_web.mapper;

import com.viettran.reading_story_web.dto.request.ErrorReporterRequest;
import com.viettran.reading_story_web.dto.response.ErrorReporterResponse;
import com.viettran.reading_story_web.entity.mysql.ErrorReporter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ErrorReporterMapper {
    @Mapping(source = "user.id", target = "userId")
    ErrorReporterResponse toErrorReporterResponse(ErrorReporter errorReporter);

    ErrorReporter toErrorReporter(ErrorReporterRequest request);
}