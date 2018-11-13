package com.volcaniccoder.volxfastscroll;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static android.view.View.VISIBLE;

public class Volx implements Runnable {

    public static final int FIT_NICELY = 0;
    public static final int NEVER_CLOSE = -1;
    private Context context;
    private int activeColor;
    private int backgroundColor;
    private int textSize;
    private int textColor;
    private int barWidth; //dp
    private float barHeightRatio;
    private int middleTextSize;
    private int middleLayoutSize; //dp
    private int middleBackgroundColor;
    private int middleStrokeColor;
    private int rightStrokeColor;
    private int middleTextColor;
    private int middleStrokeWidth; //dp
    private int rightStrokeWidth; //dp
    private int delayMillis;
    private int minItem;
    private FrameLayout parentLayout;
    private RecyclerView userRecyclerView; //adapter must be IVolxAdapter
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.OnScrollListener scrollListener;
    private TextView middleText;
    private List<String> allStringList = new ArrayList<>();
    private List<Character> charArr = new ArrayList<>();
    private List<VolxCharModel> charList = new ArrayList<>();
    private int lastPos = -1;
    private int blinkCount = 0;
    private boolean isUserScrolled = true;
    private boolean isUserTouchedRightBar = false;
    private boolean isInactive = false;
    private int itemHeight;
    private List<Integer> positionList = new ArrayList<>();
    private VolxUtils utils;
    private FrameLayout rightIndicatorLayout;
    private FrameLayout.LayoutParams rightBarParams;

