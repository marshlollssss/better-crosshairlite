package dev.crosshairlite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A Valorant-style crosshair (the "Primary" crosshair): center dot, inner lines, outer lines,
 * outlines, and movement/firing error. Reads and writes Valorant profile codes such as
 * {@code 0;P;c;5;h;0;0l;4;0o;2;0a;1;0f;0;1b;0}.
 *
 * <p>No Minecraft dependencies, so it can be unit-tested on its own. All sizes are in physical
 * screen pixels, exactly like Valorant's sliders.
 */
public final class ValorantProfile {
    public static final int[] PRESET_RGB = {
        0xFFFFFF, 0x00FF00, 0x7FFF00, 0xDFFF00, 0xFFFF00, 0x00FFFF, 0xFF00FF, 0xFF0000
    };
    public static final List<String> PRESET_NAMES = List.of(
        "White", "Green", "Yellow Green", "Green Yellow", "Yellow", "Cyan", "Pink", "Red", "Custom");

    /** One group of four lines (inner or outer). */
    public static final class Lines {
        public boolean show = true;
        public int thickness = 2, length = 6, lengthV = 6, offset = 3;
        /** Valorant's "separate horizontal / vertical length" switch. */
        public boolean separate = false;
        public double opacity = 0.8;
        public boolean moveError = false, fireError = false;
        public double moveMult = 1, fireMult = 1;

        public int horizontal() { return length; }
        public int vertical() { return separate ? lengthV : length; }

        void clamp() {
            thickness = Math.max(0, Math.min(12, thickness));
            length = Math.max(0, Math.min(40, length));
            lengthV = Math.max(0, Math.min(40, lengthV));
            offset = Math.max(0, Math.min(60, offset));
            opacity = Math.max(0, Math.min(1, opacity));
            moveMult = Math.max(0, Math.min(5, moveMult));
            fireMult = Math.max(0, Math.min(5, fireMult));
        }
    }

    /** A rectangle in pixels relative to the screen center. {@code outline} rects are drawn first. */
    public record Rect(int x1, int y1, int x2, int y2, float alpha, boolean outline) {}

    /** 0-7 are the presets in {@link #PRESET_RGB}; 8 means {@link #customRgb}. */
    public int color = 5;
    public int customRgb = 0xFFFFFF;
    public boolean outlines = false;
    public double outlineOpacity = 0.5;
    public int outlineThickness = 1;
    public boolean centerDot = false;
    public double dotOpacity = 1;
    public int dotThickness = 2;
    public boolean fade = false;
    public boolean overrideFiringOffset = false;
    public boolean overrideAll = false;
    public Lines inner = new Lines();
    public Lines outer = new Lines();

    /** Parts of an imported code that this mod does not use, kept so export round-trips. */
    public String preamble = "";
    public String tail = "";
    public Map<String, String> unknown = new LinkedHashMap<>();

    /** The mod's starting crosshair: a small cyan cross, no outlines, no error. */
    public ValorantProfile() {
        inner.length = 4; inner.lengthV = 4; inner.offset = 2; inner.opacity = 1;
        outer.show = false;
        outer.length = 2; outer.lengthV = 2; outer.offset = 10; outer.opacity = 0.35;
        outer.moveError = true; outer.fireError = true;
    }

    /**
     * A profile holding Valorant's own defaults for any value a code leaves out. Used while
     * parsing, so a missing key means what it means in the game.
     */
    static ValorantProfile blank() {
        ValorantProfile p = new ValorantProfile();
        p.color = 0;
        p.outlines = true; p.outlineOpacity = 0.5; p.outlineThickness = 1;
        p.fade = false; p.overrideAll = true;
        p.inner.show = true; p.inner.thickness = 2; p.inner.length = 6; p.inner.lengthV = 6;
        p.inner.offset = 3; p.inner.opacity = 0.8; p.inner.moveError = false; p.inner.fireError = true;
        p.outer.show = true; p.outer.thickness = 2; p.outer.length = 2; p.outer.lengthV = 2;
        p.outer.offset = 10; p.outer.opacity = 0.35; p.outer.moveError = true; p.outer.fireError = true;
        return p;
    }

    public ValorantProfile copy() {
        ValorantProfile p;
        try { p = fromCode(toCode()); } catch (IllegalArgumentException e) { return new ValorantProfile(); }
        return p;
    }

    public int rgb() {
        return color >= 0 && color < PRESET_RGB.length ? PRESET_RGB[color] : customRgb & 0xFFFFFF;
    }

