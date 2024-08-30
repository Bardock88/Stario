/*
    Copyright (C) 2024 Răzvan Albu

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package com.stario.launcher.sheet.drawer.category.list;

import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.BumpRecyclerViewAdapter;
import com.stario.launcher.sheet.drawer.DrawerAdapter;
import com.stario.launcher.sheet.drawer.apps.categories.Category;
import com.stario.launcher.sheet.drawer.apps.categories.CategoryData;
import com.stario.launcher.sheet.drawer.category.Categories;
import com.stario.launcher.sheet.drawer.category.folder.Folder;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.RecyclerItemAnimator;
import com.stario.launcher.ui.recyclers.async.AsyncRecyclerAdapter;
import com.stario.launcher.utils.UiUtils;
import com.stario.launcher.utils.animation.Animation;
import com.stario.launcher.utils.animation.FragmentTransition;
import com.stario.launcher.utils.animation.SharedAppTransition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FolderListAdapter extends AsyncRecyclerAdapter<FolderListAdapter.ViewHolder>
        implements BumpRecyclerViewAdapter {
    private static final float TARGET_ELEVATION = 10;
    private static final float TARGET_SCALE = 0.9f;
    private static boolean animating = false;
    private final List<AdaptiveIconView> sharedIcons;
    private final RecyclerView recyclerView;
    private final CategoryData categoryData;
    private final ThemedActivity activity;
    private final FolderList folderList;
    private final Folder folder;
    private boolean limit;
    private int size;

    public FolderListAdapter(ThemedActivity activity,
                             FolderList folderList, RecyclerView recyclerView) {
        super(activity);

        animating = false;

        this.activity = activity;
        this.folderList = folderList;
        this.recyclerView = recyclerView;

        this.categoryData = CategoryData.getInstance();
        this.sharedIcons = new ArrayList<>();
        this.folder = new Folder();
        this.size = 0;
        this.limit = true;

        setHasStableIds(true);

        categoryData.setOnCategoryUpdateListener(new CategoryData.CategoryListener() {
            int preparedRemovalIndex = -1;

            @Override
            public void onCreated(Category category) {
                int index = categoryData.indexOf(category);

                if (index >= 0) {
                    UiUtils.runOnUIThread(() -> notifyItemInserted(index));
                }
            }

            @Override
            public void onPrepareRemoval(Category category) {
                preparedRemovalIndex = categoryData.indexOf(category);
            }

            @Override
            public void onRemoved(Category category) {
                if (preparedRemovalIndex >= 0) {
                    UiUtils.runOnUIThread(() -> notifyItemRemoved(preparedRemovalIndex));

                    preparedRemovalIndex = -1;
                } else {
                    UiUtils.runOnUIThread(() -> notifyDataSetChanged());
                }
            }
        });
    }

    public boolean move(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder targetHolder) {
        int position = viewHolder.getAbsoluteAdapterPosition();
        int target = targetHolder.getAbsoluteAdapterPosition();

        if (position == target) {
            return false;
        }

        while (position - target != 0) {
            int newTarget = position - ((position - target) > 0 ? 1 : -1);

            categoryData.swap(position, newTarget);
            notifyItemMoved(position, newTarget);

            position = newTarget;
        }

        return true;
    }

    public void focus(RecyclerView.ViewHolder holder) {
        if (holder instanceof ViewHolder) {
            ViewHolder viewHolder = (ViewHolder) holder;

            holder.itemView.bringToFront();

            viewHolder.itemView.animate()
                    .scaleY(TARGET_SCALE)
                    .scaleX(TARGET_SCALE)
                    .translationZ(TARGET_ELEVATION)
                    .setDuration(Animation.MEDIUM.getDuration());
            viewHolder.category.animate()
                    .alpha(0)
                    .setDuration(Animation.MEDIUM.getDuration());
        }
    }

    public void reset(RecyclerView.ViewHolder holder) {
        if (holder instanceof ViewHolder) {
            ViewHolder viewHolder = (ViewHolder) holder;

            viewHolder.itemView.animate()
                    .scaleY(1f)
                    .scaleX(1f)
                    .translationZ(0)
                    .setDuration(Animation.MEDIUM.getDuration());
            viewHolder.category.animate()
                    .alpha(1f)
                    .setDuration(Animation.MEDIUM.getDuration());
        }
    }

    protected class ViewHolder extends AsyncViewHolder {
        private TextView category;
        private RecyclerView recycler;
        private FolderListItemAdapter adapter;

        @Override
        protected void onInflated() {
            itemView.setHapticFeedbackEnabled(false);

            category = itemView.findViewById(R.id.category);
            recycler = itemView.findViewById(R.id.items);

            recycler.setItemAnimator(null);

            GridLayoutManager gridLayoutManager =
                    new GridLayoutManager(activity, 4);

            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (!adapter.isCapped() || position < FolderListItemAdapter.SOFT_LIMIT) {
                        return 2;
                    } else {
                        return 1;
                    }
                }
            });

            recycler.setLayoutManager(gridLayoutManager);
            recycler.setItemAnimator(new RecyclerItemAnimator(RecyclerItemAnimator.APPEARANCE));

            adapter = new FolderListItemAdapter(activity);

            recycler.setAdapter(adapter);
        }

        public void updateCategory(Category category, boolean animate) {
            adapter.setCategory(category, animate);
        }
    }

    @Override
    public void onBind(@NonNull ViewHolder viewHolder, int index) {
        Category category = categoryData.get(index);

        viewHolder.category.setText(
                categoryData.getCategoryName(
                        category.id, activity.getResources()
                ));

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            private AdaptiveIconView getIcon(View view) {
                if (view instanceof AdaptiveIconView) {
                    return (AdaptiveIconView) view;
                }

                if (view instanceof ViewGroup) {
                    ViewGroup group = (ViewGroup) view;

                    for (int index = 0, count = group.getChildCount(); index < count; index++) {
                        AdaptiveIconView icon = getIcon(group.getChildAt(index));

                        if (icon != null) {
                            return icon;
                        }
                    }
                }

                return null;
            }

            @Override
            public void onClick(View view) {
                if (viewHolder.adapter.isCapped()) {
                    Vibrations.getInstance().vibrate();

                    for (int index = sharedIcons.size() - 1; index >= 0; index--) {
                        AdaptiveIconView icon = sharedIcons.remove(index);

                        icon.setTransitionName(null);
                    }

                    List<View> excluded = new ArrayList<>();

                    FragmentManager fragmentManager = folderList.getParentFragmentManager();
                    RecyclerView.LayoutManager layoutManager = viewHolder.recycler.getLayoutManager();

                    if (layoutManager != null) {
                        FragmentTransaction transaction = fragmentManager.beginTransaction();

                        for (int position = 0;
                             position < viewHolder.adapter.getItemCount() &&
                                     position < FolderListItemAdapter.HARD_LIMIT; position++) {

                            View group = layoutManager.findViewByPosition(position);

                            excluded.add(group);

                            AdaptiveIconView icon = getIcon(group);

                            if (icon != null) {
                                sharedIcons.add(icon);

                                String transitionName = DrawerAdapter.SHARED_ELEMENT_PREFIX + position;

                                icon.setTransitionName(transitionName);
                                transaction.addSharedElement(icon, transitionName);

                                excluded.add(icon);
                            }
                        }

                        excluded.addAll(sharedIcons);

                        SharedAppTransition transition = new SharedAppTransition(false);

                        transition.addListener(new TransitionListenerAdapter() {
                            @Override
                            public void onTransitionStart(Transition transition) {
                                animating = true;
                            }

                            @Override
                            public void onTransitionEnd(Transition transition) {
                                animating = false;
                            }

                            @Override
                            public void onTransitionCancel(Transition transition) {
                                animating = false;
                            }
                        });

                        folder.setSharedElementEnterTransition(transition);
                        folder.setEnterTransition(new FragmentTransition(true, excluded));
                        folder.setExitTransition(new FragmentTransition(false, null));
                        folderList.setExitTransition(new FragmentTransition(true, excluded));

                        transaction.setReorderingAllowed(true);
                        transaction.addToBackStack(Categories.STACK_ID);

                        transaction.hide(folderList)
                                .add(R.id.categories, folder);

                        fragmentManager.executePendingTransactions();
                        transaction.commit();

                        folder.updateCategoryID(category.id);
                    }
                }
            }
        });

        viewHolder.updateCategory(category, limit);
    }

    @Override
    protected int getLayout() {
        return R.layout.folder;
    }

    @Override
    protected Supplier<ViewHolder> getHolderSupplier() {
        return ViewHolder::new;
    }

    @Override
    public long getItemId(int position) {
        return categoryData.get(position).id;
    }

    @Override
    public int getItemCount() {
        return limit ? size : categoryData.size();
    }

    @Override
    public void bump() {
        if (limit) {
            int approximatedHolderHeight = getApproximatedHolderHeight();
            int newSize = size +
                    (approximatedHolderHeight != AsyncRecyclerAdapter.AsyncViewHolder.HEIGHT_UNMEASURED ?
                            Math.round(
                                    Math.max(1,
                                            recyclerView.getMeasuredHeight() / (float) approximatedHolderHeight)
                            ) : 1
                    );

            int inserted = newSize - size;
            size = newSize;

            if (inserted > 0) {
                notifyItemRangeInserted(getItemCount() - inserted, inserted);
            }
        }
    }

    @Override
    public void removeLimit() {
        limit = false;

        int inserted = categoryData.size() - size;
        notifyItemRangeInserted(getItemCount() - inserted, inserted);
    }

    public static boolean isAnimating() {
        return animating;
    }
}