    public Volx(Builder builder) {
        this.activeColor = builder.activeColor;
        this.backgroundColor = builder.backgroundColor;
        this.textSize = builder.textSize;
        this.textColor = builder.textColor;
        this.barWidth = builder.barWidth;
        this.barHeightRatio = builder.barHeightRatio;
        this.middleTextSize = builder.middleTextSize;
        this.middleLayoutSize = builder.middleLayoutSize;
        this.middleBackgroundColor = builder.middleBackgroundColor;
        this.middleStrokeColor = builder.middleStrokeColor;
        this.rightStrokeColor = builder.rightStrokeColor;
        this.middleTextColor = builder.middleTextColor;
        this.rightStrokeWidth = builder.rightStrokeWidth;
        this.middleStrokeWidth = builder.middleStrokeWidth;
        this.delayMillis = builder.delayMillis;
        this.minItem = builder.minItem;
        this.parentLayout = builder.parentLayout;
        this.userRecyclerView = builder.userRecyclerView;
        this.execute();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void execute() {

        context = parentLayout.getContext();
        utils = new VolxUtils(context);

        if (!(userRecyclerView.getAdapter() instanceof IVolxAdapter)) {
            Log.w("VOLX", "Please implement IVolxAdapter in your own adapter , you need to initialize the adapter before initializing Volx");
            return;
        }

        List objectList = ((IVolxAdapter) userRecyclerView.getAdapter()).getList();

        if (objectList == null || objectList.isEmpty())
            return;

        Class foo = objectList.get(0).getClass();
        int counter = -1;

        allStringList.clear();
        charArr.clear();
        charList.clear();
        positionList.clear();

        for (Object obj : objectList) {

            counter++;

            for (Field field : foo.getDeclaredFields()) {

                ValueArea annotation = field.getAnnotation(ValueArea.class);

                if (annotation != null) {

                    field.setAccessible(true);

                    try {
                        allStringList.add(field.get(obj).toString().toUpperCase());
                        Character c = field.get(obj).toString().toUpperCase().charAt(0);

                        if (!charArr.contains(c)) {
                            charArr.add(c);
                            charList.add(new VolxCharModel(c));
                            positionList.add(counter);
                        }

                    } catch (IllegalAccessException | StringIndexOutOfBoundsException e) {
                        e.printStackTrace();
                    }

                    break;
                }
            }
        }

        if (minItem > 0 && charList.size() < minItem)
            return;

        initViews();

        removeViewsWithDelay();

        parentLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                onScreenCreated(parentLayout.getMeasuredHeight(), this);
            }
        });

        scrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == SCROLL_STATE_DRAGGING && !isUserScrolled) {
                    isUserScrolled = true;
                    rightIndicatorLayout.setVisibility(View.VISIBLE);
                    removeViewsWithDelay();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int posTop = ((LinearLayoutManager) (userRecyclerView.getLayoutManager())).findFirstVisibleItemPosition();
                middleText.setText(String.valueOf(allStringList.get(posTop).toUpperCase().charAt(0)));
            }
        };

        userRecyclerView.addOnScrollListener(scrollListener);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (middleText.getVisibility() != VISIBLE)
                    middleText.setVisibility(View.VISIBLE);

                if (newState == SCROLL_STATE_IDLE)
                    rightIndicatorLayout.setVisibility(View.VISIBLE);

            }

        });

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int y = (int) event.getY();

                isUserTouchedRightBar = true;

                if (rightIndicatorLayout.getVisibility() == View.GONE)
                    setViewsVisibility(true);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        middleText.setVisibility(VISIBLE);
                        break;
                    case MotionEvent.ACTION_MOVE:

                        blinkCount = y / itemHeight;

                        if (blinkCount > -1 && blinkCount != lastPos && blinkCount < charList.size()) {

                            ((VolxAdapter) mAdapter).clearBlinks();
                            VolxCharModel changeModel = ((VolxAdapter) mAdapter).getCharListModelAt(blinkCount);
                            changeModel.setBlink(true);
                            mAdapter.notifyDataSetChanged();

                            ((LinearLayoutManager) (userRecyclerView.getLayoutManager())).scrollToPositionWithOffset(positionList.get(blinkCount), 0);
                            middleText.setText(changeModel.getCharacter().toString().toUpperCase());

                            lastPos = blinkCount;
                        }

                        break;
                    case MotionEvent.ACTION_UP:
                        if (rightIndicatorLayout.getVisibility() == View.VISIBLE) {
                            ((VolxAdapter) mAdapter).clearBlinks();
                            mAdapter.notifyDataSetChanged();
                            lastPos = -1;
                            setViewsVisibility(false);
                        }
                        break;
                }
                return true;
            }
        });

    }

    private void removeViewsWithDelay() {
        if (delayMillis > 0)
            mRecyclerView.postDelayed(Volx.this, delayMillis);
    }

    private void setViewsVisibility(boolean isShow) {
        if (!isShow) {
            middleText.setVisibility(View.GONE);
            if (delayMillis == NEVER_CLOSE)
                return;
            rightIndicatorLayout.setVisibility(View.GONE);
            isUserScrolled = false;
            isUserTouchedRightBar = false;
            return;
        }

        rightIndicatorLayout.setVisibility(View.VISIBLE);
        middleText.setVisibility(View.VISIBLE);
    }

    private void initViews() {

        // Adding middle circle text to the center of parent layout

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(utils.dpToPx(middleLayoutSize), utils.dpToPx(middleLayoutSize));
        textParams.gravity = Gravity.CENTER;

        middleText = new TextView(context);
        middleText.setTextColor(middleTextColor);
        middleText.setTextSize(middleTextSize);
        middleText.setBackgroundResource(R.drawable.middle_text_bg);
        middleText.setGravity(Gravity.CENTER);
        middleText.setVisibility(View.GONE);
        utils.changeDrawableColor(middleText, middleBackgroundColor, middleStrokeColor, middleStrokeWidth);

        parentLayout.addView(middleText, textParams);

        //Creating right side layout and adding it to the right side of parent layout

        rightBarParams = new FrameLayout.LayoutParams(utils.dpToPx(barWidth), ViewGroup.LayoutParams.MATCH_PARENT);
        rightBarParams.gravity = Gravity.CENTER | Gravity.END;
        rightBarParams.setMargins(utils.dpToPx(2), utils.dpToPx(4), utils.dpToPx(2), utils.dpToPx(4));

        rightIndicatorLayout = new FrameLayout(context);
        rightIndicatorLayout.setBackgroundResource(R.drawable.layout_shape);
        utils.changeDrawableColor(rightIndicatorLayout, backgroundColor, rightStrokeColor, rightStrokeWidth);

        parentLayout.addView(rightIndicatorLayout, rightBarParams);

        // Adding recycler view into the right side layout

        FrameLayout.LayoutParams listParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        listParams.gravity = Gravity.CENTER | Gravity.END;
        listParams.setMargins(utils.dpToPx(1), utils.dpToPx(4), utils.dpToPx(1), utils.dpToPx(4));

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setBackgroundColor(Color.TRANSPARENT);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        rightIndicatorLayout.addView(mRecyclerView, listParams);

        // Adding animate layout change to parent layout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            LayoutTransition lt = new LayoutTransition();
            lt.disableTransitionType(LayoutTransition.APPEARING);
            parentLayout.setLayoutTransition(lt);
        }

    }

    @Override
    public void run() {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isUserTouchedRightBar) return;
                if (rightIndicatorLayout.getVisibility() == VISIBLE) {
                    setViewsVisibility(false);
                }
                mRecyclerView.removeCallbacks(this);
            }
        });
    }

    public void notifyValueDataChanged() {
        parentLayout.removeView(rightIndicatorLayout);
        parentLayout.removeView(middleText);
        execute();
    }

    private void onScreenCreated(int height, ViewTreeObserver.OnGlobalLayoutListener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
            parentLayout.getViewTreeObserver().removeGlobalOnLayoutListener(listener);
        else
            parentLayout.getViewTreeObserver().removeOnGlobalLayoutListener(listener);

        rightBarParams.height = (int) (height * barHeightRatio);
        itemHeight = (int) (height * barHeightRatio - utils.dpToPx(16)) / (charList.size());

        mAdapter = new VolxAdapter(charList, new VolxAdapterFeatures(itemHeight, barHeightRatio, barWidth, textSize, textColor, activeColor));
        mRecyclerView.setAdapter(mAdapter);
    }

    public int getActiveColor() {
        return activeColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getTextSize() {
        return textSize;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getBarWidth() {
        return barWidth;
    }

    public int getMiddleTextSize() {
        return middleTextSize;
    }

    public int getMiddleLayoutSize() {
        return middleLayoutSize;
    }

    public int getMiddleBackgroundColor() {
        return middleBackgroundColor;
    }

    public int getMiddleStrokeColor() {
        return middleStrokeColor;
    }

    public int getRightStrokeColor() {
        return rightStrokeColor;
    }

    public int getMiddleTextColor() {
        return middleTextColor;
    }

    public int getMiddleStrokeWidth() {
        return middleStrokeWidth;
    }

    public int getRightStrokeWidth() {
        return rightStrokeWidth;
    }

    public boolean isInactive() {
        return isInactive;
    }

    public void setInactive(boolean state) {
        if (scrollListener == null)
            return;

        isInactive = state;
        if (state) {
            setViewsVisibility(false);
            userRecyclerView.removeOnScrollListener(scrollListener);
            return;
        }
        userRecyclerView.addOnScrollListener(scrollListener);

    }

    public static class Builder {

        private int activeColor = Color.CYAN;

        private int backgroundColor = Color.BLACK;

        private int textSize = Volx.FIT_NICELY;

        private int textColor = Color.WHITE;

        private int barWidth = 24;

        private float barHeightRatio = 1f;

        private int middleTextSize = 16;

        private int middleLayoutSize = 48;

        private int middleBackgroundColor = Color.rgb(67, 67, 67);

        private int middleStrokeColor = Color.BLACK;

        private int rightStrokeColor = Color.rgb(204, 204, 204);

        private int middleStrokeWidth = 4;

        private int rightStrokeWidth = 3;

        private int middleTextColor = Color.WHITE;

        private int delayMillis = 3000;

        private int minItem = 0;

        private FrameLayout parentLayout;

        private RecyclerView userRecyclerView;

        public Builder setActiveColor(int activeColor) {
            this.activeColor = activeColor;
            return this;
        }

        public Builder setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setTextSize(int textSize) {
            this.textSize = textSize;
            return this;
        }

        public Builder setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder setBarWidth(int barWidth) {
            this.barWidth = barWidth;
            return this;
        }

        public Builder setBarHeightRatio(float barHeightRatio) {
            if (barHeightRatio >= 0 && barHeightRatio <= 1)
                this.barHeightRatio = barHeightRatio;
            return this;
        }

        public Builder setMiddleTextSize(int middleTextSize) {
            this.middleTextSize = middleTextSize;
            return this;
        }

        public Builder setMiddleLayoutSize(int middleLayoutSize) {
            this.middleLayoutSize = middleLayoutSize;
            return this;
        }

        public Builder setMiddleBackgroundColor(int middleBackgroundColor) {
            this.middleBackgroundColor = middleBackgroundColor;
            return this;
        }

        public Builder setMiddleStrokeColor(int middleStrokeColor) {
            this.middleStrokeColor = middleStrokeColor;
            return this;
        }

        public Builder setRightStrokeColor(int rightStrokeColor) {
            this.rightStrokeColor = rightStrokeColor;
            return this;
        }

        public Builder setRightStrokeWidth(int rightStrokeWidth) {
            this.rightStrokeWidth = rightStrokeWidth;
            return this;
        }

        public Builder setMiddleStrokeWidth(int middleStrokeWidth) {
            this.middleStrokeWidth = middleStrokeWidth;
            return this;
        }

        public Builder setMiddleTextColor(int middleTextColor) {
            this.middleTextColor = middleTextColor;
            return this;
        }

        public Builder setDelayMillis(int delayMillis) {
            this.delayMillis = delayMillis;
            return this;
        }

        public Builder setMinItem(int minItem) {
            this.minItem = minItem;
            return this;
        }

        public Builder setParentLayout(FrameLayout parentLayout) {
            this.parentLayout = parentLayout;
            return this;
        }

        public Builder setUserRecyclerView(RecyclerView userRecyclerView) {
            this.userRecyclerView = userRecyclerView;
            return this;
        }

        public Volx build() {
            return new Volx(this);
        }
    }
}
