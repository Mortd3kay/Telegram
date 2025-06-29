package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class ButtonsGroupView extends FrameLayout {

    public static class Item {
        private int id;
        private int icon;
        private CharSequence text;
        private ButtonsGroupView parent;
        private final Theme.ResourcesProvider resourcesProvider;

        public Item(int id, int icon, ButtonsGroupView parent) {
            this.id = id;
            this.icon = icon;
            this.parent = parent;
            this.resourcesProvider = parent.resourcesProvider;
        }

        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public void setIcon(int icon) {
            this.icon = icon;
            if (parent != null) {
                parent.invalidate();
            }
        }

        public CharSequence getText() {
            return text;
        }

        public void setText(CharSequence text) {
            this.text = text;
            if (parent != null) {
                parent.invalidate();
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int id, float x, float y);
    }

    private static final int MAX_BUTTONS = 5;
    private final ArrayList<Item> buttons = new ArrayList<>();
    private float progressToExpand;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF buttonRect = new RectF();
    private float baseBackgroundAlpha = 0.10f;
    private float disabledContentAlpha = 0.5f;
    private int pressedButtonIndex = -1;
    private int clickedButtonIndex = -1;
    private OnItemClickListener onItemClickListener;
    private Theme.ResourcesProvider resourcesProvider;

    public ButtonsGroupView(Context context) {
        this(context, null);
    }

    public ButtonsGroupView(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setWillNotDraw(false);
        backgroundPaint.setColor(Color.BLACK);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        progressToExpand = 1.0f;
    }

    public Item addItem(int id, int icon) {
        if (buttons.size() >= MAX_BUTTONS) {
            return null;
        }

        Item item = new Item(id, icon, this);
        buttons.add(item);
        requestLayout();
        invalidate();
        return item;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void onItemClick(View view, int id, float x, float y) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(view, id, x, y);
        }
    }

    int getItemIndex(Item item) {
        return buttons.indexOf(item);
    }

    int getButtonX(int index) {
        if (index < 0 || index >= buttons.size()) {
            return 0;
        }
        
        int buttonCount = getVisibleButtonCount();
        int width = getMeasuredWidth();
        int buttonSpacing = AndroidUtilities.dp(8);
        int sideMargin = AndroidUtilities.dp(12);
        int totalSpacing = (buttonCount - 1) * buttonSpacing + 2 * sideMargin;
        int buttonWidth = (width - totalSpacing) / buttonCount;
        
        return sideMargin + index * (buttonWidth + buttonSpacing);
    }

    int getButtonWidth(int index) {
        int buttonCount = getVisibleButtonCount();
        int width = getMeasuredWidth();
        int buttonSpacing = AndroidUtilities.dp(8);
        int sideMargin = AndroidUtilities.dp(12);
        int totalSpacing = (buttonCount - 1) * buttonSpacing + 2 * sideMargin;
        return (width - totalSpacing) / buttonCount;
    }

    public void clearButtons() {
        buttons.clear();
    }

    public void setProgressToExpand(float progress) {
        if (progressToExpand == progress) {
            return;
        }
        progressToExpand = Math.max(0f, Math.min(1f, progress));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int buttonCount = getVisibleButtonCount();

        if (buttonCount == 0) {
            setMeasuredDimension(width, 0);
            return;
        }

        int maxButtonHeight = AndroidUtilities.dp(56);
        int minButtonHeight = AndroidUtilities.dp(20);
        int currentHeight = (int) (minButtonHeight + (maxButtonHeight - minButtonHeight) * progressToExpand);
        setMeasuredDimension(width, currentHeight);
    }

    private int getVisibleButtonCount() {
        return buttons.size();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int buttonCount = getVisibleButtonCount();
        if (buttonCount == 0 || progressToExpand <= 0) {
            return;
        }

        int width = getMeasuredWidth();

        int buttonSpacing = AndroidUtilities.dp(8);
        int sideMargin = AndroidUtilities.dp(12);
        int totalSpacing = (buttonCount - 1) * buttonSpacing + 2 * sideMargin;
        int buttonWidth = (width - totalSpacing) / buttonCount;

        int maxButtonHeight = AndroidUtilities.dp(56);
        int minButtonHeight = AndroidUtilities.dp(20);
        int buttonHeight = (int) (minButtonHeight + (maxButtonHeight - minButtonHeight) * progressToExpand);

        int buttonTop = 0;

        int backgroundAlpha = (int) (255 * baseBackgroundAlpha * progressToExpand);
        backgroundPaint.setAlpha(backgroundAlpha);

        int edgePadding = AndroidUtilities.dp(8);
        float iconScale = progressToExpand;
        float textScale = (float) Math.sqrt(progressToExpand);
        int iconAlpha = (int) (255 * iconScale);
        int textAlpha = (int) (255 * textScale);
        int baseIconSize = AndroidUtilities.dp(24);
        int baseTextSize = AndroidUtilities.dp(11);
        int iconSize = (int) (baseIconSize * iconScale);
        int textSize = (int) (baseTextSize * textScale);

        textPaint.setAlpha(textAlpha);
        textPaint.setTextSize(textSize);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        int textBaseY = (int) (buttonTop + buttonHeight - edgePadding - fontMetrics.descent);

        int currentX = sideMargin;
        for (int i = 0; i < buttonCount && i < buttons.size(); i++) {
            Item item = buttons.get(i);
            if (item == null) {
                currentX += buttonWidth + buttonSpacing;
                continue;
            }

            buttonRect.set(currentX, buttonTop, currentX + buttonWidth, buttonTop + buttonHeight);
            canvas.drawRoundRect(buttonRect, AndroidUtilities.dp(10), AndroidUtilities.dp(10), backgroundPaint);

            boolean isPressed = i == pressedButtonIndex;
            int currentIconAlpha = isPressed ? (int) (iconAlpha * disabledContentAlpha) : iconAlpha;
            int currentTextAlpha = isPressed ? (int) (textAlpha * disabledContentAlpha) : textAlpha;

            if (getContext() != null) {
                Drawable icon = ContextCompat.getDrawable(getContext(), item.getIcon());
                if (icon != null) {
                    icon.setAlpha(currentIconAlpha);
                    int iconLeft = currentX + (buttonWidth - iconSize) / 2;
                    int iconTop = buttonTop + edgePadding;
                    icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
                    icon.draw(canvas);
                }

                if (item.getText() != null && !item.getText().toString().isEmpty()) {
                    textPaint.setAlpha(currentTextAlpha);
                    canvas.drawText(item.getText().toString(), currentX + buttonWidth / 2f, textBaseY, textPaint);
                }
            }

            currentX += buttonWidth + buttonSpacing;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null || progressToExpand <= 0) {
            return super.onTouchEvent(event);
        }

        int action = event.getAction();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
            return super.onTouchEvent(event);
        }

        float x = event.getX();
        float y = event.getY();

        int buttonCount = getVisibleButtonCount();
        if (buttonCount == 0) {
            return super.onTouchEvent(event);
        }

        int buttonSpacing = AndroidUtilities.dp(8);
        int sideMargin = AndroidUtilities.dp(12);
        int totalSpacing = (buttonCount - 1) * buttonSpacing + 2 * sideMargin;
        int buttonWidth = (getMeasuredWidth() - totalSpacing) / buttonCount;

        int maxButtonHeight = AndroidUtilities.dp(56);
        int minButtonHeight = AndroidUtilities.dp(20);
        int buttonHeight = (int) (minButtonHeight + (maxButtonHeight - minButtonHeight) * progressToExpand);
        int buttonTop = 0;

        int currentX = sideMargin;
        int buttonIndex = 0;

        for (int i = 0; i < buttons.size() && buttonIndex < buttonCount; i++) {
            Item item = buttons.get(i);
            if (item == null) {
                currentX += buttonWidth + buttonSpacing;
                buttonIndex++;
                continue;
            }

            if (x >= currentX && x <= currentX + buttonWidth &&
                    y >= buttonTop && y <= buttonTop + buttonHeight) {

                if (action == MotionEvent.ACTION_DOWN) {
                    pressedButtonIndex = i;
                    invalidate();
                    return true;
                } else if (action == MotionEvent.ACTION_UP && pressedButtonIndex == i) {
                    pressedButtonIndex = -1;
                    clickedButtonIndex = i;
                    
                    performClick();
                    invalidate();
                    return true;
                }
            }

            currentX += buttonWidth + buttonSpacing;
            buttonIndex++;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (pressedButtonIndex != -1) {
                pressedButtonIndex = -1;
                invalidate();
            }
        }

        return super.onTouchEvent(event);
    }

    public void setBackgroundAlpha(float alpha) {
        baseBackgroundAlpha = Math.max(0f, Math.min(1f, alpha));
        invalidate();
    }

    public void setBackgroundColor(int color) {
        backgroundPaint.setColor(color);
        invalidate();
    }

    public void setContentColor(int color) {
        textPaint.setColor(color);
        invalidate();
    }

    public void setDisabledContentAlpha(float alpha) {
        disabledContentAlpha = Math.max(0f, Math.min(1f, alpha));
    }

    @Override
    public boolean performClick() {
        super.performClick();

        if (clickedButtonIndex >= 0 && clickedButtonIndex < buttons.size()) {
            Item item = buttons.get(clickedButtonIndex);
            if (item != null) {
                float clickX = getButtonX(clickedButtonIndex) + getButtonWidth(clickedButtonIndex) / 2f;
                float clickY = getMeasuredHeight() / 2f;
                onItemClick(this, item.getId(), clickX, clickY);
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
            clickedButtonIndex = -1;
            return true;
        }

        return false;
    }
}