package org.socialbiz.cog;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.socialbiz.cog.exception.NGException;
import org.socialbiz.cog.exception.ProgramLogicError;


public class HistoryRecord extends DOMFace
{

    // list of all the producers.
    public final static int CONTEXT_TYPE_PROCESS      = 0;
    public final static int CONTEXT_TYPE_TASK         = 1;
    public final static int CONTEXT_TYPE_PERMISSIONS  = 2;
    public final static int CONTEXT_TYPE_DOCUMENT     = 3;
    public final static int CONTEXT_TYPE_LEAFLET      = 4;
    public final static int CONTEXT_TYPE_ROLE         = 5;

    public final static String EVENT_TYPE_PREFIX="event.type";
    public final static String CONTEXT_TYPE_PREFIX="context.type";


    public final static String OBJECT_TYPE_USER="object.type.user";
    public final static String OBJECT_TYPE_ATTACHMENT="object.type.attachment";
    public final static String OBJECT_TYPE_PLAYER="object.type.player";
    public final static String OBJECT_TYPE_ROLE="object.type.role";
    public final static String OBJECT_TYPE_PROCESS="object.type.process";
    public final static String OBJECT_TYPE_TASK="object.type.task";
    public final static String OBJECT_TYPE_NOTE="object.type.note";
    public final static String OBJECT_TYPE_SUBTASK="object.type.subtask";


    public final static String OBJECT_CREATED="object.created";
    public final static String OBJECT_SENT_BY_EMAIL="object.sent.by.email";
    public final static String OBJECT_MODIFIED="object.modified";
    public final static String OBJECT_DELETED="object.deleted";
    public final static String OBJECT_ACCESS_LEVEL_CHANGE="object.accees.level.changed";
    public final static String USER_REMOVED="user.removed";
    public final static String USER_ADDED="user.added";
    public final static String ROLE_REMOVED="role.removed";
    public final static String ROLE_ADDED="role.added";
    public final static String TASK_COMPLETED="task.completed";
    public final static String TASK_ACCEPTED="task.accepted";
    public final static String TASK_REJECTED="task.rejected";
    public final static String TASK_APPROVED="task.approved";
    public final static String TASK_STARTED ="task.started";
    public final static String DOC_ATTACHED="document.attached";
    public final static String DOC_UPDATED="document.updated";


    // list of all the events.
    // states 0-6 are reserved as the old representation of 50-56

    public final static int EVENT_TYPE_CREATED  = 7;
    public final static int EVENT_TYPE_MODIFIED = 8;
    public final static int EVENT_TYPE_DELETED  = 9;

    public final static int EVENT_TYPE_APPROVED   = 10;
    public final static int EVENT_TYPE_REJECTED   = 11;

    public final static int EVENT_TYPE_SUBTASK_CREATED = 12;
    public final static int EVENT_TYPE_SUBLEAF_CREATED = 13;

    public final static int EVENT_TYPE_REORDERED = 14;

    public final static int EVENT_ROLE_ADDED     = 15;
    public final static int EVENT_ROLE_REMOVED   = 16;
    public final static int EVENT_ROLE_MODIFIED  = 17;


    public final static int EVENT_MEMBER_REQUEST = 20;
    public final static int EVENT_MEMBER_ADDED   = 21;
    public final static int EVENT_PLAYER_ADDED_CUSTOM_ROLE  = 41;
    public final static int EVENT_MEMBER_REMOVED = 22;
    public final static int EVENT_ADMIN_REQUEST  = 23;
    public final static int EVENT_ADMIN_ADDED    = 24;
    public final static int EVENT_ADMIN_REMOVED  = 25;
    public final static int EVENT_LEVEL_CHANGE   = 26;  //unspecified level change
    public final static int EVENT_PLAYER_ADDED   = 27;
    public final static int EVENT_PLAYER_REMOVED = 28;

    public final static int EVENT_DOC_ADDED    = 30;
    public final static int EVENT_DOC_REMOVED  = 31;
    public final static int EVENT_DOC_UPDATED  = 32;
    public final static int EVENT_DOC_APPROVED = 33;  //I read it and it is OK
    public final static int EVENT_DOC_REJECTED = 34;  //I read it and it needs improvement
    public final static int EVENT_DOC_SKIPPED  = 35;  //I decided not to read it
    public final static int EVENT_EMAIL_SENT   = 40;

