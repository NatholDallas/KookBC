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

package snw.kookbc.impl.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.UserJoinedVoiceChannelIterator;
import snw.kookbc.interfaces.Lazy;
import snw.kookbc.interfaces.Updatable;
import snw.kookbc.util.MapBuilder;

import java.util.*;

import static snw.kookbc.util.GsonUtil.get;

public class UserImpl implements User, Updatable, Lazy {
    private final KBCClient client;
    private final String id;

    /* basic info */
    private boolean bot;
    private String name;
    private int identify;
    private boolean ban;
    private boolean vip;
    private String avatarUrl;
    private String vipAvatarUrl;

    /* completed flag */
    private boolean completed;

    public UserImpl(KBCClient client, String id) {
        this.client = client;
        this.id = id;
    }

    public UserImpl(KBCClient client, String id, boolean bot, String name, int identify, boolean ban, boolean vip, String avatarUrl, String vipAvatarUrl) {
        this.client = client;
        this.id = id;
        this.bot = bot;
        this.name = name;
        this.identify = identify;
        this.ban = ban;
        this.vip = vip;
        this.avatarUrl = avatarUrl;
        this.vipAvatarUrl = vipAvatarUrl;
        this.completed = true;
    }

    @Override
    public String getId() {
        if (!completed) init();
        return id;
    }

    @Override
    public String getNickName(Guild guild) {
        if (!completed) init();
        return client.getNetworkClient()
                .get(String.format("%s?user_id=%s&guild_id=%s",
                        HttpAPIRoute.USER_WHO.toFullURL(),
                        id,
                        guild.getId()))
                .get("nickname")
                .getAsString();
    }

    @Override
    public String getFullName(@Nullable Guild guild) {
        if (!completed) init();
        return (guild != null ? getNickName(guild) : getName()) + "#" + getIdentifyNumber();
    }

    @Override
    public void setNickName(Guild guild, String s) {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("nickname", (s != null ? s : ""))
                .put("user_id", getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GUILD_CHANGE_OTHERS_NICKNAME.toFullURL(), body);
    }

    @Override
    public int getIdentifyNumber() {
        if (!completed) init();
        return identify;
    }

    @Override
    public boolean isVip() {
        if (!completed) init();
        return vip;
    }

    public void setVip(boolean vip) {
        if (!completed) init();
        this.vip = vip;
    }

    @Override
    public boolean isBot() {
        if (!completed) init();
        return bot;
    }

    @Override
    public boolean isOnline() {
        if (!completed) init();
        return client.getNetworkClient().get(String.format("%s?user_id=%s", HttpAPIRoute.USER_WHO.toFullURL(), id)).get("online").getAsBoolean();
    }

    @Override
    public boolean isBanned() {
        if (!completed) init();
        return ban;
    }

    @Override
    public Collection<Integer> getRoles(Guild guild) {
        if (!completed) init();
        JsonArray array = client.getNetworkClient()
                .get(String.format("%s?user_id=%s&guild_id=%s",
                        HttpAPIRoute.USER_WHO.toFullURL(),
                        id,
                        guild.getId()))
                .getAsJsonArray("roles");
        HashSet<Integer> result = new HashSet<>();
        for (JsonElement element : array) {
            result.add(element.getAsInt());
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public String sendPrivateMessage(String s) {
        if (!completed) init();
        return sendPrivateMessage(new MarkdownComponent(s));
    }

    @Override
    public String sendPrivateMessage(String s, PrivateMessage privateMessage) {
        if (!completed) init();
        return sendPrivateMessage(new MarkdownComponent(s), privateMessage);
    }

    @Override
    public String sendPrivateMessage(BaseComponent baseComponent) {
        if (!completed) init();
        return sendPrivateMessage(baseComponent, null);
    }

    @Override
    public String sendPrivateMessage(BaseComponent component, PrivateMessage quote) {
        if (!completed) init();
        Object[] serialize = MessageBuilder.serialize(component);
        int type = (int) serialize[0];
        String json = (String) serialize[1];
        Map<String, Object> body = new MapBuilder()
                .put("type", type)
                .put("target_id", getId())
                .put("content", json)
                .putIfNotNull("quote", quote, Message::getId)
                .build();
        return client.getNetworkClient().post(HttpAPIRoute.USER_CHAT_MESSAGE_CREATE.toFullURL(), body).get("msg_id").getAsString();
    }

    @Override
    public PageIterator<Collection<VoiceChannel>> getJoinedVoiceChannel(Guild guild) {
        if (!completed) init();
        return new UserJoinedVoiceChannelIterator(client, this, guild);
    }

    @Override
    public int getIntimacy() {
        if (!completed) init();
        return client.getNetworkClient().get(String.format("%s?user_id=%s", HttpAPIRoute.INTIMACY_INFO.toFullURL(), getId())).get("score").getAsInt();
    }

    @Override
    public IntimacyInfo getIntimacyInfo() {
        if (!completed) init();
        JsonObject object = client.getNetworkClient().get(String.format("%s?user_id=%s", HttpAPIRoute.INTIMACY_INFO.toFullURL(), getId()));
        String socialImage = get(object, "img_url").getAsString();
        String socialInfo = get(object, "social_info").getAsString();
        int lastRead = get(object, "last_read").getAsInt();
        int score = get(object, "score").getAsInt();
        JsonArray socialImageListRaw = get(object, "img_list").getAsJsonArray();
        Collection<IntimacyInfo.SocialImage> socialImages = new ArrayList<>(socialImageListRaw.size());
        for (JsonElement element : socialImageListRaw) {
            JsonObject obj = element.getAsJsonObject();
            String id = obj.get("id").getAsString();
            String url = obj.get("url").getAsString();
            socialImages.add(
                    new SocialImageImpl(id, url)
            );
        }
        return new IntimacyInfoImpl(socialImage, socialInfo, lastRead, score, socialImages);
    }

    @Override
    public void setIntimacy(int i) {
        if (!completed) init();
        if (!((i > 0) && (i < 2200)))
            throw new IllegalArgumentException("Invalid score. 0--2200 is allowed.");
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .put("score", i)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.INTIMACY_UPDATE.toFullURL(), body);
    }

    @Override
    public void setIntimacy(int i, String s, @Nullable Integer imageId) {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .put("score", i)
                .putIfNotNull("social_info", s)
                .putIfNotNull("img_id", imageId)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.INTIMACY_UPDATE.toFullURL(), body);
    }

