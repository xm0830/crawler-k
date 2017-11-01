package com.rainbow.storage;

import com.rainbow.common.DataFrame;

import java.io.IOException;

/**
 * Created by xuming on 2017/10/12.
 */
public abstract class BaseStorage {

    protected String value = null;

    public BaseStorage(String value) {
        this.value = value;
    }

    public abstract void init() throws IOException;

    public abstract void save(DataFrame df, String rowKey) throws IOException;

    public abstract void destroy() throws IOException;
}