    //These used to be states 0-6 to match the states of the task,
    //but then a new state was added to the task, but there was no room
    //for history states.  So now, the history states are 50 + task state.
    public final static int EVENT_TYPE_STATE_CHANGE_ERROR     = 50;
    public final static int EVENT_TYPE_STATE_CHANGE_UNSTARTED = 51;
    public final static int EVENT_TYPE_STATE_CHANGE_STARTED   = 52;
    public final static int EVENT_TYPE_STATE_CHANGE_ACCEPTED  = 53;
    public final static int EVENT_TYPE_STATE_CHANGE_WAITING   = 54;
    public final static int EVENT_TYPE_STATE_CHANGE_COMPLETE  = 55;
    public final static int EVENT_TYPE_STATE_CHANGE_SKIPPED   = 56;
    public final static int EVENT_TYPE_STATE_CHANGE_REVIEWED  = 57;
    // reserve states 58-69 for task state mapping

    public HistoryRecord(Document definingDoc, Element definingElement, DOMFace p)
    {
        super(definingDoc, definingElement, p);
    }

    public void copyFrom(HistoryRecord other)
        throws Exception
    {
        setEventType(other.getEventType());
        setContext(other.getContext());
        setContextType(other.getContextType());
        setContextVersion(other.getContextVersion());
        setComments(other.getComments());
        setTimeStamp(other.getTimeStamp());
        setResponsible(other.getResponsible());
    }


    public String getId()
        throws Exception
    {
        return getAttribute("id");
    }

    public void setId(String id)
        throws Exception
    {
        if (id.length()!=4) {
            throw new NGException("nugen.exception.invalid.id",null);
        }

        for (int i=0; i<4; i++)
        {
            if (id.charAt(i)<'0' || id.charAt(i)>'9') {
                throw new NGException("nugen.exception.invalid.id",null);
            }
        }
        setAttribute("id", id);
    }

    public int getEventType()
        throws Exception
    {
        int i = safeConvertInt(getScalar("type"));
        //These used to be states 0-6 to match the states of the task,
        //but then a new state was added to the task, but there was no room
        //for history states.  So now, the history states are 50 + task state.
        //This code converts the old value 0-6 to new values "on the fly"
        if (i>=0 && i<=6)
        {
            //increment by 50
            i = 50+i;
            //remember this so the file is consistent ... eventually
            setEventType(i);
        }

        if (i >= 100) {
            //strange legacy data corruption.  Somehow some event got set to 100,
            //and stored in the files.  This cleans it up.
            //Remove after Dec 2012
            i = EVENT_TYPE_MODIFIED;
            setEventType(i);
        }
        return i;
    }
    public void setEventType(int type)
        throws Exception
    {
        setScalar("type", Integer.toString(type));
    }

    /**
    * The context is the id of the "object" that the history item is
    * about.
    * In the case of task history, the context is the id of the task
    * in the case of permission, the context is the name of user changed
    * in document, the context is the path of the document
    */
    public String getContext()
        throws Exception
    {
        return getScalar("context");
    }
    public void setContext(String context)
        throws Exception
    {
        setScalar("context", context);
    }

    /**
    * Tells how to interpret the context id
    */
    public int getContextType()
        throws Exception
    {
        return safeConvertInt(getScalar("contextType"));
    }
    public void setContextType(int context)
        throws Exception
    {
        setScalar("contextType", Integer.toString(context));
    }

    /**
    * This records the specific version of the context object.
    * In the case of "reading" a document, when a new version
    * of the document arrives, it invalidates the read note.
    * For documents, the version is simply the timestamp of the document.
    */
    public long getContextVersion()
        throws Exception
    {
        return safeConvertLong(getScalar("contextVersion"));
    }
    public void setContextVersion(long context)
        throws Exception
    {
        setScalar("contextVersion", Long.toString(context));
    }


    /**
    * Each history item can have text explanation, presumably
    * entered by the user at the time of taking action.
    */
    public String getComments()
        throws Exception
    {
        return getScalar("comments");
    }
    public void setComments(String comment)
        throws Exception
    {
        if (comment==null)
        {
            comment = "";
        }
        setScalar("comments", comment);
    }

    /**
    * This is the timestamp at which the history action happened.
    */
    public long getTimeStamp()
        throws Exception
    {
        return safeConvertLong(getScalar("timestamp"));
    }
    public void setTimeStamp(long ts)
        throws Exception
    {
        setScalar("timestamp", Long.toString(ts));
    }

    public String getResponsible()
        throws Exception
    {
        return getScalar("responsible");
    }
    public void setResponsible(String resp)
        throws Exception
    {
        setScalar("responsible", resp);
    }

