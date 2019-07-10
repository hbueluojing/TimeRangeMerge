package com.hbueluojing.merge.dto;

import com.hbueluojing.merge.response.TimeRange;
import java.util.List;
import lombok.Data;

@Data
public class TimeRangeMergerCollection {
	List<TimeRange> resultList;
	List<TimeRange> removedList;
}
