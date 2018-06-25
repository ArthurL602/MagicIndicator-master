package net.lucode.hackware.magicindicator;

import android.util.SparseArray;
import android.util.SparseBooleanArray;

/**
 * 方便扩展IPagerNavigator的帮助类，将ViewPager的3个回调方法转换成
 * onSelected、onDeselected、onEnter等回调，方便扩展
 * 博客: http://hackware.lucode.net
 * Created by hackware on 2016/6/26.
 */
public class NavigatorHelper {
    private SparseBooleanArray mDeselectedItems = new SparseBooleanArray();
    private SparseArray<Float> mLeavedPercents = new SparseArray<Float>();

    private int mTotalCount;
    private int mCurrentIndex;
    private int mLastIndex;
    private float mLastPositionOffsetSum;
    private int mScrollState;

    private boolean mSkimOver;
    private NavigatorHelper.OnNavigatorScrollListener mNavigatorScrollListener;

    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        float currentPositionOffsetSum = position + positionOffset;
        boolean leftToRight = false;
        // 判断滑动方向
        if (mLastPositionOffsetSum <= currentPositionOffsetSum) {
            leftToRight = true;
        }
        // 不是滑动停止状态 state != 0，滑动的时候控制的是离开状态
        if (mScrollState != ScrollState.SCROLL_STATE_IDLE) {
            if (currentPositionOffsetSum == mLastPositionOffsetSum) {
                return;
            }
            int nextPosition = position + 1;
            boolean normalDispatch = true;
            if (positionOffset == 0.0f) {//positionOffset==0.0 意味着滑动也停止了或者位置没变化
                if (leftToRight) { // 如果往左滑动的，此时position已经是下一个位置了，nextPosition成了滑动前的那个位置
                    nextPosition = position - 1;
                    normalDispatch = false;
                }
            }
            // 分发离开状态，这里的分发控制的是非当前位置和下一个位置的其他位置
            for (int i = 0; i < mTotalCount; i++) {
                // 如果是当前 位置 或者是 下一个位置，则跳过当前循环
                if (i == position || i == nextPosition) {
                    continue;
                }
                Float leavedPercent = mLeavedPercents.get(i, 0.0f);
                if (leavedPercent != 1.0f) {
                    dispatchOnLeave(i, 1.0f, leftToRight, true);
                }
            }
            // 正常分发
            if (normalDispatch) {
                if (leftToRight) { // 往左滑动
                    dispatchOnLeave(position, positionOffset, true, false);
                    dispatchOnEnter(nextPosition, positionOffset, true, false);
                } else {
                    dispatchOnLeave(nextPosition, 1.0f - positionOffset, false, false);
                    dispatchOnEnter(position, 1.0f - positionOffset, false, false);
                }
            } else {
                dispatchOnLeave(nextPosition, 1.0f - positionOffset, true, false);
                dispatchOnEnter(position, 1.0f - positionOffset, true, false);
            }
        } else { // state == 0 , 滑动停止了， 其时的mCurrentIndextion 是滑动到的当前位置
            for (int i = 0; i < mTotalCount; i++) {
                if (i == mCurrentIndex) {
                    continue;
                }
                boolean deselected = mDeselectedItems.get(i);
                if (!deselected) { //分发没有选择
                    dispatchOnDeselected(i);
                }
                Float leavedPercent = mLeavedPercents.get(i, 0.0f);
                if (leavedPercent != 1.0f) { // 分发离开 离开状态 为 1.0f
                    dispatchOnLeave(i, 1.0f, false, true);
                }
            }
            // 分发进入
            dispatchOnEnter(mCurrentIndex, 1.0f, false, true);
            // 分发选择
            dispatchOnSelected(mCurrentIndex);
        }
        mLastPositionOffsetSum = currentPositionOffsetSum;
    }

    private void dispatchOnEnter(int index, float enterPercent, boolean leftToRight, boolean force) {
        if (mSkimOver || index == mCurrentIndex || mScrollState == ScrollState.SCROLL_STATE_DRAGGING || force) {
            if (mNavigatorScrollListener != null) {
                mNavigatorScrollListener.onEnter(index, mTotalCount, enterPercent, leftToRight);
            }
            mLeavedPercents.put(index, 1.0f - enterPercent);
        }
    }

    private void dispatchOnLeave(int index, float leavePercent, boolean leftToRight, boolean force) {
        if (mSkimOver || index == mLastIndex || mScrollState == ScrollState.SCROLL_STATE_DRAGGING || ((index == mCurrentIndex - 1 || index == mCurrentIndex + 1) && mLeavedPercents.get(index, 0.0f) != 1.0f) || force) {
            if (mNavigatorScrollListener != null) {
                mNavigatorScrollListener.onLeave(index, mTotalCount, leavePercent, leftToRight);
            }
            mLeavedPercents.put(index, leavePercent);
        }
    }

    private void dispatchOnSelected(int index) {
        if (mNavigatorScrollListener != null) {
            mNavigatorScrollListener.onSelected(index, mTotalCount);
        }
        mDeselectedItems.put(index, false);
    }

    private void dispatchOnDeselected(int index) {
        if (mNavigatorScrollListener != null) {
            mNavigatorScrollListener.onDeselected(index, mTotalCount);
        }
        mDeselectedItems.put(index, true);
    }

    public void onPageSelected(int position) {
        mLastIndex = mCurrentIndex;
        mCurrentIndex = position;
        dispatchOnSelected(mCurrentIndex);
        for (int i = 0; i < mTotalCount; i++) {
            if (i == mCurrentIndex) {
                continue;
            }
            boolean deselected = mDeselectedItems.get(i);
            if (!deselected) {
                dispatchOnDeselected(i);
            }
        }
    }

    public void onPageScrollStateChanged(int state) {
        mScrollState = state;
    }

    public void setNavigatorScrollListener(NavigatorHelper.OnNavigatorScrollListener navigatorScrollListener) {
        mNavigatorScrollListener = navigatorScrollListener;
    }

    public void setSkimOver(boolean skimOver) {
        mSkimOver = skimOver;
    }

    public int getTotalCount() {
        return mTotalCount;
    }

    public void setTotalCount(int totalCount) {
        mTotalCount = totalCount;
        mDeselectedItems.clear();
        mLeavedPercents.clear();
    }

    public int getCurrentIndex() {
        return mCurrentIndex;
    }

    public int getScrollState() {
        return mScrollState;
    }

    public interface OnNavigatorScrollListener {
        void onEnter(int index, int totalCount, float enterPercent, boolean leftToRight);

        void onLeave(int index, int totalCount, float leavePercent, boolean leftToRight);

        void onSelected(int index, int totalCount);

        void onDeselected(int index, int totalCount);
    }
}