    public static String getContext(int ptype)
    {
        switch (ptype)
        {
            case CONTEXT_TYPE_PROCESS:
                return "Process";
            case CONTEXT_TYPE_TASK:
                return "Task";
            case CONTEXT_TYPE_PERMISSIONS:
                return "Permission";
            case CONTEXT_TYPE_DOCUMENT:
                return "Document";
            case CONTEXT_TYPE_LEAFLET:
                return "Note";
            case CONTEXT_TYPE_ROLE:
                return "Role";
            default:
        }
        return "Unknown";
    }

    public void fillInWfxmlHistory(Document doc, Element histEle)  throws Exception
    {
        if (doc == null)
        {
            throw new ProgramLogicError("Null doc parameter passed to fillInWfxmlHistory");
        }
        if (histEle == null)
        {
            throw new ProgramLogicError("Null histEle parameter passed to fillInWfxmlHistory");
        }

        //this code constructs XML for the WfXML protocol
        Element eventEle = DOMUtils.createChildElement(doc, histEle, "event");
        eventEle.setAttribute("id", getId());
        DOMUtils.createChildElement(doc, eventEle, "type", String.valueOf(getEventType()));
        DOMUtils.createChildElement(doc, eventEle, "context", String.valueOf(getContext()));
        DOMUtils.createChildElement(doc, eventEle, "contexttype", String.valueOf(getContextType()));
        DOMUtils.createChildElement(doc, eventEle, "responsible", getResponsible());
        DOMUtils.createChildElement(doc, eventEle, "timestamp", UtilityMethods.getXMLDateFormat(getTimeStamp()));
        DOMUtils.createChildElement(doc, eventEle, "comments", String.valueOf(getComments()));
    }

    /**
    * deprecated: remove this method when there is a chance.
    */
    public static HistoryRecord createHistoryRecord(NGPage ngp,
        String context, int contextType, int eventType,
        AuthRequest ar, String comments) throws Exception
    {
        return createHistoryRecord(ngp, context, contextType, 0, eventType, ar, comments);
    }


    public static HistoryRecord createHistoryRecord(NGContainer ngc,
        String context, int contextType, long contextVersion, int eventType,
        AuthRequest ar, String comments) throws Exception
    {
        HistoryRecord hr = ngc.createNewHistory();
        hr.setContext(context);
        hr.setContextType(contextType);
        hr.setContextVersion(contextVersion);
        hr.setEventType(eventType);
        hr.setResponsible(ar.getBestUserId());
        hr.setComments(comments);
        hr.setTimeStamp(ar.nowTime);
        return hr;
    }




    /**
     * Returns the parameter value at the specified index.
     *
     * @param index
     *            The index of the parameter to be returned.
     * @return The parameter indicated by the given index.
     */
    /*
    private Object getParamObj(int index,String[] params) {
        if (params == null || index < 0 || index >= params.length) {
            return null;
        } else {
            return params[index];
        }
    }

    private String getParamText(int index, Locale locale,String[] params) {
        String text = null;
        Object o = getParamObj(index,params);

        if (o == null) {
            text = null;
        } else if (o instanceof Date) {
            text = getFormattedDate((Date) o, locale);
        } else if (o instanceof Long) {
            text = getFormattedNumber((Long) o, locale);
        } else if (o instanceof Integer) {
            text = getFormattedNumber((Integer) o, locale);
        } else if (o instanceof BigDecimal) {
            text = getFormattedDecimal((BigDecimal) o, locale);
        } else if (o instanceof Float) {
            text = getFormattedDecimal((Float) o, locale);
        } else {
            String param = o.toString();
            text = null;
            if (text == null) {
                text = param;
            }
        }
        return text;
    }

    private String getFormattedNumber(Integer number, Locale locale) {
        return getFormattedNumber(new Long(number.intValue()), locale);
    }
 
    private String getFormattedNumber(Long number, Locale locale) {
        String text = null;
        if (number != null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            NumberFormat nf = NumberFormat.getInstance(locale);
            text = nf.format(number.longValue());
        }
        return text;
    }
 
    private String getFormattedDecimal(BigDecimal decimal, Locale locale) {
        String text = null;
        if (decimal != null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            NumberFormat nf = DecimalFormat.getInstance(locale);
            text = nf.format(decimal);
        }
        return text;
    }

    private String getFormattedDecimal(Float decimal, Locale locale) {
        String text = null;
        if (decimal != null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            NumberFormat nf = DecimalFormat.getInstance(locale);
            text = nf.format(decimal.floatValue());
        }
        return text;
    }

    private final static String DEFAULT_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    private String getFormattedDate(Date date, Locale locale) {
        String text = null;

        if (date != null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            DateFormat df = new SimpleDateFormat(DEFAULT_DATE_FORMAT, locale);
            text = df.format(date);
        }
        return text;
    }
  */



