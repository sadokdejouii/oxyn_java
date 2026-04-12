package org.example.discussion.ui;

import org.example.entities.ConversationInboxItem;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Modes de tri de la liste des conversations (encadrant / admin).
 */
public enum DiscussionInboxSortMode {

    /** Activité la plus récente en premier. */
    RECENT("Plus récents"),

    /** Conversations où le client a écrit en dernier, puis par récence. */
    UNREAD_FIRST("Non lus en premier");

    private final String label;

    DiscussionInboxSortMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    private static LocalDateTime activityRef(ConversationInboxItem i) {
        if (i.lastMessageAt() != null) {
            return i.lastMessageAt();
        }
        return i.conversationUpdatedAt();
    }

    public Comparator<ConversationInboxItem> comparator() {
        Comparator<ConversationInboxItem> byActivity = Comparator
                .comparing(DiscussionInboxSortMode::activityRef, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<ConversationInboxItem> tie = Comparator.comparingInt(ConversationInboxItem::conversationId).reversed();
        if (this == RECENT) {
            return byActivity.thenComparing(tie);
        }
        Comparator<ConversationInboxItem> unreadFirst = Comparator
                .comparing(ConversationInboxItem::awaitingStaffReply)
                .reversed();
        return unreadFirst.thenComparing(byActivity).thenComparing(tie);
    }
}
