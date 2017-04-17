//package com.compass.utils;
//
//import android.R;
//import android.animation.Animator;
//import android.animation.AnimatorSet;
//import android.animation.ObjectAnimator;
//import android.animation.TimeInterpolator;
//import android.app.Activity;
//import android.content.Context;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.drawable.BitmapDrawable;
//import android.graphics.drawable.Drawable;
//import android.view.Gravity;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewManager;
//import android.view.animation.AccelerateInterpolator;
//import android.view.animation.OvershootInterpolator;
//import android.widget.FrameLayout;
//
//public class FloatingActionButton extends View {
//
//    private static final TimeInterpolator overshootInterpolator = new OvershootInterpolator();
//    private static final TimeInterpolator accelerateInterpolator = new AccelerateInterpolator();
//
//    private Paint mButtonPaint;
//    private Paint mDrawablePaint;
//    private Bitmap mBitmap;
//    private boolean mHidden;
//
//    public FloatingActionButton(Context context) {
//        super(context);
//        init(Color.WHITE);
//    }
//
//    void setFloatingActionButtonColor(int FloatingActionButtonColor) {
//        init(FloatingActionButtonColor);
//    }
//
//    void setFloatingActionButtonDrawable(Drawable FloatingActionButtonDrawable) {
//        mBitmap = ((BitmapDrawable) FloatingActionButtonDrawable).getBitmap();
//        invalidate();
//    }
//
//    void init(int FloatingActionButtonColor) {
//        setWillNotDraw(false);
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//
//        mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        mButtonPaint.setColor(FloatingActionButtonColor);
//        mButtonPaint.setStyle(Paint.Style.FILL);
//        mButtonPaint.setShadowLayer(10.0f, 0.0f, 3.5f, Color.argb(100, 0, 0, 0));
//
//        mDrawablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//
//        invalidate();
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        setClickable(true);
//        canvas.drawCircle(getWidth() / 2, getHeight() / 2, (float) (getWidth() / 2.6), mButtonPaint);
//        canvas.drawBitmap(mBitmap, (getWidth() - mBitmap.getWidth()) / 2, (getHeight() - mBitmap.getHeight()) / 2, mDrawablePaint);
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        if (!mHidden) {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", getScaleX(), 1);
//                ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", getScaleY(), 1);
//                AnimatorSet animSetXY = new AnimatorSet();
//                animSetXY.playTogether(scaleX, scaleY);
//                animSetXY.setInterpolator(accelerateInterpolator);
//                animSetXY.setDuration(100);
//                animSetXY.start();
//
//            } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1, 1.1f);
//                ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1, 1.1f);
//                AnimatorSet animSetXY = new AnimatorSet();
//                animSetXY.playTogether(scaleX, scaleY);
//                animSetXY.setInterpolator(accelerateInterpolator);
//                animSetXY.setDuration(100);
//                animSetXY.start();
//
//            }
//        }
//        return super.onTouchEvent(event);
//    }
//
//    public void hideFloatingActionButton() {
//        if (!mHidden) {
//            ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1, 0);
//            ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1, 0);
//            AnimatorSet animSetXY = new AnimatorSet();
//            animSetXY.playTogether(scaleX, scaleY);
//            animSetXY.setInterpolator(accelerateInterpolator);
//            animSetXY.setDuration(100);
//            animSetXY.addListener(new Animator.AnimatorListener() {
//                @Override
//                public void onAnimationStart(Animator animator) {
//
//                }
//
//                @Override
//                public void onAnimationEnd(Animator animator) {
//                    setVisibility(View.GONE);
//                }
//
//                @Override
//                public void onAnimationCancel(Animator animator) {
//
//                }
//
//                @Override
//                public void onAnimationRepeat(Animator animator) {
//
//                }
//            });
//            animSetXY.start();
//            mHidden = true;
//        }
//    }
//
//    public void showFloatingActionButton() {
//        if (mHidden) {
//            setVisibility(View.VISIBLE);
//            ObjectAnimator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 0, 1);
//            ObjectAnimator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 0, 1);
//            AnimatorSet animSetXY = new AnimatorSet();
//            animSetXY.playTogether(scaleX, scaleY);
//            animSetXY.setInterpolator(overshootInterpolator);
//            animSetXY.setDuration(200);
//            animSetXY.start();
//            mHidden = false;
//        }
//    }
//
//    public boolean isHidden() {
//        return mHidden;
//    }
//
//    @Override
//    public boolean performClick() {
//        if (!mHidden) {
//            return super.performClick();
//        }
//        return false;
//    }
//
//    public static class Builder {
//        private final Activity activity;
//        int gravity = Gravity.BOTTOM | Gravity.RIGHT; // default bottom right
//        Drawable drawable;
//        int color = Color.WHITE;
//        int size;
//        float scale;
//        boolean hidden;
//        private FrameLayout.LayoutParams params;
//
//        public Builder(Activity context) {
//            scale = context.getResources().getDisplayMetrics().density;
//            size = convertToPixels(72, scale); // default size is 72dp by 72dp
//            params = new FrameLayout.LayoutParams(size, size);
//            params.gravity = gravity;
//
//            activity = context;
//        }
//
//        public Builder hide() {
//            hidden = true;
//            return this;
//        }
//
//        /**
//         * Sets the gravity for the FAB
//         */
//        public Builder withGravity(int gravity) {
//            this.gravity = gravity;
//            return this;
//        }
//
//        /**
//         * Sets the margins for the FAB in dp
//         */
//        public Builder withMargins(int left, int top, int right, int bottom) {
//            params.setMargins(convertToPixels(left, scale), convertToPixels(top, scale), convertToPixels(right, scale), convertToPixels(bottom, scale));
//            return this;
//        }
//
//        /**
//         * Sets the FAB drawable
//         */
//        public Builder withDrawable(Drawable drawable) {
//            this.drawable = drawable;
//            return this;
//        }
//
//        /**
//         * Sets the FAB color
//         */
//        public Builder withButtonColor(int color) {
//            this.color = color;
//            return this;
//        }
//
//        /**
//         * Sets the FAB size in dp
//         */
//        public Builder withButtonSize(int size) {
//            size = convertToPixels(size, scale);
//            params = new FrameLayout.LayoutParams(size, size);
//            return this;
//        }
//
//        public FloatingActionButton create() {
//            FloatingActionButton button = new FloatingActionButton(activity);
//            if (hidden) {
//                button.setScaleX(0);
//                button.setScaleY(0);
//                button.setVisibility(View.GONE);
//                button.mHidden = hidden;
//            }
//            button.setFloatingActionButtonColor(color);
//            button.setFloatingActionButtonDrawable(drawable);
//            params.gravity = gravity;
//            ViewManager root = (ViewManager) activity.findViewById(R.id.content);
//            root.addView(button, params);
//            return button;
//        }
//
//        // The calculation (value * scale + 0.5f) is a widely used to convert to
//        // dps to pixel units
//        // based on density scale
//        // see developer.android.com (Supporting Multiple Screen Sizes)
//        private int convertToPixels(int dp, float scale) {
//            return (int) ((dp * scale) + 0.5f);
//        }
//    }
//}