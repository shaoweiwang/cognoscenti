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
 */

package org.socialbiz.cog.api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.socialbiz.cog.AuthRequest;
import org.socialbiz.cog.GoalRecord;
import org.socialbiz.cog.License;
import org.socialbiz.cog.NGPage;
import org.socialbiz.cog.NGPageIndex;
import org.socialbiz.cog.ServerInitializer;
import org.socialbiz.cog.UserPage;
import org.socialbiz.cog.UserProfile;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet serves up pages using the following URL format:
 *
 * http://{machine:port}/{application}/apu/{userKey}/{resource}
 *
 * http://{machine:port}/{application} is whatever you install the application to on
 * Tomcat could be multiple levels deep.
 *
 * "apu" is fixed. This is the indicator within the system that says
 * this servlet (API for users) will be invoked.
 *
 * {userKey} unique identifier for the user profile.
 *
 * All of the stuff above can be abbreviated {userAddr}
 * so the general pattern is:
 * {userAddr}/{resource}
 *
 * {resource} specifies the resource you are trying to access.
 * See below for the details.  NOTE: you only receive resources
 * that you have access to.  Resources you do not have access
 * to will not be included in the list, and will not be accessible
 * in any way.
 *
 * {userAddr}/goals.json
 * This will list all the goals for that user in all of the sites
 * at this server.
 *
 */
@SuppressWarnings("serial")
public class APUServlet extends javax.servlet.http.HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) {
        AuthRequest ar = AuthRequest.getOrCreate(req, resp);
        try {
            System.out.println("API_GET: "+ar.getCompleteURL());
            if (!ServerInitializer.isRunning()) {
                throw new Exception("Server is not ready to handle requests.");
            }
            ar.assertLoggedIn("must be logged in to get APU information ... for now");

            doAuthenticatedGet(ar);
        }
        catch (Exception e) {
            streamException(e, ar);
        }
        finally {
            NGPageIndex.clearLocksHeldByThisThread();
        }
        ar.logCompletedRequest();
    }

    private void doAuthenticatedGet(AuthRequest ar)  throws Exception {

        try {
            UserDecoder userDec = new UserDecoder(ar);

            JSONObject root = new JSONObject();
            if (userDec.lic != null) {
                root.put("license", getLicenseInfo(userDec.lic));
            }
            root.put("goals", getGoalList(ar, userDec.uProf));

            ar.resp.setContentType("application/json");
            root.write(ar.w, 2, 0);
            ar.flush();
        } catch (Exception e) {
            streamException(e, ar);
        }
    }

    public void doPut(HttpServletRequest req, HttpServletResponse resp) {
        AuthRequest ar = AuthRequest.getOrCreate(req, resp);
        ar.resp.setContentType("application/json");
        try {
             throw new Exception("Can not do a PUT to that resource URL: "+ar.getCompleteURL());
        }
        catch (Exception e) {
            streamException(e, ar);
        }
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) {
        AuthRequest ar = AuthRequest.getOrCreate(req, resp);
        ar.resp.setContentType("application/json");
        try {
            throw new Exception("Can not do a POST to that resource URL: "+ar.getCompleteURL());
        }
        catch (Exception e) {
            streamException(e, ar);
        }
    }

    public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        AuthRequest ar = AuthRequest.getOrCreate(req, resp);
        streamException(new Exception("not implemented yet"), ar);
    }

    public void init(ServletConfig config) throws ServletException {
        //don't initialize here.  Instead, initialize in SpringServlet!
    }

    private void streamException(Exception e, AuthRequest ar) {
        try {
            //all exceptions are delayed by 3 seconds to avoid attempts to
            //mine for valid license numbers
            Thread.sleep(3000);

            System.out.println("API_ERROR: "+ar.getCompleteURL());

            ar.logException("API Servlet", e);

            JSONObject errorResponse = new JSONObject();
            errorResponse.put("responseCode", 500);
            JSONObject exception = new JSONObject();
            errorResponse.put("exception", exception);

            JSONArray msgs = new JSONArray();
            Throwable runner = e;
            while (runner!=null) {
                System.out.println("    ERROR: "+runner.toString());
                msgs.put(runner.toString());
                runner = runner.getCause();
            }
            exception.put("msgs", msgs);

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            exception.put("stack", sw.toString());

            ar.resp.setContentType("application/json");
            errorResponse.write(ar.resp.writer, 2, 0);
            ar.flush();
        } catch (Exception eeeee) {
            // nothing we can do here...
            ar.logException("API Servlet Error Within Error", eeeee);
        }
    }



    private JSONObject getLicenseInfo(License lic) throws Exception {
        JSONObject licenseInfo = new JSONObject();
        if (lic == null) {
            throw new Exception("Program Logic Error: null license passed to getLicenseInfo");
        }
        licenseInfo.put("id", lic.getId());
        licenseInfo.put("timeout", lic.getTimeout());
        licenseInfo.put("creator", lic.getCreator());
        licenseInfo.put("role", lic.getRole());
        return licenseInfo;
    }


    public static JSONArray getGoalList(AuthRequest ar, UserProfile up) throws Exception {

        JSONArray goalArray = new JSONArray();

        if (up == null) {
            throw new Exception("invalid parameter to getTaskList");
        }

        for (NGPageIndex ngpi : NGPageIndex.getAllContainer()) {
            // start by clearing any outstanding locks in every loop
            NGPageIndex.clearLocksHeldByThisThread();

            if (!ngpi.isProject()) {
                continue;
            }
            NGPage aPage = ngpi.getPage();
            String urlRoot = ar.baseURL + "api/" + aPage.getSiteKey() + "/" +aPage.getKey()+"/";

            for (GoalRecord gr : aPage.getAllGoals()) {
                if (!gr.isAssignee(up)) {
                    continue;
                }
                goalArray.put(gr.getJSON4Goal(aPage, urlRoot));

            }
        }

        return goalArray;
    }
}
