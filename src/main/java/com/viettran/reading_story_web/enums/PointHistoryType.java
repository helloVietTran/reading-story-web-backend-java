package com.viettran.reading_story_web.enums;

import lombok.Getter;

@Getter
public enum PointHistoryType {
    ATTENDANCE("Điểm danh"),
    BUY_ITEMS("Mua vật phẩm");

    private final String type;

    PointHistoryType(String type) {
        this.type = type;
    }
}
