package me.yowal.updatehelper.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.Scroller;
import androidx.appcompat.widget.LinearLayoutCompat;

import java.lang.reflect.Field;
import java.util.List;

public class IOSOverScrollView extends LinearLayoutCompat implements OnTouchListener {

    static final int ANIMATED_SCROLL_GAP = 250;

    static final float MAX_SCROLL_FACTOR = 0.5f;
    static final float OVERSHOOT_TENSION = 0.75f;

    private long mLastScroll;

    private final Rect mTempRect = new Rect();
    private Scroller mScroller;

    protected Context mContext;

    Field mScrollYField;
    Field mScrollXField;

    boolean hasFailedObtainingScrollFields;
    int prevScrollY;
    boolean isInFlingMode = false;

    DisplayMetrics metrics;
    protected View child;

    private Runnable overScrollerSpringbackTask;

    private boolean mScrollViewMovedFocus;

    private float mLastMotionY;

    private boolean mIsLayoutDirty = true;

    private View mChildToScrollTo = null;

    private boolean mIsBeingDragged = false;

    private VelocityTracker mVelocityTracker;

    private boolean mFillViewport;

    private boolean mSmoothScrollingEnabled = true;

    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;

    private int mActivePointerId = INVALID_POINTER;

    private static final int INVALID_POINTER = -1;

    public IOSOverScrollView(Context context)
    {
        this(context, null);
        mContext = context;

        initScrollView();
        setFillViewport(true);
        initBounce();
    }

    public IOSOverScrollView(Context context, AttributeSet attrs)
    {

        this(context, attrs, 0);
        mContext = context;

        initScrollView();
        setFillViewport(true);
        initBounce();
    }

    public IOSOverScrollView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        mContext = context;

