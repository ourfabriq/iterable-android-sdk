package com.iterable.iterableapi;

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class IterableInAppMessage {
    private static final String TAG = "IterableInAppMessage";

    private final @NonNull String messageId;
    private final @NonNull Content content;
    private final @NonNull JSONObject customPayload;
    private final @NonNull Date createdAt;
    private final @NonNull Date expiresAt;
    private final @NonNull Trigger trigger;
    private final @Nullable Boolean saveToInbox;
    private final @Nullable InboxMetadata inboxMetadata;
    private final @Nullable Long campaignId;
    private boolean processed = false;
    private boolean consumed = false;
    private boolean read = false;
    private boolean loadedHtmlFromJson = false;
    private @Nullable IterableInAppStorage inAppStorageInterface;

    IterableInAppMessage(@NonNull String messageId,
                         @NonNull Content content,
                         @NonNull JSONObject customPayload,
                         @NonNull Date createdAt,
                         @NonNull Date expiresAt,
                         @NonNull Trigger trigger,
                         @Nullable Boolean saveToInbox,
                         @Nullable InboxMetadata inboxMetadata,
                         @Nullable Long campaignId) {

        this.messageId = messageId;
        this.content = content;
        this.customPayload = customPayload;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.trigger = trigger;
        this.saveToInbox = saveToInbox;
        this.inboxMetadata = inboxMetadata;
        this.campaignId = campaignId;
    }

    static class Trigger {
        enum TriggerType { IMMEDIATE, EVENT, NEVER }

        final @Nullable JSONObject triggerJson;
        final @NonNull TriggerType type;

        private Trigger(JSONObject triggerJson) {
            this.triggerJson = triggerJson;
            String typeString = triggerJson.optString(IterableConstants.ITERABLE_IN_APP_TRIGGER_TYPE);

            switch (typeString) {
                case "immediate":
                    type = TriggerType.IMMEDIATE;
                    break;
                case "never":
                    type = TriggerType.NEVER;
                    break;
                default:
                    type = TriggerType.NEVER;
            }
        }

        Trigger(@NonNull TriggerType triggerType) {
            triggerJson = null;
            this.type = triggerType;
        }

        @NonNull
        static Trigger fromJSONObject(JSONObject triggerJson) {
            if (triggerJson == null) {
                // Default to 'immediate' if there is no trigger in the payload
                return new Trigger(TriggerType.IMMEDIATE);
            } else {
                return new Trigger(triggerJson);
            }
        }

        @Nullable
        JSONObject toJSONObject() {
            return triggerJson;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Trigger)) {
                return false;
            }
            Trigger trigger = (Trigger) obj;
            return ObjectsCompat.equals(triggerJson, trigger.triggerJson);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(triggerJson);
        }
    }

    public static class Content {
        public String html;
        public final Rect padding;
        public final double backgroundAlpha;
        public boolean shouldAnimate;

        Content(String html, Rect padding, double backgroundAlpha, boolean shouldAnimate) {
            this.html = html;
            this.padding = padding;
            this.backgroundAlpha = backgroundAlpha;
            this.shouldAnimate = shouldAnimate;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Content)) {
                return false;
            }
            Content content = (Content) obj;
            return ObjectsCompat.equals(html, content.html) &&
                    ObjectsCompat.equals(padding, content.padding) &&
                    backgroundAlpha == content.backgroundAlpha;
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(html, padding, backgroundAlpha);
        }
    }

    public static class InboxMetadata {
        public final @Nullable String title;
        public final @Nullable String subtitle;
        public final @Nullable String icon;

        public InboxMetadata(@Nullable String title, @Nullable String subtitle, @Nullable String icon) {
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
        }

        @Nullable
        static InboxMetadata fromJSONObject(@Nullable JSONObject inboxMetadataJson) {
            if (inboxMetadataJson == null) {
                return null;
            }

            String title = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_TITLE);
            String subtitle = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_SUBTITLE);
            String icon = inboxMetadataJson.optString(IterableConstants.ITERABLE_IN_APP_INBOX_ICON);
            return new InboxMetadata(title, subtitle, icon);
        }

        @NonNull
        JSONObject toJSONObject() {
            JSONObject inboxMetadataJson = new JSONObject();
            try {
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_TITLE, title);
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_SUBTITLE, subtitle);
                inboxMetadataJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_ICON, icon);
            } catch (JSONException e) {
                IterableLogger.e(TAG, "Error while serializing inbox metadata", e);
            }
            return inboxMetadataJson;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof InboxMetadata)) {
                return false;
            }
            InboxMetadata inboxMetadata = (InboxMetadata) obj;
            return ObjectsCompat.equals(title, inboxMetadata.title) &&
                    ObjectsCompat.equals(subtitle, inboxMetadata.subtitle) &&
                    ObjectsCompat.equals(icon, inboxMetadata.icon);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(title, subtitle, icon);
        }
    }

    @NonNull
    public String getMessageId() {
        return messageId;
    }

    @Nullable
    public Long getCampaignId() {
        return campaignId;
    }

    @NonNull
    public Date getCreatedAt() {
        return createdAt;
    }

    @NonNull
    Date getExpiresAt() {
        return expiresAt;
    }

    @NonNull
    public Content getContent() {
        if (content.html == null) {
            content.html = inAppStorageInterface.getHTML(messageId);
        }
        return content;
    }

    @NonNull
    public JSONObject getCustomPayload() {
        return customPayload;
    }

    boolean isProcessed() {
        return processed;
    }

    void setProcessed(boolean processed) {
        this.processed = processed;
        onChanged();
    }

    boolean isConsumed() {
        return consumed;
    }

    void setConsumed(boolean consumed) {
        this.consumed = consumed;
        onChanged();
    }

    Trigger.TriggerType getTriggerType() {
        return trigger.type;
    }

    public boolean isInboxMessage() {
        return saveToInbox != null ? saveToInbox : false;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public boolean isSilentInboxMessage() {
        return isInboxMessage() && getTriggerType() == IterableInAppMessage.Trigger.TriggerType.NEVER;
    }

    @Nullable
    public InboxMetadata getInboxMetadata() {
        return inboxMetadata;
    }

    public boolean isRead() {
        return read;
    }

    void setRead(boolean read) {
        this.read = read;
        onChanged();
    }

    boolean hasLoadedHtmlFromJson() {
        return loadedHtmlFromJson;
    }

    void setLoadedHtmlFromJson(boolean loadedHtmlFromJson) {
        this.loadedHtmlFromJson = loadedHtmlFromJson;
    }

    static IterableInAppMessage fromJSONObject(@NonNull JSONObject messageJson, @Nullable IterableInAppStorage storageInterface) {

        if (messageJson == null) {
            return null;
        }

        JSONObject contentJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CONTENT);
        if (contentJson == null) {
            return null;
        }

        String messageId = messageJson.optString(IterableConstants.KEY_MESSAGE_ID);
        final Long campaignId = IterableUtil.retrieveValidCampaignIdOrNull(messageJson, IterableConstants.KEY_CAMPAIGN_ID);
        long createdAtLong = messageJson.optLong(IterableConstants.ITERABLE_IN_APP_CREATED_AT);
        Date createdAt = createdAtLong != 0 ? new Date(createdAtLong) : null;
        long expiresAtLong = messageJson.optLong(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT);
        Date expiresAt = expiresAtLong != 0 ? new Date(expiresAtLong) : null;

        String html = contentJson.optString(IterableConstants.ITERABLE_IN_APP_HTML, null);
        boolean shouldAnimate = contentJson.optBoolean(IterableConstants.ITERABLE_IN_APP_DEFAULT_ANIMATION, true);
        JSONObject paddingOptions = contentJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_DISPLAY_SETTINGS);
        Rect padding = getPaddingFromPayload(paddingOptions);
        double backgroundAlpha = contentJson.optDouble(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ALPHA, 0);

        JSONObject triggerJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_TRIGGER);
        Trigger trigger = Trigger.fromJSONObject(triggerJson);
        JSONObject customPayload = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_CUSTOM_PAYLOAD);
        if (customPayload == null) {
            customPayload = contentJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_LEGACY_PAYLOAD);
        }
        if (customPayload == null) {
            customPayload = new JSONObject();
        }

        Boolean saveToInbox = messageJson.has(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX) ? messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX) : null;
        JSONObject inboxPayloadJson = messageJson.optJSONObject(IterableConstants.ITERABLE_IN_APP_INBOX_METADATA);
        InboxMetadata inboxMetadata = InboxMetadata.fromJSONObject(inboxPayloadJson);

        IterableInAppMessage message = new IterableInAppMessage(
                messageId,
                new Content(html, padding, backgroundAlpha, shouldAnimate),
                customPayload,
                createdAt,
                expiresAt,
                trigger,
                saveToInbox,
                inboxMetadata,
                campaignId);

        message.inAppStorageInterface = storageInterface;
        if (html != null) {
            message.setLoadedHtmlFromJson(true);
        }
        message.processed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_PROCESSED, false);
        message.consumed = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_CONSUMED, false);
        message.read = messageJson.optBoolean(IterableConstants.ITERABLE_IN_APP_READ, false);
        return message;
    }

    @NonNull
    JSONObject toJSONObject() {
        JSONObject messageJson = new JSONObject();
        JSONObject contentJson = new JSONObject();
        try {
            messageJson.putOpt(IterableConstants.KEY_MESSAGE_ID, messageId);
            if (campaignId != null && IterableUtil.isValidCampaignId(campaignId)) {
                messageJson.put(IterableConstants.KEY_CAMPAIGN_ID, campaignId);
            }
            if (createdAt != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CREATED_AT, createdAt.getTime());
            }
            if (expiresAt != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_EXPIRES_AT, expiresAt.getTime());
            }
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_TRIGGER, trigger.toJSONObject());
            contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_DISPLAY_SETTINGS, encodePaddingRectToJson(content.padding));
            if (content.backgroundAlpha != 0) {
                contentJson.putOpt(IterableConstants.ITERABLE_IN_APP_BACKGROUND_ALPHA, content.backgroundAlpha);
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONTENT, contentJson);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CUSTOM_PAYLOAD, customPayload);

            if (saveToInbox != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_SAVE_TO_INBOX, saveToInbox);
            }
            if (inboxMetadata != null) {
                messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_INBOX_METADATA, inboxMetadata.toJSONObject());
            }

            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_PROCESSED, processed);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_CONSUMED, consumed);
            messageJson.putOpt(IterableConstants.ITERABLE_IN_APP_READ, read);
        } catch (JSONException e) {
            IterableLogger.e(TAG, "Error while serializing an in-app message", e);
        }
        return messageJson;
    }

    interface OnChangeListener {
        void onInAppMessageChanged(IterableInAppMessage message);
    }

    private OnChangeListener onChangeListener;

    void setOnChangeListener(OnChangeListener listener) {
        onChangeListener = listener;
    }

    private void onChanged() {
        if (onChangeListener != null) {
            onChangeListener.onInAppMessageChanged(this);
        }
    }

    /**
     * Returns a Rect containing the paddingOptions
     * @param paddingOptions
     * @return
     */
    static Rect getPaddingFromPayload(JSONObject paddingOptions) {
        Rect rect = new Rect();
        rect.top = decodePadding(paddingOptions.optJSONObject("top"));
        rect.left = decodePadding(paddingOptions.optJSONObject("left"));
        rect.bottom = decodePadding(paddingOptions.optJSONObject("bottom"));
        rect.right = decodePadding(paddingOptions.optJSONObject("right"));

        return rect;
    }

    /**
     * Returns a JSONObject containing the encoded padding options
     * @param rect Rect representing the padding options
     * @return JSON object with encoded padding values
     * @throws JSONException
     */
    static JSONObject encodePaddingRectToJson(Rect rect) throws JSONException {
        JSONObject paddingJson = new JSONObject();
        paddingJson.putOpt("top", encodePadding(rect.top));
        paddingJson.putOpt("left", encodePadding(rect.left));
        paddingJson.putOpt("bottom", encodePadding(rect.bottom));
        paddingJson.putOpt("right", encodePadding(rect.right));
        return paddingJson;
    }

    /**
     * Retrieves the padding percentage
     * @discussion -1 is returned when the padding percentage should be auto-sized
     * @param jsonObject
     * @return
     */
    static int decodePadding(JSONObject jsonObject) {
        int returnPadding = 0;
        if (jsonObject != null) {
            if ("AutoExpand".equalsIgnoreCase(jsonObject.optString("displayOption"))) {
                returnPadding = -1;
            } else {
                returnPadding = jsonObject.optInt("percentage", 0);
            }
        }
        return returnPadding;
    }

    /**
     * Encodes the padding percentage to JSON
     * @param padding integer representation of the padding value
     * @return JSON object containing encoded padding data
     * @throws JSONException
     */
    static JSONObject encodePadding(int padding) throws JSONException {
        JSONObject paddingJson = new JSONObject();
        if (padding == -1) {
            paddingJson.putOpt("displayOption", "AutoExpand");
        } else {
            paddingJson.putOpt("percentage", padding);
        }
        return paddingJson;
    }
}
