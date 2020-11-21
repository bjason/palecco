/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bjason.palecco;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.card.MaterialCardView;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;


/**
 * A fragment for displaying a grid of images.
 */
public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ImageViewHolder> {

    /**
     * A listener that is attached to all ViewHolders to handle image loading events and clicks.
     */
    private interface ViewHolderListener {

        void onLoadCompleted(ImageView view, int adapterPosition);

        void onItemClicked(View view, int adapterPosition);

        void onItemLongClicked(View view, int adapterPosition);
    }

    private final RequestManager requestManager;
    private final ViewHolderListener viewHolderListener;

    /**
     * Constructs a new grid adapter for the given {@link Fragment}.
     */
    public GridAdapter(Fragment fragment) {
        this.requestManager = Glide.with(fragment);
        this.viewHolderListener = new ViewHolderListenerImpl(fragment, fragment.getActivity());
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_card, parent, false);
        return new ImageViewHolder(view, requestManager, viewHolderListener);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        holder.onBind();
    }

    @Override
    public int getItemCount() {
        return GridFragment.mBitmaps.size();
    }


    /**
     * Default {@link ViewHolderListener} implementation.
     */
    private static class ViewHolderListenerImpl implements ViewHolderListener {

        private Fragment fragment;
        private AtomicBoolean enterTransitionStarted;
        private Context mContext;

        ViewHolderListenerImpl(Fragment fragment, Context context) {
            this.fragment = fragment;
            this.enterTransitionStarted = new AtomicBoolean();
            mContext = context;
        }

        @Override
        public void onLoadCompleted(ImageView view, int position) {
            // Call startPostponedEnterTransition only when the 'selected' image loading is completed.
            if (MainActivity.currentPosition != position) {
                return;
            }
            if (enterTransitionStarted.getAndSet(true)) {
                return;
            }
            fragment.startPostponedEnterTransition();
        }

        /**
         * Handles a view click by setting the current position to the given {@code position} and
         * starting a {@link  ImagePagerFragment} which displays the image at the position.
         *
         * @param view     the clicked {@link ImageView} (the shared element view will be re-mapped at the
         *                 GridFragment's SharedElementCallback)
         * @param position the selected view position
         */
        @Override
        public void onItemClicked(View view, int position) {
            // Update the position.
            MainActivity.currentPosition = position;

            // Exclude the clicked card from the exit transition (e.g. the card will disappear immediately
            // instead of fading out with the rest to prevent an overlapping animation of fade and move).
            ((TransitionSet) fragment.getExitTransition()).excludeTarget(view, true);

            ImageView transitioningView = view.findViewById(R.id.card_image);
            fragment.getFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true) // Optimize for shared element transition
                    .addSharedElement(transitioningView, transitioningView.getTransitionName())
                    .replace(R.id.fragment_container, new ImagePagerFragment(), ImagePagerFragment.class
                            .getSimpleName())
                    .addToBackStack(null)
                    .commit();
        }

        @Override
        public void onItemLongClicked(View view, int adapterPosition) {
            if (GridFragment.currentSelected == adapterPosition) {
                // deselect
                ((GridFragment) fragment).setNormalFab();
            } else {
                if (GridFragment.currentSelected != -1) {
                    // is already selected, then deselect the previous one
                    ((MaterialCardView) GridFragment.currentSelectedView).setStrokeWidth(0);
                }
                GridFragment.currentSelected = adapterPosition;
                GridFragment.currentSelectedView = view;

                MaterialCardView v = (MaterialCardView) view;
                v.setStrokeColor(ContextCompat.getColor(fragment.getContext(), R.color.brown_500));
                v.setStrokeWidth(15);

                ((GridFragment) fragment).setSelectedFab();
            }
        }
    }

    /**
     * ViewHolder for the grid's images.
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {

        private final ImageView image;
        private final RequestManager requestManager;
        private final ViewHolderListener viewHolderListener;
        private Context mContext;

        ImageViewHolder(View itemView, RequestManager requestManager,
                        ViewHolderListener viewHolderListener) {
            super(itemView);
            this.image = itemView.findViewById(R.id.card_image);
            this.requestManager = requestManager;
            this.viewHolderListener = viewHolderListener;
            this.mContext = itemView.getContext();

            itemView.findViewById(R.id.card_view).setOnClickListener(this);
            itemView.findViewById(R.id.card_view).setOnLongClickListener(this);
        }

        /**
         * Binds this view holder to the given adapter position.
         * <p>
         * The binding will load the image into the image view, as well as set its transition name for
         * later.
         */
        void onBind() {
            int adapterPosition = getAdapterPosition();
            setImage(adapterPosition);
            // Set the string value of the image resource as the unique transition name for the view.
            image.setTransitionName(String.valueOf(GridFragment.mBitmaps.get(adapterPosition)));
        }

        void setImage(final int adapterPosition) {
            Bitmap bitmap = GridFragment.mBitmaps.get(adapterPosition);
            // Load the image with Glide to prevent OOM error when the image drawables are very large.
            requestManager
                    .load(bitmap)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target, boolean isFirstResource) {
                            viewHolderListener.onLoadCompleted(image, adapterPosition);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable>
                                target, DataSource dataSource, boolean isFirstResource) {
                            viewHolderListener.onLoadCompleted(image, adapterPosition);
                            return false;
                        }
                    })
                    .into(image);

            // width is fixed to 200
            // then new height is 200*height/width
            float heightInDp = ImageUtil.pxFromDp(mContext, 200f * (float) bitmap.getHeight() / (float) bitmap.getWidth());
            image.getLayoutParams().height = (int) heightInDp;
            image.requestLayout();
        }

        @Override
        public void onClick(View view) {
            // Let the listener start the ImagePagerFragment.
            viewHolderListener.onItemClicked(view, getAdapterPosition());
            // TODO hide fab and toolbar

        }

        @Override
        public boolean onLongClick(View view) {
            viewHolderListener.onItemLongClicked(view, getAdapterPosition());
            return true;
        }
    }
}