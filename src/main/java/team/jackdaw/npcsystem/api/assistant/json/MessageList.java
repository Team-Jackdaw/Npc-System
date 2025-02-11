package team.jackdaw.npcsystem.api.assistant.json;

import java.util.List;

public class MessageList {
    public List<Message> data;

    public static class Message {
        public List<Content> content;

        public static class Content {
            public Text text;

            public static class Text {
                public String value;
            }
        }
    }
}
