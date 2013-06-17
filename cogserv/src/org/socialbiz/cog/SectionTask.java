package org.socialbiz.cog;

import java.io.Writer;
import java.util.Vector;
import java.util.List;
import org.w3c.dom.Element;

import org.socialbiz.cog.exception.NGException;
import org.socialbiz.cog.exception.ProgramLogicError;

/**
* Implements the process and task formatting
*/
public class SectionTask extends SectionUtil implements SectionFormat
{


    public SectionTask()
    {
    }

    public String getName()
    {
        return "Process";
    }

    public static void plugInCalenderScript(Writer out, String dateInputField, String buttonName)
        throws Exception
    {
        out.write("\n<script type=\"text/javascript\"> ");
        out.write("\n    Calendar.setup({ ");
        out.write("\n        inputField     :    \"" + dateInputField + "\",  // id of the input field ");
        out.write("\n        ifFormat       :    \"%m/%d/%Y\",                // format of the input field ");
        out.write("\n        showsTime      :    true,                        // show the time");
        out.write("\n        electric       :    false,                       // update date/time only after clicking");
        out.write("\n        date           :    new Date(),                  // show the time");
        out.write("\n        button         :    \"" + buttonName + "\",      // trigger for the calendar (button ID) ");
        out.write("\n        align          :    \"Bl\",                      // alignment (defaults to \"Bl\") ");
        out.write("\n        singleClick    :    true ");
        out.write("\n    }); ");
        out.write("\n ");
        out.write("\n</script> ");
    }

    public static void plugInDurationCalcScript(Writer out)
        throws Exception
    {
        out.write("\n<script type=\"text/javascript\"> ");
        out.write("\n    function calculateTaskEndDate(){ ");
        out.write("\n        if (document.taskForm.startDate.value == \"0/0/0000\") { ");
        out.write("\n            return; ");
        out.write("\n        } ");
        out.write("\n        var date = DateAdd(document.taskForm.startDate.value, 'D', parseInt(document.taskForm.duration.value)); ");
        out.write("\n        document.taskForm.endDate.value = (parseInt(date.getMonth()) +1) + \"/\" + date.getDate() + \"/\" +  date.getFullYear(); ");
        out.write("\n    }         ");
        out.write("\n ");
        out.write("\n    function calculateDuration(){ ");
        out.write("\n        if (document.taskForm.startDate.value == \"0/0/0000\") { ");
        out.write("\n            return; ");
        out.write("\n        } ");
        out.write("\n        var duration = DaysInBetweenDates(new Date(document.taskForm.startDate.value), new Date(document.taskForm.endDate.value)); ");
        out.write("\n        document.taskForm.duration.value = duration; ");
        out.write("\n    } ");
        out.write("\n    function defaultDate(){ ");
        out.write("\n        var d = new Date();");
        out.write("\n        d.setHours(\"13\");");
        out.write("\n        d.setMinutes(\"0\");");
        out.write("\n        d.setSeconds(\"0\");");
        out.write("\n        d.setMilliSeconds(\"0\");");
        out.write("\n        return d;");
        out.write("\n    }         ");
        out.write("\n</script> ");
    }

    public static GoalRecord createTaskWithinNGPage(NGSection section) throws Exception
    {
        String id = section.parent.getUniqueOnPage();
        GoalRecord task = section.createChildWithID("task",
                GoalRecord.class, "id", id);

        //find and give it a good rank number
        task.setRank(32000000);
        renumberRanks(section);
        return task;
    }




    public static List<GoalRecord> getAllTasks(NGSection sec)
            throws Exception
    {
        if (sec == null)
        {
            throw new ProgramLogicError("trying to get tasks from a null section does not make sense");
        }

        List<GoalRecord> list = sec.getChildren("task", GoalRecord.class);
        for (GoalRecord task : list)
        {
            //temporary -- tasks may not have had ids, so patch that up now if necessary
            //can remove this after existing pages have been converted to have id values
            String id = task.getId();
            if (id==null || id.length()!=4)
            {
                task.setId(sec.parent.getUniqueOnPage());
            }
        }
        GoalRecord.sortTasksByRank(list);
        return list;
    }

    public static GoalRecord getTaskOrFail(NGSection sec, String id)
        throws Exception
    {
        GoalRecord task = getTaskOrNull(sec, id);
        if (task==null)
        {
            throw new NGException("nugen.exception.could.not.find.task", new Object[]{id});
        }
        return task;
    }

    public static GoalRecord getTaskOrNull(NGSection sec, String id)
        throws Exception
    {
        if (id==null)
        {
            throw new NGException("nugen.exception.could.not.find.null.taskid",null);
        }
        List<GoalRecord> list = sec.getChildren("task", GoalRecord.class);
        for (GoalRecord task : list)
        {
            //temporary -- tasks may not have had ids, so patch that up now if necessary
            //can remove this after existing pages have been converted to have id values
            if (id.equals(task.getId()))
            {
                return task;
            }
        }
        return null;
    }


    public void findLinks(Vector<String> v, NGSection sec)
        throws Exception
    {
        for (GoalRecord tr : getAllTasks(sec))
        {
            String link = tr.getDisplayLink();
            v.add(link);
        }
    }

    public static void renumberRanks(NGSection sec)
        throws Exception
    {
        List<GoalRecord> taskList = getAllTasks(sec);
        renumberRanks(taskList);
    }

