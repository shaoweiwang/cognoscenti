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

package org.socialbiz.cog.rest;

import java.io.File;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.socialbiz.cog.Cognoscenti;
import org.socialbiz.cog.ConfigFile;
import org.socialbiz.cog.NGPageIndex;

/**
 * Implements the server initialization protocol.  The variable "serverInitState" tells
 * what the current state of the entire server initialization.  All other services should
 * look at this state variable, and only proceed with regular functions when the server
 * is in the STATE_RUNNING state.
 *
 * When the server is in any other state, normal user-oriented requests should not be handled.
 * User requests at those times should be forwarded to a simple page that announces only
 * that the server is still starting up.  This prevents the problem that a user request
 * might occur before the server is completely initialized.
 *
 * Some failures of the server might cause the server to re-initialize, so other parts of the
 * system should never assume that because they encountered the RUNNING state, that it will
 * always remain in the running state.
 *
 * The state variable starts in the INITIAL state, but that is immediately changed to the
 * TESTING state.
 *
 * During TESTING, the server attempts to read the config files and initialize all the
 * proper internal variable.  This may involve contacting external servers that it depends
 * on to make sure they are there.   This will either transition to RUNNING or to FAILED
 *
 * In the FAILED state, the server will not serve any end-user request, but will allow the
 * administrator to restart initialization.  The server will rest in FAILED state for
 * about 30 seconds, and then will automatically try to initialize again.  This way, if
 * there was some temporary reason that the server could not start (e.g. it needed another
 * service that had not completed its start up yet), this will eventually allow it to start.
 *
 * RUNNING state is where the server spends most of the time, and it is where the end-user
 * requests are handled.
 *
 * PAUSED state is essentially for shutting down properly.  The server should enter the PAUSED
 * state for about 30 second before actually stopping.  This allows all the current request
 * to be cleanly handled, but no further requests to be started in that time.  This might also
 * be used to re-initialize the server, go into PAUSED state for 30 seconds, before going
 * and reinitializing the entire server from config files again.
 *
 * This class is purely to implement this protocol.  It is fundamentally a thread that will
 * check on the state every 30 seconds.  If that thread finds the server in the FAILED
 * state, it will attempt to reinitialize it, possibly returning it to the FAILED state,
 * but also possibly transitioning into the running state.
 */
public class ServerInitializer extends TimerTask {

    public static int serverInitState = 0;
    public static Exception lastFailureMsg = null;
    public static long lastInitAttemptTime = 0;

    public static final int STATE_INITIAL = 0;
    public static final int STATE_TESTING = 1;
    public static final int STATE_FAILED  = 2;
    public static final int STATE_PAUSED  = 3;
    public static final int STATE_RUNNING = 4;

    private static ServerInitializer singletonInitializer = null;
    private static Timer timerForInit = null;
    private static Timer timerForOtherTasks = null;

    private ServletConfig config;
    private ServerInitializer(ServletConfig newConfig) {
        config = newConfig;
    }

    /**
     * Starts the background task to initialize the server from config files.
     * Also attempts to initialize immediately.
     * This should be called ONLY ONCE when the server starts the first time.
     * After that, methods on this can pause and restart the server.
     * @param config the ServletConfig from the hosting TomCat server
     * @param resourceBundle the resource bundle from the wrapped servlet
     */
    public static void startTheServer(ServletConfig config) {
        if (serverInitState != STATE_INITIAL) {
            //report this, but otherwise ignore it.  Not bad enough to throw exception.
            System.out.println("Somthing wrong, the server is being initialized when not in initial state!");
        }

        //in FAILED state the time task will immediately try to reinitialize it.
        serverInitState = STATE_FAILED;
        singletonInitializer = new ServerInitializer(config);
        timerForInit = new Timer("Initialization Timer", true);
        timerForInit.scheduleAtFixedRate(singletonInitializer, 30000, 30000);

        //call it directly to initialize right away if possible
        singletonInitializer.run();
    }

    /**
     * Convenience routine will return true if the server is in the running state, and
     * false if it is in any other state
     */
    public static boolean isRunning() {
        return serverInitState == STATE_RUNNING;
    }

