package com.hortonworks.ranger;

import com.google.common.base.MoreObjects;

public class Entitlement {
	String action;
	String resource;
	String group;
	
	public String toString() {
		return MoreObjects.toStringHelper("Entitlement")
				.add("Action", action)
				.add("Resource", resource)
				.add("Group", group)
				.toString();
	}
	
	public static Entitlement createEntitlements(String[] tokens) {
		Entitlement entitlement = new Entitlement();
		entitlement.action = tokens[0];
		entitlement.resource = tokens[1];
		entitlement.group = tokens[2];
		
		return entitlement;
	}
}
