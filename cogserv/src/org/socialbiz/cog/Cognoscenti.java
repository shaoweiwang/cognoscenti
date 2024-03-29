package org.socialbiz.cog;

import java.io.File;
import java.util.Timer;

/**
 * This is the main class for the Cognoscenti object package.
 * All the methods are static.
 * Use this class for initialization and accessing
 * the main index to objects within Cognoscenti
 *
 */
public class Cognoscenti {

    public static Exception lastFailureMsg = null;
    public static boolean isInitialized = false;
    public static boolean initializingNow = false;

    /**
     * Call this in order to erase everything in memory and
     * free up all of the cached values.  Returns the module to
     * uninitialized state.  You  need to reinitialize after this.
     * Useful before calling garbage collect and reinitialize.
     */
    public synchronized static void clearAllStaticVariables() {
        NGPageIndex.clearAllStaticVars();
        MicroProfileMgr.clearAllStaticVars();
        AuthDummy.clearStaticVariables();
        isInitialized = false;
        initializingNow = false;
    }

    /**
     * From the passed in values will initialize the module.
     * @param rootFolder is the root on the installed folder and requires that there
     *        be a file at {rootFolder}/WEB-INF/config.txt
     * @param backgroundTimer is used for all the background activity for
     *        sending and receiving email, passing a null in will disable
     *        email sending and receiving
     * @exception will be thrown if anything in the configuration appears to be incorrect
     */
    public static synchronized void initializeAll(File rootFolder, Timer backgroundTimer) throws Exception {
    	try {  	    
    	    if (isInitialized) {
    	        //NOTE: two or more threads might try to call this at the same time.
    	        //Method is synchronized which will block threads, but when they get in here, 
    	        //the first thing to do is check if you were initialized while blocked.
    	        if (rootFolder.equals(ConfigFile.getFileFromRoot(""))) {
    	            //all OK, just return
        	        return;
    	        }
    	        //was initialized to a different location, clean up that
    	        System.out.print("Reinitialization: changing from "+ConfigFile.getFileFromRoot("")
    	                +" to "+rootFolder);
    	        clearAllStaticVariables();
    	    }
	        initializingNow = true;
	        ConfigFile.initialize(rootFolder);
	        ConfigFile.assertConfigureCorrectInternal();
	
	        AuthDummy.initializeDummyRequest();
	        UserManager.loadUpUserProfilesInMemory();
	        String attachFolder = ConfigFile.getProperty("attachFolder");
	        File attachFolderFile = new File(attachFolder);
	        AttachmentVersionSimple.attachmentFolder = attachFolderFile;
	        NGPageIndex.initIndex();
	        MicroProfileMgr.loadMicroProfilesInMemory();
	        if (backgroundTimer!=null) {
	            EmailSender.initSender(backgroundTimer);
	            SendEmailTimerTask.initEmailSender(backgroundTimer);
	            EmailListener.initListener(backgroundTimer);
	        }
	        isInitialized = true;
    	}
    	catch (Exception e) {
    		lastFailureMsg = e;
    		throw e;
    	}
    	finally {
            initializingNow = false;   		
    	}
    }
    
}
