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

package org.socialbiz.cog.spring;

import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import org.socialbiz.cog.exception.NGException;
import org.socialbiz.cog.AttachmentRecord;
import org.socialbiz.cog.AuthRequest;
import org.socialbiz.cog.HistoryRecord;
import org.socialbiz.cog.NGBook;
import org.socialbiz.cog.NGPageIndex;
import org.socialbiz.cog.ReminderMgr;
import org.socialbiz.cog.ReminderRecord;
import org.socialbiz.cog.spring.AttachmentHelper;

@Controller
public class AccountsDocumentController extends BaseController {

    public static final String TAB_ID = "tabId";
    public static final String ACCOUNT_ID = "accountId";

    @Autowired
    public void setContext(ApplicationContext context) {
    }

    protected void initBinder(HttpServletRequest request,
            ServletRequestDataBinder binder) throws ServletException {

        binder.registerCustomEditor(byte[].class,new ByteArrayMultipartFileEditor());

    }

    @RequestMapping(value = "/{accountId}/$/upload.form", method = RequestMethod.POST)
    protected ModelAndView uploadFile(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("fname") MultipartFile file) throws Exception {

        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }

            //Handling special case for Multipart request
            ar.req = request;

            ar.setPageAccessLevels(account);

            request.setCharacterEncoding("UTF-8");

            if (file.getSize() == 0) {
                throw new NGException("nugen.exceptionhandling.no.file.attached",null);
            }

            if(file.getSize() > 500000000){
                throw new NGException("nugen.exceptionhandling.file.size.exceeded", new Object[]{"500000000"});
            }

            String fileName = file.getOriginalFilename();

            if (fileName == null || fileName.length() == 0) {
                throw new NGException("nugen.exceptionhandling.filename.empty", null);
            }

            String visibility = ar.defParam("visibility", "*MEM*");
            String comment = ar.defParam("comment", "");
            String name = ar.defParam("name", null);

            AttachmentHelper.uploadNewDocument(ar, account, file, name, visibility, comment,ar.getBestUserId());

