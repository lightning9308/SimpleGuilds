package me.lightning.simpleguilds;

import java.util.UUID;

public class Request {
    private final UUID sender;
    private final UUID target;
    private final RequestType requestType;
    private final String guildName;


    public RequestType getType() {
        return requestType;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public String getGuildName() {
        return guildName;
    }


    public Request(UUID sender, UUID target, RequestType requestType, String guildName) {
        this.sender = sender;
        this.target = target;
        this.requestType = requestType;
        this.guildName = guildName;
    }

    public enum RequestType {
        JOIN, INVITE
    }
}
