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

package com.stario.launcher.sheet.drawer.list;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.LauncherApplicationManager;
import com.stario.launcher.preferences.Vibrations;
import com.stario.launcher.sheet.drawer.RecyclerApplicationAdapter;
import com.stario.launcher.themes.ThemedActivity;
import com.stario.launcher.ui.icons.AdaptiveIconView;
import com.stario.launcher.ui.recyclers.FastScroller;
import com.stario.launcher.utils.animation.Animation;

public class ListAdapter extends RecyclerApplicationAdapter
        implements FastScroller.OnPopupViewUpdate,
        FastScroller.OnPopupViewReset {
    private final LauncherApplicationManager applicationManager;
    private final RecyclerView recyclerView;
    private int oldScrollerPosition;

    public ListAdapter(ThemedActivity activity, RecyclerView recyclerView) {
        super(activity);

        this.recyclerView = recyclerView;
        this.oldScrollerPosition = -1;
        this.applicationManager = LauncherApplicationManager.getInstance();

        applicationManager.addApplicationListener(new LauncherApplicationManager.ApplicationListener() {
            @Override
            public void onHidden(LauncherApplication application) {
                Log.e("TAG", "onHidden: " + recyclerView);
                recyclerView.post(() -> notifyItemRangeRemoved(0, getItemCount()));
            }

            @Override
            public void onInserted(LauncherApplication application) {
                recyclerView.post(() -> notifyItemInserted(applicationManager.indexOf(application)));
            }

            @Override
            public void onRemoved(LauncherApplication application) {
                recyclerView.post(() -> notifyItemRangeRemoved(0, getItemCount()));
            }

            @Override
            public void onShowed(LauncherApplication application) {
                recyclerView.post(() -> notifyItemInserted(applicationManager.indexOf(application)));
            }

            @Override
            public void onUpdated(LauncherApplication application) {
                recyclerView.post(() -> notifyItemChanged(applicationManager.indexOf(application)));
            }
        });
    }

    @Override
    public void onUpdate(int index, @NonNull TextView textView) {
        removeLimit();

        int size = applicationManager.getSize() - 1;

        if (index > size) {
            index = size;
        }

        if (oldScrollerPosition != index) {
            Vibrations.getInstance().vibrate();
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

            if (layoutManager != null) {
                View lastView = layoutManager.findViewByPosition(oldScrollerPosition);
                View currentView = layoutManager.findViewByPosition(index);

                if (currentView != null) {
                    currentView.animate().scaleX(AdaptiveIconView.MAX_SCALE)
                            .scaleY(AdaptiveIconView.MAX_SCALE)
                            .setDuration(Animation.MEDIUM.getDuration());
                }

                if (lastView != null) {
                    lastView.animate().scaleX(1).scaleY(1)
                            .setDuration(Animation.MEDIUM.getDuration());
                }
            }
        }

        oldScrollerPosition = index;
        LauncherApplication application = applicationManager.get(index, true);

        if (application != LauncherApplication.FALLBACK_APP) {
            String label = application.getLabel();

            if (!label.isEmpty()) {
                textView.setText(String.valueOf(label.charAt(0)).toUpperCase());
            }
        }
    }

    @Override
    public void onReset(int index) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        if (layoutManager != null) {
            View currentView = layoutManager.findViewByPosition(oldScrollerPosition);
            oldScrollerPosition = -1;

            if (currentView != null) {
                currentView.animate().scaleX(1)
                        .scaleY(1)
                        .setDuration(Animation.MEDIUM.getDuration())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationCancel(Animator animation) {
                                currentView.setScaleX(1);
                                currentView.setScaleY(1);
                            }
                        });
            }
        }
    }

    @Override
    protected LauncherApplication getApplication(int index) {
        return applicationManager.get(index, true);
    }

    @Override
    protected boolean allowApplicationStateEditing() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        LauncherApplication application = applicationManager.get(position, true);

        if (application != null) {
            return application.getInfo()
                    .packageName.hashCode();
        } else {
            return -1;
        }
    }

    @Override
    protected int getSize() {
        return applicationManager.getSize();
    }
}