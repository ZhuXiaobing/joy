package org.joy;


import com.sun.jna.Library;
import com.sun.jna.Native;
import org.springframework.stereotype.Component;

@Component
public class JoyNotify {

    public interface Kernel32  extends Library {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);
        boolean Beep(int freq, int duration);
    }

    public void bidNotify()  {
        Kernel32.INSTANCE.Beep(168,2000);
    }

    public void askNotify()  {
        Kernel32.INSTANCE.Beep(666,2000);
    }

    public static void main(String[] args) {
        new JoyNotify().bidNotify();
        new JoyNotify().askNotify();
    }

}


