/*
 * Copyright 2013 Keith D Swenson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors Include: Shamim Quader, Sameer Pradhan, Kumar Raja, Jim Farris,
 * Sandia Yang, CY Chen, Rajiv Onat, Neal Wang, Dennis Tam, Shikha Srivastava,
 * Anamika Chaudhari, Ajay Kakkar, Rajeev Rastogi
 */

package org.socialbiz.cog;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.socialbiz.cog.exception.NGException;
import org.w3c.dom.Document;

public class ErrorLog extends DOMFile {

    public ErrorLog(File path, Document newDoc) throws Exception {
        super(path, newDoc);
    }

    private static  ErrorLog cachedLogFile=null;

    public static ErrorLog getLogForDate(long dateValue) throws Exception {

        // create a log file name based on the date passed in.
        String encodedDate = new SimpleDateFormat("yyyy.MM.dd").format(dateValue);
        String fileName = "errorLog_"+ encodedDate.substring(0,10)+".xml";
        String userFolder = ConfigFile.getProperty("userFolder");
        File newPlace = new File(userFolder, fileName);

        ErrorLog eLog = cachedLogFile;

        //maybe this one is already cached ... if so use that.
        if (eLog!=null && newPlace.equals(eLog.getFilePath())) {
            return eLog;
        }

        //check to see if the file is there, just a schema upgrade block
        if (!newPlace.exists())  {
            //it might be in the OLD config directory.
            File oldPlace = ConfigFile.getFile(fileName);
            //if so, move it to the proper place
            if (oldPlace.exists()) {
                DOMFile.moveFile(oldPlace, newPlace);
            }
        }

        //not cached, so load or create a new one
        Document errorLogDoc = readOrCreateFile(newPlace, "errorlog");
        cachedLogFile=new ErrorLog(newPlace, errorLogDoc);
        return cachedLogFile;
    }

    /**
     * Returns the error details for the specified error id.
     * @param errorId that you are looking for details on
     * @return the error details, or null if no error with that id
     */
    public ErrorLogDetails getDetails(String errorId) throws Exception {
        for (ErrorLogDetails errorLogDetails : getChildren("error", ErrorLogDetails.class)) {
            if(errorLogDetails.getErrorNo().equals(errorId)){
                return errorLogDetails;
            }
        }
        return null;
    }


    private long logsError(UserProfile up,String msg,Throwable ex, String errorURL,long nowTime) throws Exception {

        String userName="GUEST";

        if (up!=null) {
            userName = up.getName()+"("+up.getKey()+")";
        }
        StackTraceElement[] element =ex.getStackTrace()  ;

        ErrorLogDetails errorLogDetails = createChild("error", ErrorLogDetails.class);
        long exceptionNO = SuperAdminLogFile.getNextExceptionNo();
        SuperAdminLogFile.setLastExceptionNo(exceptionNO);
        errorLogDetails.setErrorNo(String.valueOf(exceptionNO));

        errorLogDetails.setModified(userName, nowTime);

        errorLogDetails.setFileName(element[0].getFileName());
        errorLogDetails.setURI(errorURL);

        if (msg!=null && msg.length()>0) {
            errorLogDetails.setErrorMessage(msg+"\n"+NGException.getFullMessage(ex, Locale.getDefault()));
        } else {
            errorLogDetails.setErrorMessage(NGException.getFullMessage(ex, Locale.getDefault()));
        }
        errorLogDetails.setErrorDetails(convertStackTraceToString(ex));

        save();
        return exceptionNO;
    }

    public static File getErrorFileFullPath(Date date) throws Exception {

        String searchByDate=new SimpleDateFormat("yyyy.MM.dd").format(date);
        String userFolder = ConfigFile.getProperty("userFolder");
        return new File(userFolder, "errorLog_"+searchByDate+".xml");
    }

    /**
     * @deprecated - just use getDetails instead.
     *
     * This rather silly routine takes the ErrorDetailsObject, and copies all the values
     * into a hashtable, so that the calling routine can get the values out of a hash table
     * instead of getting them from the original object.    IT is more of this strange
     * desire to defeat all the safeguards that a compiler brings through strong typing,
     * and replace it will a non-typed object accessed through uncontrolled name values.
     */
    public static HashMap<String, String> getMapOfPropertiesForOneErrorID(String errorId,String searchByDate) throws Exception {

        ErrorLog errorLog = ErrorLog.getLogForDate(Long.parseLong(searchByDate));
        ErrorLogDetails errorDetails = errorLog.getDetails(errorId);

        HashMap<String, String> hashTableCopyOfErrorDetails =  new HashMap<String, String>();

        if(errorDetails!=null){
            hashTableCopyOfErrorDetails.put("ErrorDescription", errorDetails.getErrorDetails());
            hashTableCopyOfErrorDetails.put("ErrorMessage", errorDetails.getErrorMessage());

            Date newDate = new Date(errorDetails.getModTime());
            hashTableCopyOfErrorDetails.put("Date&Time", new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS").format(newDate));

            hashTableCopyOfErrorDetails.put("URL", errorDetails.getURI());
            hashTableCopyOfErrorDetails.put("errorNo", errorDetails.getErrorNo());
            hashTableCopyOfErrorDetails.put("userDetails", errorDetails.getModUser());
            hashTableCopyOfErrorDetails.put("userComments", errorDetails.getUserComment());
        }
        return hashTableCopyOfErrorDetails;
    }

    public static void logUserComments(String errorId, long logFileDate, String comments) throws Exception {

        ErrorLog errorLog = getLogForDate(logFileDate);
        ErrorLogDetails eDetails = errorLog.getDetails(errorId);
        eDetails.setUserComment(comments);
        errorLog.save();
    }

    public static String convertStackTraceToString(Throwable exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(" [ ");
        exception.printStackTrace(pw);
        pw.print(" ] ");
        return sw.toString();
    }


    public static synchronized long logException(String msg, Throwable ex, long nowTime,
            UserProfile userProfile, String errorURL) {
        try {
            ErrorLog errorLog = ErrorLog.getLogForDate(nowTime);

            //redundantly included in the system out as well
            //maybe someday this will not be necessary???
            System.out.println("\nLOGGED EXCEPTION: "+ new Date(nowTime) + " actually " + new Date());
            PrintWriter pw = new PrintWriter(System.out);
            ex.printStackTrace(pw);
            pw.flush();

            long retval = -1;
            if (errorLog != null) {
                retval = errorLog.logsError(userProfile, msg, ex, errorURL, nowTime);
            }
            else {
                System.out.println("logfile for date missing: "+new Date(nowTime));
            }

            System.out.println("\n^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            return retval;
        }
        catch (Exception e) {
            System.out.println("FATAL FAILURE TO LOG ERROR: "+e);
            // what else to do? ... crash the server. If your log file
            // is not working there is very little else to be done.
            // Might as well try throwing the exception...
            throw new RuntimeException("Can not write other exception to log file", e);
        }
    }


}

