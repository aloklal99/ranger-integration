package com.hortonworks.ranger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	if (args.length != Constants.Input.argumentCount) {
    		_Logger.fatal("Incorrect usage");
    		showUsage();
    	}
    	Input input = Input.parseInputs(args);
    	_Logger.debug(input);
    	
    	List<Entitlement> entitlements = parseFile(input.file);
    	
    	processEntitlements(input, entitlements);
    }

    private static String getPolicyURL(String host) {
    	return String.format("http://%s:6080/service/public/api/policy", host);
    }
    
    private static String getBasicAuth() {
		String basicAuth = "Basic " + DatatypeConverter.printBase64Binary("admin:admin".getBytes());
		return basicAuth;
    }
    
    private static PolicyDetails getPolicyDetails(String host, String repository, String resource) {
    	String json = ClientBuilder.newClient()
    		.target(getPolicyURL(host))
    		.queryParam(Constants.Parameter.Repository, repository)
    		.queryParam(Constants.Parameter.Resource, resource)
    		.request()
    		.header(Constants.Headers.Auth, getBasicAuth())
    		.accept(MediaType.APPLICATION_JSON)
    		.get()
    		.readEntity(String.class);
    	_Logger.info(String.format("getPolicyDetails: host[%s], repository[%s], resource[%s] => Json [%s].",
    			host, repository, resource, json));

    	PolicyDetails policyDetails = _Gson.fromJson(json, PolicyDetails.class);
    	_Logger.info("getPolicyDetails: converted to policydetails object: [" + policyDetails + "].");
    	
    	return policyDetails;
    }
    
    private static void processEntitlements(final Input input, final List<Entitlement> entitlements) {

		for (Entitlement entitlement : entitlements) {
			PolicyDetails existingPolicyDetails = getPolicyDetails(input.host, input.repository, entitlement.resource);

			if (entitlement.action.equals(Constants.Action.Add)) {
				if (existingPolicyDetails.isEmpty()) {
					_Logger.info("ADD: Adding new policy for entitlement [" + entitlement + "].");
					addNewPolicy(input.host, input.repository, entitlement.resource, entitlement.group);
				}
				else {
					_Logger.info("ADD: Updating existing policy for entitlement [" + entitlement + "].");
					updatePolicy(existingPolicyDetails, input.host, input.repository, entitlement.resource, entitlement.group);
				}
			}
			else if (entitlement.action.equals(Constants.Action.Delete)) {
				if (existingPolicyDetails.isEmpty()) {
					_Logger.info(String.format("No current policies for entitlement[%s]!  Treating as a noop!", entitlement));
				}
				else {
					revokePermissions(existingPolicyDetails, input.host, entitlement.resource, entitlement.group);
				}
			}
			
			else {
				_Logger.warn(String.format("Unsupported entitlement action [%s] for entitlement[%s]", entitlement.action, entitlement.toString()));
			}
		}
	}

	private static void updatePolicy(PolicyDetails policyDetails, String host, String repository,
			String resource, String group) {
		
		for (Policy policy : policyDetails.vXPolicies) {
			if (!policy.isForResource(resource)) {
				_Logger.warn("updatePolicy: policy resource mismatch");
				continue;
			}
			// Instead of adding update permission if one exists with all required permission-types
			Permissions permissions = new Permissions(group); 
			policy.permMapList.add(permissions);
			put(policy, host);
		}
	}

	private static void put(Policy policy, String host) {
		String json = _Gson.toJson(policy);
		_Logger.debug("updatePolicy: json [" + json + "].");
    	int status = ClientBuilder.newClient()
        		.target(getPolicyURL(host))
        		.path(policy.getId())
        		.request()
        		.header(Constants.Headers.Auth, getBasicAuth())
        		.accept(MediaType.APPLICATION_JSON)
        		.put(Entity.entity(json, MediaType.APPLICATION_JSON))
        		.getStatus();
        _Logger.debug("updatePolicy: PUT status: [" + status + "].");		
	}
	
	private static void addNewPolicy(String host, String repository, String resource, String group) {
		Permissions permissions = new Permissions(group);
		Policy policy = new Policy(resource, repository, permissions);
		String json = _Gson.toJson(policy);
		_Logger.debug("addNewPolicy: json [" + json + "].");
		
    	int status = ClientBuilder.newClient()
        		.target(getPolicyURL(host))
        		.request()
        		.header(Constants.Headers.Auth, getBasicAuth())
        		.accept(MediaType.APPLICATION_JSON)
        		.post(Entity.entity(json, MediaType.APPLICATION_JSON))
        		.getStatus();
        _Logger.debug("addNewPolicy: POST status: [" + status + "].");		
	}

	private static void revokePermissions(PolicyDetails policyDetails, String host, String resource, String group) {
		
		for (Policy policy : policyDetails.vXPolicies) {
			if (policy.hasMultipleResources()) {
				String msg = String.format("Can't Revoke permission for group[%s], resource[%s] since policy [%s] is for multiple resources",
						group, resource, policy);
				_Logger.warn(msg);
			}
			else if (policy.isACandidateForDisabling(group)) {
				_Logger.info(String.format("revokePermissions: policy has only one group based permissions for a single group[%s].  Disabling the policy", policy, group));
				policy.isEnabled = false;
				put(policy, host);
			}
			else {
				Iterator<Permissions> permissionsIterator = policy.permMapList.iterator();
				while (permissionsIterator.hasNext()) {
					Permissions permission = permissionsIterator.next();
					if (permission.isACandidateForDeletion(group)) {
						permissionsIterator.remove();
						put(policy, host);
					}
					else {
						List<String> groups = permission.groupList; 
						if (groups.contains(group)) {
							Iterator<String> groupsIterator = groups.iterator();
							while(groupsIterator.hasNext()){
							    if(groupsIterator.next().equals(group)) {
							        groupsIterator.remove();
							    }
							}
							put(policy, host);
						}
						else {
							_Logger.debug(String.format("Permissions [%s] isn't relevant for group[%s].  Leaving unchanged....!", permission, group));
						}
					}
				}
			}
		}
	}

	private static List<Entitlement> parseFile(String fileName) {
		File file = new File(fileName);
		
		List<Entitlement> result = new ArrayList<Entitlement>();
		try (FileReader fileReader = new FileReader(file);) {
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] tokens = line.split("\\s*,\\s*");
				_Logger.debug("line [" + line + "]");
				if (tokens.length == 3) {
					Entitlement entitlement = Entitlement.createEntitlements(tokens);
					_Logger.debug(entitlement);
					result.add(entitlement);
				}
				else {
					_Logger.warn(String.format("line [%s] isn't properly formatted!  Skipping...", line));
				}
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static void showUsage() {
		System.exit(Constants.ExitStatus.InvalidUsage);
	}
	
	private static final Logger _Logger = Logger.getLogger(App.class); 
	private static final Gson _Gson = new Gson();
}
