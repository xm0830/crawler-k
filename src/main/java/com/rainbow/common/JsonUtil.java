package com.rainbow.common;

/**
 * Created by xuming on 2017/10/18.
 */
public class JsonUtil {

    public static String trimJson(String str) {
        int beginIndex = 0;
        int endIndex = str.length();

        for (int j = 0; j < str.length(); j++) {
            if (str.charAt(j) != '{' && str.charAt(j) != '[') {
                beginIndex++;
            } else {
                break;
            }
        }

        for (int j = 0; j < str.length(); j++) {
            if (str.charAt(str.length()-j-1) != '}' && str.charAt(str.length()-j-1) != ']' ) {
                endIndex--;
            } else {
                break;
            }
        }

        return str.substring(beginIndex, endIndex);
    }
}
