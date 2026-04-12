package org.example.utils;

import java.util.*;

public class EmojiUtil {
    
    private static final String[] POPULAR_EMOJIS = {
        "👍", "❤️", "😂", "😍", "🤔", "😎", "🔥", "✨",
        "👏", "🎉", "🚀", "💯", "🌟", "😊", "😢", "😡",
        "🤗", "😴", "🤮", "🤢", "😈", "👿", "💪", "✋",
        "👋", "🙌", "🤝", "👌", "🤨", "🧐", "😏", "😌"
    };

    private static final String[] ALL_EMOJIS = {
        // Smileys
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
        "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
        "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜",
        "🤪", "😌", "😔", "😑", "😐", "😏", "😒", "🙁",
        "☹️", "🥺", "😲", "😞", "😖", "😢", "😭", "😤",
        "😠", "😡", "🤬", "😈", "👿", "💀", "☠️", "💩",
        "🤡", "👹", "👺", "👻", "👽", "👾", "🤖", "😺",
        
        // Hand gestures
        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
        "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👍", "👎",
        "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲",
        "🤝", "🤜", "🤛", "🦾", "🦿", "👈", "👉", "👆",
        
        // Hearts & Love
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤎",
        "🤍", "🤎", "💔", "💕", "💞", "💓", "💗", "💖",
        "💘", "💝", "💟", "💜", "💚", "💙", "💛", "🧡",
        
        // Celebration
        "🎉", "🎊", "🎈", "🎆", "🎇", "🧨", "✨", "🌟",
        "⭐", "🌠", "🎃", "🎄", "🎀", "🎁", "🏆", "🥇",
        "🥈", "🥉", "⚽", "⚾", "🥎", "🎾", "🏐", "🏈",
        
        // Objects
        "🔥", "💧", "💎", "🔔", "🔕", "🎵", "🎶", "📱",
        "💻", "⌨️", "🖥️", "🖨️", "🖱️", "🖲️", "🕹️", "🗜️",
        "💽", "💾", "💿", "📀", "🧮", "🎥", "🎬", "📺",
        "📷", "📸", "📹", "🎙️", "🎚️", "🎛️", "🧭", "⏱️",
        
        // Nature & Animals
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
        "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵",
        "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤",
        "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄"
    };

    /**
     * Get popular emojis for quick access
     */
    public static String[] getPopularEmojis() {
        return POPULAR_EMOJIS.clone();
    }

    /**
     * Get all available emojis
     */
    public static String[] getAllEmojis() {
        return ALL_EMOJIS.clone();
    }

    /**
     * Get emojis organized by category
     */
    public static Map<String, String[]> getEmojisPerCategory() {
        Map<String, String[]> categories = new LinkedHashMap<>();
        
        categories.put("Popular", POPULAR_EMOJIS);
        categories.put("Smileys", new String[]{
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
            "😘", "😗", "😚", "😙", "🥲", "😋", "😛", "😜"
        });
        categories.put("Hands", new String[]{
            "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
            "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👍", "👎"
        });
        categories.put("Hearts", new String[]{
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤎",
            "🤍", "🤎", "💔", "💕", "💞", "💓", "💗", "💖"
        });
        categories.put("Celebration", new String[]{
            "🎉", "🎊", "🎈", "🎆", "🎇", "🧨", "✨", "🌟",
            "⭐", "🌠", "🎃", "🎄", "🎀", "🎁", "🏆", "🥇"
        });
        categories.put("Animals", new String[]{
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵"
        });
        
        return categories;
    }
}
