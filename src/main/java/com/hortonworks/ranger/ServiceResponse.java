package com.hortonworks.ranger;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;

public class ServiceResponse {
    int startIndex;
    int pageSize;
    int totalCount;
    int resultSize;
    long queryTimeMS;
    List<Policy> vXPolicies;

    public String toString() {
		return MoreObjects.toStringHelper("\nServiceResponse")
				.add("startIndex", startIndex)
				.add("pageSize", pageSize)
				.add("totalCount", totalCount)
				.add("resultSize", resultSize)
				.add("queryTimeMS", queryTimeMS)
				.add("vXPolicies", vXPolicies)
				.toString();
	}

	public List<Policy> getRecursivePoliciesForResource(String resource) {
		
		List<Policy> result = new ArrayList<Policy>();
		
		for (Policy aPolicy : vXPolicies) {
			if (aPolicy.isRecursive && aPolicy.isForResource(resource)) {
				result.add(aPolicy);
			}
		}
		
		return result;
	}

	public Object getPolicyIds() {
		List<String> ids = new ArrayList<String>();
		for (Policy policy : vXPolicies) {
			ids.add(policy.getId());
		}
		return ids;
	}
}

