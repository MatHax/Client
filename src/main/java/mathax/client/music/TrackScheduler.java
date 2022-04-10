package mathax.client.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import mathax.client.gui.tabs.builtin.MusicTab;
import mathax.client.utils.misc.ChatUtils;

import java.util.ArrayList;
import java.util.List;

import static mathax.client.MatHax.mc;

public class TrackScheduler extends AudioEventAdapter {
    public List<AudioTrack> tracks = new ArrayList<>();

    public void queue(AudioTrack track) {
        if (track == null) return;
        if (!Music.player.startTrack(track, true)) {
            if (tracks.contains(track) || Music.player.getPlayingTrack() == track) track = track.makeClone();
            tracks.add(track);
        }

        refreshUI();
    }

    public void setPaused(boolean paused) {
        Music.player.setPaused(paused);
        refreshUI();
    }

    public void setVolume(float volume) {
        Music.player.setVolume((int) volume * 100);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) playNext(player);
        else refreshUI();
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        ChatUtils.error("Music", "Playback could not be continued.");
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        ChatUtils.error("Music", "Track stuck for %sms, playing next.", thresholdMs);
        playNext(player);
    }

    public void playNext(AudioPlayer player) {
        if (hasNext()) {
            player.playTrack(tracks.get(0));
            tracks.remove(0);
        } else player.stopTrack();

        refreshUI();
    }

    public boolean hasNext() {
        return !tracks.isEmpty();
    }

    public void refreshUI() {
        if (mc.currentScreen instanceof MusicTab.MusicScreen) ((MusicTab.MusicScreen) mc.currentScreen).construct();
    }
}
