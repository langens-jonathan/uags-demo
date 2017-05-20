package delta_service.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import delta_service.graphs.YAMLScopesFile;

public class Configuration {
    /*
     * Logging etc
     */
    public static boolean logAllQueries = true;
    public static boolean logImportantQueries = true;
    public static boolean logDeltaResults = true;

    /*
     * Describing the query endpoint
     */
    public static String queryEndpoint = "http://db:8890/sparql";
    public static String queryUser=null;
    public static String queryPwd=null;

    /*
     * Describing the update endpoint
     */
    public static String updateEndpoint = "http://db:8890/sparql";
    public static String updateUser=null;
    public static String updatePwd=null;

    /*
     * if calculateEffectives is turned off the service will be a lot more
     * perfromant. the consumers of the service will however not be informed
     * that effective differences are not calculated, they just are NOT.
     */
    public static boolean calculateEffectives = true;

    private static Properties properties = null;

    private static Properties getProperties()
    {
        if(Configuration.properties == null)
        {
            FileInputStream input;
            try
            {
                String filename = System.getenv("CONFIGFILE");
                System.out.println(filename);
                input = new FileInputStream(filename);
                Configuration.properties = new Properties();
                Configuration.properties.load(input);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        return Configuration.properties;
    }
    
    private static YAMLScopesFile yamlScopesFile = null;
    
    public static YAMLScopesFile getScopesConfiguration()
    {
    	if(Configuration.yamlScopesFile == null)
    	{
    	     File file = new File("/home/langens-jonathan/projects/secret/mu-delta-service/scopes.yml");
    	      final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    	      try {
    			Configuration.yamlScopesFile = mapper.readValue(file, YAMLScopesFile.class);

    		} catch (Exception e)
    	      {
    			e.printStackTrace();
    		}
    	}    	
    	return Configuration.yamlScopesFile;
    }

    public static String getProperty(String name)
    {
        return Configuration.getProperties().getProperty(name);
    }
}
