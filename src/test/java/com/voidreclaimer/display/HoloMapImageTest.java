package com.voidreclaimer.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HoloMapImageTest {
    @Test
    void aPictureIsWrittenRowByRow() {
        byte[] image = HoloMapImage.compose((x, y) -> x == 3 && y == 2 ? 7 : 0);
        assertEquals(HoloMapImage.SIZE * HoloMapImage.SIZE, image.length);
        assertEquals(7, image[2 * HoloMapImage.SIZE + 3]);
    }

    @Test
    void theMiddleOfTheMapIsTheProjector() {
        assertEquals(HoloMapImage.SIZE / 2, HoloMapImage.pixel(0, 64));
        assertEquals(1, HoloMapImage.offset(HoloMapImage.SIZE / 2, 64));
    }

    @Test
    void offsetsAndPixelsAgree() {
        int radius = 64;
        for (int pixel = 0; pixel < HoloMapImage.SIZE; pixel++) {
            assertEquals(pixel, HoloMapImage.pixel(HoloMapImage.offset(pixel, radius), radius), "pixel " + pixel);
        }
    }

    @Test
    void aBlockLandsOnItsOwnPixelOnBothSidesOfTheMiddle() {
        // Rounding towards zero would put everything west and north of the middle one block off. Only up to this radius does
        // every block have a pixel of its own: past it a pixel stands for several blocks and shows one of them.
        for (int radius : new int[] {16, 32}) {
            for (int block = -radius; block < radius; block++) {
                assertEquals(block, HoloMapImage.offset(HoloMapImage.pixel(block, radius), radius), "block " + block + " radius " + radius);
            }
        }
    }

    @Test
    void offTheMapIsNoPixel() {
        assertEquals(-1, HoloMapImage.pixel(-65, 64));
        assertEquals(-1, HoloMapImage.pixel(64, 64));
        assertEquals(0, HoloMapImage.pixel(-64, 64));
    }
}
