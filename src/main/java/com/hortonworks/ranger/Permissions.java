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
}
