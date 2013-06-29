/**
 *
 */
package org.socialbiz.cog.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import org.socialbiz.cog.exception.NGException;
import org.socialbiz.cog.AccessControl;
import org.socialbiz.cog.AddressListEntry;
import org.socialbiz.cog.AuthRequest;
import org.socialbiz.cog.BaseRecord;
import org.socialbiz.cog.HistoryRecord;
import org.socialbiz.cog.LicensedURL;
import org.socialbiz.cog.NGBook;
import org.socialbiz.cog.NGPage;
import org.socialbiz.cog.NGRole;
import org.socialbiz.cog.NGPageIndex;
import org.socialbiz.cog.ProcessRecord;
import org.socialbiz.cog.SectionUtil;
import org.socialbiz.cog.GoalRecord;
import org.socialbiz.cog.UserProfile;
import org.socialbiz.cog.UtilityMethods;
import org.socialbiz.cog.spring.NGWebUtils;

/**
 * This class will handle all requests managing process/tasks. Currently this is
 * handling only requests that are coming to create a new task, update/modify
 * task and also reassign the task to other member.
 *
 */
@Controller
public class ProcessController extends BaseController {

    public static final String CREATE_TASK = "Create Task";
    public static final String BOOK_ATT = "book";
    public static final String PROCESS_HTML = "projectActiveTasks.htm";
    public static final String PROJECT_TASKS="Project Tasks";
    public static final String SUCCESS_LOCAL = "success_local";
    public static final String SUCCESS_REMOTE = "success_remote";
    public static final String REMOTE_PROJECT = "Remote_project";
    public static final String LOCAL_PROJECT = "Local_Project";

