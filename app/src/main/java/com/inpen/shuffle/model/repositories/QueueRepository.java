package com.inpen.shuffle.model.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;

import com.inpen.shuffle.model.MutableMediaMetadata;
import com.inpen.shuffle.model.QueueProvider;
import com.inpen.shuffle.utility.CustomTypes;
import com.inpen.shuffle.utility.LogHelper;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import static com.inpen.shuffle.utility.CustomTypes.RepositoryState.INITIALIZED;
import static com.inpen.shuffle.utility.CustomTypes.RepositoryState.NON_INITIALIZED;

/**
 * Created by Abhishek on 12/10/2016.
 */

public class QueueRepository {

    ///////////////////////////////////////////////////////////////////////////
    // Static fields
    ///////////////////////////////////////////////////////////////////////////
    private static final String LOG_TAG = LogHelper.makeLogTag(QueueRepository.class);
    private static final String AUDIO_STORAGE = "com.inpen.shuffle.AUDIO_STORAGE";
    private static final String KEY_PLAYING_QUEUE = "playing_queue";
    private static final String KEY_CURRENT_TRACK_INDEX = "current_track_index";
    public static QueueRepository mQueueRepositoryInstance;

    ///////////////////////////////////////////////////////////////////////////
    // Regular fields and methods
    ///////////////////////////////////////////////////////////////////////////
    // cached playing queue
    private List<MutableMediaMetadata> mPlayingQueue;
    private int mCurrentTrackIndex = -1;
    private SharedPreferences mPreferences;
    private volatile
    @CustomTypes.RepositoryState
    int mCurrentState = CustomTypes.RepositoryState.NON_INITIALIZED;

    private QueueRepository() {
    }

    public static synchronized QueueRepository getInstance() {
        if (mQueueRepositoryInstance == null)
            mQueueRepositoryInstance = new QueueRepository();

        return mQueueRepositoryInstance;
    }

    public void initializeShuffleAll(final Context context, @Nullable final RepositoryInitializedCallback repositoryInitializedCallback) {

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... voids) {

                try {

                    mCurrentState = CustomTypes.RepositoryState.INITIALIZING;

                    QueueProvider queueProvider = new QueueProvider(context);

                    mPlayingQueue = queueProvider
                            .generateShuffledQueueMetadata(null);


                    queueProvider.shuffle(mPlayingQueue);

                    mCurrentTrackIndex = -1;

                    if (mPlayingQueue != null)
                        mCurrentState = INITIALIZED;

//                    // store playlist asynchronously
//                    new Runnable() {
//                        @Override
//                        public void run() {
//                            storeQueue(context);
//                        }
//                    }.run();

                } catch (Exception e) {

                    if (mCurrentState != INITIALIZED) {
                        // Something bad happened, so we reset state to NON_INITIALIZED to allow
                        // retries (eg if the network connection is temporary unavailable)
                        mCurrentState = CustomTypes.RepositoryState.NON_INITIALIZED;
                    }
                }

                return mCurrentState;
            }

