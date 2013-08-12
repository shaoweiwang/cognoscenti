package org.socialbiz.cog.spring;

import java.io.StringWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.socialbiz.cog.AuthDummy;
import org.socialbiz.cog.AuthRequest;
import org.socialbiz.cog.BaseRecord;
import org.socialbiz.cog.BookInfoRecord;
import org.socialbiz.cog.EmailSender;
import org.socialbiz.cog.ErrorLog;
import org.socialbiz.cog.GoalRecord;
import org.socialbiz.cog.NGBook;
import org.socialbiz.cog.NGPage;
import org.socialbiz.cog.NGPageIndex;
import org.socialbiz.cog.ProcessRecord;
import org.socialbiz.cog.SectionWiki;
import org.socialbiz.cog.UserPage;
import org.socialbiz.cog.dms.ResourceEntity;
import org.socialbiz.cog.exception.NGException;
import org.socialbiz.cog.exception.ProgramLogicError;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class AdminController extends BaseController {

    @RequestMapping(value = "/{account}/{project}/changeGoal.form", method = RequestMethod.POST)
    public ModelAndView changeGoalHandler(@PathVariable String account,@PathVariable String project,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return redirectToLoginView(ar, "message.loginalert.change.goal",null);
            }
            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(project);
            ar.setPageAccessLevels(ngp);
            ar.assertAuthor("Unable to change the name of this page.");
            ar.assertContainerFrozen(ngp);

            ProcessRecord process = ngp.getProcess();
            process.setSynopsis(ar.reqParam("goal"));
            process.setDescription(ar.reqParam("purpose"));

            ngp.saveFile(ar, "Changed Goal and/or Purpose of Project");
            NGPageIndex.refreshOutboundLinks(ngp);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.change.goal", new Object[]{project,account} , ex);
        }
        return new ModelAndView(new RedirectView( "admin.htm"));
    }

    @RequestMapping(value = "/{account}/{project}/deleteProject.htm", method = RequestMethod.POST)
    public ModelAndView deleteProjectHandler(@PathVariable String account,@PathVariable String project,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        throw new Exception("This handler 'deleteProject' is never used");
        /*
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to delete project.");

            String action   = ar.reqParam("action");
            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(project);
            ar.setPageAccessLevels(ngp);
            ar.assertAuthor("Unable to delete this page. ");

            //first, handle cancel operation.
            if ("Delete Project".equals(action))
            {
                ngp.markDeleted(ar);
            }
            else if ("Un-Delete Project".equals(action))
            {
                ngp.markUnDeleted(ar);
            }
            else
            {
                throw new NGException("nugen.exceptionhandling.system.not.understand.action", new Object[]{action});
            }
            ngp.savePage(ar, action);

            modelAndView = new ModelAndView(new RedirectView( "admin.htm"));
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.delete.project", new Object[]{project,account} , ex);
        }
        return modelAndView;
        */

    }

    @RequestMapping(value = "/{account}/{project}/changeProjectName.form", method = RequestMethod.POST)
    public ModelAndView changeProjectNameHandler(@PathVariable String account,@PathVariable String project,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {

        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to change the name of project.");
            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(project);
            ar.setPageAccessLevels(ngp);
            ar.assertAuthor("Unable to change the name of this page.");

            String newName = ar.reqParam("newName");
            String[] nameSet = ngp.getPageNames();

            //first, see if the new name is one of the old names, and if so
            //just rearrange the list
            int oldPos = findString(nameSet, newName);
            if (oldPos<0)
            {
                //we did not find the value, so just insert it
                nameSet = insertFront(nameSet, newName);
            }
            else
            {
                insertRemove(nameSet, newName, oldPos);
            }
            ngp.setPageNames(nameSet);

            ngp.saveFile(ar, "Change Name Action");

            modelAndView = new ModelAndView(new RedirectView( "admin.htm"));
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.change.project.name", new Object[]{project,account} , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/{account}/{project}/deletePreviousProjectName.htm", method = RequestMethod.GET)
    public ModelAndView deletePreviousAccountNameHandler(@PathVariable String account, @PathVariable String project,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {

        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to delete previous name of project.");
            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(project);
            ar.setPageAccessLevels(ngp);
            ar.assertAuthor("Unable to change the name of this page.");

            String oldName = ar.reqParam("oldName");

            String[] nameSet = ngp.getPageNames();
            int oldPos = findString(nameSet, oldName);

            if (oldPos>=0)
            {
                nameSet = shrink(nameSet, oldPos);
                ngp.setPageNames(nameSet);
            }
            ngp.saveFile(ar, "Change Name Action");

            modelAndView = new ModelAndView(new RedirectView( "admin.htm") );
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.delete.previous.project.name", new Object[]{project,account} , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/{account}/$/changeAccountName.form", method = RequestMethod.POST)
    public ModelAndView changeAccountNameHandler(@PathVariable String account,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to delete previous name of account.");
            NGBook ngb = (NGBook)NGPageIndex.getContainerByKeyOrFail(account);
            ar.setPageAccessLevels(ngb);
            ar.assertAuthor("Unable to change the name of this page.");

            String newName = ar.reqParam("newName");
            String[] nameSet = ngb.getAccountNames();
            //first, see if the new name is one of the old names, and if so
            //just rearrange the list
            int oldPos = findString(nameSet, newName);
            if (oldPos<0)
            {
                //we did not find the value, so just insert it
                nameSet = insertFront(nameSet, newName);
            }
            else
            {
                insertRemove(nameSet, newName, oldPos);
            }
            ngb.setAccountNames(nameSet);

            ngb.saveFile(ar, "Change Name Action");

            modelAndView = new ModelAndView(new RedirectView( "admin.htm"));
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.change.account.name", new Object[]{account} , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/{accountId}/$/changeAccountDescription.form", method = RequestMethod.POST)
    public ModelAndView changeAccountDescriptionHandler(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {

        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to change description of account.");
            NGBook account = (NGBook)NGPageIndex.getContainerByKeyOrFail(accountId);
            ar.setPageAccessLevels(account);
            String action = ar.reqParam("action");
            ar.assertAuthor("Unable to change account settings.");
            if(action.equals("Change Description")){
                String newDesc = ar.reqParam("desc");
                account.setDescription( newDesc );
            }else if(action.equals("Change Theme")){
                String theme = ar.reqParam("theme");
                account.setThemePath(BookInfoRecord.themePath(Integer.parseInt(theme)));
            }

            account.saveFile(ar, "Change Account Settings");

            modelAndView = new ModelAndView(new RedirectView( "admin.htm"));
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.change.account.description", new Object[]{accountId} , ex);
        }
        return modelAndView;

    }

    @RequestMapping(value = "/{accountId}/$/deletePreviousAccountName.htm", method = RequestMethod.GET)
    public ModelAndView deletePreviousProjectNameHandler(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {

        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to delete previous name of account.");
            NGBook account = (NGBook)NGPageIndex.getContainerByKeyOrFail(accountId);
            ar.setPageAccessLevels(account);
            ar.assertAuthor("Unable to change the name of this page.");

            String oldName = ar.reqParam("oldName");

            String[] nameSet = account.getAccountNames();
            int oldPos = findString(nameSet, oldName);

            if (oldPos>=0)
            {
                nameSet = shrink(nameSet, oldPos);
                account.setAccountNames(nameSet);
            }

            account.saveFile(ar, "Change Name Action");

            modelAndView = new ModelAndView(new RedirectView( "admin.htm") );
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.delete.previous.account.name", new Object[]{accountId} , ex);
        }
        return modelAndView;
    }

    // compare the sanitized versions of the names in the array, and if
    // the val equals one, return the index of that string, otherwise
    // return -1
    public int findString(String[] array, String val)
    {
        String sanVal = SectionWiki.sanitize(val);
        for (int i=0; i<array.length; i++)
        {
            String san2 = SectionWiki.sanitize(array[i]);
            if (sanVal.equals(san2))
            {
                return i;
            }
        }
        return -1;
    }

    //insert the specified value into the array, and shift the values
    //in the array up to the specified point.  The value at that position
    //will be effectively removed.  The values after that position remain
    //unchanged.
    public void insertRemove(String[] array, String val, int position)
    {
        String replaceVal = val;
        for (int i=0; i<position; i++)
        {
            String tmp = array[i];
            array[i] = replaceVal;
            replaceVal = tmp;
        }
        array[position] = replaceVal;
    }

    //insert at beginning, Returns a new string array that is
    //one value larger
    public String[] insertFront(String[] array, String val)
    {
        int len = array.length;
        String[] ret = new String[len+1];
        ret[0] = val;
        for (int i=0; i<len; i++)
        {
            ret[i+1] = array[i];
        }
        return ret;
    }

    //insert at beginning, Returns a new string array that is
    //one value larger
    public String[] shrink(String[] array, int pos)
    {
        int len = array.length;
        String[] ret = new String[len-1];
        for (int i=0; i<pos; i++)
        {
            ret[i] = array[i];
        }
        for (int i=pos+1; i<len; i++)
        {
            ret[i-1] = array[i];
        }
        return ret;
    }

    @RequestMapping(value = "/validateHtml.validate", method = RequestMethod.POST)
    public ModelAndView validateHtml(HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to validate HTML.");
            String htmlDom = ar.reqParam("output");

            JTidyValidator validator = new JTidyValidator();
            List<XHTMLError> errors = validator.validate(htmlDom, ar.w);

            modelAndView = new ModelAndView("htmlValidator");
            modelAndView.addObject("errors", errors);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.validate.html", null , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/sendErrortoAdmin.ajax", method = RequestMethod.POST)
    public void sendErrorToAdmin(HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        AuthRequest ar = null;
        String message = "";
        try{
            ar = AuthRequest.getOrCreate(request, response);

            String htmlDom = ar.reqParam("errorData");
            String comments = ar.defParam("user_comments","");
            String errorId = ar.reqParam("errorId");
            String searchByDate = ar.reqParam("dateTime");
            long searchDate = Long.parseLong(searchByDate);
            ErrorLog.logUserComments(errorId, searchDate, comments);

            sendErrorMessageEmail( ar, htmlDom,comments );

            message = NGWebUtils.getJSONMessage(Constant.SUCCESS, "", "");
        }catch(Exception ex){
            message = NGWebUtils.getExceptionMessageForAjaxRequest(ex, ar.getLocale());
            ar.logException(message, ex);
        }
        NGWebUtils.sendResponse(ar, message);
    }

    private static void sendErrorMessageEmail(AuthRequest ar,String errorDOM,String comments)
    throws Exception
    {
        StringWriter bodyWriter = new StringWriter();
        AuthRequest clone = new AuthDummy(ar.getUserProfile(), bodyWriter);
        clone.setNewUI(true);
        clone.retPath = ar.baseURL;
        clone.write("<html><body>\n");
        clone.write("<p>User got the following error while using Cognoscenti. ");
        if(ar.getUserProfile()!=null){
            clone.write("Reported by ");
            ar.getUserProfile().writeLink( clone );
            clone.write( ". " );
        }

        clone.write("</p>");

        if(comments!=null && comments.length()>0){
            clone.write("<h3> Comments from User: </h3>");
            clone.write( "<p>" );
            clone.writeHtml( comments );
            clone.write( "</p>" );
        }

        clone.write( errorDOM);

        clone.write("<p>You are receiving this message because you are a Super Admin of this server.</p>");
        clone.write("</body></html>");
        EmailSender.simpleEmail(NGWebUtils.getSuperAdminMailList(ar), null, "Error report", bodyWriter.toString());
    }

    @RequestMapping(value = "/{account}/{project}/updateProjectSettings.ajax", method = RequestMethod.POST)
    public void updateProjectSettings(@PathVariable String account,@PathVariable String project,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        String responseMessage = "";
        AuthRequest ar = null;
        try{
            ar = NGWebUtils.getAuthRequest(request, response, "User must be logged in to update project settings.");

            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(project);
            ar.setPageAccessLevels(ngp);
            ar.assertAuthor("Unable to change project settings.");
            String operation = ar.reqParam("operation");
            if("publicPermission".equals(operation)){
                ngp.setAllowPublic(ar.reqParam("allowPublic"));
            }else if("freezeProject".equals(operation)){
                ngp.freezeProject(ar);
            }else if("unfreezeProject".equals(operation)){
                ngp.unfreezeProject();
            }
            ngp.saveFile(ar, "Updated allow public document.");

            JSONObject jo = new JSONObject();
            jo.put(Constant.MSG_TYPE , Constant.SUCCESS);
            responseMessage = jo.toString();
        }
        catch(Exception ex){
            responseMessage = NGWebUtils.getExceptionMessageForAjaxRequest(ex, ar.getLocale());
            ar.logException("Caught by updateProjectSettings.ajax", ex);
        }
        NGWebUtils.sendResponse(ar, responseMessage);
    }

    @RequestMapping(value = "/{accountId}/{pageId}/updateProjectSettings.form", method = RequestMethod.POST)
    public ModelAndView updateProjectSettingsForm(@PathVariable String accountId,@PathVariable String pageId,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to change the goal/purpose of project.");
            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(pageId);
            ar.setPageAccessLevels(ngp);
            ar.assertAuthor("Unable to change the name of this page.");
            ar.assertContainerFrozen(ngp);

            String symbol = ar.defParam("symbol", null);
            String action = ar.reqParam("action");

            if(action.equals("Update")){

                if (symbol!=null) {
                    UserPage uPage = ar.getUserPage();
                    ResourceEntity defFolder = uPage.getResourceFromSymbol(symbol);

                    //remember, if the default folder belongs to someone else, it will
                    //not be found using the above.
                    if (defFolder!=null) {
                        ngp.setDefRemoteFolder(defFolder);
                    }
                }
                ngp.saveFile(ar, "Updated default location of project.");
                modelAndView = new ModelAndView(new RedirectView( "admin.htm"));
                modelAndView.addObject("folderId", null);
                modelAndView.addObject("path", null);
            }else {
                throw new ProgramLogicError("updateProjectSettingsForm does not understand action '"+action+"'");
            }

            NGPageIndex.refreshOutboundLinks(ngp);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.update.project.settings", new Object[]{accountId,pageId} , ex);
        }
        return modelAndView;
    }

    @RequestMapping(value = "/{account}/{project}/changeProjectSettings.form", method = RequestMethod.POST)
    public ModelAndView changeProjectSettings(@PathVariable String account,@PathVariable String project,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            ar.assertLoggedIn("User must be logged in to change the settings of project.");
            NGPage ngp = NGPageIndex.getProjectByKeyOrFail(project);
            ar.setPageAccessLevels(ngp);
            ar.assertAuthor("Unable to change the name of this page.");

            ProcessRecord process = ngp.getProcess();
            process.setSynopsis(ar.defParam("goal",""));
            process.setDescription(ar.defParam("purpose",null));

            ngp.setUpstreamLink(ar.defParam("upstream",null));

            // setting public
            ngp.setAllowPublic(ar.defParam("allowPublic","no"));

            String projectMode = ar.reqParam("projectMode");
            if("freezedMode".equals(projectMode)){
                ngp.freezeProject(ar);
                ngp.markUnDeleted(ar);
            }else if("normalMode".equals(projectMode)){

                ngp.markUnDeleted(ar);
                ngp.unfreezeProject();
            }else if ("deletedMode".equals(projectMode))
            {

                ngp.markDeleted(ar);
                ngp.freezeProject(ar);
            }

            String symbol    = ar.defParam("symbol",null);
            if (symbol!=null) {
                UserPage uPage = ar.getUserPage();
                ResourceEntity defFolder = uPage.getResourceFromSymbol(symbol);

                if (defFolder!=null) {
                    //setting default location if it belows to the logged in user
                    ngp.setDefLocation(defFolder.getFullPath());
                    ngp.setDefFolderId(defFolder.getFolderId());
                    ngp.setDefUserKey(ar.getUserProfile().getKey());
                }
            }

            List<GoalRecord> tasks = ngp.getAllGoals();
            for (GoalRecord task : tasks){
                if("freezedMode".equals(projectMode) || "deletedMode".equals(projectMode)){
                    if(task.getState() == BaseRecord.STATE_ERROR ||
                            task.getState()  == BaseRecord.STATE_ACCEPTED ||
                            task.getState()  == BaseRecord.STATE_STARTED ||
                            task.getState()  == BaseRecord.STATE_REVIEW){
                        task.setLastState(String.valueOf(task.getState()));
                        task.setState(BaseRecord.STATE_FROZEN);
                    }
                }else{
                    if(task.getState() == BaseRecord.STATE_FROZEN){
                        task.setState(GoalRecord.safeConvertInt(task.getLastState()));
                    }
                }
            }

            ngp.setProjectMailId(ar.defParam("projectMailId",""));

            ngp.saveFile(ar, "Changed Goal and/or Purpose of Project");
            NGPageIndex.refreshOutboundLinks(ngp);
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.admin.update.project.settings", new Object[]{project,account} , ex);
        }
        return new ModelAndView(new RedirectView( "admin.htm"));
    }
}