    @RequestMapping(value = "/{account}/{pageId}/CreateTask.form", method = RequestMethod.POST)
    public ModelAndView createTask(@PathVariable String account, @PathVariable String pageId,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("Must be logged in to create a task.");
            NGPageIndex.assertBook(account);

            // call a method for creating new task
            taskAction(ar, pageId, request, null, false);

            String assignto= ar.defParam("assignto", "");
            NGWebUtils.updateUserContactAndSaveUserPage(ar, "Add",assignto);

            return redirectBrowser(ar,PROCESS_HTML);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.create.task", new Object[]{pageId,account} , ex);
        }
    }

    public static String parseEmailId(String assigneTo) throws Exception {

        String assignessEmail = "";
        String[] emails = UtilityMethods.splitOnDelimiter(assigneTo, ',');

        for (String email : emails) {
            if (!email.equals("")) {
                int bindx = email.indexOf('<');
                int length = email.length();
                if (bindx > 0) {
                    email = email.substring(bindx + 1, length - 1);
                }

                assignessEmail = assignessEmail + "," + email;
            }
        }
        if (assignessEmail.startsWith(",")) {
            assignessEmail = assignessEmail.substring(1);
        }
        return assignessEmail;
    }

    @RequestMapping(value = "/{account}/{pageId}/createSubTask.form", method = RequestMethod.POST)
    public ModelAndView createSubTask(@PathVariable
    String account, @PathVariable
    String pageId, @RequestParam
    String taskId, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("Must be logged in to manipulate tasks.");
            NGPageIndex.assertBook(account);

            taskAction(ar, pageId, request, taskId, false);

            String assignto= ar.defParam("assignto", "");
            NGWebUtils.updateUserContactAndSaveUserPage(ar, "Add",assignto);

            String go = ar.reqParam("go");
            modelAndView = new ModelAndView(new RedirectView(go));

        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.create.sub.task", new Object[]{pageId,account} , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/{account}/{pageId}/subtask.htm", method = RequestMethod.GET)
    public ModelAndView showSubTask(@PathVariable
    String account, @PathVariable
    String pageId, @RequestParam
    String taskId, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("Must be logged in to open a sub task");
            NGPageIndex.assertBook(account);

            NGPage nGPage = NGPageIndex.getProjectByKeyOrFail(pageId);

            modelAndView = new ModelAndView("subtask");
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute(BOOK_ATT, account);
            request.setAttribute("tabId", PROJECT_TASKS);
            request.setAttribute("pageId", pageId);
            request.setAttribute("taskId", taskId);
            request.setAttribute("title", " : " + nGPage.getFullName());
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.create.task.page", new Object[]{pageId,account} , ex);
        }
        return modelAndView;
    }

    public void taskAction(AuthRequest ar, String pageId,
            HttpServletRequest request, String parentTaskId, boolean updateTask)
            throws Exception
    {
        NGPage ngp = NGPageIndex.getProjectByKeyOrFail(pageId);
        ar.setPageAccessLevels(ngp);
        ar.assertMember("Must be a member of a project to manipulate tasks.");
        ar.assertContainerFrozen(ngp);

        UserProfile userProfile = ar.getUserProfile();

        int eventType;
        boolean isSubTask = false;

        GoalRecord task = null;

        if (updateTask) {
            task = ngp.getGoalOrFail(parentTaskId);
            eventType = HistoryRecord.EVENT_TYPE_MODIFIED;
            task.setSynopsis(ar.reqParam("taskname_update"));
        } else {
            isSubTask = (parentTaskId != null && parentTaskId.length() > 0);
            if (isSubTask) {
                GoalRecord parentTask = ngp.getGoalOrFail(parentTaskId);
                task = ngp.createSubGoal(parentTask);
                eventType = HistoryRecord.EVENT_TYPE_SUBTASK_CREATED;
            } else {
                task = ngp.createGoal();
                eventType = HistoryRecord.EVENT_TYPE_CREATED;
            }
            task.setSynopsis(ar.reqParam("taskname"));
        }

        if (isSubTask) {
            // set the subtask's due date.
            task.setDueDate(ngp.getGoalOrFail(parentTaskId).getDueDate());
        } else {
            String duedate=null;
            if (updateTask) {
                duedate = ar.defParam("dueDate_update","");
            }else{
                duedate = ar.defParam("dueDate","");
            }
            if(duedate.length()>0){
                task.setDueDate(SectionUtil.niceParseDate(duedate));
            }
        }

        if (updateTask) {
            task.setStartDate(SectionUtil.niceParseDate(ar.defParam("startDate_update","")));
            task.setEndDate(SectionUtil.niceParseDate(ar.defParam("endDate_update","")));
        }

        String assignto= ar.defParam("assignto", null);

        if (assignto==null || assignto.length()==0) {
            //The assignee is not set, then set assignee to the current user.
            //They can change that later.
            task.getAssigneeRole().addPlayer(new AddressListEntry(userProfile));
        }
        else {
            String assignee = parseEmailId(assignto);
            if(assignee!=null) {
                //if the assignee is set, use it
                task.getAssigneeRole().addPlayer(new AddressListEntry(assignee));
            }
            else {
                //if the assignee is not set, then set assignee to the current user.
                task.getAssigneeRole().addPlayer(new AddressListEntry(userProfile));
            }
        }


        String processLink = ar.defParam("processLink", "");
        if (processLink.length()>0) {
            task.setSub(processLink);
        }

        task.setPriority(Integer.parseInt(ar.defParam("priority", "0")));
        task.setDescription(ar.defParam("description", ""));
        if (updateTask) {
            task.setState(Integer.parseInt(ar.defParam("state", "0")));
            if(task.getState()==5){
                task.setPercentComplete(100);
            }
        }else{
            // should allow the user to specify the desired state, but if not specified,
            // the default should be "offered" when creating a task.

            String startOption=ar.defParam("startActivity", "");
            if(startOption.length()>0){
                task.setState(BaseRecord.STATE_STARTED);
            }else{
                task.setState(BaseRecord.STATE_UNSTARTED);
            }

        }


        task.setCreator(userProfile.getUniversalId());
        task.setModifiedDate(ar.nowTime);
        task.setModifiedBy(ar.getBestUserId());

        HistoryRecord.createHistoryRecord(ngp, task.getId(),
                        HistoryRecord.CONTEXT_TYPE_TASK, eventType,
                        ar, "");

        ngp.savePage(ar, CREATE_TASK);
    }




    @RequestMapping(value = "/{account}/{pageId}/task{taskId}.htm", method = RequestMethod.GET)
    public ModelAndView displayTask(@PathVariable String account,
        @PathVariable String pageId,  @PathVariable String taskId,
        HttpServletRequest request,   HttpServletResponse response)
            throws Exception
    {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            NGPageIndex.assertBook(account);
            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(pageId);
            ar.setPageAccessLevels(ngp);
            GoalRecord task = ngp.getGoalOrFail(taskId);

            boolean canAccessTask = AccessControl.canAccessGoal(ar, ngp, task);

            if(!canAccessTask){
                if(!ar.isLoggedIn()){
                    request.setAttribute("property_msg_key", "message.login.to.see.task.detail");
                }else if(!ar.isMember()){
                    request.setAttribute("property_msg_key", "nugen.process.edit.task.memberlogin");
                }
                modelAndView = new ModelAndView("Warning");
            }else{
                if(!ar.isLoggedIn()){
                    modelAndView=new ModelAndView("displayTaskInfo");
                }else{
                    modelAndView=new ModelAndView("editprocess");
                    List<NGBook> memberOfAccounts = new ArrayList<NGBook>();
                    for(NGBook aBook : NGBook.getAllAccounts()) {
                        if (aBook.primaryOrSecondaryPermission(ar.getUserProfile())) {
                            memberOfAccounts.add(aBook);
                        }
                    }
                    request.setAttribute("bookList",memberOfAccounts);
                }
            }

            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute(BOOK_ATT, account);
            request.setAttribute("tabId", PROJECT_TASKS);
            request.setAttribute("pageId", pageId);
            request.setAttribute("taskId", taskId);

        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.edit.task.page", new Object[]{pageId,account} , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/{account}/{pageId}/updateTaskStatus.ajax", method = RequestMethod.POST)
    public void handleActivityUpdates(@PathVariable String account, @PathVariable String pageId,@RequestParam String pId,
                HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        System.out.println("updateTaskStatus.ajax -- START");
        AuthRequest ar = null;
        try{
            ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("Can't edit a work item.");
            NGPageIndex.assertBook(account);

            NGPage ngp=null;

            if(pId.equals(null)){
                ngp = (NGPage) NGPageIndex.getContainerByKeyOrFail(pageId);
            }else{
                ngp = (NGPage) NGPageIndex.getContainerByKeyOrFail(pId);
            }
            ar.setPageAccessLevels(ngp);
            ar.assertMember("Unable to edit tasks on this page.");

            String id = ar.reqParam("id");
            GoalRecord task = ngp.getGoalOrFail(id);
            String index=ar.reqParam("index");
            int eventType = HistoryRecord.EVENT_TYPE_MODIFIED;
            String comments = "";
            String action = ar.reqParam("action");
            if (action.equals("Start Offer")) {
                long beginTime = task.getStartDate();
                if (beginTime == 0) {
                    task.setStartDate(ar.nowTime);
                }
                task.setState(BaseRecord.STATE_STARTED);
                eventType = HistoryRecord.EVENT_TYPE_STATE_CHANGE_STARTED;
            } else if (action.equals("Mark Accepted") || action.equals("Accept Activity")) {
                long beginTime = task.getStartDate();
                if (beginTime == 0) {
                    task.setStartDate(ar.nowTime);
                }
                task.setState(BaseRecord.STATE_ACCEPTED);
                eventType = HistoryRecord.EVENT_TYPE_STATE_CHANGE_ACCEPTED;
            } else if (action.equals("Complete Activity")) {
                task.setEndDate(ar.nowTime);
                task.setState(BaseRecord.STATE_COMPLETE);
                task.setPercentComplete(100);
                eventType = HistoryRecord.EVENT_TYPE_STATE_CHANGE_COMPLETE;
            } else if (action.equals("Update Status")) {
                String newStatus = ar.defParam("status", null);
                task.setStatus(newStatus);
            } else if (action.equals("Approve")) {
                eventType = HistoryRecord.EVENT_TYPE_APPROVED;
                task.approve(ar.getBestUserId());
                comments = ar.defParam("reason", "");
            } else if (action.equals("Reject")) {
                eventType = HistoryRecord.EVENT_TYPE_REJECTED;
                task.reject(ar.getBestUserId());
                comments = ar.defParam("reason", "");
            } else {
                throw new NGException("nugen.exceptionhandling.did.not.understand.option", new Object[]{action});
            }

            task.setModifiedDate(ar.nowTime);
            task.setModifiedBy(ar.getBestUserId());
            HistoryRecord.createHistoryRecord(ngp, task.getId(),
                    HistoryRecord.CONTEXT_TYPE_TASK, eventType, ar, comments);

            ngp.savePage(ar, "Edit Work Item");

            JSONObject jo = new JSONObject();
            jo.put("msgType", "success");
            jo.put("taskId", task.getId());
            jo.put("index", index);
            jo.put("taskState", String.valueOf(task.getState()));
            jo.put("stateImage",BaseRecord.stateImg(task.getState()));
            NGWebUtils.sendResponse(ar, jo.toString());
        }catch (Exception ex) {
            ar.logException("Caught by updateTaskStatus.ajax", ex);
            NGWebUtils.sendResponse(ar, NGWebUtils.getExceptionMessageForAjaxRequest(ex, ar.getLocale()));
        }
    }

    @RequestMapping(value = "/{account}/{pageId}/reassignTask.htm", method = RequestMethod.GET)
    public ModelAndView showReassignTask(@PathVariable
    String account, @PathVariable
    String pageId, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("Must be logged in to reassign task.");
            NGPageIndex.assertBook(account);

            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(pageId);
            ar.setPageAccessLevels(ngp);

            String taskId = ar.reqParam("taskid");
            GoalRecord task = ngp.getGoalOrFail(taskId);

            modelAndView = new ModelAndView("reassigntask");

            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute("task", task);
            request.setAttribute(BOOK_ATT, account);
            request.setAttribute("tabId", PROJECT_TASKS);
            request.setAttribute("pageId", pageId);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.reassign.task.page", new Object[]{pageId,account} , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/{account}/{pageId}/reassignTaskSubmit.form", method = RequestMethod.POST)
    public ModelAndView reassignTask(@PathVariable
    String account, @PathVariable
    String pageId, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("Must be logged in to reassign task.");
            NGPageIndex.assertBook(account);

            NGPage ngp =  (NGPage)NGPageIndex.getContainerByKeyOrFail(pageId);
            ar.setPageAccessLevels(ngp);
            ar.assertContainerFrozen(ngp);

            String taskId = ar.reqParam("taskid");
            GoalRecord task = ngp.getGoalOrFail(taskId);
            String go = ar.reqParam("go");

            String assignee = null;
            UserProfile userProfile = ar.getUserProfile();

            String assignto = ar.defParam("assignto","");
            if (assignto == null || "".equals(assignto)) {
                assignee = userProfile.getPreferredEmail();
            } else {
                assignee = parseEmailId(assignto);
            }

            String removeUser=ar.defParam("remove","");
            NGRole goalRole = task.getAssigneeRole();
            if(removeUser!=""){
                String removeAssignee= ar.defParam("removeAssignee","") ;
                goalRole.removePlayer(new AddressListEntry(removeAssignee));
            }
            else{
                goalRole.clear();
                goalRole.addPlayer(new AddressListEntry(assignee));
                NGWebUtils.updateUserContactAndSaveUserPage(ar, "Add" ,assignto);
            }

            task.setModifiedDate(ar.nowTime);
            task.setModifiedBy(ar.getBestUserId());
            HistoryRecord.createHistoryRecord(ngp, task.getId(),
                    HistoryRecord.CONTEXT_TYPE_TASK,
                    HistoryRecord.EVENT_TYPE_MODIFIED, ar, "");

            ngp.savePage(ar, CREATE_TASK);

            return redirectBrowser(ar,go);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.reassign.task", new Object[]{pageId,account} , ex);
        }
    }

    @RequestMapping(value = "/{account}/{pageId}/updateTask.form", method = RequestMethod.POST)
    public ModelAndView updateTask(@PathVariable
    String account, @PathVariable
    String pageId, @RequestParam
    String taskId,HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("Must be logged in to update task.");
            NGPageIndex.assertBook(account);
            String go = ar.reqParam("go");

            taskAction(ar, pageId, request, taskId, true);

            return redirectBrowser(ar,go);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.update.task", new Object[]{pageId,account} , ex);
        }
    }

    @RequestMapping(value = "/errorPage.htm", method = RequestMethod.GET)
     public ModelAndView errorPage(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        return new ModelAndView(Constant.COMMON_ERROR);
    }

    @RequestMapping(value = "/subProcess.ajax", method = RequestMethod.POST)
    public void searchSubProcess(HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        String responseMessage = "";
        AuthRequest ar = null;
        try{
            ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("You need to login to perform this function.");

            String processURL = ar.reqParam("processURL");
            int pcolon = processURL.indexOf(":");
            int pfs = processURL.indexOf("/");

            String processLocation = processURL.substring(0, pcolon);
            String accoundID = processURL.substring(pcolon + 1, pfs);
            String projectID = processURL.substring(pfs + 1, processURL.length());

            JSONObject param = new JSONObject();
            if (processLocation.equalsIgnoreCase("local")) {
                param.put(Constant.MSG_TYPE, SUCCESS_LOCAL);
                param.put(LOCAL_PROJECT, getLocalProcess(accoundID, projectID, ar));
            }else if(processLocation.equalsIgnoreCase("http")){
                param.put(Constant.MSG_TYPE, SUCCESS_REMOTE);
                param.put(Constant.MESSAGE, REMOTE_PROJECT);
            }
            responseMessage = param.toString();
        }catch(Exception ex){
            responseMessage = NGWebUtils.getExceptionMessageForAjaxRequest(ex, ar.getLocale());
            ar.logException("Caught by updateTaskStatus.ajax", ex);
        }
        NGWebUtils.sendResponse(ar, responseMessage);
    }

    public String getLocalProcess(String accountId, String projectID,
            AuthRequest ar) throws Exception {

        for (NGPageIndex ngpi : NGPageIndex.getAllProjectsInAccount(accountId)) {
            NGPage page = ngpi.getPage();
            if (page.getKey().equalsIgnoreCase(projectID)) {
                ar.setPageAccessLevels(page);
                ProcessRecord process = page.getProcess();
                LicensedURL thisProcessUrl = process.getWfxmlLink(ar);
                return thisProcessUrl.getCombinedRepresentation();
            }
            NGPageIndex.releaseLock(page);
        }

        return "No project found";
    }

    @RequestMapping(value = "/{account}/{pageId}/statusUpdateForTask.form", method = RequestMethod.POST)
    public ModelAndView statusUpdateForTask(@PathVariable
    String account, @PathVariable
    String pageId, @RequestParam
    String taskId,HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                super.redirectToLoginPage(ar, "Must be logged in to update task.", new Exception("Must be logged in"));
                return null;
            }
            NGPageIndex.assertBook(account);

            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(pageId);
            ar.setPageAccessLevels(ngp);
            ar.assertMember("Must be a member of a project to manipulate tasks.");
            ar.assertContainerFrozen(ngp);

            String accomp=ar.defParam("accomp", "");
            String status=ar.defParam("status", "");
            GoalRecord task  = ngp.getGoalOrFail(taskId);
            task.setStatus(status);
            task.setState(Integer.parseInt(ar.defParam("states", "0")));
            if(task.getState()==5){
                task.setPercentComplete(100);
            }else{
                task.setPercentComplete(Integer.parseInt(ar.defParam("percentage", "0")));
            }
            int eventType = HistoryRecord.EVENT_TYPE_MODIFIED;

            task.setModifiedDate(ar.nowTime);
            task.setModifiedBy(ar.getBestUserId());
            HistoryRecord.createHistoryRecord(ngp, task.getId(),
                            HistoryRecord.CONTEXT_TYPE_TASK, eventType,
                            ar, accomp);
            String go = ar.reqParam("go");

            ngp.savePage(ar, "updated a task status");

            return redirectBrowser(ar,go);

        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.project.update.status", new Object[]{pageId,account} , ex);
        }
    }

    @RequestMapping(value = "/{account}/{pageId}/setOrderTasks.ajax", method = RequestMethod.POST)
    public void updateOrderTasks( @PathVariable String pageId,@RequestParam String indexString,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        AuthRequest ar = null;
        String responseMessage = "";
        try{
            ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("You need to login to perform this function.");

            NGPage ngp = (NGPage)NGPageIndex.getContainerByKeyOrFail(pageId);
            List<GoalRecord> tasks = ngp.getAllGoals();

            //Get the ranks of all the tasks from the browser
            Vector<String> idlist = UtilityMethods.splitString(indexString, ',');
            int last = idlist.size();
            int[] previous = new int[last];

            for (int i=0; i<last; i++){
                String taskId=idlist.elementAt(i);
                boolean found = false;
                for (GoalRecord task : tasks) {
                    if(task.getId().equals(taskId)){
                        previous[i] = task.getRank();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new NGException("nugen.exception.task.not.found", new Object[]{taskId});
                }
            }

            //sort the array so that the tasks will be in order
            Arrays.sort(previous);

            //set the ranks back into the tasks so they take the spot that the
            //other task was in.  In effect, when you reorder tasks, they take
            //the same absolute position that the other task used to have.
            for (int i=0; i<last; i++){
                String taskId=idlist.elementAt(i);
                for (GoalRecord task : tasks) {
                    if(task.getId().equals(taskId)){
                        task.setRank(previous[i]);
                        break;
                    }
                }
            }

            //clean up and normalize into canonical form
            ngp.renumberGoalRanks();

            JSONObject paramMap = new JSONObject();
            paramMap.put(Constant.MSG_TYPE,SUCCESS_LOCAL);
            paramMap.put(Constant.MSG_DETAIL,"Update Order Successfull");
            responseMessage = paramMap.toString();
            ngp.savePage(ar, "Reordered the tasks");
        }catch (Exception ex) {
            responseMessage = NGWebUtils.getExceptionMessageForAjaxRequest(ex, ar.getLocale());
            ar.logException("Caught by SUCCESS_LOCAL.ajax", ex);
        }
        NGWebUtils.sendResponse(ar, responseMessage);
    }


    @RequestMapping(value = "/{account}/{projectId}/tasks.csv", method = RequestMethod.GET)
    public void getTasks( @PathVariable String account, @PathVariable String projectId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            NGPageIndex.assertBook(account);
            NGPage nGPage = NGPageIndex.getProjectByKeyOrFail(projectId);

            response.setContentType("text/csv;charset=UTF-8");
            MultiLevelNumber numbers = new MultiLevelNumber();

            ar.write("Task,Assignee,Status,Percent,StartDate,EndDate,DueDate,Description,ID\n");

            List<GoalRecord> tasks = nGPage.getAllGoals();
            for (GoalRecord task : tasks) {

                int percent = task.getCorrectedPercentComplete();
                int taskLevel = determineTaskLevel(task);
                String taskNums = numbers.nextAtLevel(taskLevel);

                writeStrForCsv(ar,taskNums);
                writeStrForCsv(ar," ");
                writeStrForCsv(ar,task.getSynopsis());
                ar.write(",");
                NGRole assignee = task.getAssigneeRole();
                List<AddressListEntry> players = assignee.getExpandedPlayers(nGPage);
                boolean needspace = false;
                for (AddressListEntry ale : players) {
                    if (needspace) {
                        ar.write(" ");
                    }
                    writeStrForCsv(ar,ale.getName());
                    needspace = true;
                }
                ar.write(",");
                writeStrForCsv(ar,BaseRecord.stateName(task.getState()));
                ar.write(",");
                ar.write(Integer.toString(percent));
                ar.write("%,");
                writeDateForCsv(ar, task.getStartDate());
                ar.write(",");
                writeDateForCsv(ar, task.getEndDate());
                ar.write(",");
                writeDateForCsv(ar, task.getDueDate());
                ar.write(",");
                writeStrForCsv(ar,task.getDescription());
                ar.write(",");
                writeStrForCsv(ar,task.getId());
                ar.write("\n");
            }
            ar.flush();

        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.task.csv", new Object[]{projectId,account} , ex);
        }
    }

    private int determineTaskLevel(GoalRecord task) throws Exception
    {
        int level=0;
        GoalRecord t2 = task.getParentGoal();
        while (t2 != null)
        {
            level++;
            t2 = t2.getParentGoal();
        }
        return level;
    }

    private void writeStrForCsv(AuthRequest ar, String str) throws Exception
    {
        StringBuffer stripped = new StringBuffer();
        for (int i=0; i<str.length(); i++)
        {
            char ch = str.charAt(i);

            // just strip commas and returns (all control characters) from text for now
            // there is another mode that uses quotes to allow commas, but not supported yet
            if (ch!=',' && ch>=' ') {
                stripped.append(ch);
            }
        }
        ar.write( stripped.toString() );
    }

    private void writeDateForCsv(AuthRequest ar, long date) throws Exception
    {
        //TODO: figure out how CSV dates are stored
        SectionUtil.nicePrintTimestamp(ar.w, date);
    }
}
