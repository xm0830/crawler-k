package com.rainbow.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xuming on 2017/9/27.
 */
public class DataMessage {

    public String spiderId = null;
    public Map<String, String> params = new HashMap<>();

    private DataMessage() {}

    public static DataMessage create(String str) {
        String[] tokens = str.split(Const.Item_Sep, -1);
        String spiderId = tokens[0];
        String keyValues = tokens[1];

        DataMessage data = new DataMessage();
        data.spiderId = spiderId;

        String[] pairs = keyValues.split(Const.Key_Value_Item_Sep, -1);
        for (String pair : pairs) {
            String[] parts = pair.split(Const.Key_Value_Sep, -1);

            data.params.put(parts[0], parts[1]);
        }

        return data;
    }

}
