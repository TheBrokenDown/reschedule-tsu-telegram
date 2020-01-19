package space.delusive.tversu.util;

import space.delusive.tversu.entity.DayOfWeek;
import space.delusive.tversu.entity.WeekSign;
import space.delusive.tversu.manager.IDataManager;

public class BaseUtils {
    public static String capitalizeString(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String getLocalizedNameOfDay(DayOfWeek dayOfWeek, IDataManager messages) {
        return messages.getString("day.of.week." + dayOfWeek.toString().toLowerCase());
    }

    public static String getLocalizedNameOfDayInAccusative(DayOfWeek dayOfWeek, IDataManager messages) {
        return messages.getString("day.of.week." + dayOfWeek.toString().toLowerCase() + ".accusative");
    }

    public static String getLocalizedNameOfWeekSign(WeekSign weekSign, IDataManager messages) {
        return messages.getString("week.sign." + weekSign.toString().toLowerCase());
    }
}