    /**
     * Convenience routine will return true if the server is currently reading the config
     * file, and initializing.  You will only see this if initialization takes a long time,
     * or if you are extremely lucky.
     */
    public static boolean isActivelyStarting() {
        return serverInitState == STATE_TESTING;
    }

    /**
     * puts the server into the PAUSED state.
     * You should delay for an additional time (30 seconds) before
     * doing anything else to the server to all all the other threads
     * to complete what they are doing.
     */
    public static void pauseServer() {
        serverInitState = STATE_PAUSED;

        //cancel all the background processing from this existing timer
        if (timerForOtherTasks!=null) {
            timerForOtherTasks.cancel();
        }
        timerForOtherTasks = null;
        Cognoscenti.isInitialized = false;
        System.out.println("COG SERVER CHANGE - New state "+getServerStateString());
    }

    /**
     * Takes the server from the PAUSED mode and attempts to reinitialize it.
     * Results in server in either RUNNING mode or FAILED mode.
     * @return
     */
    public static void reinitServer() throws Exception {
        if (serverInitState != STATE_PAUSED) {
            pauseServer();
            //throw new Exception("The server can only be reinitialized after it has been paused.");
        }
        serverInitState = STATE_FAILED;
        singletonInitializer.run();
        System.out.println("COG SERVER CHANGE - New state "+getServerStateString());
    }

    public synchronized void run()
    {
        //any non-FAILED state, there is nothing to do, so exit quick as possible
        //this get hit every 30 seconds or so while running.
        if (serverInitState != STATE_FAILED) {
            return;
        }

        System.out.println("COG SERVER INIT - Starting state ("+getServerStateString()+") at "+new Date());
        //only if it is in FAILED state, it should attempt to reinitialize everything.
        //Init fails if any init method throws an exception
        try {
            ServletContext sc = config.getServletContext();
            File rootFolder = new File(sc.getRealPath(""));
            serverInitState = STATE_TESTING;
            lastInitAttemptTime = System.currentTimeMillis();

            //I don't know if this is needed.  Basically, you should never be in this
            //situation, but it makes sense to clean things up before restarting.
            if (timerForOtherTasks!=null) {
                timerForOtherTasks.cancel();
            }
            timerForOtherTasks = new Timer("Main Cog Timer", true);

            //start by clearing everything ... in case there is mess left over.
            Cognoscenti.clearAllStaticVariables();

            //garbage collect at this time, cleans out the heap space
            //freeing up and defragmenting memory
            System.gc();

            Cognoscenti.initializeAll(rootFolder, timerForOtherTasks);

            SSOFIUserManager.initSSOFI(ConfigFile.getProperty("baseURL"));
            
            System.out.println("ServerInitializer: successfully initialized and ready");
            serverInitState = STATE_RUNNING;
            lastFailureMsg = null;
        }
        catch (Exception e) {
            lastFailureMsg = e;
            serverInitState = STATE_FAILED;
            try {
                System.out.println("ServerInitializer: (FAILED) because "+e.toString());
                e.printStackTrace();
                if (timerForOtherTasks!=null) {
                    timerForOtherTasks.cancel();
                }
                timerForOtherTasks = null;
	            Cognoscenti.clearAllStaticVariables();
                NGPageIndex.clearAllStaticVars();
            }
            catch (Exception eee) {
                //just ignore this failure to report the real exception and cleanup
            }
        }
        finally {
            NGPageIndex.clearLocksHeldByThisThread();
        }
        System.out.println("COG SERVER INIT - Concluding state "+getServerStateString());
    }

    public static String getServerStateString() {
        if (serverInitState==STATE_RUNNING) {
            return "Running";
        }
        if (serverInitState==STATE_FAILED) {
            return "Failed";
        }
        if (serverInitState==STATE_PAUSED) {
            return "Paused";
        }
        if (serverInitState==STATE_INITIAL) {
            return "Initial";
        }
        if (serverInitState==STATE_TESTING) {
            return "Testing";
        }
        return "Unknown";
    }
}
