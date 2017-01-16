package com.me4502.me4bot.discord.module.audio;

import com.google.common.collect.Queues;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AudioQueue extends AudioEventAdapter {

    private Queue<AudioTrack> tracks = Queues.newArrayDeque();
    private AudioPlayer player;
    private TextChannel textChannel;

    public AudioQueue(AudioPlayer player) {
        this.player = player;
        this.player.addListener(this);
    }

    public void queue(AudioTrack track) {
        tracks.offer(track);

        if (this.player.getPlayingTrack() == null) {
            playNext();
        }
    }

    public void queueNext(AudioTrack track) {
        List<AudioTrack> trackTemp = new ArrayList<>(tracks);
        trackTemp.add(0, track);
        tracks.clear();
        for (AudioTrack oldTrack : trackTemp) {
            tracks.offer(oldTrack);
        }

        if (this.player.getPlayingTrack() == null) {
            playNext();
        }
    }

    public AudioTrack remove(int index) {
        List<AudioTrack> trackTemp = new ArrayList<>(tracks);
        AudioTrack removed = trackTemp.remove(index);
        tracks.clear();
        for (AudioTrack track : trackTemp) {
            tracks.offer(track);
        }
        return removed;
    }

    public void setTextChannel(TextChannel textChannel) {
        this.textChannel = textChannel;
    }

    public TextChannel getTextChannel() {
        return textChannel;
    }

    public void playNext() {
        if (!tracks.isEmpty()) {
            AudioTrack track = tracks.poll();
            this.player.playTrack(track);
            textChannel.sendMessage("Playing track: " + prettify(track)).queue();
        } else if (this.player.getPlayingTrack() != null) {
            this.player.stopTrack();
        }
    }

    public void clearQueue() {
        tracks.clear();
        playNext();
        textChannel.sendMessage("Queue cleared!").queue();
    }

    public List<String> getPrettyQueue() {
        return tracks.stream().map(AudioQueue::prettify).collect(Collectors.toList());
    }

    public static String prettify(AudioTrack track) {
        String pretty = track.getInfo().title + " by " + track.getInfo().author;
        if (track.getDuration() < Integer.MAX_VALUE) {
            pretty += " (" + prettifyTime(track.getDuration()) + ')';
        }
        return pretty;
    }

    private static String prettifyTime(long time) {
        time /= 1000;

        long seconds = time % 60;
        long minutes = time / 60;
        long hours = minutes / 60;
        minutes %= 60;

        String format = seconds + "s";
        if (minutes > 0 || hours > 0) {
            format = minutes + "m" + format;
            if (hours > 0) {
                format = hours + "h" + format;
            }
        }

        return format;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        super.onTrackEnd(player, track, endReason);
        if (endReason == AudioTrackEndReason.FINISHED) {
            playNext();
        } else if (endReason == AudioTrackEndReason.LOAD_FAILED) {
            textChannel.sendMessage("Encountered an error playing " + prettify(track) + '.');
            playNext();
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        super.onTrackException(player, track, exception);

        textChannel.sendMessage("Encountered an error playing " + prettify(track) + ". " + exception.getMessage());
        playNext();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        super.onTrackStuck(player, track, thresholdMs);

        textChannel.sendMessage("Got stuck whilst playing " + prettify(track) + ". Skipping.");
        playNext();
    }

    public void shuffle() {
        List<AudioTrack> trackTemp = new ArrayList<>(tracks);
        Collections.shuffle(trackTemp, ThreadLocalRandom.current());
        tracks.clear();
        for (AudioTrack track : trackTemp) {
            tracks.offer(track);
        }
    }

    public int size() {
        return tracks.size();
    }
}