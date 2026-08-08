package com.nachiket.connectra.utility;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.nachiket.connectra.R;

/**
 * Central helper for the system bars.
 *
 * <p>Since the app targets Android 15+ (SDK 35/36) edge-to-edge is enforced and
 * {@link Window#setStatusBarColor(int)} is a no-op, which left the status bar transparent with
 * light (white) clock/battery icons on top of the white app background. This helper paints a
 * scrim behind the status bar, keeps the icon colour in sync with that scrim and pads the
 * activity content so nothing is hidden underneath the system bars.
 */
public final class SystemBarsUtil {

    private SystemBarsUtil() {
    }

    /**
     * Applies {@code statusBarColor} to the status bar of {@code activity} and makes the status
     * bar icons dark or light depending on how bright that colour is.
     */
    public static void apply(@NonNull Activity activity, @ColorInt int statusBarColor) {
        apply(activity, statusBarColor, true);
    }

    /**
     * @param applyInsetPadding when {@code true} the activity content is padded by the system bar
     *                          (and keyboard) insets, so it is never covered by them.
     */
    public static void apply(@NonNull Activity activity, @ColorInt int statusBarColor,
                             boolean applyInsetPadding) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }

        WindowCompat.setDecorFitsSystemWindows(window, false);
        // Ignored on Android 15+, still needed on older versions.
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
        }

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(isLight(statusBarColor));

        View scrim = installScrim(window, statusBarColor);
        listenForInsets(activity, scrim, applyInsetPadding);
    }

    private static View installScrim(@NonNull Window window, @ColorInt int color) {
        ViewGroup decor = (ViewGroup) window.getDecorView();
        View scrim = decor.findViewById(R.id.status_bar_scrim);
        if (scrim == null) {
            scrim = new View(decor.getContext());
            scrim.setId(R.id.status_bar_scrim);
            decor.addView(scrim, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP));
        }
        scrim.setBackgroundColor(color);
        return scrim;
    }

    private static void listenForInsets(@NonNull Activity activity, @NonNull View scrim,
                                        boolean applyInsetPadding) {
        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            resizeScrim(scrim, insets);
            if (applyInsetPadding) {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private static void resizeScrim(@NonNull View scrim, @NonNull WindowInsetsCompat insets) {
        int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        ViewGroup.LayoutParams params = scrim.getLayoutParams();
        if (params.height != top) {
            params.height = top;
            scrim.setLayoutParams(params);
        }
    }

    private static boolean isLight(@ColorInt int color) {
        return ColorUtils.calculateLuminance(color) > 0.5d;
    }
}
