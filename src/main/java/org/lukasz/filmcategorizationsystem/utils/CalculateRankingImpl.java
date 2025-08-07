package org.lukasz.filmcategorizationsystem.utils;

import org.springframework.stereotype.Component;

@Component
public class CalculateRankingImpl implements CalculateRanking {
     @Override
    public int ranking(final long size, final String language, final double vote) {
        long smallFile = 209_715_200L;
        if (size < smallFile) {
            return 100;
        }
        int ranking = 0;

        if ("pl".equalsIgnoreCase(language)) {
            ranking += 200;

        }

        if (vote >= 5.0) {
            ranking += 100;
        }


        return ranking;
    }
}
