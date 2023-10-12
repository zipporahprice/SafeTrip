package seng202.team0.unittests.business;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import seng202.team0.business.FilterManager;

public class FilterManagerTest {

    @Test
    void testGetInstance() {
        FilterManager filters = FilterManager.getInstance();
        Assertions.assertTrue(filters instanceof FilterManager);
    }

    @Test
    void testUpdateFiltersWithQueryString() {
        FilterManager filters = FilterManager.getInstance();
        String expectedString = "severity IN (1, 2) AND (bicycle_involved = 1 OR moped_involved = 1) AND crash_year BETWEEN 2000 AND 2023 " +
                "AND weather IN (\"Fine\", \"Light Rain\", \"Heavy Rain\") AND region IN (\"Canterbury\") AND holiday IN (0, 1)";
        filters.updateFiltersWithQueryString(expectedString);
        Assertions.assertEquals(expectedString, filters.toString());
    }
}
