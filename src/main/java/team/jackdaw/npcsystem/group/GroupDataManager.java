package team.jackdaw.npcsystem.group;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import team.jackdaw.npcsystem.NPCSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * A serializer used to read or write the data from the files.
 * <p>
 * This data is related to the Group's content.
 *
 * <p>Read or Write the data file with some information, each file just record one relative information.</p>
 */
public class GroupDataManager {
    static {
        mkdir();
    }

    private final File theFile;
    private final Group group;

    /**
     * Create a new GroupDataManager.
     *
     * @param group The group to be managed.
     */
    GroupDataManager(@NotNull Group group) {
        this.group = group;
        this.theFile = new File(NPCSystem.workingDirectory.toFile(), "group/" + group.getName() + ".json");
    }

    private static void mkdir() {
        Path workingDirectory = NPCSystem.workingDirectory.resolve("group");
        if (!Files.exists(workingDirectory)) {
            try {
                Files.createDirectories(workingDirectory);
            } catch (IOException e) {
                NPCSystem.LOGGER.error("[npc-system] Failed to create the npc directory");
                NPCSystem.LOGGER.error(e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Get the list of the groups from the files.
     *
     * @return The list of the groups.
     */
    protected static @NotNull ArrayList<String> getGroupList() {
        File workingDirectory = NPCSystem.workingDirectory.resolve("group").toFile();
        File[] files = workingDirectory.listFiles();
        ArrayList<String> groupList = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.endsWith(".json")) {
                    groupList.add(name.substring(0, name.length() - 5));
                }
            }
        }
        return groupList;
    }

    /**
     * Check if the file is existed.
     *
     * @return true if the file is existed, otherwise false.
     */
    public boolean isExist() {
        return theFile.exists();
    }

    /**
     * Read the data from the file and set the group's content.
     */
    public void sync() {
        if (!isExist()) {
            save();
            return;
        }
        try {
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(theFile.toPath()));
            GroupData data = gson.fromJson(json, GroupData.class);
            data.set(group);
        } catch (IOException e) {
            NPCSystem.LOGGER.error("[npc-system] Can't open the data file.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Write the group's content to the file.
     */
    public void save() {
        try {
            if (!isExist()) {
                if (!theFile.createNewFile()) {
                    NPCSystem.LOGGER.error("[npc-system] Can't create the data file.");
                    return;
                }
            }
            Gson gson = new Gson();
            GroupData data = new GroupData(group);
            String json = gson.toJson(data);
            Files.write(theFile.toPath(), json.getBytes());
        } catch (IOException e) {
            NPCSystem.LOGGER.error("[npc-system] Can't write the data file.");
        }
    }

    private static final class GroupData {
        private final String name;
        private final String parentGroup;
        private final String instruction;
        private final ArrayList<String> event;
        private final ArrayList<String> memberList;

        private GroupData(Group group) {
            this.name = group.getName();
            this.parentGroup = group.getParentGroup();
            this.instruction = group.getInstruction();
            this.event = group.getEvent();
            this.memberList = group.getMemberList();
        }

        private void set(Group group) {
            group.setParentGroup(parentGroup);
            group.setInstruction(instruction);
            group.setEvent(event);
            group.setMemberList(memberList);
        }
    }
}
