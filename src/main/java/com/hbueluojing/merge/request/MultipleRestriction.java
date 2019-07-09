package com.hbueluojing.merge.request;

import com.hbueluojing.merge.annotation.DateTimePattern;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MultipleRestriction implements Serializable {

	private String key;

	@DateTimePattern
	private LocalDateTime startDateTime;

	@DateTimePattern
	private LocalDateTime endDateTime;
}
