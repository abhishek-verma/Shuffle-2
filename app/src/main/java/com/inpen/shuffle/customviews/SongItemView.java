package com.inpen.shuffle.customviews;

import android.content.Context;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.inpen.shuffle.R;
import com.inpen.shuffle.mainscreen.items.SongItem;
import com.inpen.shuffle.utility.LogHelper;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Abhishek on 11/2/2016.
 */

public class SongItemView extends FrameLayout {
    private static final String TAG = LogHelper.makeLogTag(SongItemView.class);
    private static final int ANIMATION_DURATION = 275;

    @BindView(R.id.albumArt)
    public FixedRatioImageView mAlbumArtView;
    @BindView(R.id.mask)
    public FixedRatioImageView mMaskView;
    @BindView(R.id.itemTitle)
    public TextView mTitleTextView;
    @BindView(R.id.songArtist)
    public TextView mArtistView;
    private SongItem mItem;
    private boolean mIsSelected = false;

    public SongItemView(Context context) {
        super(context);

        initView(context);
    }

    private void initView(Context context) {
        View.inflate(context, R.layout.song_item_view_layout, this);

        ButterKnife.bind(this);
    }

    public SongItem getItem() {
        return mItem;
    }

    public void setItem(SongItem item, boolean selected) {
        mItem = item;

        if (mTitleTextView != null)
            mTitleTextView.setText(mItem.title);

        if (mArtistView != null) {
            mArtistView.setText(mItem.artist);
        }

        if (mAlbumArtView != null) {
            Glide.with(getContext())
                    .load(mItem.imagePath)
                    .error(getResources().getDrawable(R.drawable.shuffle_bg))
                    .into(mAlbumArtView);
        }

//        if (mIsSelected) {
//            mAlbumArtView.setAlpha(0.6f);
//            mMaskView.setAlpha(1f);
//        } else {
//            mAlbumArtView.setAlpha(1f);
//            mMaskView.setAlpha(0f);
//        }

        setSelected(selected);
    }

    public void setSelected(boolean select) {
        if (mIsSelected == select) {
            return;
        }

        mIsSelected = select;

        if (mIsSelected) {
            mMaskView.animate()
                    .alpha(0.8f)
                    .withLayer()
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new AccelerateInterpolator())
                    .start();

            mAlbumArtView.animate()
                    .alpha(0.8f)
                    .withLayer()
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new AccelerateInterpolator())
                    .start();

        } else {
            mMaskView.animate()
                    .alpha(0f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new AccelerateInterpolator())
                    .withLayer()
                    .start();

            mAlbumArtView.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION)
                    .setInterpolator(new AccelerateInterpolator())
                    .withLayer()
                    .start();

        }
    }
}
