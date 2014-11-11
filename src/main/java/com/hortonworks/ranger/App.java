package com.hortonworks.ranger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

import com.google.common.base.MoreObjects;
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

    private static void processEntitlements(final Input input, final List<Entitlement> entitlements) {

		for (Entitlement entitlement : entitlements) {
			_Logger.info(String.format("Processing entitlement [%s]", entitlement));
			ServiceResponse serviceResponse = listPolicies(input.host, input.repository, entitlement.resource);
			
			List<Policy> policies = serviceResponse.getRecursivePoliciesForResource(entitlement.resource); 
			if (policies.size() == 0) {
		    	_Logger.info(String.format("No recursive policy exist for resource [%s].", entitlement.resource));
				handleNoMatchingPolicies(input, entitlement);
			}
			else {
		    	_Logger.info(String.format("Found [%d] recursive policy/policies for resource [%s]", policies.size(), entitlement.resource));
				handleExistingMatchingPolicy(policies, input, entitlement);
			}
		}
    }

    private static void handleExistingMatchingPolicy(final List<Policy> policies, final Input input, final Entitlement entitlement) {
    	
    	if (entitlement.isAdd()) {
    		// we just need to update 1 policy.  We know these are matching policies, pick the first one that isn't multi-resource
    		Policy policy = null;
    		for (Policy aPolicy : policies) {
    			if (!aPolicy.hasMultipleResources()) {
    				policy = aPolicy;
    				break;
    			}
    		}
    		Permissions permissions = newPermission(entitlement.group);
    		if (policy == null) {
    			_Logger.info("Didn't fund any non multi-resource policy.  Creating a new one");
    			createPolicy(input.host, input.repository, entitlement.resource, entitlement.group);
    		}
    		else {
    			_Logger.info(String.format("Updating policy[%s] for resource [%s] with group[%s]", policy.getId(), entitlement.resource, entitlement.group));
    			if (policy.isEnabled) {
		    		policy.permMapList.add(permissions);
    			}
    			else {
    				_Logger.warn(String.format("Policy [%s] is diabled! Purging all existing permissions for the new one", policy.getId()));
    				if (policy.isForASingleGroup(entitlement.group)) {
    					_Logger.info(String.format("Policy[%s] is for a single group[%s].  Hence, setting right access type and enabling it.", policy.getId(), entitlement.group));
    					// we are replacing it to ensure that access-types are right
        				policy.permMapList = new ArrayList<Permissions>();
        				policy.permMapList.add(permissions);
        				policy.isEnabled = true;
    				}
    				else {
    					_Logger.warn(String.format("Policy[%s] has permissions for user/groups other than group[%s]! Updating it with new permissions, but leaving it disabled!",
    							policy.getId(), entitlement.group));
        				policy.permMapList.add(permissions);
    				}
    			}
	    		updatePolicy(policy, input.host);
	    		
    		}
    	}
    	else if (entitlement.isDelete()) {
    		// We have to edit all policies and yank any access permissions for the entitlement group
    		Iterator<Policy> policyIterator = policies.iterator();
    		while (policyIterator.hasNext()) {
    			Policy policy = policyIterator.next();
    			// We don't manage multi-resource policies!
    			if (policy.hasMultipleResources()) {
    				_Logger.warn(String.format("Found multi-resource policy [%s] that entitlement resource [%s].  Ignoring...!", policy, entitlement.resource));
    			}
    			else {
        			Iterator<Permissions> permissionsIterator = policy.permMapList.iterator();
        			boolean updated = false;
        			while (permissionsIterator.hasNext()) {
        				Permissions permissions = permissionsIterator.next();
        				if (permissions.removeGroup(entitlement.group)) {
        					_Logger.debug(String.format("Removed group [%s] from policy[%s].", entitlement.group, policy.getId()));
	        				// if after removal of our group there aren't any users or groups left in the permission then delete it!
	        				if (permissions.isEmpty()) {
	        					permissionsIterator.remove();
	        					_Logger.debug(String.format("Permissions empty after group removal.  Removing Permissions record from policy[%s].", policy.getId()));
	        				}
	        				updated = true;
        				}
        				else {
        					_Logger.info(String.format("Permissions didn't have group [%s] in it! ok", entitlement.group)); 
        				}
        			}
        			// if after our pruning the policy doesn't have any permissions in it then delete it, else update it
        			if (updated) {
	        			if (policy.isEmpty()) {
        					_Logger.info(String.format("Policy is empty after group removal.  Removing policy[%s].", policy.getId()));
	        				deletePolicy(policy, input.host);
	        			}
	        			else {
        					_Logger.info(String.format("Saving permissions changes to policy[%s].", policy.getId()));
	        				updatePolicy(policy, input.host);
	        			}
        			}
        			else {
        				_Logger.info(String.format("Policy[%s] left unchanged.", policy.getId()));
        			}
    			}
    		}
    	}
    	else {
			_Logger.warn(String.format("Unsupported entitlement action [%s] for entitlement[%s]", entitlement.action, entitlement.toString()));
    	}
    }

	private static void handleNoMatchingPolicies(final Input input,
			final Entitlement entitlement) {

    	if (entitlement.isAdd()) {
    		_Logger.info(String.format("Creating a new policy for entitlement[%s].", entitlement));
    		createPolicy(input.host, input.repository, entitlement.resource, entitlement.group);
    	}
    	else if (entitlement.isDelete()) {
    		_Logger.info(String.format("Leaving policies unchanged for entitlement [%s].", entitlement));
    	}
    	else {
			_Logger.warn(String.format("Unsupported entitlement action [%s] for entitlement[%s]", entitlement.action, entitlement.toString()));
    	}
	}

	private static ServiceResponse listPolicies(String host, String repository, String resource) {
    	_Logger.info(String.format("listPolicies: input: host[%s], repository[%s], resource[%s].",
    			host, repository, resource));
    	String json = ClientBuilder.newClient()
    		.target(getPolicyURL(host))
    		.queryParam(Constants.Parameter.Repository, repository)
    		.queryParam(Constants.Parameter.Resource, resource)
    		.request()
    		.header(Constants.Headers.Auth, getBasicAuth())
    		.accept(MediaType.APPLICATION_JSON)
    		.get()
    		.readEntity(String.class);
    	_Logger.debug(String.format("listPolicies: Json response [%s].", json));

    	ServiceResponse serviceResponse = _Gson.fromJson(json, ServiceResponse.class);
    	_Logger.debug(String.format("listPolicies: converted to ServiceResponse object [%s].", serviceResponse));
    	
    	_Logger.info(String.format("listPolicies: returning [%d] matching policies with ids[%s].", serviceResponse.vXPolicies.size(), serviceResponse.getPolicyIds())); 
    	return serviceResponse;
    }
    
	private static void deletePolicy(Policy policy, String host) {
		String json = _Gson.toJson(policy);
		_Logger.debug("deletePolicy: json [" + json + "].");
    	int status = ClientBuilder.newClient()
        		.target(getPolicyURL(host))
        		.path(policy.getId())
        		.request()
        		.header(Constants.Headers.Auth, getBasicAuth())
        		.accept(MediaType.APPLICATION_JSON)
        		.delete()
        		.getStatus();
        _Logger.info(String.format("deletePolicy: plicy[%s] DELETE status: [%d]", policy.getId(), status));		
	}
	
	private static void updatePolicy(Policy policy, String host) {
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
        _Logger.info(String.format("updatePolicy: policy[%s] PUT status: [%d].", policy.getId(), status));
	}
	
	private static void createPolicy(String host, String repository, String resource, String group) {
		Permissions permissions = newPermission(group);
		Policy policy = new Policy(resource, repository, permissions);
		String json = _Gson.toJson(policy);
		_Logger.debug("createPolicy: json [" + json + "].");
		
    	int status = ClientBuilder.newClient()
        		.target(getPolicyURL(host))
        		.request()
        		.header(Constants.Headers.Auth, getBasicAuth())
        		.accept(MediaType.APPLICATION_JSON)
        		.post(Entity.entity(json, MediaType.APPLICATION_JSON))
        		.getStatus();
        _Logger.info(String.format("createPolicy: POST status: [%d].", status));		
	}

	private static List<Entitlement> parseFile(String fileName) {
		File file = new File(fileName);
		
		List<Entitlement> result = new ArrayList<Entitlement>();
		try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
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
	
	private static Permissions newPermission(String group) {
		List<String> groups = new ArrayList<String>();
		groups.add(group);
		List<String> accesses = Arrays.asList(new String[] { "Read", "Write", "Execute" });
		List<String> users = new ArrayList<String>();
		
		Permissions permissions = new Permissions(groups, users, accesses);
		return permissions;
	}
	
    private static String getPolicyURL(String host) {
    	return String.format("http://%s:6080/service/public/api/policy", host);
    }
    
    private static String getBasicAuth() {
		String basicAuth = "Basic " + DatatypeConverter.printBase64Binary("admin:admin".getBytes());
		return basicAuth;
    }
    
	private static void showUsage() {
		System.exit(Constants.ExitStatus.InvalidUsage);
	}
	
	static public class Entitlement {
		final String action;
		final String resource;
		final String group;
		
		public Entitlement(String anAction, String aResource, String aGroup) {
			this.action = anAction;
			this.resource = aResource;
			this.group = aGroup;
		}
		
		public boolean isDelete() {
			return action.equals(Constants.Action.Delete);
		}

		public boolean isAdd() {
			return action.equals(Constants.Action.Add);
		}

		public String toString() {
			return MoreObjects.toStringHelper("Entitlement")
					.add("Action", action)
					.add("Resource", resource)
					.add("Group", group)
					.toString();
		}
		
		public static Entitlement createEntitlements(String[] tokens) {
			String action = tokens[0];
			String resource = tokens[1];
			String group = tokens[2];
			Entitlement entitlement = new Entitlement(action, resource, group);
			
			return entitlement;
		}
	}

	
	static public class Input {
		final String host;
		final String repository;
		final String file;
		
		public Input(String hostName, String repositoryName, String fileName) {
			this.host = hostName;
			this.repository = repositoryName;
			this.file = fileName;
		}

		public String toString() {
			return MoreObjects.toStringHelper("Input")
					.add("Host", host)
					.add("RepositoryName", repository)
					.add("FileName", file)
					.toString();
		}
		
		static Input parseInputs(String[] args) {
			String hostName = args[0];
			String repositoryName = args[1];
			String fileName = args[2];
			Input input = new Input(hostName, repositoryName, fileName);
			
			return input;
		}
	}

	private static final Logger _Logger = Logger.getLogger(App.class); 
	private static final Gson _Gson = new Gson();
}
