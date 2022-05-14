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
    private boolean terminou = true;

    public Player() {
        this.musicas_shuffle = new ArrayList<Song>();
        this.musicas = new ArrayList<Song>();
        ActionListener buttonListenerPlayNow = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //se alguma música tiver tocando, a gente para ela
                if (getPlay()){
                    setStop();
                }
                //inicia a musica selecionada
                start(window.getSelectedSong());
            }
        };

        ActionListener buttonListenerAddSong = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //adiciona a música na array list e no player
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
                //checa se ta pausado
                if (!getPause()) {
                    //muda o booleano de pausado para não pausado e o ícone
                    setPause();
                    window.updatePlayPauseButtonIcon(false);
                    //tira do pause
                    lockReproducao.lock();
                    try {
                        paused.signalAll();
                    } finally {
                        lockReproducao.unlock();
                    }
                } else {
                    //muda o booleano para pausar na thread q ta tocando
                    setPause();
                    window.updatePlayPauseButtonIcon(true);
                }
            }
        };

        ActionListener buttonListenerStop = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //muda o booleano de stop
                setStop();
            }
        };

        ActionListener buttonListenerRemove = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = window.getSelectedSong();
                //checa se a música que vai ser removida é a que ta tocando
                if (currentSong.getFilePath().equals(id) && thread!=null){
                    setStop();
                }
                //remove a musica do array e do player
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
                //muda o booleando de random
                setRandom();
            }
        };

        ActionListener buttonListenerPrevious = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //checa se ta no shuffle
                if (getRandom()){
                    //checa se ta misturado ou não ja
                    if (!mexido){
                        random();
                    } else {
                        for (int i = 0; i < musicas_shuffle.size(); i++) {
                            if (musicas_shuffle.get(i).getFilePath() == currentSong_shuffle.getFilePath()) {
                                //checa se a música atual é a primeira da playlist
                                if (i == 0){
                                    setStop();
                                    song = musicas_shuffle.get(musicas_shuffle.size()-1).getFilePath();
                                    //manda começar a última musica
                                    start_shuffle(song);
                                    break;
                                } else {
                                    setStop();
                                    //manda começar a música anterior
                                    start_shuffle(song);
                                    break;
                                }
                            } else {
                                // define a música anterior a cada loop do for
                                song = musicas_shuffle.get(i).getFilePath();
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < musicas.size(); i++) {
                        if (musicas.get(i).getFilePath() == currentSong.getFilePath()) {
                            //checa se a música atual é a primeira da playlist
                            if (i == 0){
                                setStop();
                                song = musicas.get(musicas.size()-1).getFilePath();
                                //manda começar a última musica
                                start(song);
                                break;
                            } else {
                                setStop();
                                //manda começar a música anterior
                                start(song);
                                break;
                            }
                        } else {
                            // define a música anterior a cada loop do for
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
                //checa se ta no shuffle
                if (getRandom()){
                    //checa se ta misturado ou não ja
                    if (!mexido){
                        random();
                    } else {
                        for (int i = 0; i < musicas_shuffle.size(); i++) {
                            //checa a cada loop do for se a música da lista é a atual, se for, muda o contador.
                            if (musicas_shuffle.get(i).getFilePath() == currentSong_shuffle.getFilePath()) {
                                counter = 1;
                            }
                            else if (counter == 1){
                                song = musicas_shuffle.get(i).getFilePath();
                                setStop();
                                //manda começar a próxima música
                                start_shuffle(song);
                                break;
                            }
                            //checa se a música atual é a última da playlist
                            if (i+1 == musicas_shuffle.size()) {
                                song = musicas_shuffle.get(0).getFilePath();
                                setStop();
                                //manda começar a primeira música
                                start_shuffle(song);
                                break;
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < musicas.size(); i++) {
                        //checa a cada loop do for se a música da lista é a atual, se for, muda o contador.
                        if (musicas.get(i).getFilePath() == currentSong.getFilePath()) {
                            counter = 1;
                        }
                        else if (counter == 1){
                            song = musicas.get(i).getFilePath();
                            setStop();
                            //manda começar a próxima música
                            start(song);
                            break;
                        }
                        //checa se a música atual é a última da playlist
                        if (i+1 == musicas.size()) {
                            song = musicas.get(0).getFilePath();
                            setStop();
                            //manda começar a primeira música
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
                //muda o booleando de repeat
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

                //pega o frame do toque
                int Frame = (int) (window.getScrubberValue() / currentSong.getMsPerFrame());
                System.out.println(window.getScrubberValue());
                //checa se ele é antes ou depois do frame atual da música, se for depois, entra aqui
                if (Frame > currentFrame) {
                    //checa se tá pausado
                    if (getPause()){
                        //manda mudar o boolenao de pausa
                        setPause();
                        lock.lock();
                        try {
                            //manda ir até o frame
                            skipToFrame(Frame);
                        } catch (BitstreamException ex) {
                            ex.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                        //despausa
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
                            //manda mudar o frame
                            skipToFrame(Frame);
                        } catch (BitstreamException ex) {
                            ex.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    }
                } else {
                    //checa o pause
                    if (getPause()){
                        //pause
                        setPause();
                        lock.lock();
                        //manda recomeçar a música
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
                            //vai até o frame certo
                            skipToFrame(Frame);
                        } catch (BitstreamException ex) {
                            ex.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                        //despausa
                        setPause();
                        lockReproducao.lock();
                        try {
                            paused.signalAll();
                        } finally {
                            lockReproducao.unlock();
                        }
                    } else {
                        lock.lock();
                        //manda recomeçar a música
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
                            //vai até o frame certo
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
                //checa se já tem a música na playlist
                if (caminho.equals(song.getFilePath())){
                    repetido = true;
                }
            }
            if (!repetido){
                //adiciona se não for repetida
                musicas.add(song);
                musicas_shuffle.add(song);
            } else {
                repetido = false;
                //avisa que já foi adicionada
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

    //aqui esta as funções que mandam o booleano quando são chamadas, ou mudam ele.

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

    //termina aqui elas

    //</editor-fold>

    //<editor-fold desc="Controls">
    public void start(String filePath) {
        //checa se ta no shuffle
        if (getPlayRandom()){
            setPlayRandom();
        }

        for (int i = 0; i < musicas.size(); i++) {
            if (filePath.equals(musicas.get(i).getFilePath())) {
                currentSong = musicas.get(i);
            }
        }
        //manda iniciar uma thread pra tocar a musica
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
        //manda iniciar uma thread pra tocar a musica
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

    //aqui temos a função que manda tocar a próxima música
    public void resume() {
        int x = 0;
        if (getRandom()){
            for (int i = 0; i < musicas_shuffle.size(); i++){
                if (musicas_shuffle.get(i) == currentSong_shuffle){
                    x = i;
                }
            }
            //checa se é a última música da playlist, se for, checa se o repeat está ligado ou não para reiniciála
            if (((x+1) == musicas_shuffle.size()) && (getRepeat())){
                currentSong_shuffle = musicas_shuffle.get(0);
                terminou = false;
            } else if ((x+1) != musicas.size()) {
                currentSong_shuffle = musicas_shuffle.get(x+1);
                terminou = false;
            }
            //checa se é para mandar começar a próxima música ou não
            if (!terminou){
                terminou = true;
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
        } else {
            //igual ao passado
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
                terminou = false;
            } else if ((x+1) != musicas.size()){
                currentSong = musicas.get(x+1);
                terminou = false;
            }
            if (!terminou){
                terminou = true;
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
    }

    public void random() {
        //verifica se tem alguma música tocando
        if (getPlay()){
            setStop();
        }
        mexido = true;
        if (!getPlayRandom()){
            setPlayRandom();
        }
        //mistura as músicas
        Collections.shuffle(musicas_shuffle);
        for (int i = 0; i < musicas_shuffle.size(); i++){
            //coloca a música atual como a última da fila
            if (musicas_shuffle.get(i) == currentSong){
                Song provisorio = musicas_shuffle.get(i);
                musicas_shuffle.add(provisorio);
                musicas_shuffle.remove(i);
                break;
            }
        }
        song = musicas_shuffle.get(0).getFilePath();
        //manda iniciar a música
        start_shuffle(song);

    }

    class Play implements Runnable {

        @Override
        public void run() {
            window.updatePlayPauseButtonIcon(false);
            currentFrame = 0;
            setPlay();
            boolean x = true;
            //checa se ativou a flag que a música acabou ou se ainda é pra tocar
            while (x && playerEnabled) {
                //permite que os botões funcionem
                window.setEnabledScrubber(true);
                window.setEnabledScrubberArea(true);
                window.setEnabledPlayPauseButton(true);
                window.setEnabledStopButton(true);
                window.setEnabledPreviousButton(true);
                window.setEnabledNextButton(true);
                window.setEnabledRepeatButton(true);
                window.setEnabledShuffleButton(true);
                //checa se pediu pra pausar
                if (!getPause()){
                    lockReproducao.lock();
                    try {
                        //manda pausar
                        paused.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        lockReproducao.unlock();
                    }

                }
                //checa se pedira pra dar stop, caso tenham, para a reprodução geral
                if (getStop()){
                    setStop();
                    apertado = true;
                    break;
                }
                try {
                    if (getPause()){
                        //pede pra tocar o próximo frame e recebe um booleano avisando se existe um proximo frame ou não
                        x = playNextFrame();
                    }
                    //atualiza o scrubber
                    window.setTime((int) (currentFrame * currentSong.getMsPerFrame()), (int) currentSong.getMsLength());
                } catch (JavaLayerException ignore) {
                }
            }
            //reseta o player
            window.resetMiniPlayer();
            currentFrame = 0;
            setPlay();
            if (apertado){
                apertado = false;
            }else {
                //checa se eh pra dar um shuffle, se for, ativa a função
                if ((getRandom()) && (!mexido)){
                    random();
                } else {
                    //manda ativar a função de seguir
                    resume();
                }
            }
        }
    }

    class Play_shuffle implements Runnable {

        //player igual ao passado só que do shuffle
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