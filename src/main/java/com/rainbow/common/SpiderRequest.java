package com.rainbow.common;

import java.util.Map;

/**
 * Created by xuming on 2017/9/26.
 */
public class SpiderRequest {
    public String spiderId;
    public Map<String, String> params;

    public SpiderRequest(String spiderId, Map<String, String> params) {
        this.spiderId = spiderId;
        this.params = params;
    }

    public boolean check() {
        return spiderId != null && !spiderId.isEmpty();
    }
}
