package com.hbueluojing.merge.dto;

import com.hbueluojing.merge.response.TimeRange;
import lombok.Data;

@Data
public class TimeRangeResult {
	private TimeRange modified;
	private TimeRange removed;
}
