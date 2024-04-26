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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.*;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

public class VoiceChannelImpl extends NonCategoryChannelImpl implements VoiceChannel {
    private boolean passwordProtected;
    private int maxSize;
    private int quality;
    private int chatLimitTime;

    public VoiceChannelImpl(KBCClient client, String id) {
        super(client, id);
    }

    public VoiceChannelImpl(
            KBCClient client,
            String id,
            String masterId,
            String guildId,
            boolean permSync,
            String name,
            Collection<RolePermissionOverwrite> rpo,
            Collection<UserPermissionOverwrite> upo,
            int level,
            Category parent,
            int chatLimitTime,
            boolean passwordProtected,
            int maxSize,
            int quality
    ) {
        super(client, id, masterId, guildId, permSync, name, rpo, upo, level, parent, chatLimitTime);
        this.passwordProtected = passwordProtected;
        this.maxSize = maxSize;
        this.quality = quality;
        this.chatLimitTime = chatLimitTime;
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        if (completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return get(object, "url").getAsString();
    }

    @Override
    public boolean hasPassword() {
        if (completed) init();
        return passwordProtected;
    }

    @Override
    public int getMaxSize() {
        if (completed) init();
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        if (completed) init();
        this.maxSize = maxSize;
    }

    @Override
    public Collection<User> getUsers() {
        if (completed) init();
        String rawContent = client.getNetworkClient().getRawContent(HttpAPIRoute.CHANNEL_USER_LIST.toFullURL() + "?channel_id=" + getId());
        JsonArray array = JsonParser.parseString(rawContent).getAsJsonObject().getAsJsonArray("data");
        Set<User> users = new HashSet<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            users.add(client.getStorage().getUser(obj.get("id").getAsString(), obj));
        }
        return Collections.unmodifiableCollection(users);
    }

    @Override
    public void moveToHere(Collection<User> users) {
        if (completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("target_id", getId())
                .put("user_ids", users.stream().map(User::getId).toArray(String[]::new))
                .build();
        client.getNetworkClient().post(HttpAPIRoute.MOVE_USER.toFullURL(), body);
    }

    public void setPasswordProtected(boolean passwordProtected) {
        if (completed) init();
        this.passwordProtected = passwordProtected;
    }

    @Override
    public int getQuality() { // must query because we can't update this value by update(JsonObject) method
        if (completed) init();
        final JsonObject self = client.getNetworkClient()
                .get(HttpAPIRoute.CHANNEL_INFO.toFullURL() + "?target_id=" + getId());
        return get(self, "voice_quality").getAsInt();
    }

    @Override
    public void setQuality(int i) {
        if (completed) init();
        final Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("voice_quality", i)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
    }

    @Override
    public void update(JsonObject data) {
        if (completed) init();
        synchronized (this) {
            super.update(data);
            boolean hasPassword = has(data, "has_password") && get(data, "has_password").getAsBoolean();
            int size = has(data, "limit_amount") ? get(data, "limit_amount").getAsInt() : 0;
            int chatLimitTime = get(data, "slow_mode").getAsInt();
            this.passwordProtected = hasPassword;
            this.maxSize = size;
            this.chatLimitTime = chatLimitTime;
        }
    }

    @Override
    public void init() {
        super.init();
        final VoiceChannelImpl channel = (VoiceChannelImpl) super.channel;
        this.passwordProtected = channel.passwordProtected;
        this.maxSize = channel.maxSize;
        this.quality = channel.quality;
        this.chatLimitTime = channel.chatLimitTime;
    }
}
