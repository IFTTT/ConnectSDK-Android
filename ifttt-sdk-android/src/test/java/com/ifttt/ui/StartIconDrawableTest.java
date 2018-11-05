package com.ifttt.ui;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static android.graphics.Color.parseColor;
import static com.google.common.truth.Truth.assertThat;
import static com.ifttt.ui.StartIconDrawable.isDarkColor;

@RunWith(RobolectricTestRunner.class)
public final class StartIconDrawableTest {

    @Test
    public void testIsDarkColor() {
        String[] colors = new String[] {
                "#333333", "#3B579D", "#E4405F", "#0099FF", "#4D4E4D", "#1ED760", "#000000", "#1E2023", "#FFFFFF"
        };

        assertThat(isDarkColor(parseColor(colors[0]))).isTrue();
        assertThat(isDarkColor(parseColor(colors[1]))).isFalse();
        assertThat(isDarkColor(parseColor(colors[2]))).isFalse();
        assertThat(isDarkColor(parseColor(colors[3]))).isFalse();
        assertThat(isDarkColor(parseColor(colors[4]))).isFalse();
        assertThat(isDarkColor(parseColor(colors[5]))).isFalse();
        assertThat(isDarkColor(parseColor(colors[6]))).isTrue();
        assertThat(isDarkColor(parseColor(colors[7]))).isTrue();
        assertThat(isDarkColor(parseColor(colors[8]))).isFalse();
    }
}
