package com.hortonworks.ranger;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;

public class Policy {
	// Integer (and not int) because for post we need to leave it zapped 
    Integer id;
    String policyName;
    String resourceName;
    String description;
    String repositoryName;
    String repositoryType;
    boolean isEnabled;
    boolean isRecursive;
    boolean isAuditEnabled;
    List<Permissions> permMapList;
    
    Policy() {
    	isEnabled = true;
    	isRecursive = true;
    	isAuditEnabled = false;
    	repositoryType = "HDFS";
    	permMapList = new ArrayList<Permissions>();
    }

    /**
     * This ctor is expected to be used for POST.  If not then use with caution! 
     * @param resource
     * @param repository
     * @param aPermission
     */
    Policy(String resource, String repository, Permissions aPermission) {
    	this();
    	id = null; // policy being created for post so we don't have an id yet!
    	resourceName = resource;
    	repositoryName = repository;
    	permMapList.add(aPermission);
    }

    // the following fields are not needed for add/delete/update
//    String createDate;
//    String updateDate;
//    String owner;
//    String updatedBy;
//    String version;

    boolean replacePerm = true;
    
    public String toString() {
		return MoreObjects.toStringHelper("\n\tPolicy")
				.add("id", id)
				.add("policyName", policyName)
				.add("resourceName", resourceName)
				.add("description", description)
				.add("repositoryName", repositoryName)
				.add("isEnabled", isEnabled)
				.add("isRecursive", isRecursive)
				.add("isAuditEnabled", isAuditEnabled)
				.add("replacePerm", replacePerm)
				.add("permMapList", permMapList)
//				.add("createDate", createDate)
//				.add("updateDate", updateDate)
//				.add("owner", owner)
//				.add("updatedBy", updatedBy)
//				.add("version", version)
				.toString();
    }

	public boolean hasMultipleResources() {
		String[] tokens = resourceName.split("\\s*,\\s*"); 
		return tokens.length > 1; 
	}

	public boolean isForResource(String resource) {
		String[] resources = resourceName.split("\\s*,\\s*");
		for (String aResource : resources) {
			if (aResource.equals(resource)) {
				return true;
			}
		}
		return false;
	}
	
	public void save() {
		
	}

	public String getId() {
		return id.toString();
	}

	/**
	 * If the policy is a candidate for deleting (instead of editing) for a group if all of the following are true
	 * 1 - There is just one permission item in the policy
	 * 2 - That permission item could safely be deleted without altering permissions for anyone else.
	 * @param group
	 * @return
	 */
	public boolean isForASingleGroup(String group) {
		if (permMapList.size() != 1) {
			return false;
		}
		Permissions permissions = permMapList.get(0);
		return permissions.isForASingleGroup(group);
	}

	public boolean isEmpty() {
		return permMapList.size() == 0;
	}
}
