
class SpeakerController extends Thread
{
    private String STARTING = "\"Starting...\"";
    private String SETTING = "\"Setting..\"";
    private String SETUPCOMPLETED= "\"Set up completed!\"";
    private String OBJDETECTED = "\"Object detected\"";
    private String WALLDETECTED = "\"Wall detected\"";
    private String INVESTIGATING = "\"Investigating...\"";
    private String PROCEDING = "\"Proceding with picking it up\"";
    private String PICKITUP = "\"Object picked up\"";
    private String RELEASED = "\"Object released\"";
    private String NOTPICKIT = "\"Isn't an object to pickup\"";
    private boolean go = true;
    //private Speaker speaker;
    private boolean mutex = false;
    private boolean starting;
    private boolean setting;
    private boolean setupcompleted;
    private boolean object;
    private boolean wall;
    private boolean investigating;
    private boolean proceding;
    private boolean pickit;
    private boolean released;
    private boolean notpick;
    private boolean speak;
/*
    public SpeakerController()
    {
        super("SpeakerController");
        this.speaker = new Speaker()
        ;
    }

    @Override
    public void run()
    {
        setup();
        while (go)
        {
            if(speak)
            {
                if (starting)
                {
                    speak(STARTING);
                    reset();
                }
                else if (setting)
                {
                    speak(SETTING);
                    reset();
                }
                else if (setupcompleted)
                {
                    speak(SETUPCOMPLETED);
                    reset();
                }
                else if (object)
                {
                    speak(OBJDETECTED);
                    reset();
                }
                else if (wall)
                {
                    speak(WALLDETECTED);
                    reset();
                }
                else if (investigating)
                {
                    speak(INVESTIGATING);
                    reset();
                }
                else if (proceding)
                {
                    speak(PROCEDING);
                    reset();
                }
                else if (pickit)
                {
                    speak(PICKITUP);
                    reset();
                }
                else if (released)
                {
                    speak(RELEASED);
                    reset();
                }
                else if (notpick)
                {
                    speak(NOTPICKIT);
                    reset();
                }
            }
        }
    }

    private void speak(String text){
        speaker.playText(text);
    }

    public synchronized void getControl()
    {
        while (!mutex){
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        mutex=false;
    }

    public synchronized void controlDone(){
        mutex=true;
        notifyAll();
    }

    private void setup()
    {
        speaker.setPCMVolume(256);
        speaker.setSpeed(100);
        speaker.setAmplitude(Speaker.AMPLITUDE);
        speaker.setPitch(Speaker.PITCH);
        reset();
    }*/

    private void reset()
    {
        starting = false;
        setting = false;
        setupcompleted = false;
        object = false;
        wall = false;
        investigating = false;
        proceding = false;
        pickit = false;
        released = false;
        notpick = false;
        speak = false;
    }

    public void terminate()
    {
        go=false;
    }

    public void starting()
    {
        this.starting = true;
    }

    public void setting()
    {
        this.setting = true;
    }

    public void setupcompleted()
    {
        this.setupcompleted = true;
    }

    public void object()
    {
        this.object = true;
    }

    public void wall()
    {
        this.wall = true;
    }

    public void investigating()
    {
        this.investigating = true;
    }

    public void proceding()
    {
        this.proceding = true;
    }

    public void pickit()
    {
        this.pickit = true;
    }

    public void released()
    {
        this.released = true;
    }

    public void notpick()
    {
        this.notpick = true;
    }
}
