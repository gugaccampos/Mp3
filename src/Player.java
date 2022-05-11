import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.UnsupportedTagException;
import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import java.util.Collection;
import java.util.Collections;
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
    ArrayList<Song> musicas_shuffle = new ArrayList<Song>();


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



    private Song currentSong;
    private Song currentSong_shuffle;
    private Song new_music;
    private String song;
    private int currentFrame = 0;
    private int newFrame;
    private Thread thread;
    private boolean tocando = false;
    private Lock lock = new ReentrantLock();
    private Lock lockReproducao = new ReentrantLock();
    private Condition paused = lockReproducao.newCondition();
    private boolean playerEnabled = true;
    private boolean playerPaused = true;
    private boolean stop = false;
    private boolean repetido = false;
    private boolean aleatorio = false;
    private boolean apertado = false;
    private boolean repeat = false;
    private boolean mexido = false;
    private boolean playing_random = false;

    public Player() {
        this.musicas_shuffle = new ArrayList<Song>();
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
                        musicas_shuffle.remove(i);
                    }
                }
                window.updateQueueList(getQueueAsArray());
            }
        };

        ActionListener buttonListenerShuffle = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mexido = false;
                setRandom();
            }
        };

        ActionListener buttonListenerPrevious = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getRandom()){
                    if (!mexido){
                        random();
                    } else {
                    for (int i = 0; i < musicas_shuffle.size(); i++) {
                        if (musicas_shuffle.get(i).getFilePath() == currentSong_shuffle.getFilePath()) {
                            if (i == 0){
                                setStop();
                                song = musicas_shuffle.get(musicas_shuffle.size()-1).getFilePath();
                                start_shuffle(song);
                                break;
                            } else {
                                setStop();
                                start_shuffle(song);
                                break;
                            }
                        } else {
                            song = musicas_shuffle.get(i).getFilePath();
                        }
                    }
                    }
                } else {
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
            }
        };

        ActionListener buttonListenerNext = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int counter = 0;
                if (getRandom()){
                    if (!mexido){
                        random();
                    } else {
                    for (int i = 0; i < musicas_shuffle.size(); i++) {
                        if (musicas_shuffle.get(i).getFilePath() == currentSong_shuffle.getFilePath()) {
                            counter = 1;
                        }
                        else if (counter == 1){
                            song = musicas_shuffle.get(i).getFilePath();
                            setStop();
                            start_shuffle(song);
                            break;
                        }
                        if (i+1 == musicas_shuffle.size()) {
                            song = musicas_shuffle.get(0).getFilePath();
                            setStop();
                            start_shuffle(song);
                            break;
                        }
                    }
                    }
                } else {
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
            }
        };
        ActionListener buttonListenerRepeat = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setRepeat();
            }
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
                musicas_shuffle.add(song);
            } else {
                repetido = false;
                System.out.println("Esta música já foi adicionada");
            }

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

    private void setPlayRandom() {
        lock.lock();
        try {
            playing_random = !playing_random;
        } finally {
            lock.unlock();
        }
    }

    private boolean getPlayRandom() {
        lock.lock();
        try {
            return playing_random;
        } finally {
            lock.unlock();
        }
    }

    private void setRepeat() {
        lock.lock();
        try {
            repeat = !repeat;
        } finally {
            lock.unlock();
        }
    }

    private boolean getRepeat() {
        lock.lock();
        try {
            return repeat;
        } finally {
            lock.unlock();
        }
    }

    //</editor-fold>

    //<editor-fold desc="Controls">
    public void start(String filePath) {
        if (getPlayRandom()){
            setPlayRandom();
        }

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

    public void start_shuffle(String filePath) {

        for (int i = 0; i < musicas_shuffle.size(); i++) {
            if (filePath.equals(musicas_shuffle.get(i).getFilePath())) {
                currentSong_shuffle = musicas_shuffle.get(i);
            }
        }
        try {
            bitstream = new Bitstream(currentSong_shuffle.getBufferedInputStream());
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());
            thread = new Thread(new Play_shuffle());
            thread.start();

        } catch (FileNotFoundException | JavaLayerException e) {
            e.printStackTrace();
        }

    }

    public void resume() {
        int x = 0;
        if (getRandom()){
            for (int i = 0; i < musicas_shuffle.size(); i++){
                if (musicas_shuffle.get(i) == currentSong_shuffle){
                    x = i;
                }
            }
            if (((x+1) == musicas_shuffle.size()) && (getRepeat())){
                currentSong_shuffle = musicas_shuffle.get(0);
            } else {
                currentSong_shuffle = musicas_shuffle.get(x+1);
            }
            try {
                bitstream = new Bitstream(currentSong_shuffle.getBufferedInputStream());
                device = FactoryRegistry.systemRegistry().createAudioDevice();
                device.open(decoder = new Decoder());
                thread = new Thread(new Play_shuffle());
                thread.start();
            } catch (FileNotFoundException | JavaLayerException e) {
                e.printStackTrace();
            }
        } else {
            if (getPlayRandom()){
                setPlayRandom();
            }
            for (int i = 0; i < musicas.size(); i++){
                if (musicas.get(i) == currentSong){
                    x = i;
                }
            }
            if (((x+1) == musicas.size()) && (getRepeat())){
                currentSong = musicas.get(0);
            } else {
                currentSong = musicas.get(x+1);
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
    }

    public void random() {
        if (getPlay()){
            setStop();
        }
        mexido = true;
        if (!getPlayRandom()){
            setPlayRandom();
        }
        Collections.shuffle(musicas_shuffle);
        for (int i = 0; i < musicas_shuffle.size(); i++){
            if (musicas_shuffle.get(i) == currentSong){
                Song provisorio = musicas_shuffle.get(i);
                musicas_shuffle.add(provisorio);
                musicas_shuffle.remove(i);
                break;
            }
        }
        song = musicas_shuffle.get(0).getFilePath();
        start_shuffle(song);

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
                    apertado = true;
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
            if (apertado){
                apertado = false;
            }else {
                if ((getRandom()) && (!mexido)){
                    random();
                } else {
                    resume();
                }
            }
        }
    }

    class Play_shuffle implements Runnable {

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
                    apertado = true;
                    break;
                }
                try {
                    if (getPause()){
                        x = playNextFrame();
                    }
                    window.setTime((int) (currentFrame * currentSong_shuffle.getMsPerFrame()), (int) currentSong_shuffle.getMsLength());
                } catch (JavaLayerException ignore) {
                }
            }
            window.resetMiniPlayer();
            currentFrame = 0;
            setPlay();
            if (apertado){
                apertado = false;
            }else {
                resume();
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="Getters and Setters">

    //</editor-fold>
}