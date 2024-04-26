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

package snw.kookbc.impl.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.JsonObject;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.Message;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.CustomEmojiImpl;
import snw.kookbc.impl.entity.GuildImpl;
import snw.kookbc.impl.entity.RoleImpl;
import snw.kookbc.impl.entity.UserImpl;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.ChannelImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.concurrent.TimeUnit;

public class EntityStorage {
    private static final int RETRY_TIMES = 1;

    private final KBCClient client;

    // See the notes of these member variables in the constructor.
    private final LoadingCache<String, User> users;
    private final LoadingCache<String, Guild> guilds;
    private final LoadingCache<String, Channel> channels;
    private final LoadingCache<String, Category> categories;
    private final LoadingCache<String, TextChannel> textChannels;
    private final LoadingCache<String, VoiceChannel> voiceChannels;

    // The following data types can be loaded manually, but it costs too many network resource.
    // So we won't remove them if the memory is enough.
    private final Cache<String, Role> roles;
    private final Cache<String, CustomEmoji> emojis;
    private final Cache<String, Message> msgs;
    private final Cache<String, Reaction> reactions;
    private final Cache<Integer, Game> games;

    public EntityStorage(KBCClient client) {
        this.client = client;
        this.users = newCaffeineBuilderWithWeakRef().build(withRetry(id -> new UserImpl(client, id)));
        this.guilds = newCaffeineBuilderWithWeakRef().build(withRetry(id -> new GuildImpl(client, id)));
        this.channels = newCaffeineBuilderWithSoftRef().build(withRetry(id -> client.getEntityBuilder().buildChannel(
                client.getNetworkClient().get(String.format("%s?target_id=%s", HttpAPIRoute.CHANNEL_INFO.toFullURL(), id))
        )));
        this.categories = newCaffeineBuilderWithWeakRef().build(withRetry(id -> new CategoryImpl(client, id)));
        this.textChannels = newCaffeineBuilderWithWeakRef().build(withRetry(id -> new TextChannelImpl(client, id)));
        this.voiceChannels = newCaffeineBuilderWithSoftRef().build(withRetry(id -> new VoiceChannelImpl(client, id)));
        this.msgs = newCaffeineBuilderWithSoftRef().build();
        this.roles = newCaffeineBuilderWithSoftRef().build();
        this.emojis = newCaffeineBuilderWithSoftRef().build();
        this.reactions = newCaffeineBuilderWithSoftRef().build();
        this.games = newCaffeineBuilderWithSoftRef().build();
    }

    public Game getGame(int id) {
        return games.getIfPresent(id);
    }

    public Message getMessage(String id) {
        return msgs.getIfPresent(id);
    }

    public User getUser(String id) {
        return users.get(id);
    }

    public Guild getGuild(String id) {
        return guilds.get(id);
    }

    public Channel getChannel(String id) {
        return channels.get(id);
    }

    public Category getCategory(String id) {
        return categories.get(id);
    }

    public TextChannel getTextChannel(String id) {
        return textChannels.get(id);
    }

    public VoiceChannel getVoiceChannel(String id) {
        return voiceChannels.get(id);
    }

    public Role getRole(Guild guild, int id) {
        return roles.getIfPresent(guild.getId() + "#" + id);
    }

    public CustomEmoji getEmoji(String id) {
        return emojis.getIfPresent(id);
    }

    public User getUser(String id, JsonObject def) {
        // use getIfPresent, because the def should not be wasted
        User result = users.getIfPresent(id);
        if (result == null) {
            result = new UserImpl(client, id);
            addUser(result);
        } else {
            ((UserImpl) result).update(def);
        }
        return result;
    }

    public Guild getGuild(String id, JsonObject def) {
        Guild result = guilds.getIfPresent(id);
        if (result == null) {
            result = client.getEntityBuilder().buildGuild(def);
            addGuild(result);
        } else {
            ((GuildImpl) result).update(def);
        }
        return result;
    }

