package com.hortonworks.ranger;

import java.util.List;

import com.google.common.base.MoreObjects;

public class PolicyDetails {
    int startIndex;
    int pageSize;
    int totalCount;
    int resultSize;
    long queryTimeMS;
    List<Policy> vXPolicies;

    public String toString() {
		return MoreObjects.toStringHelper("\nPolicyDetails")
				.add("startIndex", startIndex)
				.add("pageSize", pageSize)
				.add("totalCount", totalCount)
				.add("resultSize", resultSize)
				.add("queryTimeMS", queryTimeMS)
				.add("vXPolicies", vXPolicies)
				.toString();
	}

	public boolean isEmpty() {
		return vXPolicies.size() == 0;
	}
	

}
