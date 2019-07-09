package com.hbueluojing.merge.result;

import java.time.LocalDateTime;

public interface TimeRange {

	LocalDateTime getStartDateTime();

	LocalDateTime getEndDateTime();

	TimeRange setStartDateTime(LocalDateTime startDateTime);

	TimeRange setEndDateTime(LocalDateTime endDateTime);

	void resetKeyAndStartDateTime(LocalDateTime startDateTime);

	String getKey();
}