    public String getCombinedKey()
        throws Exception
    {
        String context;
        int ctx = getContextType();
        switch (ctx) {
            case CONTEXT_TYPE_PROCESS:
                context = "history.process.";
                break;
            case CONTEXT_TYPE_TASK:
                context = "history.task.";
                break;
            case CONTEXT_TYPE_PERMISSIONS:
                context = "history.permission.";
                break;
            case CONTEXT_TYPE_DOCUMENT:
                context = "history.doc.";
                break;
            case CONTEXT_TYPE_LEAFLET:
                context = "history.note.";
                break;
            case CONTEXT_TYPE_ROLE:
                context = "history.role.";
                break;
            default:
                throw new ProgramLogicError("HistoryRecord.getCombinedKey does "
                + "not know how to handle a context value: "+ctx);
        }

        int event = getEventType();
        switch (event)
        {
            case EVENT_TYPE_CREATED:
                return context+"created";
            case EVENT_TYPE_MODIFIED:
                return context+"modified";
            case EVENT_TYPE_DELETED:
                return context+"deleted";
            case EVENT_TYPE_APPROVED:
                return context+"approved";
            case EVENT_TYPE_REJECTED:
                return context+"rejected";
            case EVENT_TYPE_SUBTASK_CREATED:
                return context+"subtask.add";
            case EVENT_TYPE_SUBLEAF_CREATED:
                return context+"subproject.add";
            case EVENT_TYPE_REORDERED:
                return context+"reordered";
            case EVENT_ROLE_ADDED:
                return context+"role.add";
            case EVENT_ROLE_REMOVED:
                return context+"role.remove";
            case EVENT_ROLE_MODIFIED:
                return context+"role.mod";
            case EVENT_TYPE_STATE_CHANGE_ERROR:
                return context+"state.error";
            case EVENT_TYPE_STATE_CHANGE_UNSTARTED:
                return context+"state.unstarted";
            case EVENT_TYPE_STATE_CHANGE_STARTED:
                return context+"state.started";
            case EVENT_TYPE_STATE_CHANGE_ACCEPTED:
                return context+"state.accepted";
            case EVENT_TYPE_STATE_CHANGE_WAITING:
                return context+"state.waiting";
            case EVENT_TYPE_STATE_CHANGE_COMPLETE:
                return context+"state.completed";
            case EVENT_TYPE_STATE_CHANGE_SKIPPED:
                return context+"state.skipped";
            case EVENT_TYPE_STATE_CHANGE_REVIEWED:
                return context+"state.reviewed";
            case EVENT_MEMBER_REQUEST:
                return context+"member.request";
            case EVENT_MEMBER_ADDED:
                return context+"member.add";
            case EVENT_MEMBER_REMOVED:
                return context+"member.remove";
            case EVENT_ADMIN_REQUEST:
                return context+"admin.request";
            case EVENT_ADMIN_ADDED:
                return context+"admin.add";
            case EVENT_ADMIN_REMOVED:
                return context+"admin.remove";
            case EVENT_LEVEL_CHANGE:
                return context+"access.level.change";
            case EVENT_PLAYER_ADDED:
                return context+"player.add";
            case EVENT_PLAYER_REMOVED:
                return context+"player.removed";
            case EVENT_PLAYER_ADDED_CUSTOM_ROLE:
                return context+"player.custom";
            case EVENT_DOC_ADDED:
                return context+"attached";
            case EVENT_DOC_REMOVED:
                return context+"removed";
            case EVENT_DOC_UPDATED:
                return context+"updated";
            case EVENT_DOC_APPROVED:
                return context+"mark.read";
            case EVENT_DOC_REJECTED:
                return context+"mark.reject";
            case EVENT_DOC_SKIPPED:
                return context+"mark.skipped";
            case EVENT_EMAIL_SENT:
                return context+"email.sent";
            default:
                return context+"((HISTORY TYPE "+event+"))";
        }

    }



