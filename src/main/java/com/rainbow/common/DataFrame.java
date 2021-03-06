package com.rainbow.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuming on 2017/9/26.
 */
public class DataFrame {
    private List<String> schema = new ArrayList<>();
    private List<ArrayList<String>> data = new ArrayList<>();

    public void insertColumn(int rowIndex, String name, String value) {
        if (data.size() - 1 >= rowIndex) {
            int index = schema.indexOf(name);
            if (index != -1) {
                data.get(rowIndex).set(index, value);
            } else {
                schema.add(name);
                for (int i = 0; i < data.size(); i++) {
                    data.get(i).add(null);
                }

                int columnIndex = schema.indexOf(name);
                data.get(rowIndex).set(columnIndex, value);
            }
        } else if (data.size() == rowIndex) {
            ArrayList<String> row = new ArrayList<>();
            for (int i = 0; i < schema.size(); i++) {
                row.add(null);
            }

            int index = schema.indexOf(name);
            if (index != -1) {
                row.set(index, value);
            } else {
                schema.add(name);
                for (ArrayList<String> tmp : data) {
                    tmp.add(null);
                }

                row.add(value);
            }

            data.add(row);
        } else {
            throw new RuntimeException("cannot insert, because current max row num is: " + data.size());
        }
    }

    public void addColumn(String name, String value) {
        if (!data.isEmpty()) {
            int index = schema.indexOf(name);
            if (index == -1) {
                schema.add(name);
                for (ArrayList<String> row : data) {
                    row.add(value);
                }
            } else {
                throw new RuntimeException("column: " + name + " already exists!");
            }
        } else {
            schema.add(name);

            ArrayList<String> list = new ArrayList<>();
            list.add(value);
            data.add(list);
        }
    }

    public List<String> getColumnNames() {
        return schema;
    }

    public List<String> getRow(int index) {
        return data.get(index);
    }

    public String getColumnValue(int index, String name) {
        int i = schema.indexOf(name);
        if (i != -1) {
            return getRow(index).get(i);
        }

        return null;
    }

    public void append(DataFrame result) {
        if (schema.isEmpty() || schema.size() == result.schema.size()) {
            int rowIndex = data.size();
            for (ArrayList<String> row : result.data) {
                for (int i = 0; i < result.schema.size(); i++) {
                    insertColumn(rowIndex, result.schema.get(i), row.get(i));
                }

                rowIndex++;
            }
        } else {
            throw new RuntimeException("cannot append another DataFrame, because has different column num!");
        }
    }

    public List<String> select(String name) {
        List<String> result = new ArrayList<>();
        int index = schema.indexOf(name);

        if (index != -1) {
            for (ArrayList<String> row : data) {
                result.add(row.get(index));
            }
        }

        return result;
    }


    public int rows() {
        return data.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            ArrayList<String> row = data.get(i);
            int index = 0;
            for (String column : row) {
                String value = column;
                if (value.length() > 80) {
                    value = value.substring(0, 80) + String.format("...(共 %s 字)", value.length());
                }
                sb.append("\t").append(schema.get(index)).append("=>").append(value).append(",");

                index++;
            }

            if (row.size() > 0) {
                sb.deleteCharAt(sb.length() - 1);
            }

            if (i < data.size() - 1) {
                sb.append(System.lineSeparator());
            }
        }

        return sb.toString();
    }

    public static DataFrame merge(DataFrame singleValueResult, DataFrame multiValueResult) {
        if (multiValueResult.rows() > 0) {
            if (singleValueResult.rows() > 0) {
                for (int i = 0; i < singleValueResult.rows(); i++) {
                    List<String> row = singleValueResult.getRow(i);
                    for (int j = 0; j < row.size(); j++) {
                        multiValueResult.addColumn(singleValueResult.schema.get(j), row.get(j));
                    }
                }
            }

            return multiValueResult;
        } else {
            if (singleValueResult.rows() > 0) {
                return singleValueResult;
            }
        }

        return new DataFrame();
    }
}