    public Channel getChannel(String id, JsonObject def) {
        Channel result = channels.getIfPresent(id);
        if (result == null) {
            result = client.getEntityBuilder().buildChannel(def);
            addChannel(result);
        } else {
            ((ChannelImpl) result).update(def);
        }
        return result;
    }

    public Role getRole(Guild guild, int id, JsonObject def) {
        // getRole is Nullable
        Role result = getRole(guild, id);
        if (result == null) {
            result = client.getEntityBuilder().buildRole(guild, def);
            addRole(guild, result);
        } else {
            ((RoleImpl) result).update(def);
        }
        return result;
    }

    public CustomEmoji getEmoji(String id, JsonObject def) {
        CustomEmoji emoji = getEmoji(id);
        if (emoji == null) {
            emoji = client.getEntityBuilder().buildEmoji(def);
            addEmoji(emoji);
        } else {
            ((CustomEmojiImpl) emoji).update(def);
        }
        return emoji;
    }

    public Reaction getReaction(String msgId, CustomEmoji emoji, User sender) {
        return reactions.getIfPresent(msgId + "#" + emoji.getId() + "#" + sender.getId());
    }

    public void addGame(Game game) {
        games.put(game.getId(), game);
    }

    public void addReaction(Reaction reaction) {
        reactions.put(reaction.getMessageId() + "#" + reaction.getEmoji().getId() + "#" + reaction.getSender().getId(), reaction);
    }

    public void addMessage(Message message) {
        msgs.put(message.getId(), message);
    }

    public void addEmoji(CustomEmoji emoji) {
        emojis.put(emoji.getId(), emoji);
    }

    public void addUser(User user) {
        users.put(user.getId(), user);
    }

    public void addGuild(Guild guild) {
        guilds.put(guild.getId(), guild);
    }

    public void addChannel(Channel channel) {
        channels.put(channel.getId(), channel);
    }

    public void addRole(Guild guild, Role role) {
        roles.put(guild.getId() + "#" + role.getId(), role);
    }

    public void removeReaction(Reaction reaction) {
        reactions.invalidate(reaction.getMessageId() + "#" + reaction.getEmoji().getId() + "#" + reaction.getSender().getId());
    }

    // Only called when the message is invalid
    public void removeMessage(String id) {
        msgs.invalidate(id);
        reactions.asMap().keySet().removeIf(i -> i.startsWith(id));
    }

    public void removeChannel(String id) {
        channels.invalidate(id);
    }

    public void removeGuild(String id) {
        guilds.invalidate(id);
    }

    public void removeRole(Role role) {
        roles.invalidate(role.getGuild().getId() + "#" + role.getId());
    }

    public void removeEmoji(CustomEmoji emoji) {
        emojis.invalidate(emoji.getId());
    }

    private static Caffeine<Object, Object> newCaffeineBuilderWithWeakRef() {
        return Caffeine.newBuilder()
                .weakValues()
                .expireAfterAccess(10, TimeUnit.MINUTES);
    }

    private static Caffeine<Object, Object> newCaffeineBuilderWithSoftRef() {
        return Caffeine.newBuilder()
                .softValues();
    }

    private static <K, V> UncheckedFunction<K, V> funcWithRetry(UncheckedFunction<K, V> func) {
        return k -> {
            int retries = RETRY_TIMES;
            Exception latestException;
            do {
                try {
                    return func.apply(k);
                } catch (Exception e) {
                    latestException = e;
                }
            } while (retries-- > 0);
            throw new RuntimeException("Unable to load resource", latestException);
        };
    }

    private static <K, V> CacheLoader<K, V> withRetry(CacheLoader<K, V> original) {
        return funcWithRetry(original::load)::apply;
    }

    public void cleanUpUserPermissionOverwrite(Guild guild, User user) {
        channels.asMap().values()
                .stream()
                .filter(i -> i.getGuild() == guild)
                .map(i -> ((ChannelImpl) i).getOverwrittenUserPermissions0())
                .forEach(i -> i.removeIf(o -> o.getUser() == user));
    }
}

interface UncheckedFunction<K, V> {
    V apply(K k) throws Exception;
}