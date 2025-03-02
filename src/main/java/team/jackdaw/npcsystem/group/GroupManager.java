package team.jackdaw.npcsystem.group;

import team.jackdaw.npcsystem.BaseManager;
import team.jackdaw.npcsystem.Config;

import java.util.*;

public class GroupManager extends BaseManager<String, Group> {
    private static final GroupManager INSTANCE = new GroupManager();

    private GroupManager() {
    }

    public static GroupManager getInstance() {
        return INSTANCE;
    }

    public void register(String name, boolean canCreate) {
        if (isRegistered(name)) return;
        Group group = new Group(name);
        GroupDataManager groupDataManager = group.getDataManager();
        if (!groupDataManager.isExist() && !canCreate && !name.equals("Global")) return;
        groupDataManager.sync();
        register(name, group);
    }


    @Override
    public boolean discard(String name) {
        Group group = super.get(name);
        if (group == null) return false;
        group.getDataManager().save();
        return true;
    }

    @Override
    public Group get(String name) {
        if (!isRegistered(name)) {
            register(name, false);
        }
        Group group = super.get(name);
        if (group == null) return null;
        group.updateLastLoadTime(System.currentTimeMillis());
        return group;
    }

    public void removeTimeout() {
        map.forEach((name, environment) -> {
            if (environment.getLastLoadTime() + Config.outOfTime < System.currentTimeMillis()) {
                environment.getDataManager().save();
                remove(name);
            }
        });
    }

    /**
     * Get all the parent groups of the group. Include the group itself.
     *
     * @param currentGroup the parent group of the group.
     * @return parentGroups the parent groups of the group.
     */
    public ArrayList<Group> getParentGroups(String currentGroup) {
        ArrayList<String> visited = new ArrayList<>();
        return getParentGroups(currentGroup, visited);
    }

    private ArrayList<Group> getParentGroups(String currentGroup, ArrayList<String> visited) {
        ArrayList<Group> parentGroups = new ArrayList<>();
        Group current = GroupManager.getInstance().get(currentGroup);
        if (current == null || current.getName().equals("Global")) {
            parentGroups.add(GroupManager.getInstance().get("Global"));
            return parentGroups;
        }
        parentGroups.add(current);
        String parentGroup = current.getParentGroup();
        if (!visited.contains(parentGroup)) {
            visited.add(currentGroup);
            parentGroups.addAll(GroupManager.getInstance().getParentGroups(parentGroup, visited));
        } else {
            parentGroups.add(GroupManager.getInstance().get("Global"));
        }
        return parentGroups;
    }

    /**
     * Get the list of the groups from the files.
     *
     * @return The list of the groups.
     */
    public ArrayList<String> getGroupList() {
        return GroupDataManager.getGroupList();
    }

    /**
     * Add a member to the group, and all the parent groups.
     *
     * @param groupName The name of the group
     * @param member    The name of the member
     */
    public void addGroupMember(String groupName, String member) {
        ArrayList<Group> groups = getParentGroups(groupName);
        groups.forEach(group -> group.addMember(member));
    }

    /**
     * Remove a member from the group, and all the parent groups.
     *
     * @param groupName The name of the group
     * @param member    The name of the member
     */
    public void removeGroupMember(String groupName, String member) {
        ArrayList<Group> groups = getParentGroups(groupName);
        groups.forEach(group -> group.removeMember(member));
    }

    /**
     * Set the parent group of the group. And move all the members to the new parent group.
     *
     * @param groupName     The name of the group
     * @param newParentName The name of the new parent group
     */
    public void setGroupParent(String groupName, String newParentName) {
        Group group = get(groupName);
        if (group == null) return;
        if (!getGroupList().contains(newParentName) || groupName.equals(newParentName) || getParentGroups(newParentName).contains(group))
            return;
        ArrayList<String> groupMembers = group.getMemberList();
        groupMembers.forEach(member -> {
            removeGroupMember(group.getParentGroup(), member);
            addGroupMember(newParentName, member);
        });
        group.setParentGroup(newParentName);
    }

    /**
     * Get the prompt of the group.
     *
     * @param group the group name
     * @return the prompt of the group
     */
    public String getGroupsPrompt(String group) {
        Group current = get(group);
        if (current == null) return null;
        StringBuilder groupsPrompt = new StringBuilder();
        groupsPrompt.append("The place ").append(group).append(" is ").append(current.getInstruction()).append(". ");
        if (current.getEvent().isEmpty()) return groupsPrompt.toString();
        groupsPrompt.append("There are some events happening here: ");
        for (String event : current.getEvent()) {
            groupsPrompt.append(event);
            if (current.getEvent().indexOf(event) != current.getEvent().size() - 1) groupsPrompt.append(", ");
            else groupsPrompt.append(". ");
        }
        return groupsPrompt.toString();
    }

    public String getParentGroupListPrompt(String group) {
        return "Your are living in: " + getParentGroups(group).stream().map(Group::getName).reduce((a, b) -> a + ", " + b).orElse("Global") + ".";
    }

    /**
     * Get the group tree.
     *
     * @param root The root of the tree
     * @return The group tree
     */
    public String getGroupTree(String root) {
        GroupToTree groupToTree = new GroupToTree();
        List<Group> groupList = getGroupList().stream().map(GroupManager.getInstance()::get).filter(Objects::nonNull).toList();
        ArrayList<Group> visited = new ArrayList<>();
        groupList.forEach(group -> {
            if (group.getParentGroup() != null) {
                if (visited.contains(group)) return;
                groupToTree.addEdge(group.getParentGroup(), group.getName());
                visited.add(group);
            }
        });
        return TreeNode.toString(groupToTree.generateTree(root), 0);
    }

    protected static class GroupToTree {
        protected final Map<String, List<String>> graph = new HashMap<>();

        protected void addEdge(String source, String destination) {
            graph.computeIfAbsent(source, x -> new ArrayList<>()).add(destination);
            graph.computeIfAbsent(destination, x -> new ArrayList<>()).add(source);
        }

        protected TreeNode generateTree(String root) {
            return buildTree(root, new HashSet<>());
        }

        protected TreeNode buildTree(String node, Set<String> visited) {
            visited.add(node);
            TreeNode treeNode = new TreeNode(node);
            List<TreeNode> children = new ArrayList<>();
            if (graph.containsKey(node)) {
                for (String neighbor : graph.get(node)) {
                    if (!visited.contains(neighbor)) {
                        children.add(buildTree(neighbor, visited));
                    }
                }
            }
            treeNode.children = children;
            return treeNode;
        }
    }

    protected static class TreeNode {
        protected String val;
        protected List<TreeNode> children;

        protected TreeNode(String val) {
            this.val = val;
            this.children = new ArrayList<>();
        }

        protected static String toString(TreeNode node, int level) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < level; i++) {
                if (i == level - 1)
                    sb.append("└── ");
                else
                    sb.append("│   ");
            }
            sb.append(node.val).append("\n");
            for (TreeNode child : node.children) {
                sb.append(toString(child, level + 1));
            }
            return sb.toString();
        }
    }

}