    public static void renumberRanks(List<GoalRecord> taskList)
        throws Exception
    {
        int rankVal = 0;
        for (GoalRecord tr : taskList)
        {
            String myParent = tr.getParentGoalId();
            //only renumber tasks that have no parent.  Others renumbered recursively
            if (myParent==null || myParent.length()==0) {
                rankVal += 10;
                tr.setRank( rankVal );
                rankVal = renumberRankChildren(taskList, rankVal, tr.getId());
            }
        }
    }

    private static int renumberRankChildren(List<GoalRecord> taskList, int rankVal, String parentId)
        throws Exception
    {
        for (GoalRecord tr : taskList)
        {
            if (parentId.equals(tr.getParentGoalId()))
            {
                rankVal += 10;
                tr.setRank( rankVal );
                rankVal = renumberRankChildren(taskList, rankVal, tr.getId());
            }
        }
        return rankVal;
    }


    public void writePlainText(NGSection section, Writer out) throws Exception
    {
        ProcessRecord process = section.parent.getProcess();
        if (process != null) {
            SectionUtil.writeTextWithLB(process.getId() , out);
            SectionUtil.writeTextWithLB(process.getSynopsis() , out);
            SectionUtil.writeTextWithLB(process.getDescription() , out);
            SectionUtil.writeTextWithLB(String.valueOf(process.getState()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(process.getDueDate()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(process.getStartDate()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(process.getEndDate()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(process.getPriority()) , out);

            LicensedURL[] pp = process.getLicensedParents();
            for (int i=0; i<pp.length; i++) {
                SectionUtil.writeTextWithLB(String.valueOf(pp[i].url) , out);
            }
        }

        for (GoalRecord task : getAllTasks(section)) {
            SectionUtil.writeTextWithLB(task.getId() , out);
            SectionUtil.writeTextWithLB(task.getSynopsis() , out);
            SectionUtil.writeTextWithLB(task.getDescription() , out);
            SectionUtil.writeTextWithLB(task.getAssigneeCommaSeparatedList() , out);
            SectionUtil.writeTextWithLB(task.getStatus() , out);
            SectionUtil.writeTextWithLB(task.getSub() , out);
            SectionUtil.writeTextWithLB(task.getActionScripts() , out);
            SectionUtil.writeTextWithLB(String.valueOf(task.getRank()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(task.getState()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(task.getDueDate()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(task.getStartDate()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(task.getEndDate()) , out);
            SectionUtil.writeTextWithLB(String.valueOf(task.getPriority()) , out);
        }
    }

    public void removeTask(String taskId, NGSection section) {
        Element secElem = section.getElement();
        for (Element taskElem : DOMUtils
                .getNamedChildrenVector(secElem, "task")) {
            String id = taskElem.getAttribute("id");
            if (id.equals(taskId)) {
                secElem.removeChild(taskElem);
                return;
            }
        }
    }

    /**
    * Walk through whatever elements this owns and put all the four digit
    * IDs into the vector so that we can generate another ID and assure it
    * does not duplication any id found here.
    */
    public void findIDs(Vector<String> v, NGSection sec)
        throws Exception
    {
        for (GoalRecord tr : getAllTasks(sec))
        {
            v.add(tr.getId());
        }
    }

    public static boolean canEditTask(NGPage ngp, AuthRequest ar, String taskId) throws Exception
    {
        boolean edit = false;

        ProcessRecord pr = ngp.getProcess();
        String prlicense = pr.accessLicense().getId();
        if(prlicense != null && prlicense.equals(ar.licenseid))
        {
            edit = true;
            return edit;
        }
        try {
            GoalRecord tr = ngp.getGoalOrFail(taskId.trim());
            String trlicense = tr.accessLicense().getId();
            if(trlicense != null && trlicense.equals(ar.licenseid))
            {
                edit = true;
                return edit;
            }

        }catch(Exception e){
            edit = false;
            //Not a Task Licence
        }
        //Check if it has page licence
        return ar.isMember();
    }


    public void copyTaskRecord(GoalRecord sourceRecord, GoalRecord destinationRecord) throws Exception{
        destinationRecord.setSynopsis(sourceRecord.getSynopsis().toString());
        destinationRecord.setDueDate(SectionUtil.niceParseDate(String.valueOf(sourceRecord.getDueDate())));
        destinationRecord.setAssigneeCommaSeparatedList(sourceRecord.getAssigneeCommaSeparatedList());
        destinationRecord.setPriority(sourceRecord.getPriority());
        destinationRecord.setDescription(sourceRecord.getDescription());
        destinationRecord.setState(sourceRecord.getState());
        destinationRecord.setCreator(sourceRecord.getCreator());

        destinationRecord.setActionScripts(sourceRecord.getActionScripts());
        destinationRecord.setDuration(sourceRecord.getDuration());
        destinationRecord.setEndDate(sourceRecord.getEndDate());
        destinationRecord.setFreePass(sourceRecord.getFreePass());
        destinationRecord.setId(sourceRecord.getId());

        destinationRecord.setStartDate(sourceRecord.getStartDate());
        destinationRecord.setStatus(sourceRecord.getStatus());
        destinationRecord.setRank(sourceRecord.getRank());
        if(!"".equals(sourceRecord.getParentGoalId())){
            destinationRecord.setParentGoal(sourceRecord.getParentGoalId());
        }
    }
}
