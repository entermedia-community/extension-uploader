package org.openedit.uploader;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class English
{
	public String inEnglish(Double inNum) 
	{
		if ( inNum == null)
		{
			return "";
		}
        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);

        if ( inNum.longValue() < 1024)
		{
		    BigDecimal bd = new BigDecimal(inNum.doubleValue());
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);

			return format.format(bd.doubleValue()) + "bytes";
		}
		else if ( inNum.longValue() < 1024000)
		{
			double ks = (double)inNum.doubleValue()/1024D;

		    BigDecimal bd = new BigDecimal(ks);
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);
		    ks = bd.doubleValue();
		    
			return format.format(ks) + "KB";
		}
		else
		{
			double ks = (double)inNum.doubleValue()/1024000D;

		    BigDecimal bd = new BigDecimal(ks);
		    bd = bd.setScale(2,BigDecimal.ROUND_UP);
		    ks = bd.doubleValue();
		    
			return format.format(ks)  + "MB";
			
		}

	}
	public String inEnglishTime(double secondsremain)
	{
        DecimalFormat format = new DecimalFormat();
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);

        String english = null;
		BigDecimal bd = new BigDecimal(secondsremain);
		bd = bd.setScale(2,BigDecimal.ROUND_UP);
		secondsremain =  bd.doubleValue();
		if ( secondsremain < 60)
		{
			english =  format.format( secondsremain ) + " sec";
		}
		else
		{
			double minrem = secondsremain/60D;
		    BigDecimal min = new BigDecimal(minrem);
		    min = min.setScale(2,BigDecimal.ROUND_UP);
		    minrem = min.doubleValue();
		    english = format.format( minrem) + " min";
		}
		return english;
	}

}
