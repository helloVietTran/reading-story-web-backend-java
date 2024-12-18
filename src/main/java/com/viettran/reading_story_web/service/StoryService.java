package com.viettran.reading_story_web.service;

import com.viettran.reading_story_web.dto.request.StoryRequest;
import com.viettran.reading_story_web.dto.response.ChapterResponse;
import com.viettran.reading_story_web.dto.response.FollowResponse;
import com.viettran.reading_story_web.dto.response.PageResponse;
import com.viettran.reading_story_web.dto.response.StoryResponse;
import com.viettran.reading_story_web.entity.mysql.Chapter;
import com.viettran.reading_story_web.entity.mysql.Follow;
import com.viettran.reading_story_web.entity.mysql.Genre;
import com.viettran.reading_story_web.entity.mysql.Story;
import com.viettran.reading_story_web.enums.Gender;
import com.viettran.reading_story_web.exception.AppException;
import com.viettran.reading_story_web.exception.ErrorCode;
import com.viettran.reading_story_web.mapper.ChapterMapper;
import com.viettran.reading_story_web.mapper.StoryMapper;
import com.viettran.reading_story_web.repository.ChapterRepository;
import com.viettran.reading_story_web.repository.FollowRepository;
import com.viettran.reading_story_web.repository.StoryCacheRepository;
import com.viettran.reading_story_web.repository.StoryRepository;
import com.viettran.reading_story_web.repository.custom.CustomStoryRepository;
import com.viettran.reading_story_web.scheduler.StoryJobScheduler;
import com.viettran.reading_story_web.utils.DateTimeFormatUtil;
import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryService {
    StoryRepository storyRepository;
    FollowRepository followRepository;
    ChapterRepository chapterRepository;
    CustomStoryRepository customStoryRepository;

    StoryMapper storyMapper;
    ChapterMapper chapterMapper;

    FileService fileService;
    GenreService genreService;
    AuthenticationService authenticationService;

    DateTimeFormatUtil dateTimeFormatUtil;

    StoryCacheRepository storyCacheRepository;

    StoryJobScheduler storyJobScheduler;

    @NonFinal
    @Value("${app.folder.story}")
    protected String STORY_FOLDER;

    @PreAuthorize("hasRole('ADMIN')")
    public StoryResponse createStory(StoryRequest request) throws IOException {
        String imgSrc = fileService.uploadFile(request.getFile(), STORY_FOLDER);

        Story story = storyMapper.toStory(request);
        Set<Genre> genres = genreService.getGenresByIds(request.getGenreIds());

        story.setImgSrc(imgSrc);
        story.setGenres(genres);
        story.setCreatedAt(Instant.now());
        story.setUpdatedAt(Instant.now());
        story.setStatus(request.getStatus());

        StoryResponse response = storyMapper.toStoryResponse(storyRepository.save(story));
        response.setStatus(story.getStatus().getFullName());
        return response;
    }

    public PageResponse<StoryResponse> getStories(int page, int size) {
        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        Pageable pageable = PageRequest.of(page - 1, size, sorting);

        Page<Story> pageData = storyRepository.findAll(pageable);
        List<Story> stories = pageData.getContent();

        List<StoryResponse> storyResponses = maptoStoryResponseList(stories);

        return PageResponse.<StoryResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .data(storyResponses)
                .build();
    }

    public StoryResponse getStoryById(Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_EXISTED));
        StoryResponse response = storyMapper.toStoryResponse(story);

        response.setUpdatedAt(dateTimeFormatUtil.format(story.getUpdatedAt()));
        response.setCreatedAt(dateTimeFormatUtil.format(story.getCreatedAt()));
        response.setStatus(story.getStatus().getFullName());
        response.setGenres(story.getGenres()
                .stream()
                .map(Genre::getName)
                .collect(Collectors.toSet()));

        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteStory(Integer id) {
        storyRepository.deleteById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public StoryResponse updateStory(StoryRequest request, Integer storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_EXISTED));

        story.setUpdatedAt(Instant.now());
        storyMapper.updateStory(story, request);

        return storyMapper.toStoryResponse(storyRepository.save(story));
    }

    public PageResponse<StoryResponse> searchStories(String keyword, int page, int size) {
        Sort sort = Sort.by("updatedAt").descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        var pageData = storyRepository.searchByKeyword(keyword, pageable);

        return PageResponse.<StoryResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .data(pageData.getContent().stream().map(storyMapper::toStoryResponse).toList())
                .build();
    }

    public StoryResponse rateStory(int storyId, int point) {
        if (point < 1 || point > 5)
            throw new AppException(ErrorCode.RATING_POINT_INVALID);

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_EXISTED));

        story.setRatingCount(story.getRatingCount() + 1);
        story.setTotalRatingPoint(story.getTotalRatingPoint() + point);

        double newRate = (double) story.getTotalRatingPoint() / story.getRatingCount();

        DecimalFormat df = new DecimalFormat("#.##");
        newRate = Double.parseDouble(df.format(newRate));

        story.setRate(newRate);

        storyRepository.save(story);
        return storyMapper.toStoryResponse(story);
    }

    public List<StoryResponse> filterStory(
            Integer genreCode,
            Integer status,
            Integer sort,
            String keyword
    ) {
        List<Story> stories = customStoryRepository.findStories(genreCode, status, sort, keyword);
        return maptoStoryResponseList(stories);
    }

    public PageResponse<StoryResponse> getStoriesByGender(int page, int size, Gender gender) {
        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        Pageable pageable = PageRequest.of(page - 1, size, sorting);

        Page<Story> pageData = storyRepository.findByGenderNot(gender, pageable);
        List<Story> stories = pageData.getContent();

        List<StoryResponse> storyResponses = maptoStoryResponseList(stories);

        return PageResponse.<StoryResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .data(storyResponses)
                .build();
    }

    public PageResponse<StoryResponse> getHotStories(int page, int size) {
        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        Pageable pageable = PageRequest.of(page-1, size, sorting);

        Page<Story> pageData = storyRepository.findByHotTrue(pageable);
        List<Story> stories = pageData.getContent();

        List<StoryResponse> storyResponses = maptoStoryResponseList(stories);

        return PageResponse.<StoryResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPages(pageData.getTotalPages())
                .data(storyResponses)
                .build();
    }

    public List<StoryResponse> getTop10StoriesByViewCount() {
        //Iterable<StoryCache> iterable = storyCacheRepository.findAll();
        Pageable pageable = PageRequest.of(0, 10);
        List<Story> topStories = storyRepository.findTop10ByOrderByViewCountDesc(pageable);

        return topStories.stream().map(storyMapper::toStoryResponse).toList();

       /* if(!iterable.iterator().hasNext()){
            // cache không thấy


            for(Story story : topStories){
                // lưu cache
                storyCacheRepository.save(storyMapper.toStoryCache(story));
            }

        }else{
            List<StoryCache> cacheTopStories = new ArrayList<>();
            iterable.forEach(cacheTopStories::add);

            return cacheTopStories.stream().map(storyMapper::toStoryResponse).toList();
        }*/
    }

    public List<FollowResponse> getFollowedStories() {
        String userId = authenticationService.getCurrentUserId();
        List<Follow> followedStories = followRepository.findFollowedStories(userId);

        return followedStories.stream()
                .map(follow -> {
                    Story story = follow.getStory();
                    return FollowResponse.builder()
                            .id(follow.getId())
                            .story(
                                    StoryResponse.builder()
                                            .imgSrc(story.getImgSrc())
                                            .id(story.getId())
                                            .name(story.getName())
                                            .slug(story.getSlug())
                                            .updatedAt(dateTimeFormatUtil.format(story.getUpdatedAt()))
                                            .newestChapter(story.getNewestChapter())
                                            .viewCount(story.getViewCount())
                                            .build()
                            )
                            .followTime(dateTimeFormatUtil.format(follow.getFollowTime()))
                            .build();
                })
                .toList();

    }

    public FollowResponse getFollowedStoryByUserIdAndStoryId(int storyId) {
        String userId = authenticationService.getCurrentUserId();

        Optional<Follow> followOptional = followRepository.findByUserIdAndStoryId(userId, storyId);
        if (followOptional.isEmpty())
            throw new AppException(ErrorCode.FOLLOWED_STORY);
        Follow follow = followOptional.get();

        return FollowResponse
                .builder()
                .id(follow.getId())
                .story(storyMapper.toStoryResponse(follow.getStory()))
                .followTime(dateTimeFormatUtil.format(follow.getFollowTime()))
                .build();
    }


    public List<StoryResponse> filterAdvanced
            (List<Integer> genreCodes,
             List<Integer> notGenreCodes,
             Integer status,
             Integer sort,
             Integer minChapter,
             Integer gender
            ) {
        List<Story> stories = customStoryRepository.findStoriesAdvanced(
                genreCodes,
                notGenreCodes,
                status,
                sort,
                minChapter,
                gender,
                null
        );

        return maptoStoryResponseList(stories);
    }

    public List<StoryResponse> getFeaturedStories() {

        return storyRepository.findTop5ByViewCountGreaterThan5()
                .stream().map(story -> {
                    StoryResponse storyResponse = storyMapper.toStoryResponse(story);
                    storyResponse.setUpdatedAt(dateTimeFormatUtil.format(story.getUpdatedAt()));

                    return storyResponse;
                }).toList();
    }

    private List<StoryResponse> maptoStoryResponseList(List<Story> stories) {
        return stories.stream().map(story -> {
            StoryResponse storyResponse = storyMapper.toStoryResponse(story);

            Set<String> genreNames = story.getGenres().stream()
                    .map(Genre::getName)
                    .collect(Collectors.toSet());
            storyResponse.setGenres(genreNames);

            List<Chapter> chapters = chapterRepository.findTop3ChaptersByStoryId(story.getId());

            List<ChapterResponse> chapterResponses = chapters.stream()
                    .map(chapter -> {
                        ChapterResponse chapterResponse = chapterMapper.toChapterResponse(chapter);
                        chapterResponse.setCreatedAt(dateTimeFormatUtil.format(chapter.getCreatedAt()));
                        chapterResponse.setUpdatedAt(dateTimeFormatUtil.format(chapter.getUpdatedAt()));
                        return chapterResponse;
                    })
                    .collect(Collectors.toList());

            storyResponse.setChapters(chapterResponses);
            storyResponse.setStatus(story.getStatus().getFullName());
            storyResponse.setCreatedAt(dateTimeFormatUtil.format(story.getCreatedAt()));
            storyResponse.setUpdatedAt(dateTimeFormatUtil.format(story.getUpdatedAt()));

            return storyResponse;
        }).toList();
    }

    @PostConstruct
    public void cacheStoryViewCountInitially() {
        storyJobScheduler.syncDataFromRedisToMySQL();
        storyJobScheduler.cacheStoryViewCountInRedis();
    }
}
