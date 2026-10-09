package com.smartsplit.ui;

import java.awt.Color;

/** Centralised palette for SmartSplit. Swap here to retheme the whole app. */
public final class AppColors {

    // Brand
    public static final Color PRIMARY        = new Color(0x5C6BC0); // indigo
    public static final Color PRIMARY_DARK   = new Color(0x3949AB);
    public static final Color PRIMARY_LIGHT  = new Color(0x9FA8DA);
    public static final Color ACCENT         = new Color(0x26A69A); // teal

    // Backgrounds
    public static final Color BG_APP         = new Color(0xF5F5F5);
    public static final Color BG_CARD        = Color.WHITE;
    public static final Color BG_SIDEBAR     = new Color(0x3D4466);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(0x212121);
    public static final Color TEXT_SECONDARY = new Color(0x757575);
    public static final Color TEXT_ON_DARK   = Color.WHITE;

    // Semantic
    public static final Color SUCCESS        = new Color(0x43A047);
    public static final Color ERROR          = new Color(0xE53935);
    public static final Color WARNING        = new Color(0xFB8C00);

    // UI chrome
    public static final Color BORDER         = new Color(0xE0E0E0);
    public static final Color INPUT_BG       = Color.WHITE;
    public static final Color HOVER          = new Color(0xEEEEEE);

    private AppColors() {}
}
