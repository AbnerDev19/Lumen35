package br.com.lume35.camera.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import br.com.lume35.camera.R;

/** Click advances one detent, long press backs up, keyboard and TalkBack expose both actions. */
final class IndustrialChoice {
    final View view;
    private final TextView value;
    private final String label;
    private final String[] values;
    private int index;

    IndustrialChoice(Activity activity, String label, String[] values, int selected) {
        this.label = label;
        this.values = values;
        index = Math.max(0, Math.min(selected, values.length - 1));
        view = activity.getLayoutInflater().inflate(R.layout.industrial_menu_row, null, false);
        ((TextView) view.findViewById(R.id.industrial_label)).setText(label);
        value = view.findViewById(R.id.industrial_value);
        view.setOnClickListener(v -> step(1));
        view.setOnLongClickListener(v -> { step(-1); return true; });
        view.setOnKeyListener((v, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (code == KeyEvent.KEYCODE_DPAD_LEFT) { step(-1); return true; }
            if (code == KeyEvent.KEYCODE_DPAD_RIGHT) { step(1); return true; }
            return false;
        });
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
            }
            @Override public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.getId()) { step(1); return true; }
                if (action == AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD.getId()) { step(-1); return true; }
                return super.performAccessibilityAction(host, action, args);
            }
        });
        render();
    }

    int getSelectedItemPosition() { return index; }
    boolean isChecked() { return index != 0; }

    private void step(int direction) {
        index = Math.floorMod(index + direction, values.length);
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        render();
    }

    private void render() {
        value.setText(values[index]);
        view.setContentDescription(label);
        if (android.os.Build.VERSION.SDK_INT >= 30) view.setStateDescription(values[index]);
        else view.setContentDescription(label + ", " + values[index]);
    }
}
