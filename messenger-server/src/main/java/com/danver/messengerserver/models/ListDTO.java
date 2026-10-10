package com.danver.messengerserver.models;

import com.danver.messengerserver.models.util.Direction;

public class ListDTO {
    /**
     *  Generic class for list methods
     */

    class Paging {
        String field;
        String idField;
        Direction direction;
    }

    class Filter {
        String search;

    }
}
