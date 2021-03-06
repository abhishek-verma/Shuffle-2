package com.inpen.shuffle.utility;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CustomTypes {


    /**
     * Created by Abhishek on 12/10/2016.
     */

    public enum ItemType {
        SONG, ALBUM_KEY, ARTIST_KEY, PLAYLIST, GENRE, FOLDER;

        public static int toInt(ItemType it) {
            switch (it) {
                case SONG:
                    return 0;
                case ALBUM_KEY:
                    return 1;
                case ARTIST_KEY:
                    return 2;
                case PLAYLIST:
                    return 3;
                case GENRE:
                    return 4;
                case FOLDER:
                    return 5;
                default:
                    return -1;
            }
        }

        public static ItemType fromInt(int i) {
            switch (i) {
                case 0:
                    return SONG;
                case 1:
                    return ALBUM_KEY;
                case 2:
                    return ARTIST_KEY;
                case 3:
                    return PLAYLIST;
                case 4:
                    return GENRE;
                case 5:
                    return FOLDER;
                default:
                    return null;
            }
        }
    }

    @IntDef({RepositoryState.NON_INITIALIZED, RepositoryState.INITIALIZING, RepositoryState.INITIALIZED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepositoryState {
        int NON_INITIALIZED = -1, INITIALIZING = 0, INITIALIZED = 1;
    }

    @IntDef({FabMode.SHUFFLE, FabMode.PLAYER, FabMode.PLAYER_WITH_ADD, FabMode.DISABLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FabMode {
        int SHUFFLE = 100,
                PLAYER = 110,
                PLAYER_WITH_ADD = 120,
                DISABLED = 130;
    }

    @IntDef({PlayingQueueEventType.CLICKED, PlayingQueueEventType.SWIPED, PlayingQueueEventType.MOVED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayingQueueEventType {
        int CLICKED = 100,
                SWIPED = 110,
                MOVED = 120;
    }

    @IntDef({PlayingQueueItemPlayingState.PLAYED,
            PlayingQueueItemPlayingState.PLAYING,
            PlayingQueueItemPlayingState.UNPLAYED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayingQueueItemPlayingState {
        int PLAYED = 1,
                PLAYING = 2,
                UNPLAYED = 3;
    }

}
