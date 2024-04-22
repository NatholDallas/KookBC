/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.entity.channel;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.Message;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.impl.pageiter.TextChannelMessageIterator;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.get;

public class TextChannelImpl extends NonCategoryChannelImpl implements TextChannel {
    private int chatLimitTime;
    private String topic;

    public TextChannelImpl(
            KBCClient client,
            String id,
            String masterId,
            String guildId,
            boolean permSync,
            Category parent,
            String name,
            Collection<RolePermissionOverwrite> rpo,
            Collection<UserPermissionOverwrite> upo,
            int level,
            int chatLimitTime,
            String topic
    ) {
        super(client, id, masterId, guildId, permSync, parent, name, rpo, upo, level);
        this.chatLimitTime = chatLimitTime;
        this.topic = topic;
    }

    @Override
    public String getTopic() {
        if (completed) init();
        return topic;
    }

    @Override
    public void setTopic(String topic) {
        if (completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("topic", topic)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setTopic0(topic);
    }

    public void setTopic0(String topic) {
        if (completed) init();
        this.topic = topic;
    }

    @Override
    public PageIterator<Collection<TextChannelMessage>> getMessages(@Nullable String refer, boolean isPin, String queryMode) {
        if (completed) init();
        Validate.isTrue(Objects.equals(queryMode, "before") || Objects.equals(queryMode, "around") || Objects.equals(queryMode, "after"), "Invalid queryMode");
        return new TextChannelMessageIterator(client, this, refer, isPin, queryMode);
    }

    @Override
    public String sendComponent(String message) {
        if (completed) init();
        return sendComponent(new MarkdownComponent(message));
    }

    @Override
    public String sendComponent(String message, @Nullable TextChannelMessage quote, @Nullable User tempTarget) {
        if (completed) init();
        return sendComponent(new MarkdownComponent(message), quote, tempTarget);
    }

    @Override
    public String sendComponent(BaseComponent baseComponent) {
        if (completed) init();
        return sendComponent(baseComponent, null, null);
    }

    @Override
    public String sendComponent(BaseComponent component, @Nullable TextChannelMessage quote, @Nullable User tempTarget) {
        if (completed) init();
        Object[] result = MessageBuilder.serialize(component);
        Map<String, Object> body = new MapBuilder()
                .put("target_id", getId())
                .put("type", result[0])
                .put("content", result[1])
                .putIfNotNull("quote", quote, Message::getId)
                .putIfNotNull("temp_target_id", tempTarget, User::getId)
                .build();
        try {
            return client.getNetworkClient().post(HttpAPIRoute.CHANNEL_MESSAGE_SEND.toFullURL(), body).get("msg_id").getAsString();
        } catch (BadResponseException e) {
            if ("资源不存在".equals(e.getRawMessage())) {
                // 2023/1/17: special case for the resources that aren't created by Bots.
                // Thanks: Edint386@Github
                throw new IllegalArgumentException("Unable to send component. Is the resource created by Bot?", e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public int getChatLimitTime() {
        if (completed) init();
        return chatLimitTime;
    }

    @Override
    public void setChatLimitTime(int chatLimitTime) {
        if (completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("slow_mode", chatLimitTime)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setChatLimitTime0(chatLimitTime);
    }

    public void setChatLimitTime0(int chatLimitTime) {
        if (completed) init();
        this.chatLimitTime = chatLimitTime;
    }

    @Override
    public void update(JsonObject data) {
        if (completed) init();
        synchronized (this) {
            super.update(data);
            int chatLimitTime = get(data, "slow_mode").getAsInt();
            String topic = get(data, "topic").getAsString();

            this.chatLimitTime = chatLimitTime;
            this.topic = topic;
        }
    }

    @Override
    public void init() {
        super.init();
        final TextChannelImpl textChannel = (TextChannelImpl) super.channel;
        this.chatLimitTime = textChannel.chatLimitTime;
        this.topic = textChannel.topic;
    }
}
