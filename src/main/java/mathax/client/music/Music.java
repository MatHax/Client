package mathax.client.music;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import mathax.client.utils.music.PlaylistUtils;
import mathax.client.utils.music.StreamPlayer;

public class Music {
    public static DefaultAudioPlayerManager playerManager;

    public static AudioPlayer player;

    public static TrackScheduler trackScheduler;

    public static StreamPlayer streamPlayer;

    public static Thread soundThread;

    public static void init() {
        playerManager = new DefaultAudioPlayerManager();
        playerManager.getConfiguration().setOutputFormat(StandardAudioDataFormats.COMMON_PCM_S16_BE);

        AudioSourceManagers.registerRemoteSources(playerManager);

        player = playerManager.createPlayer();
        trackScheduler = new TrackScheduler();
        player.addListener(trackScheduler);

        streamPlayer = new StreamPlayer();

        PlaylistUtils.load();

        soundThread = new Thread(streamPlayer, "Music");
        soundThread.start();
    }

    public static String getTime() {
        AudioTrack current = player.getPlayingTrack();

        if (current == null) return "";

        long durationTotal = current.getDuration() - current.getPosition();
        for (AudioTrack track : trackScheduler.tracks) {
            durationTotal += track.getDuration();
        }

        return String.format("%02d:%02d:%02d", (durationTotal / (1000 * 60 * 60)) % 24, (durationTotal / (1000 * 60)) % 60, (durationTotal / 1000) % 60);
    }
}