    public static String convertEventTypeToString(int type)
    {
        switch (type)
        {
            case EVENT_TYPE_CREATED:
                return "created";
            case EVENT_TYPE_MODIFIED:
                return "modified";
            case EVENT_TYPE_DELETED:
                return "deleted";
            case EVENT_TYPE_APPROVED:
                return "approved";
            case EVENT_TYPE_REJECTED:
                return "rejected";
            case EVENT_TYPE_SUBTASK_CREATED:
                return "subtask added";
            case EVENT_TYPE_SUBLEAF_CREATED:
                return "subleaf added";
            case EVENT_TYPE_REORDERED:
                return "reordered";
            case EVENT_ROLE_ADDED:
                return "added new role";
            case EVENT_ROLE_REMOVED:
                return "removed role";
            case EVENT_ROLE_MODIFIED:
                return "modified role";
            case EVENT_TYPE_STATE_CHANGE_ERROR:
                return "set into error state";
            case EVENT_TYPE_STATE_CHANGE_UNSTARTED:
                return "reset to unstarted state";
            case EVENT_TYPE_STATE_CHANGE_STARTED:
                return "started";
            case EVENT_TYPE_STATE_CHANGE_ACCEPTED:
                return "accepted";
            case EVENT_TYPE_STATE_CHANGE_WAITING:
                return "set to waiting state";
            case EVENT_TYPE_STATE_CHANGE_COMPLETE:
                return "completed";
            case EVENT_TYPE_STATE_CHANGE_SKIPPED:
                return "set to skipped state";
            case EVENT_TYPE_STATE_CHANGE_REVIEWED:
                return "marked as reviewed";
            case EVENT_MEMBER_REQUEST:
                return "made prospective member";
            case EVENT_MEMBER_ADDED:
                return "added to members";
            case EVENT_MEMBER_REMOVED:
                return "removed from being a member";
            case EVENT_ADMIN_REQUEST:
                return "made prospective admin";
            case EVENT_ADMIN_ADDED:
                return "made admin";
            case EVENT_ADMIN_REMOVED:
                return "removed from being an admin";
            case EVENT_LEVEL_CHANGE:
                return "had access level changed";
            case EVENT_PLAYER_ADDED:
                return "added to role";
            case EVENT_PLAYER_REMOVED:
                return "removed from role";
            case EVENT_DOC_ADDED:
                return "attached";
            case EVENT_DOC_REMOVED:
                return "removed";
            case EVENT_DOC_UPDATED:
                return "updated";
            case EVENT_DOC_APPROVED:
                return "marked as read";
            case EVENT_DOC_REJECTED:
                return "marked as revisions needed";
            case EVENT_DOC_SKIPPED:
                return "marked as skipped";
            case EVENT_EMAIL_SENT:
                return "sent as email";
            default:
                return "modified (#"+type+")";
        }
    }


    public static void sortByTimeStamp(List<HistoryRecord> list)
    {
        Collections.sort(list, new HistoryRecord.HistoryTimeStampComparator());
    }

    public static void sortByContext(List<HistoryRecord> list)
    {
        Collections.sort(list, new HistoryRecord.HistoryContextComparator());
    }



    public static class HistoryTimeStampComparator implements Comparator<HistoryRecord>
    {
        public HistoryTimeStampComparator() {}

        public int compare(HistoryRecord o1, HistoryRecord o2) {
            try {
                long ts1 = o1.getTimeStamp();
                long ts2 = o2.getTimeStamp();
                if (ts1 == ts2) {
                    return 0;
                }
                if (ts1 > ts2) {
                    return -1;
                }
                return 1;
            }
            catch (Exception e) {
                return 0;
            }
        }
    }

    public static class HistoryContextComparator implements Comparator<HistoryRecord>
    {
        public HistoryContextComparator() {}

        public int compare(HistoryRecord o1, HistoryRecord o2) {
            try {
                String c1 = o1.getContext();
                String c2 = o2.getContext();
                int comp = c1.compareTo(c2);
                if (comp != 0) {
                    return comp;
                }
                long ts1 = o1.getContextVersion();
                long ts2 = o2.getContextVersion();
                if (ts1 > ts2) {
                    return -1;
                }
                if (ts1 < ts2) {
                    return 1;
                }
                ts1 = o1.getTimeStamp();
                ts2 = o2.getTimeStamp();
                if (ts1 > ts2) {
                    return -1;
                }
                if (ts1 < ts2) {
                    return 1;
                }
                return 0;
            }
            catch (Exception e) {
                return 0;
            }
        }
    }

}
