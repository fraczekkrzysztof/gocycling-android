package com.fraczekkrzysztof.gocycling.model.v2;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class PageDto implements Serializable {

    private int pageSize;
    private int thisPage;
    private int nextPage;
    private int lastPage;
}
