import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;
import java.util.Random;

import java.awt.event.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class Player {
    ArrayList<Song> musicas = new ArrayList<Song>();

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
    private boolean playerEnabled = true;
    private boolean playerPaused = true;
    private Song currentSong;
    private Song new_music;
    private String song;
    private int currentFrame = 0;
    private int newFrame;
    private Thread thread;
    private boolean tocando = false;
    private Lock lock = new ReentrantLock();
    private Lock lockReproducao = new ReentrantLock();
    private Condition paused = lockReproducao.newCondition();
    private boolean stop = false;
    private boolean repetido = false;
    private boolean aleatorio = false;

    public Player() {
        this.musicas = new ArrayList<Song>();
        ActionListener buttonListenerPlayNow = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getPlay()){
                    setStop();
                }
                start(window.getSelectedSong());
            }
        };

        ActionListener buttonListenerAddSong = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new_music = window.getNewSong();
                    addToQueue(new_music);
                    window.updateQueueList(getQueueAsArray());
                } catch (BitstreamException | InvalidDataException | UnsupportedTagException | IOException ex) {
                    ex.printStackTrace();
                }

            }
        };

        ActionListener buttonListenerPlayPause = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!getPause()) {
                    setPause();
                    window.updatePlayPauseButtonIcon(false);
                    lockReproducao.lock();
                    try {
                        paused.signalAll();
                    } finally {
                        lockReproducao.unlock();
                    }
                } else {
                    setPause();
                    window.updatePlayPauseButtonIcon(true);
                }
            }
        };

        ActionListener buttonListenerStop = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setStop();
            }
        };

        ActionListener buttonListenerRemove = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = window.getSelectedSong();
                if (currentSong.getFilePath().equals(id) && thread!=null){
                    setStop();
                }
                for (int i = 0; i < musicas.size(); i++) {
                    if (musicas.get(i).getFilePath() == id) {
                        musicas.remove(i);
                    }
                }
                window.updateQueueList(getQueueAsArray());
            }
        };

        ActionListener buttonListenerShuffle = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRandom();

            }
        };

        ActionListener buttonListenerPrevious = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < musicas.size(); i++) {
                    if (musicas.get(i).getFilePath() == currentSong.getFilePath()) {
                        if (i == 0){
                            setStop();
                            song = musicas.get(musicas.size()-1).getFilePath();
                            start(song);
                            break;
                        } else {
                            setStop();
                            start(song);
                            break;
                        }
                    } else {
                        song = musicas.get(i).getFilePath();
                    }
                }
            }
        };

        ActionListener buttonListenerNext = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int counter = 0;
                for (int i = 0; i < musicas.size(); i++) {
                    if (musicas.get(i).getFilePath() == currentSong.getFilePath()) {
                        counter = 1;
                    }
                    else if (counter == 1){
                        song = musicas.get(i).getFilePath();
                        setStop();
                        start(song);
                        break;
                    }
                    if (i+1 == musicas.size()) {
                        song = musicas.get(0).getFilePath();
                        setStop();
                        start(song);
                        break;
                    }
                }
            }
        };
        ActionListener buttonListenerRepeat = e -> {

        };
        MouseListener scrubberListenerClick = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {
                //Funciona constantemente se a música tiver parada mas se estiver rodando pega de maneira inconsistente.

                int Frame = (int) (window.getScrubberValue() / currentSong.getMsPerFrame());
                System.out.println(window.getScrubberValue());
                if (Frame > currentFrame) {
                    if (getPause()){
                        setPause();
                        lock.lock();
                        try {
                            skipToFrame(Frame);
                        } catch (BitstreamException ex) {
                            ex.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                        setPause();
                        lockReproducao.lock();
                        try {
                            paused.signalAll();
                        } finally {
                            lockReproducao.unlock();
                        }
                    } else {
                        lock.lock();
                        try {
                            skipToFrame(Frame);
                        } catch (BitstreamException ex) {
                            ex.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    }
                } else {
                    if (getPause()){
                        setPause();
                        lock.lock();
                        try {
                            bitstream = new Bitstream(currentSong.getBufferedInputStream());
                            device = FactoryRegistry.systemRegistry().createAudioDevice();
                            device.open(decoder = new Decoder());
                            currentFrame = 0;
                        } catch (FileNotFoundException | JavaLayerException j) {
                            j.printStackTrace();
                        }
                        finally {
                            lock.unlock();
                        }
                        lock.lock();
                        try {
                            skipToFrame(Frame);
                        } catch (BitstreamException ex) {
                            ex.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                        setPause();
                        lockReproducao.lock();
                        try {
                            paused.signalAll();
                        } finally {
                            lockReproducao.unlock();
                        }
                    } else {
                        lock.lock();
                        try {
                            bitstream = new Bitstream(currentSong.getBufferedInputStream());
                            device = FactoryRegistry.systemRegistry().createAudioDevice();
                            device.open(decoder = new Decoder());
                            currentFrame = 0;
                        } catch (FileNotFoundException | JavaLayerException j) {
                            j.printStackTrace();
                        }
                        finally {
                            lock.unlock();
                        }
                        lock.lock();
                        try {
                            skipToFrame(Frame);
                        } catch (BitstreamException ex) {
                            ex.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    }

                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        };

        MouseMotionListener scrubberListenerMotion = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {

            }

            @Override
            public void mouseMoved(MouseEvent e) {

            }
        };


        this.window = new PlayerWindow(title, this.musicas.toArray(new String[0][0]), buttonListenerPlayNow,
                buttonListenerRemove, buttonListenerAddSong, buttonListenerShuffle, buttonListenerPrevious,
                buttonListenerPlayPause, buttonListenerStop, buttonListenerNext, buttonListenerRepeat ,scrubberListenerClick
                ,scrubberListenerMotion);
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
            lock.lock();
            try {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
                device.write(output.getBuffer(), 0, output.getBufferLength());
                bitstream.closeFrame();
                currentFrame ++;
            } finally {
                lock.unlock();
            }
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
        lock.lock();
        try {
            for (int i=0;i<musicas.size();i++) {
                String caminho = musicas.get(i).getFilePath();
                if (caminho.equals(song.getFilePath())){
                    repetido = true;
                }
            }
            if (!repetido){
                musicas.add(song);
            } else {
                repetido = false;
                System.out.println("Esta música já foi adicionada");
            }

        } finally {
            lock.unlock();
        }
    }

    private void setPlay() {
        lock.lock();
        try {
            tocando = !tocando;
        } finally {
            lock.unlock();
        }
    }

    private boolean getPlay() {
        lock.lock();
        try {
            return tocando;
        } finally {
            lock.unlock();
        }
    }

    private void setPause() {
        lock.lock();
        try {
            playerPaused = !playerPaused;
        } finally {
            lock.unlock();
        }
    }

    private boolean getPause() {
        lock.lock();
        try {
            return playerPaused;
        } finally {
            lock.unlock();
        }
    }

    private void setStop() {
        lock.lock();
        try {
            stop = !stop;
        } finally {
            lock.unlock();
        }
    }

    private boolean getStop() {
        lock.lock();
        try {
            return stop;
        } finally {
            lock.unlock();
        }
    }

    private void setRandom() {
        lock.lock();
        try {
            aleatorio = !aleatorio;
        } finally {
            lock.unlock();
        }
    }

    private boolean getRandom() {
        lock.lock();
        try {
            return aleatorio;
        } finally {
            lock.unlock();
        }
    }

    public void removeFromQueue(String filePath) {
    }

    public String[][] getQueueAsArray() {
        String[][] array = new String[musicas.size()][7];
        for (int i = 0; i < musicas.size(); i++) {
            if (musicas.get(i) != null) {
                array[i] = musicas.get(i).getDisplayInfo();
            }
        }
        return array;
    }

    //</editor-fold>

    //<editor-fold desc="Controls">
    public void start(String filePath) {

        for (int i = 0; i < musicas.size(); i++) {
            if (filePath.equals(musicas.get(i).getFilePath())) {
                currentSong = musicas.get(i);
            }
        }
        try {
            bitstream = new Bitstream(currentSong.getBufferedInputStream());
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());
            thread = new Thread(new Play());
            thread.start();

        } catch (FileNotFoundException | JavaLayerException e) {
            e.printStackTrace();
        }

    }

    public void stop() {
    }

    public void random() {
        Random x = new Random();
        int t;
        t = (x.nextInt() % musicas.size());
        if (t < 0){
            t = t + musicas.size();
        }
        if (currentSong == musicas.get(t)){
            if (t == (musicas.size()-1)){
                currentSong = musicas.get(t-1);
            } else {
                currentSong = musicas.get(t+1);
            }
        } else {
            currentSong = musicas.get(t);
        }

        try {
                bitstream = new Bitstream(currentSong.getBufferedInputStream());
                device = FactoryRegistry.systemRegistry().createAudioDevice();
                device.open(decoder = new Decoder());
                thread = new Thread(new Play());
                thread.start();

        } catch (FileNotFoundException | JavaLayerException e) {
            e.printStackTrace();
        }

    }

    public void resume() {
    }

    public void next() {
    }

    public void previous() {
    }
    class Play implements Runnable {

        @Override
        public void run() {
            window.updatePlayPauseButtonIcon(false);
            currentFrame = 0;
            setPlay();
            boolean x = true;
            while (x && playerEnabled) {
                window.setEnabledScrubber(true);
                window.setEnabledScrubberArea(true);
                window.setEnabledPlayPauseButton(true);
                window.setEnabledStopButton(true);
                window.setEnabledPreviousButton(true);
                window.setEnabledNextButton(true);
                window.setEnabledRepeatButton(true);
                window.setEnabledShuffleButton(true);
                if (!getPause()){
                    lockReproducao.lock();
                    try {
                        paused.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lockReproducao.unlock();
                    }

                }
                if (getStop()){
                    setStop();
                    break;
                }
                try {
                    if (getPause()){
                        x = playNextFrame();
                    }
                    window.setTime((int) (currentFrame * currentSong.getMsPerFrame()), (int) currentSong.getMsLength());
                } catch (JavaLayerException ignore) {
                }
            }
            window.resetMiniPlayer();
            currentFrame = 0;
            setPlay();
            if (getRandom()){
                random();
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="Getters and Setters">

    //</editor-fold>
}