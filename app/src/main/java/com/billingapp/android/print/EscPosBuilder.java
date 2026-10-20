package com.billingapp.android.print;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Builds raw ESC/POS command bytes understood by most thermal receipt printers
 * (the same command set used by Epson TM series and the many compatible/clone
 * printers sold under other brands). No SDK/library needed - these are just bytes
 * written directly to the Bluetooth socket's output stream.
 *
 * Reference: https://reference.epson-biz.com/modules/ref_escpos/index.php
 */
public class EscPosBuilder {

    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public EscPosBuilder init() {
        write(ESC, 0x40); // ESC @ - initialize printer
        return this;
    }

    public EscPosBuilder alignCenter() {
        write(ESC, 0x61, 0x01); // ESC a 1
        return this;
    }

    public EscPosBuilder alignLeft() {
        write(ESC, 0x61, 0x00); // ESC a 0
        return this;
    }

    public EscPosBuilder bold(boolean on) {
        write(ESC, 0x45, (byte) (on ? 1 : 0)); // ESC E n
        return this;
    }

    public EscPosBuilder doubleHeight(boolean on) {
        // GS ! n - bit 0x10 = double height, 0x20 = double width
        write(GS, 0x21, (byte) (on ? 0x11 : 0x00));
        return this;
    }

    public EscPosBuilder text(String text) {
        try {
            buffer.write(text.getBytes("windows-1252"));
        } catch (UnsupportedEncodingException | IOException e) {
            // windows-1252 is safe for basic Latin text on virtually all thermal printers;
            // fall back to platform default if it's somehow unavailable.
            buffer.write(text.getBytes());
        }
        return this;
    }

    public EscPosBuilder newLine() {
        buffer.write('\n');
        return this;
    }

    public EscPosBuilder line(String text) {
        return text(text).newLine();
    }

    /** A dashed divider sized for a standard 32-column (58mm) thermal printer. */
    public EscPosBuilder divider() {
        return line("--------------------------------");
    }

    public EscPosBuilder feed(int lines) {
        for (int i = 0; i < lines; i++) newLine();
        return this;
    }

    public EscPosBuilder cutPaper() {
        write(GS, 0x56, 0x42, 0x00); // GS V B 0 - partial cut, most common on receipt printers
        return this;
    }

    public byte[] build() {
        return buffer.toByteArray();
    }

    private void write(byte... bytes) {
        buffer.write(bytes, 0, bytes.length);
    }
}
