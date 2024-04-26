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

package snw.kookbc.impl.entity.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Game;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.*;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.ChannelImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.network.exceptions.BadResponseException;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import static snw.kookbc.util.GsonUtil.get;

// The class for building entities.
public final class EntityBuilder {
    private final KBCClient client;

    public EntityBuilder(KBCClient client) {
        this.client = client;
    }

    public UserImpl buildUser(JsonObject object) {
        return new UserImpl(
                client,
                get(object, "id").getAsString(),
                get(object, "bot").getAsBoolean(),
                get(object, "username").getAsString(),
                get(object, "identify_num").getAsInt(),
                get(object, "status").getAsInt() == 10,
                get(object, "is_vip").getAsBoolean(),
                get(object, "avatar").getAsString(),
                get(object, "vip_avatar").getAsString()
        );
    }

    public GuildImpl buildGuild(JsonObject object) {
        String id = get(object, "id").getAsString();
        String name = get(object, "name").getAsString();
        boolean isPublic = get(object, "enable_open").getAsBoolean();
        String region = get(object, "region").getAsString();
        String masterId = get(object, "master_id").getAsString();
        int rawNotifyType = get(object, "notify_type").getAsInt();
        Guild.NotifyType type = null;
        String avatar = get(object, "icon").getAsString();
        for (Guild.NotifyType value : Guild.NotifyType.values()) {
            if (value.getValue() == rawNotifyType) {
                type = value;
                break;
            }
        }
        Validate.notNull(type, String.format("Internal Error: Unexpected NotifyType from remote: %s", rawNotifyType));
        return new GuildImpl(
                client,
                id,
                name,
                isPublic,
                region,
                masterId,
                type,
                avatar
        );
    }

    public ChannelImpl buildChannel(JsonObject object) {
        if (get(object, "is_category").getAsBoolean()) {
            return buildCategory(object);
        }
        switch (get(object, "type").getAsInt()) {
            case 1:
                return buildTextChannel(object);
            case 2:
                return buildVoiceChannel(object);
            default:
                throw new IllegalArgumentException("We can't construct the Channel using given information. Is your information correct?");
        }
    }

    public CategoryImpl buildCategory(JsonObject object) {
        return new CategoryImpl(
                client,
                get(object, "id").getAsString(),
                get(object, "user_id").getAsString(),
                get(object, "guild_id").getAsString(),
                get(object, "permission_sync").getAsInt() != 0,
                get(object, "name").getAsString(),
                parseRPO(object),
                parseUPO(client, object),
                get(object, "level").getAsInt()
        );
    }

    public TextChannelImpl buildTextChannel(JsonObject object) {
        String parentId = get(object, "parent_id").getAsString();
        return new TextChannelImpl(
                client,
                get(object, "id").getAsString(),
                get(object, "user_id").getAsString(),
                get(object, "guild_id").getAsString(),
                get(object, "permission_sync").getAsInt() != 0,
                get(object, "name").getAsString(),
                parseRPO(object),
                parseUPO(client, object),
                get(object, "level").getAsInt(),
                ("".equals(parentId) || "0".equals(parentId)) ? null : client.getStorage().getCategory(parentId),
                get(object, "slow_mode").getAsInt(),
                get(object, "topic").getAsString()
        );
    }

    public VoiceChannelImpl buildVoiceChannel(JsonObject object) {
        String parentId = get(object, "parent_id").getAsString();
        return new VoiceChannelImpl(
                client,
                get(object, "id").getAsString(),
                get(object, "user_id").getAsString(),
                get(object, "guild_id").getAsString(),
                get(object, "permission_sync").getAsInt() != 0,
                get(object, "name").getAsString(),
                parseRPO(object),
                parseUPO(client, object),
                get(object, "level").getAsInt(),
                ("".equals(parentId) || "0".equals(parentId)) ? null : client.getStorage().getCategory(parentId),
                get(object, "slow_mode").getAsInt(),
                object.has("has_password") && get(object, "has_password").getAsBoolean(),
                get(object, "limit_amount").getAsInt(),
                get(object, "voice_quality").getAsInt()
        );
    }

    public Role buildRole(Guild guild, JsonObject object) {
        int id = get(object, "role_id").getAsInt();
        String name = get(object, "name").getAsString();
        int color = get(object, "color").getAsInt();
        int pos = get(object, "position").getAsInt();
        boolean hoist = get(object, "hoist").getAsInt() == 1;
        boolean mentionable = get(object, "mentionable").getAsInt() == 1;
        int permissions = get(object, "permissions").getAsInt();
        return new RoleImpl(client, guild, id, color, pos, permissions, mentionable, hoist, name);
    }

    public CustomEmoji buildEmoji(JsonObject object) {
        String id = get(object, "id").getAsString();
        Guild guild = null;
        if (id.contains("/")) {
            try {
                guild = client.getStorage().getGuild(id.substring(0, id.indexOf("/")));
            } catch (BadResponseException e) {
                if (!(e.getCode() == 403)) {
                    throw e;
                }
                // or you don't have permission to access it!
            }
        }
        String name = get(object, "name").getAsString();
        return new CustomEmojiImpl(client, id, name, guild);
    }

    public Game buildGame(JsonObject object) {
        int id = get(object, "id").getAsInt();
        String name = get(object, "name").getAsString();
        String icon = get(object, "icon").getAsString();
        return new GameImpl(client, id, name, icon);
    }

    public static Collection<Channel.RolePermissionOverwrite> parseRPO(JsonObject object) {
        JsonArray array = get(object, "permission_overwrites").getAsJsonArray();
        Collection<Channel.RolePermissionOverwrite> rpo = new ConcurrentLinkedQueue<>();
        for (JsonElement element : array) {
            JsonObject orpo = element.getAsJsonObject();
            rpo.add(
                    new Channel.RolePermissionOverwrite(
                            orpo.get("role_id").getAsInt(),
                            orpo.get("allow").getAsInt(),
                            orpo.get("deny").getAsInt()
                    )
            );
        }
        return rpo;
    }

    public static Collection<Channel.UserPermissionOverwrite> parseUPO(KBCClient client, JsonObject object) {
        JsonArray array = get(object, "permission_users").getAsJsonArray();
        Collection<Channel.UserPermissionOverwrite> upo = new ConcurrentLinkedQueue<>();
        for (JsonElement element : array) {
            JsonObject oupo = element.getAsJsonObject();
            JsonObject rawUser = oupo.getAsJsonObject("user");
            upo.add(
                    new Channel.UserPermissionOverwrite(
                            client.getStorage().getUser(rawUser.get("id").getAsString(), rawUser),
                            oupo.get("allow").getAsInt(),
                            oupo.get("deny").getAsInt()
                    )
            );
        }
        return upo;
    }
}