            modelAndView = new ModelAndView(new RedirectView("account_attachment.htm"));
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.upload.document", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/getEditAttachmentForm.form", method = RequestMethod.GET)
    protected ModelAndView getEditAttachmentForm(@PathVariable String accountId,
            @PathVariable String pageId, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }

            modelAndView = createModelView(accountId,request, response,ar,"edit_attachment","Account Documents");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.edit.attachment.page", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/emailReminder.form", method = RequestMethod.POST)
    protected ModelAndView submitEmailReminderForAttachment(
            @PathVariable String accountId,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }

            String comment = ar.reqParam("comment");
            String pname = ar.defParam("pname", "");
            String assignee = ar.reqParam("assignee");
            String instruct = ar.reqParam("instruct");
            String subj = ar.reqParam("subj");
            String destFolder = ar.reqParam("destFolder");

            ReminderMgr rMgr = account.getReminderMgr();
            ReminderRecord rRec = rMgr.createReminder(account.getUniqueOnPage());
            rRec.setFileDesc(comment);
            rRec.setInstructions(instruct);
            rRec.setAssignee(assignee);
            rRec.setFileName(pname);
            rRec.setSubject(subj);
            rRec.setModifiedBy(ar.getBestUserId());
            rRec.setModifiedDate(ar.nowTime);
            rRec.setDestFolder(destFolder);

            account.saveFile(ar, "Modified attachments");

            modelAndView = new ModelAndView(new RedirectView("account_attachment.htm"));
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.create.email.reminder", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/sendemailReminder.htm", method = RequestMethod.GET)
    protected ModelAndView sendEmailReminderForAttachment(
            @PathVariable String accountId,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }

            request.setAttribute(TAB_ID, "Account Documents");
            modelAndView = new ModelAndView("ReminderEmail");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.send.email.reminder", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/resendemailReminder.htm", method = RequestMethod.POST)
    protected ModelAndView resendEmailReminderForAttachment(
            @PathVariable String accountId,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }

            String reminderId = ar.reqParam("rid");
            String emailto = ar.defParam("emailto", null);

            ReminderRecord.reminderEmail(ar, accountId, reminderId, emailto, account);

            modelAndView = createModelView(accountId, request, response,ar,"account_attachment","Account Documents");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.resend.email.reminder", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/editAttachment.form", method = RequestMethod.POST)
    protected ModelAndView editAttachment(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "fname", required = false) MultipartFile file)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }

            //Handling special case for Multipart request
            ar.req = request;

            String visibility = ar.defParam("visibility", "*MEM*");
            String action = ar.defParam("action", "");

            modelAndView = new ModelAndView(new RedirectView("account_attachment.htm"));// createModelView(accountId, request, response,ar,"accountDocumentPage","Account Documents");
            request.setAttribute("subTabId", "nugen.projecthome.subtab.documents");
            // first, handle cancel operation. "Go back" is used for ie7
            if ("Cancel".equals(action) || "Go back".equals(action)) {
                return modelAndView;
            }else{
                AttachmentHelper.updateAttachmentFile(accountId, request, file, ar, action, visibility, account);
            }
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.edit.attachment", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/createLinkURL.form", method = RequestMethod.POST)
    protected ModelAndView createLinkURL(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.login.to.see.task.detail");
            }
            NGBook account = prepareAccountView(ar, accountId);

            String destFolder = ar.reqParam("visibility");
            String comment = ar.reqParam("comment");
            String taskUrl = ar.reqParam("taskUrl");
            String ftype = ar.reqParam("ftype");

            AttachmentRecord attachment = null;
            attachment = account.createAttachment();
            attachment.setComment(comment);
            attachment.setModifiedBy(ar.getBestUserId());
            attachment.setModifiedDate(ar.nowTime);
            attachment.setType(ftype);

            AttachmentHelper.setDisplayName(account, attachment, taskUrl);

            if (destFolder.equals("PUB")) {
                attachment.setVisibility(1);
            } else {
                attachment.setVisibility(2);
            }
            attachment.setStorageFileName(taskUrl);
            HistoryRecord.createHistoryRecord(account, attachment.getId(), HistoryRecord.CONTEXT_TYPE_DOCUMENT,
                    ar.nowTime, HistoryRecord.EVENT_DOC_ADDED, ar, "Created Link URL");

            account.saveContent(ar, "Created Link URL");

            return new ModelAndView(new RedirectView("account_attachment.htm"));
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.create.link.url", new Object[]{accountId} , ex);
        }
    }

    private ModelAndView createModelView(String accountId,
            HttpServletRequest request, HttpServletResponse response,AuthRequest ar,
            String view,  String tabId)
            throws Exception {

        request.setAttribute(TAB_ID, tabId);
        request.setAttribute(ACCOUNT_ID, accountId);
        request.setAttribute( "headerType", "account" );
        ModelAndView modelAndView = new ModelAndView(view);
        return modelAndView;
    }

    @RequestMapping(value = "/{accountId}/$/uploadDocument.htm", method = RequestMethod.GET)
    public ModelAndView uploadDocumentForm(@PathVariable String accountId, HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            request.setAttribute( "subTabId","nugen.projecthome.subtab.upload.document" );
            request.setAttribute( "realRequestURL", ar.getRequestURL() );
            request.setAttribute( "tabId", "Account Documents" );
            request.setAttribute( "pageTitle", account.getFullName() );

            modelAndView = new ModelAndView("upload_document_form_account" );
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.upload.document.page", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/linkURLToProject.htm", method = RequestMethod.GET)
    protected ModelAndView getLinkURLToProjectForm(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute("subTabId", "nugen.projecthome.subtab.link.url.to.project");
            request.setAttribute( "realRequestURL", ar.getRequestURL() );
            request.setAttribute( "tabId", "Account Documents" );
            request.setAttribute( "accountId", accountId );
            request.setAttribute( "pageTitle", account.getFullName() );

            modelAndView = createModelView(accountId, request, response,ar,"linkurlproject_form_account","Account Documents");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.link.project.page", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/emailReminder.htm", method = RequestMethod.GET)
    protected ModelAndView getEmailRemainderForm(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            request.setAttribute( "tabId", "Account Documents" );
            request.setAttribute("subTabId", "nugen.projecthome.subtab.emailreminder");
            request.setAttribute( "headerType", "account" );
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute( "pageTitle", account.getFullName() );
            modelAndView = createModelView(accountId, request, response,ar,"emailreminder_form_account","Account Documents");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.email.reminder.page", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/editDocumentForm.htm", method = RequestMethod.GET)
    protected ModelAndView getEditDocumentForm(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute("subTabId", "nugen.projectdocument.subtab.attachmentdetails");
            request.setAttribute("aid",ar.reqParam("aid"));
            request.setAttribute( "pageTitle", account.getFullName() );
            modelAndView = createModelView(accountId, request, response,ar,"edit_document_form_account","Account Documents");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.edit.document.page", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/fileVersions.htm", method = RequestMethod.GET)
    protected ModelAndView getFileVersion(@PathVariable String accountId, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute("subTabId", "nugen.projectdocument.subtab.fileversions");
            request.setAttribute("aid",ar.reqParam("aid"));
            request.setAttribute( "pageTitle", account.getFullName() );
            modelAndView = createModelView(accountId, request, response,ar,"file_version_account","Account Documents");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.file.version.page", new Object[]{accountId} , ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/updateAttachment.form", method = RequestMethod.POST)
    protected ModelAndView updateAttachment(@PathVariable String accountId,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "fname", required = false) MultipartFile file)
            throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            //Handling special case for Multipart request
            ar.req = request;
            ar.setPageAccessLevels(account);

            String aid = ar.reqParam("aid");
            AttachmentRecord attachment = account.findAttachmentByIDOrFail(aid);

            String action = ar.reqParam("actionType");

            if(action.equalsIgnoreCase("renameDoc")){
                String accessName = ar.reqParam("accessName");
                String proposedName = AttachmentHelper.assureExtension(accessName, attachment.getDisplayName());
                attachment.setDisplayName(proposedName);

            }else if(action.equalsIgnoreCase("changePermission")){
                String visibility =  ar.reqParam("visibility");

                if (visibility.equals("PUB")) {
                    attachment.setVisibility(1);
                } else {
                    attachment.setVisibility(2);
                }

            }else if(action.equalsIgnoreCase("UploadRevisedDoc")){
                if(file.getSize() <= 0){
                    throw new NGException("nugen.exceptionhandling.file.length.zero",null);
                }
                String comment_panel = ar.reqParam("comment_panel");

                attachment.setComment(comment_panel);
                AttachmentHelper.setDisplayName(account, attachment,AttachmentHelper.assureExtension(attachment.getDisplayName(), file.getOriginalFilename()));
                AttachmentHelper.saveUploadedFile(ar, attachment, file);
            }
            account.saveFile(ar, "Modified attachments");
            modelAndView = new ModelAndView(new RedirectView("account_attachment.htm"));
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.edit.attachment", new Object[]{accountId} , ex);
        }
    }

     public String loginCheckMessage(AuthRequest ar) throws Exception {
         String errorMsg = "";
        if (!ar.isLoggedIn()){
            String go = ar.getCompleteURL();
            errorMsg = "redirect:"+URLEncoder.encode(go,"UTF-8")+":"+URLEncoder.encode("Can not open form","UTF-8");
        }
        return errorMsg;
     }

    @RequestMapping(value = "/{accountId}/$/remindAttachment.htm", method = RequestMethod.GET)
    protected ModelAndView remindAttachment(@PathVariable String accountId,
        HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            request.setAttribute("subTabId", "nugen.projecthome.subtab.upload.document");
            modelAndView = createModelView(accountId, request, response,ar,"remind_attachment","Account Documents");
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.remind.attachment.page", new Object[]{accountId} , ex);
        }
    }


    @RequestMapping(value="/{accountId}/$/a/{docId}", method = RequestMethod.GET)
    public void loadDocument(@PathVariable String accountId, @PathVariable String docId,
        HttpServletRequest request, HttpServletResponse response) throws Exception
    {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                sendRedirectToLogin(ar, "message.loginalert.download.document",null);
                return;
            }
            NGBook ngb = NGPageIndex.getAccountByKeyOrFail(accountId);

            String path = request.getPathInfo();
            String attachmentName = path.substring(path.lastIndexOf("/")+1);

            ar.setPageAccessLevels(ngb);

            String version = ar.reqParam("version");
            if(version != null && version.length()!=0){
                AttachmentHelper.serveUpFileNewUI(ar, ngb, attachmentName , Integer.parseInt(version));
            }
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.download.document", new Object[]{docId, accountId}, ex);
        }
    }

    @RequestMapping(value = "/{accountId}/$/account_reminders.htm", method = RequestMethod.GET)
    public ModelAndView remindersTab(@PathVariable String accountId,
                                        HttpServletRequest request, HttpServletResponse response)
                                        throws Exception {
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook account = prepareAccountView(ar, accountId);
            ModelAndView modelAndView = executiveCheckViews(ar);
            if (modelAndView != null) {
                return modelAndView;
            }
            modelAndView=new ModelAndView("reminders_account");
            request.setAttribute("subTabId", "nugen.projecthome.subtab.reminders");
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute( "tabId", "Account Documents" );
            request.setAttribute( "pageTitle", account.getFullName() );
            return modelAndView;
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.remind.attachment.page", new Object[]{accountId} , ex);
        }
    }


    @RequestMapping(value = "/{accountId}/$/docinfo{docId}.htm", method = RequestMethod.GET)
    protected ModelAndView accountDocInfo(@PathVariable String accountId,
            @PathVariable String docId, HttpServletRequest request,
            HttpServletResponse response) throws Exception
    {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            //Note: this view displays for both logged in and not logged in people
            NGBook ngb = NGPageIndex.getAccountByKeyOrFail(accountId);
            ar.setPageAccessLevels(ngb);
            request.setAttribute("aid", docId);
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute("accountId", accountId);
            request.setAttribute("headerType", "account");
            request.setAttribute("tabId", "Account Documents" );
            request.setAttribute("subTabId", "nugen.projectdocument.subtab.attachmentdetails");
            request.setAttribute( "pageTitle", NGPageIndex.getContainerByKey(accountId).getFullName() );

            modelAndView = new ModelAndView("download_account_document");

        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.download.document.page", new Object[]{accountId} , ex);
        }
        return modelAndView;
    }

     @RequestMapping(value = "/{accountId}/$/leaflet{lid}.htm", method = RequestMethod.GET)
     public ModelAndView displayZoomedLeaflet(@PathVariable String lid,@PathVariable String accountId,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        ModelAndView modelAndView = null;
        try{
            AuthRequest ar = AuthRequest.getOrCreate(request, response);
            if(!ar.isLoggedIn()){
                return showWarningView(ar, "message.loginalert.see.page");
            }
            NGBook ngb = NGPageIndex.getAccountByKeyOrFail(accountId);
            ar.setPageAccessLevels(ngb);

            modelAndView=new ModelAndView("AccountNoteZoomView");
            request.setAttribute("lid", lid);
            request.setAttribute("p", accountId);
            request.setAttribute("realRequestURL", ar.getRequestURL());
            request.setAttribute("tabId", "Account Notes");
        }catch(Exception ex){
            throw new NGException("nugen.operation.fail.account.note", new Object[]{accountId} , ex);
        }
        return modelAndView;
    }

}