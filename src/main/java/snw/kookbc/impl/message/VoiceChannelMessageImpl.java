package snw.kookbc.impl.message;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.Message;
import snw.jkook.message.VoiceChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;

public class VoiceChannelMessageImpl extends ChannelMessageImpl implements VoiceChannelMessage {
    public VoiceChannelMessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp, Message quote, String channelId) {
        super(client, id, user, component, timeStamp, quote, channelId);
    }

    @Override
    public VoiceChannel getChannel() {
        return (VoiceChannel) super.getChannel();
    }
}