            @Override
            protected void onPostExecute(Integer repoState) {
                super.onPostExecute(repoState);

                if (repositoryInitializedCallback != null) {
                    repositoryInitializedCallback.onRepositoryInitialized(isInitialized() && mPlayingQueue.size() > 0);
                }
            }
        }.execute();
    }

    public synchronized void initialize(@NonNull final Context context,
                                        @Nullable final SelectedItemsRepository selectedItemsRepository,
                                        @Nullable final RepositoryInitializedCallback repositoryInitializedCallback) {


        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {

                try {

                    if (selectedItemsRepository != null) {
                        mCurrentState = CustomTypes.RepositoryState.INITIALIZING;
                        mPlayingQueue = retrieveQueue(context, selectedItemsRepository);

                        if (mPlayingQueue != null)
                            mCurrentState = INITIALIZED;

                        // store playlist asynchronously
                        new Runnable() {
                            @Override
                            public void run() {
                                storeQueue(context);
                            }
                        }.run();
                    } else if (mCurrentState == NON_INITIALIZED && !isCatchEmpty(context)) {
                        loadCachedQueue(context, repositoryInitializedCallback);
                    }

                } finally {
                    if (mCurrentState != INITIALIZED) {
                        // Something bad happened, so we reset state to NON_INITIALIZED to allow
                        // retries (eg if the network connection is temporary unavailable)
                        mCurrentState = CustomTypes.RepositoryState.NON_INITIALIZED;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (repositoryInitializedCallback != null) {
                    repositoryInitializedCallback.onRepositoryInitialized(isInitialized() && mPlayingQueue.size() > 0);
                }
            }
        }.execute();

    }

    public synchronized void addItemsToQueue(@NonNull final Context context,
                                             @Nullable final SelectedItemsRepository selectedItemsRepository,
                                             @Nullable final RepositoryInitializedCallback repositoryInitializedCallback) {

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {

                try {

                    if (mCurrentState == NON_INITIALIZED && !isCatchEmpty(context)) {
                        loadCachedQueue(context, repositoryInitializedCallback);
                    }

                    if (selectedItemsRepository != null) {
                        mCurrentState = CustomTypes.RepositoryState.INITIALIZING;

                        addSongs(context, selectedItemsRepository);

                        mCurrentState = INITIALIZED;

                        // store playlist asynchronously
                        new Runnable() {
                            @Override
                            public void run() {
                                storeQueue(context);
                            }
                        }.run();
                    }

                } finally {
                    if (mCurrentState != INITIALIZED) {
                        // Something bad happened, so we reset state to NON_INITIALIZED to allow
                        // retries (eg if the network connection is temporary unavailable)
                        mCurrentState = CustomTypes.RepositoryState.NON_INITIALIZED;
                    }
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                if (repositoryInitializedCallback != null) {
                    repositoryInitializedCallback.onRepositoryInitialized(isInitialized() && mPlayingQueue.size() > 0);
                }
            }
        }.execute();
    }

    public synchronized void addNextSongs(List<MutableMediaMetadata> metadataList) {
        if(mPlayingQueue!=null) {
            mPlayingQueue.addAll(mCurrentTrackIndex + 1, metadataList);
            EventBus.getDefault().post(new QueueContentsChangedEvent());
        }
    }

    public
    @Nullable
    MutableMediaMetadata getCurrentSong() {
        if (!isInitialized())
            return null;

        if (mCurrentTrackIndex < 0)
            setCurrentQueueIndex(0);

        return mPlayingQueue.get(mCurrentTrackIndex);
    }

    public MutableMediaMetadata getSongForIndex(int index) {
        return mPlayingQueue.get(index);
    }

    public int getSize() {
        return mPlayingQueue.size();
    }

    public void skipQueuePosition(int amt) {

        int index = mCurrentTrackIndex + amt;
        if (index < 0) {
            // skip backwards before the first song will keep you on the first song
            index = 0;
        } else {
            // skip forwards when in last song will cycle back to play of the queue
            index %= mPlayingQueue.size();
        }
        setCurrentQueueIndex(index);
    }

    public void setCurrentQueueIndex(int index) {
        mCurrentTrackIndex = index;
        EventBus.getDefault().post(new QueueIndexChangedEvent());
    }

    public int getCurrentIndex() {
        return mCurrentTrackIndex;
    }

    public void removeItemAtIndex(int position) {
        mPlayingQueue.remove(position);
        EventBus.getDefault().post(new QueueContentsChangedEvent());
    }

    public boolean isInitialized() {
        return mCurrentState == INITIALIZED;
    }

    public void clearQueue(Context context) {

        mCurrentState = CustomTypes.RepositoryState.NON_INITIALIZED;
        mPlayingQueue.clear();
        setCurrentQueueIndex(-1);

        SharedPreferences.Editor editor = getmPreferences(context).edit();
        editor.clear();
        editor.apply();
    }

    public boolean isCatchEmpty(Context context) {
        return getmPreferences(context).contains(KEY_PLAYING_QUEUE);
    }

    public void setRating(RatingCompat rating, Context context) {
        MediaMetadataCompat metadata = getCurrentSong().metadata;
        mPlayingQueue.get(mCurrentTrackIndex).metadata = new MediaMetadataCompat
                .Builder(metadata)
                .putRating(MediaMetadataCompat.METADATA_KEY_USER_RATING, rating)
                .build();

        EventBus
                .getDefault()
                .post(new QueueMetadataChangedEvent(getCurrentSong().trackId));

        SongsRepository songsRepository = new SongsRepository(context);
        //noinspection ResourceType
        songsRepository.storeSongRating(metadata.getString(MutableMediaMetadata.CUSTOM_METADATA_KEY_TRACK_ID),
                rating);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Private methods
    ///////////////////////////////////////////////////////////////////////////

    private List<MutableMediaMetadata> retrieveQueue(Context context, SelectedItemsRepository selectedItemsRepository) {

        QueueProvider queueProvider = new QueueProvider(context);

        List<MutableMediaMetadata> playingQueue = queueProvider
                .generateShuffledQueueMetadata(selectedItemsRepository);


        queueProvider.shuffle(playingQueue);

        mCurrentTrackIndex = -1;

        return playingQueue;
    }


    private void addSongs(Context context, SelectedItemsRepository selectedItemsRepository) {

        QueueProvider queueProvider = new QueueProvider(context);

        List<MutableMediaMetadata> newQueue = queueProvider
                .generateShuffledQueueMetadata(selectedItemsRepository);

        MutableMediaMetadata[] queueArray = newQueue.toArray(new MutableMediaMetadata[newQueue.size()]);

        for (MutableMediaMetadata queueItem : queueArray) {
            if (mPlayingQueue.contains(queueItem)) {
                newQueue.remove(queueItem);
            }
        }

        List<MutableMediaMetadata> playedAndPlayingSongs = new ArrayList<>(mPlayingQueue.subList(0, mCurrentTrackIndex + 1));
        List<MutableMediaMetadata> unplayedSongs = new ArrayList<>(mPlayingQueue.subList(mCurrentTrackIndex + 1, mPlayingQueue.size()));

        unplayedSongs.addAll(newQueue);

        queueProvider.shuffle(unplayedSongs);

        playedAndPlayingSongs.addAll(unplayedSongs);

        mPlayingQueue = playedAndPlayingSongs;
    }

    private void loadCachedQueue(Context context, final RepositoryInitializedCallback callback) {
        LogHelper.d(LOG_TAG, "loadCachedQueue called but unimplemented!");

//
//        Gson gson = new Gson();
//        String json = getmPreferences(context).getString(KEY_PLAYING_QUEUE, null);
//
//        Type type = new TypeToken<ArrayList<MutableMediaMetadata>>() {
//        }.getType();
//
//        mPlayingQueue = gson.fromJson(json, type);
//        mCurrentTrackIndex = getmPreferences(context).getInt(KEY_CURRENT_TRACK_INDEX, -1);
//
//        if (mPlayingQueue != null && mPlayingQueue.size() > 0) {
//            mCurrentState = CustomTypes.RepositoryState.INITIALIZED;
//        }
    }

    private void storeQueue(Context context) {
        LogHelper.d(LOG_TAG, "storeQueue called but unimplemented!");

//        SharedPreferences.Editor editor = getmPreferences(context).edit();
//        Gson gson = new Gson();
//        String json = gson.toJson(mPlayingQueue);
//        editor.putString(KEY_PLAYING_QUEUE, json);
//
//        editor.putInt(KEY_CURRENT_TRACK_INDEX, mCurrentTrackIndex);
//        editor.apply();
    }

    private SharedPreferences getmPreferences(Context context) {

        if (mPreferences == null)
            mPreferences = context.getSharedPreferences(AUDIO_STORAGE, Context.MODE_PRIVATE);

        return mPreferences;
    }

    public List<MutableMediaMetadata> getQueue() {
        return mPlayingQueue;
    }


    public interface RepositoryInitializedCallback {
        void onRepositoryInitialized(boolean success);
    }

    public class QueueMetadataChangedEvent {
        private final String mediaId;

        public QueueMetadataChangedEvent(String mediaId) {
            this.mediaId = mediaId;
        }
    }

    public class QueueContentsChangedEvent {
    }

    public class QueueIndexChangedEvent {
    }
}