        initScrollView();
        setFillViewport(true);
        initBounce();
    }

    private void initBounce()
    {
        metrics = this.mContext.getResources().getDisplayMetrics();
        mScroller = new Scroller(getContext(), new OvershootInterpolator(OVERSHOOT_TENSION));
        overScrollerSpringbackTask = new Runnable()
        {
            @Override
            public void run()
            {
                mScroller.computeScrollOffset();
                scrollTo(0, mScroller.getCurrY());

                if (!mScroller.isFinished())
                {
                    post(this);
                }
            }
        };
        prevScrollY = getPaddingTop();
    }

    public void initChildPointer()
    {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        int height = wm.getDefaultDisplay().getHeight() + 50;
        child = getChildAt(0);
        child.setPadding(getPaddingLeft(), height, getPaddingRight(), height);
    }

    @Override
    protected float getTopFadingEdgeStrength()
    {
        if (getChildCount() == 0)
        {
            return 0.0f;
        }

        final int length = getVerticalFadingEdgeLength();
        if (getScrollY() < length)
        {
            return getScrollY() / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength()
    {
        if (getChildCount() == 0)
        {
            return 0.0f;
        }

        final int length = getVerticalFadingEdgeLength();
        final int bottomEdge = getHeight() - getPaddingBottom();
        final int span = getChildAt(0).getBottom() - getScrollY() - bottomEdge;
        if (span < length)
        {
            return span / (float) length;
        }

        return 1.0f;
    }

    public int getMaxScrollAmount()
    {
        return (int) (MAX_SCROLL_FACTOR * (getBottom() - getTop()));
    }

    private void initScrollView()
    {
        mScroller = new Scroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(mContext);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        setOnTouchListener(this);

        post(() -> scrollTo(0, child.getPaddingTop()));
    }

    @Override
    public void addView(View child)
    {
        if (getChildCount() > 0)  {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child);
        initChildPointer();
    }

    @Override
    public void addView(View child, int index)
    {
        if (getChildCount() > 0)
        {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index);
        initChildPointer();
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params)
    {
        if (getChildCount() > 0)
        {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, params);
        initChildPointer();
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        if (getChildCount() > 0)
        {
            throw new IllegalStateException("ScrollView can host only one direct child");
        }

        super.addView(child, index, params);
    }

    private boolean canScroll()
    {
        View child = getChildAt(0);
        if (child != null)
        {
            int childHeight = child.getHeight();
            return getHeight() < childHeight + getPaddingTop() + getPaddingBottom();
        }
        return false;
    }

    public boolean isFillViewport()
    {
        return mFillViewport;
    }

    public void setFillViewport(boolean fillViewport)
    {
        if (fillViewport != mFillViewport)
        {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }

    public boolean isSmoothScrollingEnabled()
    {
        return mSmoothScrollingEnabled;
    }

    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled)
    {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mFillViewport)
        {
            return;
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED)
        {
            return;
        }

        if (getChildCount() > 0)
        {
            final View child = getChildAt(0);
            int height = getMeasuredHeight();
            if (child.getMeasuredHeight() < height)
            {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);
                height -= getPaddingTop();
                height -= getPaddingBottom();
                int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    public boolean executeKeyEvent(KeyEvent event)
    {
        mTempRect.setEmpty();

        if (!canScroll())
        {
            if (isFocused() && event.getKeyCode() != KeyEvent.KEYCODE_BACK)
            {
                View currentFocused = findFocus();
                if (currentFocused == this)
                    currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, View.FOCUS_DOWN);
                return nextFocused != null && nextFocused != this && nextFocused.requestFocus(View.FOCUS_DOWN);
            }
            return false;
        }

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN)
        {
            switch (event.getKeyCode())
            {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!event.isAltPressed())
                    {
                        handled = arrowScroll(View.FOCUS_UP);
                    } else
                    {
                        handled = fullScroll(View.FOCUS_UP);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (!event.isAltPressed())
                    {
                        handled = arrowScroll(View.FOCUS_DOWN);
                    } else
                    {
                        handled = fullScroll(View.FOCUS_DOWN);
                    }
                    break;
                case KeyEvent.KEYCODE_SPACE:
                    pageScroll(event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
                    break;
            }
        }

        return handled;
    }

    public boolean inChild(int x, int y)
    {
        if (getChildCount() > 0)
        {
            final int scrollY = getScrollY();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY || y >= child.getBottom() - scrollY || x < child.getLeft() || x >= child.getRight());
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged))
        {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_MOVE:
            {
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER)
                {
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                final float y = ev.getY(pointerIndex);
                final int yDiff = (int) Math.abs(y - mLastMotionY);
                if (yDiff > mTouchSlop)
                {
                    mIsBeingDragged = true;
                    mLastMotionY = y;
                }
                break;
            }

            case MotionEvent.ACTION_DOWN:
            {
                final float y = ev.getY();
                if (!inChild((int) ev.getX(), (int) y))
                {
                    mIsBeingDragged = false;
                    break;
                }

                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);

                mIsBeingDragged = !mScroller.isFinished();
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {

        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0)
        {
            return false;
        }

        if (mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_DOWN:
            {
                final float y = ev.getY();
                if (!(mIsBeingDragged = inChild((int) ev.getX(), (int) y)))
                {
                    return false;
                }

                if (!mScroller.isFinished())
                {
                    mScroller.abortAnimation();
                }

                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (mIsBeingDragged)
                {
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float y = ev.getY(activePointerIndex);
                    final int deltaY = (int) (mLastMotionY - y);
                    mLastMotionY = y;

                    if (isOverScrolled())
                    {
                        scrollBy(0, deltaY / 2);
                    } else
                    {
                        scrollBy(0, deltaY);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged)
                {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                    //Toast.makeText(mContext, initialVelocity + "", Toast.LENGTH_SHORT).show();
                    if (getChildCount() > 0 && Math.abs(initialVelocity) > mMinimumVelocity)
                    {
                        fling(-initialVelocity);
                    }

                    mActivePointerId = INVALID_POINTER;
                    mIsBeingDragged = false;

                    if (mVelocityTracker != null)
                    {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0)
                {
                    mActivePointerId = INVALID_POINTER;
                    mIsBeingDragged = false;
                    if (mVelocityTracker != null)
                    {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        return true;
    }

    public boolean isOverScrolled()
    {
        return (getScrollY() < child.getPaddingTop() || getScrollY() > child.getBottom() - child.getPaddingBottom() - getHeight());
    }

    private void onSecondaryPointerUp(MotionEvent ev)
    {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId)
        {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null)
            {
                mVelocityTracker.clear();
            }
        }
    }

    private View findFocusableViewInMyBounds(final boolean topFocus, final int top, View preferredFocusable)
    {
        /*
         * The fading edge's transparent side should be considered for focus
         * since it's mostly visible, so we divide the actual fading edge length
         * by 2.
         */
        final int fadingEdgeLength = getVerticalFadingEdgeLength() / 2;
        final int topWithoutFadingEdge = top + fadingEdgeLength;
        final int bottomWithoutFadingEdge = top + getHeight() - fadingEdgeLength;

        if ((preferredFocusable != null) && (preferredFocusable.getTop() < bottomWithoutFadingEdge)
                && (preferredFocusable.getBottom() > topWithoutFadingEdge))
        {
            return preferredFocusable;
        }

        return findFocusableViewInBounds(topFocus, topWithoutFadingEdge, bottomWithoutFadingEdge);
    }

    private View findFocusableViewInBounds(boolean topFocus, int top, int bottom)
    {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        boolean foundFullyContainedFocusable = false;

        int count = focusables.size();
        for (int i = 0; i < count; i++)
        {
            View view = focusables.get(i);
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();

            if (top < viewBottom && viewTop < bottom)
            {
                final boolean viewIsFullyContained = (top < viewTop) && (viewBottom < bottom);

                if (focusCandidate == null)
                {
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else
                {
                    final boolean viewIsCloserToBoundary = (topFocus && viewTop < focusCandidate.getTop())
                            || (!topFocus && viewBottom > focusCandidate.getBottom());

                    if (foundFullyContainedFocusable)
                    {
                        if (viewIsFullyContained && viewIsCloserToBoundary)
                        {
                            focusCandidate = view;
                        }
                    } else
                    {
                        if (viewIsFullyContained)
                        {
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary)
                        {
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }

    public boolean pageScroll(int direction)
    {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        if (down)
        {
            mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0)
            {
                View view = getChildAt(count - 1);
                if (mTempRect.top + height > view.getBottom())
                {
                    mTempRect.top = view.getBottom() - height;
                }
            }
        } else
        {
            mTempRect.top = getScrollY() - height;
            if (mTempRect.top < 0)
            {
                mTempRect.top = 0;
            }
        }
        mTempRect.bottom = mTempRect.top + height;

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    public boolean fullScroll(int direction)
    {
        boolean down = direction == View.FOCUS_DOWN;
        int height = getHeight();

        mTempRect.top = 0;
        mTempRect.bottom = height;

        if (down)
        {
            int count = getChildCount();
            if (count > 0)
            {
                View view = getChildAt(count - 1);
                mTempRect.bottom = view.getBottom();
                mTempRect.top = mTempRect.bottom - height;
            }
        }

        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom);
    }

    private boolean scrollAndFocus(int direction, int top, int bottom)
    {
        boolean handled = true;

        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = direction == View.FOCUS_UP;

        View newFocused = findFocusableViewInBounds(up, top, bottom);
        if (newFocused == null)
        {
            newFocused = this;
        }

        if (top >= containerTop && bottom <= containerBottom)
        {
            handled = false;
        } else
        {
            int delta = up ? (top - containerTop) : (bottom - containerBottom);
            doScrollY(delta);
        }

        if (newFocused != findFocus() && newFocused.requestFocus(direction))
        {
            mScrollViewMovedFocus = true;
            mScrollViewMovedFocus = false;
        }

        return handled;
    }

    public boolean arrowScroll(int direction)
    {

        View currentFocused = findFocus();
        if (currentFocused == this)
            currentFocused = null;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

        final int maxJump = getMaxScrollAmount();

        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJump, getHeight()))
        {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollY(scrollDelta);
            nextFocused.requestFocus(direction);
        } else
        {
            int scrollDelta = maxJump;

            if (direction == View.FOCUS_UP && getScrollY() < scrollDelta)
            {
                scrollDelta = getScrollY();
            } else if (direction == View.FOCUS_DOWN)
            {
                if (getChildCount() > 0)
                {
                    int daBottom = getChildAt(0).getBottom();

                    int screenBottom = getScrollY() + getHeight();

                    if (daBottom - screenBottom < maxJump)
                    {
                        scrollDelta = daBottom - screenBottom;
                    }
                }
            }
            if (scrollDelta == 0)
            {
                return false;
            }
            doScrollY(direction == View.FOCUS_DOWN ? scrollDelta : -scrollDelta);
        }

        if (currentFocused != null && currentFocused.isFocused() && isOffScreen(currentFocused))
        {
            final int descendantFocusability = getDescendantFocusability();
            setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            requestFocus();
            setDescendantFocusability(descendantFocusability);
        }
        return true;
    }

    private boolean isOffScreen(View descendant)
    {
        return !isWithinDeltaOfScreen(descendant, 0, getHeight());
    }

    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height)
    {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);

        return (mTempRect.bottom + delta) >= getScrollY() && (mTempRect.top - delta) <= (getScrollY() + height);
    }

    private void doScrollY(int delta)
    {
        if (delta != 0)
        {
            if (mSmoothScrollingEnabled)
            {
                smoothScrollBy(0, delta);
            } else
            {
                scrollBy(0, delta);
            }
        }
    }

    public final void smoothScrollBy(int dx, int dy)
    {
        if (getChildCount() == 0)
        {
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP)
        {
            final int height = getHeight() - getPaddingBottom() - getPaddingTop();
            final int bottom = getChildAt(0).getHeight();
            final int maxY = Math.max(0, bottom - height);
            final int scrollY = getScrollY();
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;

            mScroller.startScroll(getScrollX(), scrollY, 0, dy);
            invalidate();
        } else
        {
            if (!mScroller.isFinished())
            {
                mScroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    public final void smoothScrollToTop()
    {
        smoothScrollTo(0, child.getPaddingTop());
    }

    public final void smoothScrollToBottom()
    {
        smoothScrollTo(0, child.getHeight() - child.getPaddingTop() - getHeight());
    }

    public final void smoothScrollTo(int x, int y)
    {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    @Override
    protected int computeVerticalScrollRange()
    {
        final int count = getChildCount();
        final int contentHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0)
        {
            return contentHeight;
        }

        return getChildAt(0).getBottom();
    }

    @Override
    protected int computeVerticalScrollOffset()
    {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec)
    {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec;
        int childHeightMeasureSpec;

        childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight(), lp.width);

        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec,
                                           int heightUsed)
    {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp.leftMargin
                + lp.rightMargin + widthUsed, lp.width);
        final int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll()
    {
        if (hasFailedObtainingScrollFields)
        {
            super.computeScroll();
            return;
        }

        if (mScroller.computeScrollOffset())
        {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (getChildCount() > 0)
            {
                View child = getChildAt(0);
                x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
                y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
                if (x != oldX || y != oldY)
                {
                    setScrollX(x);
                    setScrollY(y);
                    onScrollChanged(x, y, oldX, oldY);
                }
            }
            awakenScrollBars();
            postInvalidate();
        }
    }

    private void scrollToChild(View child)
    {
        child.getDrawingRect(mTempRect);

        offsetDescendantRectToMyCoords(child, mTempRect);

        int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);

        if (scrollDelta != 0)
        {
            scrollBy(0, scrollDelta);
        }
    }

    private boolean scrollToChildRect(Rect rect, boolean immediate)
    {
        final int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        final boolean scroll = delta != 0;
        if (scroll)
        {
            if (immediate)
            {
                scrollBy(0, delta);
            } else
            {
                smoothScrollBy(0, delta);
            }
        }
        return scroll;
    }

    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect)
    {
        if (getChildCount() == 0)
            return 0;

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;

        int fadingEdge = getVerticalFadingEdgeLength();

        if (rect.top > 0)
        {
            screenTop += fadingEdge;
        }

        if (rect.bottom < getChildAt(0).getHeight())
        {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop)
        {
            if (rect.height() > height)
            {
                scrollYDelta += (rect.top - screenTop);
            } else
            {
                scrollYDelta += (rect.bottom - screenBottom);
            }

            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.top < screenTop && rect.bottom < screenBottom)
        {
            if (rect.height() > height)
            {
                scrollYDelta -= (screenBottom - rect.bottom);
            } else
            {
                scrollYDelta -= (screenTop - rect.top);
            }

            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }

    @Override
    public void requestChildFocus(View child, View focused)
    {
        if (!mScrollViewMovedFocus)
        {
            if (!mIsLayoutDirty)
            {
                scrollToChild(focused);
            } else
            {
                mChildToScrollTo = focused;
            }
        }
        super.requestChildFocus(child, focused);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect)
    {
        if (direction == View.FOCUS_FORWARD)
        {
            direction = View.FOCUS_DOWN;
        } else if (direction == View.FOCUS_BACKWARD)
        {
            direction = View.FOCUS_UP;
        }

        final View nextFocus = previouslyFocusedRect == null ? FocusFinder.getInstance().findNextFocus(this, null, direction) : FocusFinder
                .getInstance().findNextFocusFromRect(this, previouslyFocusedRect, direction);

        if (nextFocus == null)
        {
            return false;
        }

        if (isOffScreen(nextFocus))
        {
            return false;
        }

        return nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate)
    {
        rectangle.offset(child.getLeft() - child.getScrollX(), child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    @Override
    public void requestLayout()
    {
        mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this))
        {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;
        scrollTo(getScrollX(), getScrollY());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);

        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused)
            return;
        if (isWithinDeltaOfScreen(currentFocused, 0, oldh))
        {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect);
            doScrollY(scrollDelta);
        }
    }

    @Override
    protected void onScrollChanged(int leftOfVisibleView, int topOfVisibleView, int oldLeftOfVisibleView, int oldTopOfVisibleView)
    {
        int displayHeight = getHeight();
        int paddingTop = child.getPaddingTop();
        int contentBottom = child.getHeight() - child.getPaddingBottom();

        if (isInFlingMode)
        {

            if (topOfVisibleView < paddingTop || topOfVisibleView > contentBottom - displayHeight)
            {
                if (topOfVisibleView < paddingTop)
                {
                    mScroller.startScroll(0, topOfVisibleView, 0, paddingTop - topOfVisibleView, 1000);
                } else if (topOfVisibleView > contentBottom - displayHeight)
                {
                    mScroller.startScroll(0, topOfVisibleView, 0, contentBottom - displayHeight - topOfVisibleView, 1000);
                }
                post(overScrollerSpringbackTask);
                isInFlingMode = false;
                return;

            }
        }
        super.onScrollChanged(leftOfVisibleView, topOfVisibleView, oldLeftOfVisibleView, oldTopOfVisibleView);
    }

    private boolean isViewDescendantOf(View child, View parent)
    {
        if (child == parent)
        {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return (theParent instanceof ViewGroup) && isViewDescendantOf((View) theParent, parent);
    }

    public void fling(int velocityY)
    {


        if (getChildCount() > 0)
        {
            int height = getHeight() - getPaddingBottom() - getPaddingTop();
            int bottom = getChildAt(0).getHeight();

            mScroller.fling(getScrollX(), getScrollY(), 0, velocityY, 0, 0, 0, Math.max(0, bottom - height));

            final boolean movingDown = velocityY > 0;

            View newFocused = findFocusableViewInMyBounds(movingDown, mScroller.getFinalY(), findFocus());
            if (newFocused == null)
            {
                newFocused = this;
            }

            if (newFocused != findFocus() && newFocused.requestFocus(movingDown ? View.FOCUS_DOWN : View.FOCUS_UP))
            {
                mScrollViewMovedFocus = true;
                mScrollViewMovedFocus = false;
            }

            invalidate();
        }
    }

    @Override
    public void scrollTo(int x, int y)
    {
        if (getChildCount() > 0)
        {
            View child = getChildAt(0);
            x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
            y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
            if (x != getScrollX() || y != getScrollY())
            {
                super.scrollTo(x, y);
            }
        }
    }

    private int clamp(int n, int my, int child)
    {
        if (my >= child || n < 0)
        {
            return 0;
        }
        if ((my + n) > child)
        {
            return child - my;
        }
        return n;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        mScroller.forceFinished(true);
        removeCallbacks(overScrollerSpringbackTask);

        if (event.getAction() == MotionEvent.ACTION_UP)
        {
            return overScrollView();
        }

        else if (event.getAction() == MotionEvent.ACTION_CANCEL)
        {
            return overScrollView();
        }

        return false;
    }

    private boolean overScrollView()
    {
        int displayHeight = getHeight();
        int contentTop = child.getPaddingTop();
        int contentBottom = child.getHeight() - child.getPaddingBottom();
        int currScrollY = getScrollY();

        int scrollBy;

        if (currScrollY < contentTop)
        {

            onOverScroll(currScrollY);
            scrollBy = contentTop - currScrollY;
        } else if (currScrollY + displayHeight > contentBottom)
        {
            if (child.getHeight() - child.getPaddingTop() - child.getPaddingBottom() < displayHeight)
            {

                scrollBy = contentTop - currScrollY;
            }
            else
            {
                scrollBy = contentBottom - displayHeight - currScrollY;
            }
            scrollBy += onOverScroll(currScrollY);
        }
        else
        {
            isInFlingMode = true;
            return false;
        }
        mScroller.startScroll(0, currScrollY, 0, scrollBy, 500);
        post(overScrollerSpringbackTask);
        prevScrollY = currScrollY;
        return true;

    }

    protected int onOverScroll(int scrollY)
    {
        return 0;
    }

}
