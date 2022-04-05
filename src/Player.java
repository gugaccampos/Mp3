import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

public class Player {
    ArrayList<Song> musicas = new ArrayList<Song>();
    int tam_musicas;

    private int numero_de_musica = 0;

    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice the audio samples are written to.
     */
    String title = "MP3 TOPADO";
    private AudioDevice device;

    private PlayerWindow window;

    private boolean repeat = false;
    private boolean shuffle = false;
    private boolean playerEnabled = false;
    private boolean playerPaused = true;
    private Song currentSong;
    private Song[] arr_musicas = new Song[0];
    private Song new_music;
    private int currentFrame = 0;
    private int newFrame;

    public Player() {
        this.musicas = new ArrayList<Song>();
        ActionListener buttonListenerPlayNow = e ->{
            //tocar(); função para tocar
        };

        ActionListener buttonListenerAddSong = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new_music = window.getNewSong();
                    addToQueue(new_music);
                    window.updateQueueList(getQueueAsArray());
                }
                catch (BitstreamException | InvalidDataException | UnsupportedTagException | IOException ex) {
                ex.printStackTrace();
            }

            }
        };

        ActionListener buttonListenerPlayPause= e ->{
            //pause(); função para pausar
        };

        ActionListener buttonListenerStop = e ->{

        };

        ActionListener buttonListenerRemove = e ->{
            String id = window.getSelectedSong();
            musicas.remove(Integer.parseInt(id));
            window.updateQueueList(getQueueAsArray());
        };

        ActionListener buttonListenerShuffle = e ->{

        };

        ActionListener buttonListenerPrevious = e ->{

        };

        ActionListener buttonListenerNext = e ->{

        };

        ActionListener buttonListenerRepeat = e ->{

        };
        ActionListener scrubberListenerClick = e ->{

        };
        ActionListener scrubberListenerMotion = e ->{

        };


        this.window = new PlayerWindow(title, this.musicas.toArray(new String[0][0]), buttonListenerPlayNow,
                buttonListenerRemove, buttonListenerAddSong, buttonListenerShuffle, buttonListenerPrevious,
                buttonListenerPlayPause, buttonListenerStop, buttonListenerNext, buttonListenerRepeat);
    }

    //<editor-fold desc="Essential">

    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        // TODO Is this thread safe?
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        // TODO Is this thread safe?
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     * @throws BitstreamException
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        // TODO Is this thread safe?
        if (newFrame > currentFrame) {
            int framesToSkip = newFrame - currentFrame;
            boolean condition = true;
            while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
        }
    }
    //</editor-fold>




    //<editor-fold desc="Queue Utilities">
    public void addToQueue(Song song) {
        Song[] queue = new Song[musicas.size()+1];
        for (int i=0;i<musicas.size();i++){
            queue[i]=musicas.get(i);
        }
        queue[musicas.size()]=song;
        for (int i = 0; i< queue.length; i++){
            if (queue[i]!=null){
                musicas.add(queue[i]);
            }
        }


    }

    public void removeFromQueue(String filePath) {
    }

    public String[][] getQueueAsArray() {
        String[][] array = new String[musicas.size()][7];
        for (int i = 0; i< musicas.size(); i++){
            if (musicas.get(i)!=null){
                array[i] = musicas.get(i).getDisplayInfo();}
        }
        return array;
    }

    //</editor-fold>

    //<editor-fold desc="Controls">
    public void start(String filePath) {
    }

    public void stop() {
    }

    public void pause() {
    }

    public void resume() {
    }

    public void next() {
    }

    public void previous() {
    }
    //</editor-fold>

    //<editor-fold desc="Getters and Setters">

    //</editor-fold>
}