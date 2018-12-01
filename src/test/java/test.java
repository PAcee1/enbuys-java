import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @Author: Pace
 * @Data: 2018/12/1 12:20
 * @Version: v1.0
 */
public class test {

    public static void main(String[] args) throws ParseException {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        System.out.println(dateString);
        Date date = formatter.parse(dateString);
        System.out.println(date);

        ParsePosition pos = new ParsePosition(8);
        Date currentTime_2 = formatter.parse(dateString, pos);
        System.out.println(currentTime_2);
    }
}
