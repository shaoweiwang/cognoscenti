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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import org.w3c.dom.Document;

/**
* NGProj is a Container that represents a Project.
* This kind of project exists anywhere in a library hierarchy.
* The old project (NGPage) existed only in a single date folder, and all the attachments existed in the attachment folder.
* This project is represented by a folder anywhere on disk,
* and the attachments are just files within that folder.
* The project file itself has a reserved name "ProjectDetails.xml"
*/
public class NGProj extends NGPage
{
    /**
    * This project inhabits a folder on disk, and this is the path to the folder.
    */
    public File containingFolder;


    public NGProj(File theFile, Document newDoc) throws Exception {
        super(theFile, newDoc);

        containingFolder = theFile.getParentFile();
    }


    public static NGProj readProjAbsolutePath(File theFile) throws Exception {
        NGPage newPage = NGPage.readPageAbsolutePath(theFile);
        if (!(newPage instanceof NGProj)) {
            throw new Exception("Attempt to create an NGProj when there is already a NGPage at "+theFile+".  Are you trying to create a NGProj INSIDE the NGPage data folder?");
        }
        return (NGProj) newPage;
    }


    public List<AttachmentRecord> getAllAttachments() throws Exception {
        @SuppressWarnings("unchecked")
        List<AttachmentRecord> list = (List<AttachmentRecord>)(List<?>)
                attachParent.getChildren("attachment", ProjectAttachment.class);
        for (AttachmentRecord att : list) {
            att.setContainer(this);
            String atype = att.getType();
            boolean isDel = att.isDeleted();
            if (atype.equals("FILE") && !isDel)
            {
                File attPath = new File(containingFolder, att.getDisplayName());
                if (!attPath.exists()) {
                    //the file is missing, set to GONE, but should this be persistent?
                    att.setType("GONE");
                }
            }
            else if (atype.equals("GONE"))
            {
                File attPath = new File(containingFolder, att.getDisplayName());
                if (isDel || attPath.exists()) {
                    //either attachment deleted, or we found it again, so set it back to file
                    att.setType("FILE");
                }
            }
        }
        return list;
    }

    public AttachmentRecord createAttachment() throws Exception {
        AttachmentRecord attach = attachParent.createChild("attachment", ProjectAttachment.class);
        String newId = getUniqueOnPage();
        attach.setId(newId);
        attach.setContainer(this);
        attach.setUniversalId( getContainerUniversalId() + "@" + newId );
        return attach;
    }

    public void scanForNewFiles() throws Exception
    {
        File[] children = containingFolder.listFiles();
        List<AttachmentRecord> list = getAllAttachments();
        for (File child : children)
        {
            if (child.isDirectory()) {
                continue;
            }
            String fname = child.getName();
            if (fname.endsWith(".sp")) {
                continue;
            }
            if (fname.startsWith(".cog")) {
                continue;
            }

            //all others are possible documents at this point
            AttachmentRecord att = null;
            for (AttachmentRecord knownAtt : list) {
                if (fname.equals(knownAtt.getDisplayName())) {
                    att = knownAtt;
                }
            }
            if (att!=null) {
                continue;
            }
            att = createAttachment();
            att.setDisplayName(fname);
            att.setType("EXTRA");
            list.add(att);
        }
        for (AttachmentRecord knownAtt : list) {
            AttachmentVersion aVer = knownAtt.getLatestVersion(this);
            if (aVer==null) {
                continue;
            }
            File attFile = aVer.getLocalFile();
            if (!attFile.exists()) {
                knownAtt.setType("GONE");
            }
        }
    }


    public void removeExtrasByName(String name) throws Exception
    {
        List<ProjectAttachment> list = attachParent.getChildren("attachment", ProjectAttachment.class);
        for (ProjectAttachment att : list) {
            if (att.getType().equals("EXTRA") && att.getDisplayName().equals(name)) {
                attachParent.removeChild(att);
                break;
            }
        }
    }

    public void saveFile(AuthRequest ar, String comment) throws Exception {
        super.saveFile(ar, comment);
        assureLaunchingPad(ar);
    }

    public void assureLaunchingPad(AuthRequest ar) throws Exception {
        File launchFile = new File(containingFolder, ".cogProjectView.htm");
        if (!launchFile.exists()) {
            boolean previousUI = ar.isNewUI();
            ar.setNewUI(true);
            OutputStream os = new FileOutputStream(launchFile);
            Writer w = new OutputStreamWriter(os, "UTF-8");
            w.write("<html><body><script>document.location = \"");
            UtilityMethods.writeHtml(w, ar.baseURL);
            UtilityMethods.writeHtml(w, ar.getResourceURL(this, "public.htm"));
            w.write("\";</script></body></html>");
            w.flush();
            w.close();
            ar.setNewUI(previousUI);
        }
    }
}