package com.hbueluojing.merge.logic;

import com.google.common.collect.Lists;
import com.hbueluojing.merge.dto.TimeRangeMergerCollection;
import com.hbueluojing.merge.request.MultipleRestriction;
import com.hbueluojing.merge.response.TimeRange;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class MultipleRestrictionLogic<E, T> {
	@Autowired
	private TimePeriodMerger timePeriodMerger;
	private List<TimeRange> EMPTY_LIST = Lists.newArrayList();
	private final LocalDateTime MIN_DATE_TIME = LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0);
	private final LocalDateTime MAX_DATE_TIME = LocalDateTime.of(9999, 12, 31, 23, 59, 59, 0);

	protected abstract List<T> getPresentDataByPeriod(E entity, LocalDateTime periodStart, LocalDateTime periodEnd);

	protected abstract void deleteAllAndFlush(List<T> presentList);

	protected abstract void saveAll(List<T> resultingList);

	protected abstract void remove(List<T> removedList);

	protected abstract T findByKey(String key);

	protected abstract void deleteByKeyIn(List<String> keys);

	protected abstract void linkParents(E entity, List<T> resultingList);

	protected abstract void linkChildren(T self);

	protected abstract void deleteChildren(T self, T newEntity);

	protected abstract List<T> convertRequestList(List<MultipleRestriction> multipleRestrictions, Class<T> tClass);

	protected abstract void convertIfNotNull(T newEntity, T current);

	public void create(E item, List<MultipleRestriction> requestList, Class<T> tClass, Supplier<TimeRange> constructor) {
		if (CollectionUtils.isNotEmpty(requestList)) {
			List<TimeRange> requestTimeRangeList = convertMultipleRestrictionAndReplaceNullDate(requestList, tClass);
			List<TimeRange> presentList = getPresentDataByPeriod(item, requestTimeRangeList);
			deleteAllAndFlush(convert(presentList));

			presentList.addAll(requestTimeRangeList);
			TimeRangeMergerCollection mergerCollection = timePeriodMerger.mergeRequestList(presentList, constructor);
			List<TimeRange> resultList = getNonOverLappedList(mergerCollection);
			saveResultingList(item, resultList, EMPTY_LIST);
		}
	}

	public void patch(E item, List<MultipleRestriction> requestList, Class<T> tClass,
			Supplier<TimeRange> constructor, Function<T, String> keySupplier) {
		if (CollectionUtils.isNotEmpty(requestList)
			&& requestList.stream().noneMatch(multipleRestriction -> Objects.isNull(multipleRestriction.getKey()))) {
			TimeRangeMergerCollection requestCollection = handleRequestListForUpdate(requestList, tClass, constructor);
			List<TimeRange> nonOverLappedRequestList = getNonOverLappedList(requestCollection);
			List<TimeRange> needRemoveFromRequestList = getRemovedList(requestCollection);

			List<TimeRange> presentList = getPresentDataByPeriod(item, nonOverLappedRequestList);
			presentList.removeIf(present -> needRemoveFromRequestList.stream().anyMatch(removed -> removed.getKey().equals(present.getKey())));

			List<TimeRange> selfList = patchSelf(nonOverLappedRequestList, presentList, keySupplier);

			List<TimeRange> resultingList;
			if (CollectionUtils.isNotEmpty(presentList)) {
				TimeRangeMergerCollection mergerCollection = timePeriodMerger.mergeListForUpdate(nonOverLappedRequestList, presentList, constructor);
				resultingList = getNonOverLappedList(mergerCollection);
				List<TimeRange> needRemoveFromDb = getRemovedList(mergerCollection);
				resultingList.addAll(selfList);
				needRemoveFromRequestList.addAll(needRemoveFromDb);
			} else {
				resultingList = selfList;
			}
			saveResultingList(item, resultingList, needRemoveFromRequestList);
		}
	}

	public void delete(E item, List<MultipleRestriction> multipleRestrictions, Class<T> tClass, Supplier<TimeRange> constructor) {
		if (CollectionUtils.isNotEmpty(multipleRestrictions)) {
			List<String> restrictionKeys = multipleRestrictions.stream().filter(multipleRestriction -> Objects.nonNull(multipleRestriction.getKey()))
					.map(MultipleRestriction::getKey).collect(Collectors.toList());
			if (CollectionUtils.isNotEmpty(restrictionKeys)) {
				deleteByKeyIn(restrictionKeys);
			}
			multipleRestrictions.removeIf(multipleRestriction -> Objects.nonNull(multipleRestriction.getKey()));
			if (CollectionUtils.isNotEmpty(multipleRestrictions)) {
				TimeRangeMergerCollection requestCollection = handleRequestListForUpdate(multipleRestrictions, tClass, constructor);
				List<TimeRange> nonOverLappedRequestList = getNonOverLappedList(requestCollection);
				List<TimeRange> needRemoveFromRequestList = getRemovedList(requestCollection);

				List<TimeRange> presentList = getPresentDataByPeriod(item, nonOverLappedRequestList);
				if (CollectionUtils.isNotEmpty(presentList)) {
					TimeRangeMergerCollection mergerRequestAndPresent = timePeriodMerger.mergeListForUpdate(nonOverLappedRequestList, presentList, constructor);
					List<TimeRange> resultingList = getNonOverLappedList(mergerRequestAndPresent);
					needRemoveFromRequestList.addAll(getRemovedList(mergerRequestAndPresent));
					saveResultingList(item, resultingList, needRemoveFromRequestList);
				}
			}
		}
	}

	private List<TimeRange> getNonOverLappedList(TimeRangeMergerCollection requestCollection) {
		return Optional.ofNullable(requestCollection).map(TimeRangeMergerCollection::getResultList).orElse(EMPTY_LIST);
	}

	private List<TimeRange> getRemovedList(TimeRangeMergerCollection requestCollection) {
		return Optional.ofNullable(requestCollection).map(TimeRangeMergerCollection::getRemovedList).orElse(EMPTY_LIST);
	}

	private List<TimeRange> patchSelf(List<TimeRange> nonOverLappedRequestList, List<TimeRange> baseList, Function<T, String> keySupplier) {
		List<T> requestList = convert(nonOverLappedRequestList);
		List<T> result = requestList.stream().map(newEntity -> {
			T self;
			String key = keySupplier.apply(newEntity);
			if (Objects.nonNull(key)) {
				self = findByKey(key);
				baseList.removeIf(oldEntity -> key.equals(oldEntity.getKey()));
			} else {
				self = newEntity;
			}
			deleteChildren(self, newEntity);
			convertIfNotNull(newEntity, self);
			linkChildren(self);
			return self;
		}).collect(Collectors.toList());
		return convert(result);
	}

	private void saveResultingList(E item, List<TimeRange> resultingTimeRangeList, List<TimeRange> removedTimeRangeList) {
		if (CollectionUtils.isNotEmpty(removedTimeRangeList)) {
			removedTimeRangeList.removeIf(entity -> Objects.isNull(entity.getKey()));
			remove(convert(removedTimeRangeList));
		}
		List<T> resultingList = convert(resultingTimeRangeList);
		linkParents(item, resultingList);
		saveAll(resultingList);
	}

	private TimeRangeMergerCollection handleRequestListForUpdate(List<MultipleRestriction> multipleRestrictions, Class<T> tClass, Supplier<TimeRange> constructor) {
		List<TimeRange> requestList = convertMultipleRestrictionAndReplaceNullDate(multipleRestrictions, tClass);
		return timePeriodMerger.mergeRequestList(requestList, constructor);
	}

	private List<TimeRange> convertMultipleRestrictionAndReplaceNullDate(List<MultipleRestriction> multipleRestrictions, Class<T> tClass) {
		List<T> requestList = convertRequestList(multipleRestrictions, tClass);
		List<TimeRange> requestTimeRangeList = convert(requestList);
		replaceNullDate(requestTimeRangeList);
		return requestTimeRangeList;
	}


	private void replaceNullDate(List<TimeRange> requestList) {
		requestList.forEach(entity -> {
			entity.setStartDateTime(Objects.isNull(entity.getStartDateTime()) ? MIN_DATE_TIME : entity.getStartDateTime());
			entity.setEndDateTime(Objects.isNull(entity.getEndDateTime()) ? MAX_DATE_TIME : entity.getEndDateTime());
		});
	}

	private List<TimeRange> getPresentDataByPeriod(E entity, List<TimeRange> requestList) {
		LocalDateTime periodStart = requestList.stream().map(TimeRange::getStartDateTime)
				.min(LocalDateTime::compareTo).orElse(MIN_DATE_TIME);
		LocalDateTime periodEnd = requestList.stream().map(TimeRange::getEndDateTime)
				.max(LocalDateTime::compareTo).orElse(MAX_DATE_TIME);
		return convert(getPresentDataByPeriod(entity, periodStart, periodEnd));
	}

	private <T, K> List<K> convert(List<T> requests) {
		return Objects.isNull(requests)
				? new ArrayList<K>()
				: requests.stream().map(source -> (K) source).collect(Collectors.toList());
	}

}
