package dev.saseq.services;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;

import java.util.List;

/**
 * Finds threads by ID, including archived ones. JDA only caches active threads, so archived threads are looked up
 * by paging through the archived public threads of every channel that can contain threads.
 */
final class ThreadLookup {

    private ThreadLookup() {
    }

    /**
     * Find a thread in any server the bot is in.
     *
     * @return the thread, or null if no visible channel has a thread with this ID.
     */
    static ThreadChannel find(JDA jda, String threadId) {
        ThreadChannel cached = jda.getThreadChannelById(threadId);
        return cached != null ? cached : findArchived(jda.getGuilds(), threadId);
    }

    /**
     * Find a thread in one server.
     *
     * @return the thread, or null if no visible channel of the server has a thread with this ID.
     */
    static ThreadChannel find(Guild guild, String threadId) {
        ThreadChannel cached = guild.getThreadChannelById(threadId);
        return cached != null ? cached : findArchived(List.of(guild), threadId);
    }

    private static ThreadChannel findArchived(List<Guild> guilds, String threadId) {
        for (Guild guild : guilds) {
            for (GuildChannel channel : guild.getChannels()) {
                if (!(channel instanceof IThreadContainer container)) {
                    continue;
                }
                try {
                    // Pages lazily, so this stops at the page that has the thread.
                    ThreadChannel found = container.retrieveArchivedPublicThreadChannels().stream()
                            .filter(thread -> thread.getId().equals(threadId))
                            .findFirst()
                            .orElse(null);
                    if (found != null) {
                        return found;
                    }
                } catch (InsufficientPermissionException e) {
                    // The bot can't see this channel's archive; the thread can't be read there anyway.
                }
            }
        }
        return null;
    }
}
