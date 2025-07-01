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
        int currentHeight = (int) (maxButtonHeight * progressToExpand);
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

        int buttonWidth = (getMeasuredWidth() - (buttonCount - 1) * AndroidUtilities.dp(8) - 2 * AndroidUtilities.dp(12)) / buttonCount;

        int buttonHeight = (int) (AndroidUtilities.dp(56) * progressToExpand);
        int buttonTop = 0;

        backgroundPaint.setAlpha((int) (255 * baseBackgroundAlpha * progressToExpand));

        int availableContentHeight = Math.max(0, buttonHeight - 2 * AndroidUtilities.dp(8));
        int textSize = (int) (AndroidUtilities.dp(11) * progressToExpand);
        int iconSize = Math.min(AndroidUtilities.dp(24), Math.max(0, availableContentHeight - textSize - (textSize > 0 ? AndroidUtilities.dp(4) : 0)));

        textPaint.setAlpha((int) (255 * progressToExpand));
        textPaint.setTextSize(textSize);
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();

        int currentX = AndroidUtilities.dp(12);
        for (int i = 0; i < buttonCount && i < buttons.size(); i++) {
            Item item = buttons.get(i);
            if (item == null) {
                currentX += buttonWidth + AndroidUtilities.dp(8);
                continue;
            }

            buttonRect.set(currentX, buttonTop, currentX + buttonWidth, buttonTop + buttonHeight);
            canvas.drawRoundRect(buttonRect, AndroidUtilities.dp(10), AndroidUtilities.dp(10), backgroundPaint);

            boolean isPressed = i == pressedButtonIndex;
            int baseAlpha = (int) (255 * progressToExpand);
            int currentIconAlpha = isPressed ? (int) (baseAlpha * disabledContentAlpha) : baseAlpha;
            int currentTextAlpha = isPressed ? (int) (baseAlpha * disabledContentAlpha) : baseAlpha;

            if (getContext() != null && availableContentHeight > 0) {
                int contentOffsetY = Math.max(0, (availableContentHeight - (iconSize + (iconSize > 0 && textSize > 0 ? AndroidUtilities.dp(4) : 0) + 
                    (textSize > 0 ? (int) (fontMetrics.descent - fontMetrics.ascent) : 0))) / 2);
                
                if (iconSize > 0) {
                    Drawable icon = ContextCompat.getDrawable(getContext(), item.getIcon());
                    if (icon != null) {
                        icon.setAlpha(currentIconAlpha);
                        icon.setBounds(currentX + (buttonWidth - iconSize) / 2, AndroidUtilities.dp(8) + contentOffsetY, 
                            currentX + (buttonWidth + iconSize) / 2, AndroidUtilities.dp(8) + contentOffsetY + iconSize);
                        icon.draw(canvas);
                    }
                }

                if (textSize > 0 && item.getText() != null && !item.getText().toString().isEmpty()) {
                    textPaint.setAlpha(currentTextAlpha);
                    canvas.drawText(item.getText().toString(), currentX + buttonWidth / 2f, 
                        AndroidUtilities.dp(8) + contentOffsetY + iconSize + (iconSize > 0 ? AndroidUtilities.dp(4) : 0) + (int) (-fontMetrics.ascent), textPaint);
                }
            }

            currentX += buttonWidth + AndroidUtilities.dp(8);
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
        int buttonHeight = (int) (maxButtonHeight * progressToExpand);
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