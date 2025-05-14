package com.viettran.reading_story_web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.viettran.reading_story_web.entity.mysql.PointHistory;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, String> {}
