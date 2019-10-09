import org.apache.commons.collections.list.TreeList;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author: Pace
 * @Data: 2018/12/1 12:20
 * @Version: v1.0
 */
public class F {

    private String s = "aa";

    public String getS(){
        return s;
    }

        public static void main(String[] args) {
            Scanner sc = new Scanner(System.in);

            System.out.println ("请输入x的值：");
            int x = sc.nextInt();

            int y;

            if (x >= 3){
                y = 2*x + 1;
            }else if (x >= -1 && x < 3){
                y = 2*x;
            }else{
                y = 2*x - 1;
            }

            System.out.println("y:"+y);

        }



}
class S extends  F {

    public String s = "bb";

    public void asd(){
    }
    public void wqe(){
    }
    public void ad(){
    }

}
