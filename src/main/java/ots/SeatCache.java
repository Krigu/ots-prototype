package ots;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Direction;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.aggregator.Aggregators;
import net.sf.ehcache.search.impl.GroupedResultImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SeatCache {

    private final CacheManager cm;

    public SeatCache() {
        cm = CacheManager.getInstance();
    }

    public void buildCaches(Set<String> cacheNames) {
        for (String cacheName : cacheNames) {
            CacheConfiguration cacheConfig = new CacheConfiguration(cacheName, 0).eternal(true);
            Searchable searchable = new Searchable();
            searchable.addSearchAttribute(new SearchAttribute().name("sector").expression("value.getSector()"));
            searchable.addSearchAttribute(new SearchAttribute().name("row").expression("value.getRow()"));
            searchable.addSearchAttribute(new SearchAttribute().name("number").expression("value.getNumber()"));
            cacheConfig.addSearchable(searchable);

            Cache cache = new Cache(cacheConfig);
            cm.addCache(cache);
        }
    }

    public void buildCache(List<SeatEntity> seats) {

        for (SeatEntity seat : seats) {
            Cache c = cm.getCache(seat.getCategory());
            c.put(new Element(seat.getId(), seat));
        }
    }

    public synchronized List<SeatEntity> getAllEmptySeatsFromCategory(String category, int maxResults) {
        Cache cache = cm.getCache(category);

        final List<SeatEntity> seatList = new ArrayList<>(maxResults);

        Attribute<String> sec = cache.getSearchAttribute("sector");
        Attribute<Integer> row = cache.getSearchAttribute("row");
        Attribute<Integer> number = cache.getSearchAttribute("number");

        Results results = cache.createQuery()
                .addGroupBy(sec)
                .addGroupBy(row)
                .includeAggregator()
                .includeAggregator(Aggregators.count())
                .execute();

        for (Result result : results.all()) {
            GroupedResultImpl group = (GroupedResultImpl) result;
            Integer freeSeats = (Integer) group.getAggregatorResults().get(0);
            if (freeSeats >= maxResults) {
                String sector = (String) group.getGroupByValues().get("sector");
                Integer rowNumber = (Integer) group.getGroupByValues().get("row");

                Results seatResult = cache.createQuery().includeValues()
                        .addCriteria(sec.eq(sector))
                        .addCriteria(row.eq(rowNumber))
                        .maxResults(maxResults)
                        .addOrderBy(number, Direction.ASCENDING)
                        .execute();

                for (Result r : seatResult.all()) {
                    final SeatEntity seat = (SeatEntity) r.getValue();
                    seatList.add(seat);
                }

                for (int i = 0; i < seatList.size() - 1; i++) {
                    if (seatList.get(i).getRow() != seatList.get(i + 1).getRow()
                            || seatList.get(i).getNumber() != seatList.get(i + 1).getNumber() - 1) {
                        seatList.clear();
                        break;
                    }
                }

                if (!seatList.isEmpty())
                    break;
            }
        }

        for (SeatEntity seatEntity : seatList) {
            cache.remove(seatEntity.getId());
        }

        return seatList;
    }
}
