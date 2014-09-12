package edu.ku.brc.specify.config;

import net.sf.jasperreports.engine.JRDefaultScriptlet;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: ben
 * Date: 9/4/14
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class Scriptlet extends JRDefaultScriptlet {
    DateConverter dateConverter = new DateConverter();

    private static final String SCRPLT_N = "N";
    private static final String SCRPLT_S = "S";
    private static final String SCRPLT_E = "E";
    private static final String SCRPLT_W = "W";
    /**
     * Converts Integer object to int nul -> 0.
     * @param val the value
     * @return an int value
     */
    protected int convertInt(final Integer val)
    {
        return val == null ? 0 : val.intValue();
    }

    /**
     * Returns the count minus quantityReturned minus quantityResolved to see if any are available.
     * @param countArg the count of preps
     * @param QuantityReturnedArg the quant returned
     * @param QuantityResolvedArg the ones remaining
     * @return
     */
    public Integer calcLoanQuantity(final Integer countArg,
                                    final Integer QuantityReturnedArg,
                                    final Integer QuantityResolvedArg)
    {
        int count = convertInt(countArg);
        int quantityReturned = convertInt(QuantityReturnedArg);
        int quantityResolved = convertInt(QuantityResolvedArg);
        return count - quantityReturned - quantityResolved;
    }

    public String dateDifference(java.sql.Date startDate, java.sql.Date endDate)
    {
        String loanLength = "N/A";

        if (startDate != null && endDate != null)
        {
            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startDate);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endDate);

            int monthCount = 0;
            while (startCal.before(endCal))
            {
                startCal.add(Calendar.MONTH, 1);
                monthCount++;
            }

            loanLength = String.format(monthCount == 1 ? "Month" : "Months", monthCount);
        }
        return loanLength;
    }


    public String dateStringDifference(String startDate, String endDate)
    {
        try
        {
            return dateDifference(new java.sql.Date(dateConverter.convert(startDate).getTimeInMillis()),
                    new java.sql.Date(dateConverter.convert(endDate).getTimeInMillis()));
        } catch (Exception pex)
        {
            return "N/A";
        }
    }

    /**
     * @param text
     * @return text with characters such as '&' replaced by their html codes.
     *
     * Currently only replaces '&'.
     *
     */
    public String escapeForHtml(final String text)
    {
        String[] subs = {"&", "&amp;"};
        String result = text;
        for (int s = 0; s < subs.length; s+=2)
        {
            result = result.replaceAll(subs[s], subs[s+1]);
        }
        return result;
    }

    /**
     * @param value
     * @param numberOfDecimalPlaces
     * @return returns value formatted with the specified number of decimal places
     */
    public String formatBigDecimal(final BigDecimal value,
                                   final int numberOfDecimalPlaces)
    {
        return String.format("%." + numberOfDecimalPlaces + "f", value);
    }

    public String formatCatNo(String catalogNumber)
    {
        return catalogNumber;
    }
    /**
     * Formats a BigDecimal into a lat/lon with "N","S","E", "W".
     * @param value
     * @param latLngUnit
     * @param isLat
     * @return
     */
    public String formatLatLon(final BigDecimal value,
                               final Integer    latLngUnit,
                               final boolean    isLat)
    {
        if (value != null)
        {
            return LatLonConverter.format(value,
                    isLat ? LatLonConverter.LATLON.Latitude : LatLonConverter.LATLON.Longitude,
                    LatLonConverter.convertIntToFORMAT(latLngUnit),
                    LatLonConverter.DEGREES_FORMAT.Symbol,
                    LatLonConverter.DECIMAL_SIZES[latLngUnit]);
        }
        return "";
        //return null;
    }

    /**
     * Formats a BigDecimal to a string with "N","S","E", "W".
     * @param bdValue the float value
     * @param isLat whether it is a lat or lon
     * @return Formats a float to a string with "N","S","E", "W"
     */
    public String getDirChar(final BigDecimal bdValue, final boolean isLat)
    {
        if (bdValue == null) { return ""; }

        if (isLat)
        {
            return bdValue.floatValue() > 0.0 ? SCRPLT_N : SCRPLT_S;
        } else
        {
            return bdValue.floatValue() > 0.0 ? SCRPLT_E : SCRPLT_W;
        }
    }

    public String getDirChar(final String strVal, final boolean isLat)
    {
        if (strVal == null) { return ""; }
        return getDirChar(new Float(Float.parseFloat(strVal)), isLat);
    }

    /**
     * Formats a Float to a string with "N","S","E", "W".
     * @param floatVal the Float value
     * @param isLat whether it is a lat or lon
     * @return Formats a float to a string with "N","S","E", "W"
     */
    public String getDirChar(final Float floatVal, final boolean isLat)
    {
        if (floatVal == null) { return ""; }

        if (isLat)
        {
            return floatVal.floatValue() > 0.0 ? SCRPLT_N : SCRPLT_S;
        } else
        {
            return floatVal.floatValue() > 0.0 ? SCRPLT_E : SCRPLT_W;
        }
    }
}
