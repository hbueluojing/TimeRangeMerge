package com.hbueluojing.merge.dto;

import com.hbueluojing.merge.result.TimeRange;
import lombok.Data;

@Data
public class TimeRangeResult {
	private TimeRange modified;
	private TimeRange removed;
}
