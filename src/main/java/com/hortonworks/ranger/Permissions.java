package com.hortonworks.ranger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.MoreObjects;

public class Permissions {
	List<String> groupList;
	List<String> userList;
	List<String> permList;
	
	Permissions() {
		groupList = new ArrayList<String>();
		userList = new ArrayList<String>();
		permList = new ArrayList<String>();
	}
	
	Permissions(String group) {
		this();
		groupList.add(group);
		permList.addAll(Arrays.asList(new String[] { "Read", "Write", "Execute" })); 
	}
	
    public String toString() {
		return MoreObjects.toStringHelper("\n\t\tPermissions")
				.add("groupList", groupList)
				.add("userList", userList)
				.add("permList", permList)
				.toString();
    }

    /**
     * A permission is a candidate for removal for a group if and only if
     * it is purely a group-based policy for just that group
     * @param group
     * @return
     */
    public boolean isACandidateForDeletion(String group) {
    	if (userList.size() != 0) {
    		return false;
    	}
    	if (groupList.size() == 0) {
    		// it would be odd to have a permission that has neither user not groups in it.
    		// but if there exists on then sure, delete it.  It is bogus anyway!
    		return true;
    	}
    	if (groupList.size() != 1) {
    		return false;
    	}
    	String policyGroup = groupList.get(0);
    	return policyGroup.equals(group);
	}
}
