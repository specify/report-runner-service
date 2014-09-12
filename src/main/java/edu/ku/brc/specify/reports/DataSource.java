package edu.ku.brc.specify.reports;

import com.google.gson.*;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ben
 * Date: 8/15/14
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataSource implements JRDataSource {
    private final Gson gson = new Gson();

    private final Map<String, Integer> fieldColumns = new HashMap<>();
    private JsonArray rows;
    private int rowIdx = -1;

    public DataSource(String json) {
        JsonParser parser = new JsonParser();
        JsonObject root = parser.parse(json).getAsJsonObject();
        JsonArray fields = root.getAsJsonArray("fields");
        for(int i = 0; i < fields.size(); i++) {
            fieldColumns.put(fields.get(i).getAsString(), i);
        }
        rows = root.getAsJsonArray("rows");
    }
    @Override
    public boolean next() throws JRException {
        rowIdx++;
        return rowIdx < rows.size();
    }

    @Override
    public Object getFieldValue(JRField jrField) throws JRException {
        String fieldName = jrField.getName();
        if (fieldName.equalsIgnoreCase("resultsetsize")) {
            return String.valueOf(rows.size());
        }

        Integer col = fieldColumns.get(fieldName);
        JsonArray row = rows.get(rowIdx).getAsJsonArray();
        Object value;
        try
        {
            value = col == null ? null : gson.fromJson(row.get(col), jrField.getValueClass());
        } catch (Exception e) {
            throw new RuntimeException(fieldName + ": " + row.get(col).toString(), e);
        }
        if (fieldName.endsWith("catalogNumber") && value instanceof Double) {
            value = ((Double) value).intValue();
        }
        return value;
    }
}