    public void sanitize() {
        color = Math.max(0, Math.min(8, color));
        customRgb &= 0xFFFFFF;
        outlineOpacity = Math.max(0, Math.min(1, outlineOpacity));
        outlineThickness = Math.max(0, Math.min(12, outlineThickness));
        dotOpacity = Math.max(0, Math.min(1, dotOpacity));
        dotThickness = Math.max(0, Math.min(12, dotThickness));
        if (inner == null) inner = new Lines();
        if (outer == null) outer = new Lines();
        if (preamble == null) preamble = "";
        if (tail == null) tail = "";
        if (unknown == null) unknown = new LinkedHashMap<>();
        inner.clamp();
        outer.clamp();
    }

    // ------------------------------------------------------------------ layout

    /**
     * Builds the crosshair as pixel rectangles around (0,0).
     *
     * @param move   movement error, 0..1
     * @param fire   firing error, 0..1
     * @param movePx pixels a full movement error pushes the lines outward (before the multiplier)
     * @param firePx pixels a full firing error pushes the lines outward (before the multiplier)
     */
    public List<Rect> layout(double move, double fire, double movePx, double firePx) {
        move = Math.max(0, Math.min(1, move));
        fire = Math.max(0, Math.min(1, fire));
        float fadeFactor = fade ? (float) (1 - 0.7 * fire) : 1f;
        List<Rect> fills = new ArrayList<>(9);

        if (centerDot && dotThickness > 0 && dotOpacity > 0) {
            int lo = -(dotThickness / 2);
            fills.add(new Rect(lo, lo, lo + dotThickness, lo + dotThickness, (float) dotOpacity * fadeFactor, false));
        }
        addLines(fills, inner, move, fire, movePx, firePx, fadeFactor);
        addLines(fills, outer, move, fire, movePx, firePx, fadeFactor);

        List<Rect> result = new ArrayList<>(fills.size() * 2);
        if (outlines && outlineThickness > 0 && outlineOpacity > 0) {
            int o = outlineThickness;
            for (Rect r : fills)
                result.add(new Rect(r.x1 - o, r.y1 - o, r.x2 + o, r.y2 + o, (float) outlineOpacity * fadeFactor, true));
        }
        result.addAll(fills);
        return result;
    }

    private static void addLines(List<Rect> out, Lines g, double move, double fire, double movePx, double firePx, float fadeFactor) {
        if (!g.show || g.thickness <= 0 || g.opacity <= 0) return;
        double spread = (g.moveError ? movePx * move * g.moveMult : 0) + (g.fireError ? firePx * fire * g.fireMult : 0);
        int off = g.offset + (int) Math.round(spread);
        int t = g.thickness, lo = -(t / 2), hi = lo + t;
        float a = (float) g.opacity * fadeFactor;
        int h = g.horizontal(), v = g.vertical();
        if (h > 0) {
            out.add(new Rect(off, lo, off + h, hi, a, false));
            out.add(new Rect(-off - h, lo, -off, hi, a, false));
        }
        if (v > 0) {
            out.add(new Rect(lo, -off - v, hi, -off, a, false));
            out.add(new Rect(lo, off, hi, off + v, a, false));
        }
    }

    // ------------------------------------------------------------------ codes

    /** Parses a Valorant profile code. Only the Primary section is used; others are kept for export. */
    public static ValorantProfile fromCode(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Empty crosshair code");
        String[] t = code.strip().split(";", -1);
        ValorantProfile p = blank();
        List<String> pre = new ArrayList<>(), tail = new ArrayList<>();
        int i = (t[0].isEmpty() || t[0].matches("\\d+")) ? 1 : 0;
        char section = 0;
        boolean sawPrimary = false;
        Map<String, Boolean> seen = new LinkedHashMap<>();
        while (i < t.length) {
            String k = t[i];
            if (isSection(k)) {
                section = k.charAt(0);
                if (section == 'P') sawPrimary = true; else tail.add(k);
                i++;
                continue;
            }
            if (k.isEmpty()) { i++; continue; }
            String v = i + 1 < t.length ? t[i + 1] : null;
            if (v == null || isSection(v)) { i++; continue; }
            i += 2;
            if (section == 0) { pre.add(k); pre.add(v); }
            else if (section == 'P') { applyPrimary(p, k, v); seen.put(k, true); }
            else { tail.add(k); tail.add(v); }
        }
        if (!sawPrimary) throw new IllegalArgumentException("No primary crosshair found in code");
        // A vertical length that differs from the horizontal one implies the split switch.
        for (int g = 0; g < 2; g++) {
            Lines l = g == 0 ? p.inner : p.outer;
            if (!seen.containsKey(g + "g") && seen.containsKey(g + "v") && l.lengthV != l.length) l.separate = true;
        }
        p.preamble = String.join(";", pre);
        p.tail = String.join(";", tail);
        p.sanitize();
        return p;
    }

