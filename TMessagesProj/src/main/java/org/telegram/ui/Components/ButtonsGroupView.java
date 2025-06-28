package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public class ButtonsGroupView extends FrameLayout {

    public static class Item {
        private int id;
        private int icon;
        private CharSequence text;
        private ButtonsGroupView parent;
        private ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout;
        private ActionBarPopupWindow popupWindow;
        private boolean hasSubMenu = false;
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

        private void createPopupLayout() {
            if (popupLayout != null) {
                return;
            }
            popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(parent.getContext(), 
                R.drawable.popup_fixed_alert2, resourcesProvider, 
                ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK);
            
            popupLayout.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (popupWindow != null && popupWindow.isShowing()) {
                        popupWindow.dismiss();
                    }
                }
                return false;
            });
            
            popupLayout.setDispatchKeyEventListener(keyEvent -> {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                }
            });
        }

        public ActionBarMenuSubItem addSubItem(int id, int icon, CharSequence text) {
            return addSubItem(id, icon, null, text, true, false);
        }

        public ActionBarMenuSubItem addSubItem(int id, int icon, Drawable iconDrawable, CharSequence text, boolean dismiss, boolean needCheck) {
            createPopupLayout();
            hasSubMenu = true;

            ActionBarMenuSubItem cell = new ActionBarMenuSubItem(parent.getContext(), needCheck, false, false, resourcesProvider);
            cell.setTextAndIcon(text, icon, iconDrawable);
            cell.setMinimumWidth(AndroidUtilities.dp(196));
            cell.setTag(id);
            popupLayout.addView(cell);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) cell.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new LinearLayout.LayoutParams(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(48));
            } else {
                layoutParams.width = LayoutHelper.MATCH_PARENT;
                layoutParams.height = AndroidUtilities.dp(48);
            }
            cell.setLayoutParams(layoutParams);
            
            cell.setOnClickListener(view -> {
                if (popupWindow != null && popupWindow.isShowing()) {
                    if (dismiss) {
                        popupWindow.dismiss();
                    }
                }
                if (parent.onSubItemClickListener != null) {
                    parent.onSubItemClickListener.onSubItemClick(this.id, (Integer) view.getTag());
                }
            });
            return cell;
        }

        public void removeAllSubItems() {
            if (popupLayout == null) {
                return;
            }
            popupLayout.removeInnerViews();
            hasSubMenu = false;
        }

        public boolean hasSubMenu() {
            return hasSubMenu && popupLayout != null;
        }

        public void showSubMenu() {
            if (!hasSubMenu() || parent == null) {
                return;
            }

            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
                return;
            }

            if (popupLayout.getParent() != null) {
                ((ViewGroup) popupLayout.getParent()).removeView(popupLayout);
            }

            popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            popupWindow.setAnimationStyle(R.style.PopupAnimation);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setClippingEnabled(true);
            popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
            popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
            
            popupLayout.setFocusableInTouchMode(true);
            popupLayout.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_MENU && event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_UP && popupWindow != null && popupWindow.isShowing()) {
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            });

            popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.displaySize.x - AndroidUtilities.dp(40), View.MeasureSpec.AT_MOST), 
                               View.MeasureSpec.makeMeasureSpec(AndroidUtilities.displaySize.y, View.MeasureSpec.AT_MOST));
            
            popupWindow.setFocusable(true);
            
            int[] location = new int[2];
            parent.getLocationOnScreen(location);
            int buttonIndex = parent.getItemIndex(this);
            if (buttonIndex >= 0) {
                int buttonX = parent.getButtonX(buttonIndex);
                int buttonWidth = parent.getButtonWidth(buttonIndex);
                
                int popupX = location[0] + buttonX + (buttonWidth - popupLayout.getMeasuredWidth()) / 2;
                int popupY = location[1] - popupLayout.getMeasuredHeight() - AndroidUtilities.dp(8);
                
                if (popupX < AndroidUtilities.dp(16)) {
                    popupX = AndroidUtilities.dp(16);
                } else if (popupX + popupLayout.getMeasuredWidth() > AndroidUtilities.displaySize.x - AndroidUtilities.dp(16)) {
                    popupX = AndroidUtilities.displaySize.x - AndroidUtilities.dp(16) - popupLayout.getMeasuredWidth();
                }
                
                if (popupY < AndroidUtilities.dp(16)) {
                    popupY = location[1] + parent.getMeasuredHeight() + AndroidUtilities.dp(8);
                }
                
                popupWindow.showAtLocation(parent, Gravity.LEFT | Gravity.TOP, popupX, popupY);
            } else {
                popupWindow.showAsDropDown(parent);
            }
            
            popupWindow.startAnimation();
        }

        public void hideSubMenu() {
            if (popupWindow != null && popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int id);
    }

    public interface OnSubItemClickListener {
        void onSubItemClick(int itemId, int subItemId);
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
    private OnSubItemClickListener onSubItemClickListener;
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

    public void setOnSubItemClickListener(OnSubItemClickListener listener) {
        this.onSubItemClickListener = listener;
    }

    public void onItemClick(int id) {
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(id);
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
                    
                    if (item.hasSubMenu()) {
                        item.showSubMenu();
                    } else {
                        performClick();
                    }
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
            if (item != null && !item.hasSubMenu()) {
                onItemClick(item.getId());
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
            clickedButtonIndex = -1;
            return true;
        }

        return false;
    }

    public void hideAllPopupMenus() {
        for (Item item : buttons) {
            item.hideSubMenu();
        }
    }
}