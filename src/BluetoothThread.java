public class BluetoothThread extends Thread
{
    /**
     * Color detected by the sensor, categorized by overall value. <br>
     * - 0: No color<br>
     * - 1: Black<br>
     * - 2: Blue<br>
     * - 3: Green<br>
     * - 4: Yellow<br>
     * - 5: Red<br>
     * - 6: White<br>
     * - 7: Brown
     **/
    private String colorConvert (int color){
        if(color == 1) return "Black";
        else if(color == 2) return "Black";
        else if(color == 3) return "Black";
        else if(color == 4) return "Black";
        else if(color == 5) return "Black";
        else if(color == 6) return "Black";
        else if(color == 7) return "Black";
        else return "Nocolor";
    }
}
