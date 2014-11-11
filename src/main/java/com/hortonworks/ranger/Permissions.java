package com.hortonworks.ranger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.MoreObjects;

public class Permissions {
	final List<String> groupList;
	final List<String> userList;
	final List<String> permList;
	
	Permissions() {
		groupList = new ArrayList<String>();
		userList = new ArrayList<String>();
		permList = new ArrayList<String>();
	}
	
	Permissions(List<String> groups, List<String> users, List<String> accesses) {
		this();
		groupList.addAll(groups);
		userList.addAll(users);
		permList.addAll(accesses);
	}
	
	public String toString() {
		return MoreObjects.toStringHelper("\n\t\tPermissions")
				.add("groupList", groupList)
				.add("userList", userList)
				.add("permList", permList)
				.toString();
    }

    public boolean isForASingleGroup(String group) {
    	if (userList.size() != 0) {
    		return false;
    	}
    	if (groupList.size() != 1) {
    		return false;
    	}
    	String policyGroup = groupList.get(0);
    	return policyGroup.equals(group);
	}

    /**
     * Removes the specified group.  Returns true if group was currently in the grouplist, false otherwise
     * @param group
     */
	public boolean removeGroup(String group) {
		boolean result = false;
		Iterator<String> iterator = groupList.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().equals(group)) {
				iterator.remove();
				result = true;
			}
		}
		
		return result;
	}
	
	public boolean isEmpty() {
		return groupList.size() == 0 && userList.size() == 0;
	}
}
