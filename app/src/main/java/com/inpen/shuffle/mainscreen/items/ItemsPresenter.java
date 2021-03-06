package com.inpen.shuffle.mainscreen.items;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;

import com.abhi.bottomslidingdialog.BottomSlidingDialog;
import com.inpen.shuffle.R;
import com.inpen.shuffle.model.MutableMediaMetadata;
import com.inpen.shuffle.model.database.MediaContract;
import com.inpen.shuffle.model.repositories.QueueRepository;
import com.inpen.shuffle.model.repositories.SearchRepositiory;
import com.inpen.shuffle.model.repositories.SelectedItemsRepository;
import com.inpen.shuffle.model.repositories.SongsRepository;
import com.inpen.shuffle.playlist.AddToPlaylistDialogFragment;
import com.inpen.shuffle.utility.BaseItem;
import com.inpen.shuffle.utility.CustomTypes;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.gson.internal.$Gson$Preconditions.checkNotNull;

/**
 * Created by Abhishek on 12/20/2016.
 */

public class ItemsPresenter
        implements ItemsContract.ItemsFragmentListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private LoaderManager mLoaderManager;
    private ItemsContract.ItemsView mItemsView;
    private AppCompatActivity mActivityContext;
    private CustomTypes.ItemType mItemType;
    private SelectedItemsRepository mSelectedItemsRepository;

    private boolean mActive = false;

    public ItemsPresenter(@NonNull LoaderManager loaderManager,
                          @NonNull AppCompatActivity activity,
                          @NonNull ItemsContract.ItemsView itemsView,
                          @NonNull CustomTypes.ItemType itemType,
                          @NonNull SelectedItemsRepository selectedItemsRepository) {
        mLoaderManager = checkNotNull(loaderManager);
        mItemsView = checkNotNull(itemsView);
        mActivityContext = checkNotNull(activity);
        mItemType = checkNotNull(itemType);
        mSelectedItemsRepository = checkNotNull(selectedItemsRepository);

    }


    @Override
    public void initialize() {
        mSelectedItemsRepository = SelectedItemsRepository.getInstance();
        
        EventBus.getDefault().register(this);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        mLoaderManager.initLoader(0, null, this);
    }

    @Override
    public void itemLongPressed(String itemId) {

    }

    @Override
    public void itemClicked(Item item) {

    }

    @Override
    public void stop() {

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void shuffleAllClicked(final FragmentActivity activity) {
        // Play all songs
        QueueRepository.getInstance().initializeShuffleAll(mActivityContext,
                new QueueRepository.RepositoryInitializedCallback() {
                    @Override
                    public void onRepositoryInitialized(boolean success) {
                        if (success) {
                            activity
                                    .getSupportMediaController()
                                    .getTransportControls()
                                    .play();

                            SelectedItemsRepository
                                    .getInstance()
                                    .clearItems(); //TODO removed since it flickered the fab, do something else instead
                        }
                    }
                });
    }

    /**
     * Called when {@link SelectedItemsRepository} all items deselected.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSelectedItesEmptyStateChanged(SelectedItemsRepository.RepositoryEmptyStateChangedEvent event) {
        if (event.isEmpty)
            mItemsView.clearSelection();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        mItemsView.setProgressIndicator(true);

        String selection;
        switch (mItemType) {
            case SONG:
                selection = MediaContract.MediaEntry.COLUMN_TITLE + " NOT NULL";
                return new CursorLoader(mActivityContext,
                        MediaContract.MediaEntry.CONTENT_URI,
                        SONGS_QUERY_CURSOR_COLUMNS,
                        selection,
                        null, null);
            case ALBUM_KEY:
                selection = MediaContract.MediaEntry.COLUMN_ALBUM_KEY + " NOT NULL GROUP BY " + MediaContract.MediaEntry.COLUMN_ALBUM_KEY;
                return new CursorLoader(mActivityContext,
                        MediaContract.MediaEntry.CONTENT_URI,
                        ALBUMS_QUERY_CURSOR_COLUMNS,
                        selection,
                        null, null);
            case ARTIST_KEY:
                selection = MediaContract.MediaEntry.COLUMN_ARTIST_KEY + " NOT NULL GROUP BY " + MediaContract.MediaEntry.COLUMN_ARTIST_KEY;
                return new CursorLoader(mActivityContext,
                        MediaContract.MediaEntry.CONTENT_URI,
                        ARTISTS_QUERY_CURSOR_COLUMNS,
                        selection,
                        null, null);
            case FOLDER:
                selection = MediaContract.MediaEntry.COLUMN_FOLDER_PATH + " NOT NULL GROUP BY " + MediaContract.MediaEntry.COLUMN_FOLDER_PATH;
                return new CursorLoader(mActivityContext,
                        MediaContract.MediaEntry.CONTENT_URI,
                        FOLDERS_QUERY_CURSOR_COLUMNS,
                        selection,
                        null, null);
            case PLAYLIST:
                selection = MediaContract.PlaylistsEntry.COLUMN_PLAYLIST_NAME + " NOT NULL) GROUP BY (" + MediaContract.PlaylistsEntry.COLUMN_PLAYLIST_NAME;
                return new CursorLoader(mActivityContext,
                        MediaContract.PlaylistsEntry.CONTENT_URI,
                        PLAYLISTS_QUERY_CURSOR_COLUMNS,
                        selection,
                        null, null);

        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || !data.moveToFirst()) {
            mItemsView.showItems(new ArrayList<BaseItem>());
            return;
        }

        data.moveToFirst();

        List<BaseItem> itemList = new ArrayList<>();

        switch (mItemType) {
            case SONG:
                do {
                    SongItem item = new SongItem(
                            data.getString(0),//song id
                            data.getString(1),//track name
                            data.getString(2),//Album art
                            data.getString(3));
                    itemList.add(item);
                } while (data.moveToNext());
                break;
            case ALBUM_KEY:
                do {
                    Item item = new Item(
                            data.getString(0),//album id
                            data.getString(1),//album title
                            data.getString(2),//Album art
                            data.getInt(3));
                    itemList.add(item);
                } while (data.moveToNext());
                break;
            case ARTIST_KEY:
                do {
                    Item item = new Item(
                            data.getString(0),//artist id
                            data.getString(1),//artist title
                            data.getString(2),//Album art
                            data.getInt(3));
                    itemList.add(item);
                } while (data.moveToNext());
                break;
            case FOLDER:
                do {
                    Item item = new Item(
                            data.getString(0),//folder path
                            MediaContract.MediaEntry.getSongFolderFromFolderPath(data.getString(0)),//folder name from folder path
                            data.getString(1),//Album art
                            data.getInt(2));
                    itemList.add(item);
                } while (data.moveToNext());
                break;
            case PLAYLIST:
                do {
                    Item item = new Item(
                            data.getString(0),//playlist name
                            data.getString(0),//playlist name
                            data.getString(1),//Album art
                            data.getInt(2));
                    itemList.add(item);
                } while (data.moveToNext());
                break;
        }

        Collections.sort(itemList);

        mItemsView.showItems(itemList);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void itemMenuClicked(final SongItem songItem) {
        BottomSlidingDialog.build(mActivityContext)
                .setDialogTitle(songItem.title)
                .addAction(R.string.play_now, R.drawable.ic_play_arrow_black_24dp, 0)
                .addAction(R.string.play_next, R.drawable.ic_play_next_black_24dp, 1)
                .addAction(R.string.add_to_playlist, R.drawable.ic_playlist_add_black_24dp, 2)
                .addAction(R.string.delete, R.drawable.ic_delete_black_24dp, 3)
                .setActionListener(new BottomSlidingDialog.ActionListener() {
                    @Override
                    public void onActionSelected(int actionId) {
                        SongsRepository songsRepo = new SongsRepository(mActivityContext);

                        List<MutableMediaMetadata> metadataList = new ArrayList<>();
                        metadataList
                                .add(songsRepo
                                        .getSongMetadataForId(songItem.id));

                        switch (actionId) {
                            case 0: // play now
                                QueueRepository.getInstance().addNextSongs(metadataList, mActivityContext);
                                mActivityContext.getMediaController().getTransportControls().skipToNext();
                                break;
                            case 1: // play next
                                QueueRepository.getInstance().addNextSongs(metadataList, mActivityContext);
                                break;
                            case 2: // add to playlist
                                // TODO implement add to playlist
                                // show a dialog with list of playlist's with add button
                                // add the song to the returned playlist by calling
                                // songsRepo.addToPlaylist(songMetadata, playlistName)
                                new AddToPlaylistDialogFragment
                                        .Builder(songItem.id)
                                        .show(mActivityContext.getFragmentManager());
                                break;
                            case 3: // delete song
                                // TODO implement delete song
                                // show alert dialog to confirm deletion
                                // if yes call songsRepo.deleteSong()
                                break;
                        }
                    }
                })
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSearchQueryChanged(SearchRepositiory.SearchQueryChangedEvent event) {
        mItemsView.filterItems(event.searchTerm);
    }
}