    private static boolean isSection(String s) { return s.equals("P") || s.equals("A") || s.equals("S"); }

    private static void applyPrimary(ValorantProfile p, String k, String v) {
        if (k.length() == 2 && (k.charAt(0) == '0' || k.charAt(0) == '1') && Character.isLetter(k.charAt(1))) {
            Lines g = k.charAt(0) == '0' ? p.inner : p.outer;
            switch (k.charAt(1)) {
                case 'b' -> g.show = bool(v, g.show);
                case 't' -> g.thickness = (int) Math.round(num(v, g.thickness));
                case 'l' -> g.length = (int) Math.round(num(v, g.length));
                case 'v' -> g.lengthV = (int) Math.round(num(v, g.lengthV));
                case 'g' -> g.separate = bool(v, g.separate);
                case 'o' -> g.offset = (int) Math.round(num(v, g.offset));
                case 'a' -> g.opacity = num(v, g.opacity);
                case 'm' -> g.moveError = bool(v, g.moveError);
                case 's' -> g.moveMult = num(v, g.moveMult);
                case 'f' -> g.fireError = bool(v, g.fireError);
                case 'e' -> g.fireMult = num(v, g.fireMult);
                default -> p.unknown.put(k, v);
            }
            return;
        }
        switch (k) {
            case "c" -> p.color = (int) Math.round(num(v, p.color));
            case "u" -> {
                if (v.matches("[0-9a-fA-F]{6}([0-9a-fA-F]{2})?")) p.customRgb = Integer.parseInt(v.substring(0, 6), 16);
            }
            case "h" -> p.outlines = bool(v, p.outlines);
            case "o" -> p.outlineOpacity = num(v, p.outlineOpacity);
            case "t" -> p.outlineThickness = (int) Math.round(num(v, p.outlineThickness));
            case "d" -> p.centerDot = bool(v, p.centerDot);
            case "z" -> p.dotThickness = (int) Math.round(num(v, p.dotThickness));
            case "a" -> p.dotOpacity = num(v, p.dotOpacity);
            case "f" -> p.fade = bool(v, p.fade);
            case "m" -> p.overrideFiringOffset = bool(v, p.overrideFiringOffset);
            case "s" -> p.overrideAll = bool(v, p.overrideAll);
            default -> p.unknown.put(k, v);
        }
    }

    private static double num(String v, double fallback) {
        try { return Double.parseDouble(v.strip()); } catch (NumberFormatException e) { return fallback; }
    }

    private static boolean bool(String v, boolean fallback) {
        try { return Double.parseDouble(v.strip()) != 0; } catch (NumberFormatException e) { return fallback; }
    }

    /** Writes a complete Valorant profile code for this crosshair. */
    public String toCode() {
        sanitize();
        List<String> o = new ArrayList<>();
        o.add("0");
        if (!preamble.isEmpty()) o.add(preamble);
        o.add("P");
        add(o, "c", color);
        if (color == 8) add(o, "u", String.format(Locale.ROOT, "%06XFF", customRgb & 0xFFFFFF));
        add(o, "h", outlines ? 1 : 0);
        add(o, "o", fmt(outlineOpacity));
        add(o, "t", outlineThickness);
        add(o, "d", centerDot ? 1 : 0);
        add(o, "z", dotThickness);
        add(o, "a", fmt(dotOpacity));
        add(o, "f", fade ? 1 : 0);
        add(o, "m", overrideFiringOffset ? 1 : 0);
        add(o, "s", overrideAll ? 1 : 0);
        for (var e : unknown.entrySet()) add(o, e.getKey(), e.getValue());
        writeLines(o, "0", inner);
        writeLines(o, "1", outer);
        if (!tail.isEmpty()) o.add(tail);
        return String.join(";", o);
    }

    private static void writeLines(List<String> o, String prefix, Lines g) {
        add(o, prefix + "b", g.show ? 1 : 0);
        if (!g.show) return;
        add(o, prefix + "t", g.thickness);
        add(o, prefix + "l", g.length);
        if (g.separate) { add(o, prefix + "v", g.lengthV); add(o, prefix + "g", 1); }
        add(o, prefix + "o", g.offset);
        add(o, prefix + "a", fmt(g.opacity));
        add(o, prefix + "m", g.moveError ? 1 : 0);
        add(o, prefix + "s", fmt(g.moveMult));
        add(o, prefix + "f", g.fireError ? 1 : 0);
        add(o, prefix + "e", fmt(g.fireMult));
    }

    private static void add(List<String> o, String k, Object v) { o.add(k); o.add(String.valueOf(v)); }

    static String fmt(double d) {
        String s = String.format(Locale.ROOT, "%.3f", d);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