    @Override
    public void grantRole(Role role) {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", role.getGuild().getId())
                .put("user_id", getId())
                .put("role_id", role.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_GRANT.toFullURL(), body);
    }

    @Override
    public void revokeRole(Role role) {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", role.getGuild().getId())
                .put("user_id", getId())
                .put("role_id", role.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_REVOKE.toFullURL(), body);
    }

    @Override
    public void grantRole(Guild guild, int roleId) {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("user_id", getId())
                .put("role_id", roleId)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_GRANT.toFullURL(), body);
    }

    @Override
    public void revokeRole(Guild guild, int roleId) {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", guild.getId())
                .put("user_id", getId())
                .put("role_id", roleId)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_REVOKE.toFullURL(), body);
    }

    @Override
    public void block() {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.FRIEND_BLOCK.toFullURL(), body);
    }

    @Override
    public void unblock() {
        if (!completed) init();
        Map<String, Object> body = new MapBuilder()
                .put("user_id", getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.FRIEND_UNBLOCK.toFullURL(), body);
    }

    @Override
    public @Nullable String getAvatarUrl(boolean b) {
        if (!completed) init();
        return b ? vipAvatarUrl : avatarUrl;
    }

    @Override
    public String getName() {
        if (!completed) init();
        return name;
    }

    public void setName(String name) {
        if (!completed) init();
        this.name = name;
    }

    public void setIdentify(int identify) {
        if (!completed) init();
        this.identify = identify;
    }

    public void setBan(boolean ban) {
        if (!completed) init();
        this.ban = ban;
    }

    public void setAvatarUrl(String avatarUrl) {
        if (!completed) init();
        this.avatarUrl = avatarUrl;
    }

    public void setVipAvatarUrl(String vipAvatarUrl) {
        if (!completed) init();
        this.vipAvatarUrl = vipAvatarUrl;
    }

    @Override
    public void update(JsonObject data) {
        if (!completed) init();
        Validate.isTrue(Objects.equals(getId(), get(data, "id").getAsString()), "You can't update user by using different data");
        synchronized (this) {
            name = get(data, "username").getAsString();
            avatarUrl = get(data, "avatar").getAsString();
            vipAvatarUrl = get(data, "vip_avatar").getAsString();
            identify = get(data, "identify_num").getAsInt();
            ban = get(data, "status").getAsInt() == 10;
            vip = get(data, "is_vip").getAsBoolean();
        }
    }

    @Override
    public void init() {
        final UserImpl user = client.getEntityBuilder().buildUser(
                client.getNetworkClient().get(
                        String.format("%s?user_id=%s", HttpAPIRoute.USER_WHO.toFullURL(), id)
                )
        );
        bot = user.bot;
        name = user.name;
        identify = user.identify;
        ban = user.ban;
        vip = user.vip;
        avatarUrl = user.avatarUrl;
        vipAvatarUrl = user.vipAvatarUrl;
        completed = true;
    }
}

class IntimacyInfoImpl implements User.IntimacyInfo {
    private final String socialImage;
    private final String socialInfo;
    private final int lastRead;
    private final int score;
    private final Collection<SocialImage> socialImages;

    IntimacyInfoImpl(String socialImage, String socialInfo, int lastRead, int score, Collection<SocialImage> socialImages) {
        this.socialImage = socialImage;
        this.socialInfo = socialInfo;
        this.lastRead = lastRead;
        this.score = score;
        this.socialImages = Collections.unmodifiableCollection(socialImages);
    }

    @Override
    public String getSocialImage() {
        return socialImage;
    }

    @Override
    public String getSocialInfo() {
        return socialInfo;
    }

    @Override
    public int getLastRead() {
        return lastRead;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public Collection<SocialImage> getSocialImages() {
        return socialImages;
    }
}

class SocialImageImpl implements User.IntimacyInfo.SocialImage {
    private final String id;
    private final String url;

    SocialImageImpl(String id, String url) {
        this.id = id;
        this.url = url;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }
}
