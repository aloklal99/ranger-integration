package com.hortonworks.ranger;

public class Constants {
	
	static class Input {
		static final int argumentCount = 3;
	}
	static class ExitStatus {
		static final int InvalidUsage = 1;
		static final int MissingInputFile = 2;
		static final int FileOpenError = 3;
	}
	
	static class Parameter {
		static final String Resource = "resourceName";
		public static final String Repository = "repositoryName";
		public static final String Enabled = "isEnabled";
		public static final String Recursive = "isRecursive";
	}

	static class MediaType {
		static final String JSON = "application/json";
	}
	
	static class Ranger {
		static final String PolicyURL = "http://192.168.56.101:6080/service/public/api/policy";
		
	}

	static class Action {
		static final String Add = "ADD";
		static final String Delete = "DELETE";
	}

	static class Headers {
		static final String Auth = "Authorization";
	}
}

