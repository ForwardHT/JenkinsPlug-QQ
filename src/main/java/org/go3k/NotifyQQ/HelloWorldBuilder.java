package org.go3k.NotifyQQ;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class HelloWorldBuilder extends 
    // Builder {
    Notifier {
    // private final String qqnumber;
    private final List<QQNumber> qQNumbers;
    private final String qqmessage;


    private PrintStream logger;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public HelloWorldBuilder(List<QQNumber> qQNumbers, String qqmessage) {
        // this.qQNumbers = qQNumbers;
        this.qQNumbers = new ArrayList<QQNumber>( qQNumbers );
        this.qqmessage = qqmessage;
    }

    public List<QQNumber> getQQNumbers() {
        return qQNumbers;
    }

    public String getQqmessage() {
        return qqmessage;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        // This is where you 'build' the project.
        // Since this is a dummy, we just say 'hello world' and call that a build.

        logger = listener.getLogger();

        Jenkins.getInstance();
        String jobURL = "";
        String build_series = "";
        String build_num = "";
        String projectName = "";
        String jobName = "";
        String buildID = "";
        if (qqmessage == null || qqmessage.isEmpty()){

            //检查代码构建。
            try
            {
                jobName = build.getEnvironment(listener).expand("${JOB_NAME}");

            }
            catch (Exception e)
            {
                logger.println("tokenmacro expand error.");
            }
            projectName = jobName + "";


        }else {
            // 正常构建
            try
            {
                jobURL = build.getEnvironment(listener).expand("${JOB_URL}");
                jobName = build.getEnvironment(listener).expand("${JOB_NAME}");
                logger.println("jobURL = " + jobURL);
                build_series = build.getEnvironment(listener).expand("${BUILD_SERIES}");
                build_num = build.getEnvironment(listener).expand("${BUILD_NUM}");
                buildID = build.getEnvironment(listener).expand("${BUILD_ID}");

            }
            catch (Exception e)
            {
                logger.println("tokenmacro expand error.");
            }
            int build_Num_Total = Integer.parseInt(build_num) + Integer.parseInt(buildID);

            projectName =jobName + "_" + build_series + "." + build_Num_Total;

        }



        String msg = "";
        if (build.getResult()==Result.SUCCESS)
        {
            if (qqmessage == null || qqmessage.isEmpty()){


            }else {
                msg = qqmessage;
                for (int i = 0; i < qQNumbers.size(); i++) {
                    QQNumber number = (QQNumber)qQNumbers.get(i);
                    if( number.getType() == QQType.Qun ){
                        send(QunGenerateMessageURL(number.GetUrlString(),msg));
                    }
                    else{
                        send(GenerateMessageURL(number.GetUrlString(), msg));

                    }
                }

            }

        }
        else
        {
            msg += projectName + "---编译失败了...";
            msg += "jenkins地址:" + jobURL;
            for (int i = 0; i < qQNumbers.size(); i++) {
                QQNumber number = (QQNumber)qQNumbers.get(i);
                send(GenerateMessageURL(number.GetUrlString(), msg));
            }
        }

        msg = URLEncoder.encode(msg);
        msg = msg.replaceAll("\\+", "_");



        return true;
    }
    private String QunGenerateMessageURL(String qq, String msg)
    {
        return String.format("http://127.0.0.1:5000/openqq/send_group_message?uid%s&content=%s", qq, msg);
    }
    private String GenerateMessageURL(String qq, String msg)
    {
        return String.format("http://127.0.0.1:5000/openqq/send_friend_message?uid%s&content=%s", qq, msg);
    }

    protected void send(String url){
        logger.println("Sendurl: " + url);
        
        HttpURLConnection connection = null;
        InputStream is = null;
        String resultData = "";
        try {
            URL targetUrl = new URL(url);
            connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.connect();
            is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);  
            BufferedReader bufferReader = new BufferedReader(isr);
            String inputLine = "";  
            while((inputLine = bufferReader.readLine()) != null){  
                resultData += inputLine + "\n";  
            }

            logger.println("response: " + resultData);
        } catch (Exception e) {
            logger.println("http error." + e);
        } finally {
          if(is != null){  
                try {  
                    is.close();  
                } catch (IOException e) {  
                    // TODO Auto-generated catch block  
                    e.printStackTrace();  
                }  
            }  
            if(connection != null){  
                connection.disconnect();  
            }
        }
        logger.println("Send url finish");
    }
    
    protected void sendAsync(String url){
    	RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(100000)
                .setConnectTimeout(100000).build();
    	CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().
    			setDefaultRequestConfig(requestConfig)
    			.build();
    	try {
            httpclient.start();
            final HttpGet request = new HttpGet(url);
            httpclient.execute(request, new FutureCallback<HttpResponse>() {

                @Override
                public void completed(final HttpResponse response) {
                	logger.println(request.getRequestLine() + "->" + response.getStatusLine());
                }

                @Override
                public void failed(final Exception ex) {
                	logger.println(request.getRequestLine() + "->" + ex);
                }

                @Override
                public void cancelled() {
                	logger.println(request.getRequestLine() + " cancelled");
                }
            });
        } catch (Exception e) {
        	logger.println("http error." + e); 
        } finally {
        	try { httpclient.close(); } catch (Exception e) {}
        }
    	logger.println("send Done");
    }
    
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends 
        BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        // public FormValidation doCheckName(@QueryParameter String value)
        //         throws IOException, ServletException {
        //     if (value.length() == 0)
        //         return FormValidation.error("Please set a name");
        //     if (value.length() < 4)
        //         return FormValidation.warning("Isn't the name too short?");
        //     return FormValidation.ok();
        // }

        public FormValidation doCheckNumber(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() <= 4)
                return FormValidation.error("你QQ号太短了吧。。。");
            else if (value.length() > 15)
                return FormValidation.error("QQ号有这么长吗？");
            else if (!isNumeric(value))
                return FormValidation.error("QQ号格式不对，数字数字数字！");
            return FormValidation.ok();
        }

        private boolean isNumeric(String str){
            Pattern pattern = Pattern.compile("[0-9]*");
            Matcher isNum = pattern.matcher(str);
            if( !isNum.matches() ){
               return false;
            }
            return true;
        }

        public FormValidation doCheckQqmessage(@QueryParameter String value)
                throws IOException, ServletException {
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "QQ通知";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
    }
}

