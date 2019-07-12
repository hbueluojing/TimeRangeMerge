package com.hbueluojing.merge.logic;

import com.google.common.collect.Lists;
import com.hbueluojing.merge.dto.TimeRangeMergerCollection;
import com.hbueluojing.merge.dto.TimeRangeResult;
import com.hbueluojing.merge.response.TimeRange;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TimePeriodMerger {

	public TimeRangeMergerCollection mergeRequestList(List<TimeRange> requestList, Supplier<TimeRange> constructor) {
		TimeRangeMergerCollection result = new TimeRangeMergerCollection();
		if (requestList.size() <= 1) {
			result.setResultList(requestList);
			return result;
		}
		List<TimeRange> resultList = Lists.newArrayList(requestList.get(0));
		List<TimeRange> removedList = Lists.newArrayList();
		for (int i = 1; i < requestList.size(); i++) {
			TimeRange newEntity = requestList.get(i);
			removedList.addAll(mergedCollection(newEntity, resultList, constructor));
			resultList.add(newEntity);
		}
		result.setResultList(resultList);
		result.setRemovedList(removedList);
		return result;
	}

	public TimeRangeMergerCollection mergeListForUpdate(List<TimeRange> newList, List<TimeRange> baseList, Supplier<TimeRange> constructor) {
		TimeRangeMergerCollection result = new TimeRangeMergerCollection();
		List<TimeRange> removedList = newList.stream()
				.map(newEntity -> mergedCollection(newEntity, baseList, constructor))
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
		result.setRemovedList(removedList);
		result.setResultList(baseList);
		return result;
	}

	private List<TimeRange> mergedCollection(TimeRange newEntity, List<TimeRange> requestList, Supplier<TimeRange> constructor) {
		List<TimeRange> removed = Lists.newArrayList();
		List<TimeRange> modified = Lists.newArrayList();
		for (TimeRange oldEntity : requestList) {
			TimeRangeResult mergedResult = mergeEntity(newEntity, oldEntity, constructor);
			Optional.ofNullable(mergedResult).map(TimeRangeResult::getModified).ifPresent(modified::add);
			Optional.ofNullable(mergedResult).map(TimeRangeResult::getRemoved).ifPresent(removed::add);
		}
		requestList.addAll(modified);
		requestList.removeAll(removed);
		return removed;
	}

	private TimeRangeResult mergeEntity(TimeRange newEntity, TimeRange oldEntity, Supplier<TimeRange> constructor) {
		TimeRangeResult mergedResult = new TimeRangeResult();
		if (isOverlapped(newEntity, oldEntity)) {
			shift(newEntity, oldEntity);
		} else if (isEclipsed(newEntity, oldEntity)) {
			mergedResult.setRemoved(oldEntity);
		} else if (isDivided(newEntity, oldEntity)) {
			TimeRange newOldAfter = constructor.get();
			newOldAfter.setStartDateTime(oldEntity.getStartDateTime());
			newOldAfter.setEndDateTime(oldEntity.getEndDateTime());
			newOldAfter.resetKeyAndStartDateTime(newEntity.getEndDateTime().plusSeconds(1));
			mergedResult.setModified(newOldAfter);

			oldEntity.setEndDateTime(newEntity.getStartDateTime().minusSeconds(1));
		}
		return mergedResult;
	}

	private void shift(TimeRange newEntity, TimeRange oldEntity) {
		if (!newEntity.getStartDateTime().isAfter(oldEntity.getStartDateTime())) {
			oldEntity.setStartDateTime(newEntity.getEndDateTime().plusSeconds(1));
		} else {
			oldEntity.setEndDateTime(newEntity.getStartDateTime().minusSeconds(1));
		}
	}

	private Boolean isEclipsed(TimeRange newEntity, TimeRange oldEntity) {
		return !newEntity.getStartDateTime().isAfter(oldEntity.getStartDateTime())
			   && !newEntity.getEndDateTime().isBefore(oldEntity.getEndDateTime());
	}

	private Boolean isDivided(TimeRange newEntity, TimeRange oldEntity) {
		return newEntity.getStartDateTime().isAfter(oldEntity.getStartDateTime())
			   && newEntity.getEndDateTime().isBefore(oldEntity.getEndDateTime());
	}

	private Boolean isOverlapped(TimeRange newEntity, TimeRange oldEntity) {
		return (newEntity.getStartDateTime().isAfter(oldEntity.getStartDateTime())
				&& !newEntity.getStartDateTime().isAfter(oldEntity.getEndDateTime())
				&& newEntity.getEndDateTime().isAfter(oldEntity.getEndDateTime()))
			   || (newEntity.getEndDateTime().isBefore(oldEntity.getEndDateTime())
				   && !newEntity.getEndDateTime().isBefore(oldEntity.getStartDateTime())
				   && newEntity.getStartDateTime().isBefore(oldEntity.getStartDateTime()))
			   || (newEntity.getStartDateTime().equals(oldEntity.getStartDateTime())
				   && newEntity.getEndDateTime().isBefore(oldEntity.getEndDateTime()))
			   || (newEntity.getStartDateTime().isAfter(oldEntity.getStartDateTime())
				   && newEntity.getEndDateTime().equals(oldEntity.getEndDateTime()));
	}
}
