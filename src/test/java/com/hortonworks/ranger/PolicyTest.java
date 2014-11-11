package com.hortonworks.ranger;

import junit.framework.TestCase;

public class PolicyTest extends TestCase {

	public void testHasMultipleResources() {
		Policy policy = new Policy("/data,/data1", "repository", new Permissions());
		
		assertTrue(policy.hasMultipleResources());
	}
}
