<%@page errorPage="/spring/jsp/error.jsp"
%><%@ include file="MyTaskList.jsp"
%>
        <div class="content tab04" style="display: block;">
            <div class="section_body">
                <div style="height:10px;"></div>
                <div id="activeTaskscontainer">
                    <div id="activeTasksPaging"></div>
                    <div id="activeTasksDiv">
                    </div>
                </div>
            </div>
        </div>
    </div>
    <br>
    <div class="generalHeadingBorderLess">This is the First Listing</div>
    <div id="paging5"></div>
    <div id="reminderDiv1">
        <table id="reminderTable1">
            <thead>
                <tr>From</tr>
                <tr>Subject</tr>
                <tr>DueDate</tr>
                <th>Project</th>
                <th>timePeriod</th>
                <th>rid</th>
                <th>projectKey</th>
                <th>bookKey</th>
            </thead>
        <%
            UserPage uPage2 = uProf.getUserPage();

            for (RemoteGoal tr : uPage2.getUserTaskRefs())
            {
        %>

            <tr>
                <td>ts_accepted.gif</td>
                <td><%
                    ar.writeHtml(tr.getSynopsis());
                %></td>
                <td><%
                    SectionUtil.nicePrintTime(ar, tr.getDueDate(), ar.nowTime);
                %></td>
                <td>ViewRemoteTask.htm?taskId=<%=tr.getId()%>&projKey=<%=tr.getProjectKey()%></td>
                <td>4444</td>
                <td>Project X</td>
                <td>xx</td>
                <td>xx</td>
            </tr>
        <%

        }
        %>
        </table>



    <!-- Display the search results here -->

    <form name="taskList">
        <input type="hidden" name="filter" value="<%ar.writeHtml(DataFeedServlet.ALLTASKS);%>"/>
        <input type="hidden" name="rssfilter" value="<%ar.writeHtml(RssServlet.STATUS_ALL);%>"/>
    </form>


<script type="text/javascript">

    function invokeRSSLink(link) {
        window.location.href = "<%=ar.retPath + rssLink%>&status=" + document.taskList.rssfilter.value ;
    }

    YAHOO.util.Event.addListener(window, "load", function()
    {
        YAHOO.example.EnhanceFromMarkup = function()
        {
            var connectionCallback = {
                    success: function(o) {
                        var xmlDoc = o.responseXML;
                        var stateUrlFormater = function(elCell, oRecord, oColumn, sData)
                        {
                            elCell.innerHTML = '<a name="' + oRecord.getData("StateImg") + '" href="<%=ar.retPath%>'
                                                + oRecord.getData("PageURL") + 'task'
                                                + oRecord.getData("Id") + '.htm"  target=\"_blank\" title=\"View details and modify activity state\">'
                                                + '<img src="<%=ar.retPath%>assets/images/'
                                                + oRecord.getData("StateImg") +'"/></a>';
                        };

                        var pageNameUrlFormater = function(elCell, oRecord, oColumn, sData)
                        {
                            elCell.innerHTML = '<a name="' + oRecord.getData("PageName") + '" href="<%=ar.retPath%>'
                                                + oRecord.getData("PageURL") +
                                                'public.htm" target=\"_blank\" title=\"Navigate to project\">'
                                                + oRecord.getData("PageName") + '</a>';
                        };


                        var assigneeFormater = function(elCell, oRecord, oColumn, sData)
                        {
                            var assignee=oRecord.getData("Assignee") ;
                            var loggingUser=<%=UtilityMethods.quote4JS(loggingUserName)%>;
                            if(assignee!=loggingUser){
                                 elCell.innerHTML =assignee;
                             }
                        };

                        var activeTasksCD = [
                            {key:"State",label:"State", formatter:stateUrlFormater, sortable:true,resizeable:true},
                            {key:"NameAndDescription",label:"Task", sortable:true,resizeable:true},
                            {key:"Page",label:"Project", formatter:pageNameUrlFormater, sortable:true,resizeable:true},
                            {key:"Assignee",label:"Assignee",sortable:true,resizeable:true,formatter:assigneeFormater},
                            {key:"Priority",label:"Priority",formatter:YAHOO.widget.DataTable.formatNumber,sortable:true,resizeable:true},
                            {key:"DueDate",label:"DueDate",formatter:YAHOO.widget.DataTable.formatDate,sortable:true,sortOptions:{sortFunction:sortDates},resizeable:true},
                            {key:"timePeriod",label:"timePeriod",sortable:true,resizeable:false,hidden:true}
                        ];

                        var activeTasksDS = new YAHOO.util.DataSource(xmlDoc);
                        activeTasksDS.responseType = YAHOO.util.DataSource.TYPE_XML;
                        activeTasksDS.responseSchema = {
                            resultNode: "Result",

                            fields: [{key:"Id"},
                                     {key:"State", parser:"number"},
                                     {key:"StateImg"},
                                     {key:"NameAndDescription"},
                                     {key:"Assignee"},
                                     {key:"Priority", parser:"number"},
                                     {key:"DueDate"},
                                     {key:"PageKey"},
                                     {key:"PageName"},
                                     {key:"PageURL"},
                                     {key:"timePeriod", parser:YAHOO.util.DataSource.parseNumber}
                            ]};

                        var oConfigs = { paginator: new YAHOO.widget.Paginator({rowsPerPage:200,containers:'activeTasksPaging'}), initialRequest:"results=99999999"};

                        var activeTasksDT = new YAHOO.widget.DataTable(
                                          "activeTasksDiv",
                                          activeTasksCD,
                                          activeTasksDS,
                                          oConfigs,
                                          {caption:"",sortedBy:{key:"No",dir:"desc"}}
                                      );

                        var oColumn = activeTasksDT.getColumn(3);
                        activeTasksDT.hideColumn(oColumn);




                    },
                    failure: function(o)
                    {
                        // hide the loading panel.
                        YAHOO.example.activeTasksContainer.wait.hide();
                    }
                };

            var servletURL = "<%=ar.retPath%>servlet/DataFeedServlet?<%ar.writeHtml(DataFeedServlet.PARAM_OPERATION);%>=<%ar.writeHtml(DataFeedServlet.OPERATION_GETTASKLIST);%>"+
                                    "&<%ar.writeHtml(DataFeedServlet.PARAM_TASKLIST);%>="+document.taskList.filter.value+
                                    "&u=<%ar.writeHtml(URLEncoder.encode(uProf.getUniversalId(), "UTF-8"));%>&isNewUI=yes";

            //var getXML = YAHOO.util.Connect.asyncRequest("GET",servletURL, connectionCallback);


        }();
    });

    YAHOO.util.Event.addListener(window, "load", function()
    {

        YAHOO.example.EnhanceFromMarkup = function()
        {
            var myColumnDefs = [
                {key:"State",    label:"State", formatter:stateUrlFormater2, sortable:true,resizeable:true},
                {key:"synopsis", label:"Synopsis", sortable:true, resizeable:true},
                {key:"Page",label:"Project", formatter:pageNameUrlFormater2, sortable:true,resizeable:true},
                {key:"projectName",label:"Project Name",formatter:prjectNameFormater,sortable:true,resizeable:true},
                {key:"DueDate",label:"DueDate",formatter:YAHOO.widget.DataTable.formatDate,sortable:true,sortOptions:{sortFunction:sortDates},resizeable:true},
                {key:"timePeriod",label:"timePeriod",sortable:true,resizeable:false,hidden:true},
                {key:"rid",label:"rid",sortable:true,resizeable:false,hidden:true},
                {key:"pageKey",label:"pageKey",sortable:true,resizeable:false,hidden:true},
                {key:"bookKey",label:"bookKey",sortable:true,resizeable:false,hidden:true}
                ];

            var myDataSource = new YAHOO.util.DataSource(YAHOO.util.Dom.get("reminderTable1"));
            myDataSource.responseType = YAHOO.util.DataSource.TYPE_HTMLTABLE;
            myDataSource.responseSchema = {
                fields: [
                        {key:"StateImg"},
                        {key:"synopsis"},
                        {key:"DueDate"},
                        {key:"PageURL"},
                        {key:"timePeriod", parser:YAHOO.util.DataSource.parseNumber},
                        {key:"PageName"},
                        {key:"pageKey"},
                        {key:"bookKey"}]
            };

             var oConfigs = {
                paginator: new YAHOO.widget.Paginator({
                    rowsPerPage: 200,
                    containers: 'paging5'
                }),
                initialRequest: "results=999999"

            };

            var myDataTable = new YAHOO.widget.DataTable("reminderDiv1", myColumnDefs, myDataSource, oConfigs,
            {caption:""});

            myDataTable.sortColumn(myDataTable.getColumn(4));
            return {
                oDS: myDataSource,
                oDT: myDataTable
            };
        }();
    });
    var reminderNameFormater = function(elCell, oRecord, oColumn, sData)
    {
        var name = oRecord.getData("subject");
        var pageKey = oRecord.getData("pageKey");
        var bookKey = oRecord.getData("bookKey");
        var rid = oRecord.getData("rid");
        elCell.innerHTML = '<a href="<%=ar.baseURL%>t/'+bookKey+'/'+pageKey+'/viewEmailReminder.htm?rid='+rid+'" ><div style="color:gray;">'+name+'</a></div>';

    };
    var prjectNameFormater = function(elCell, oRecord, oColumn, sData)
    {
        var name = oRecord.getData("subject");
        var pageKey = oRecord.getData("pageKey");
        var bookKey = oRecord.getData("bookKey");
        var projectName = oRecord.getData("projectName");
        elCell.innerHTML = '<a href="<%=ar.baseURL%>t/'+bookKey+'/'+pageKey+'/public.htm" >'+projectName+'</a>';

    };

    var stateUrlFormater2 = function(elCell, oRecord, oColumn, sData)
    {
        elCell.innerHTML = '<a name="' + oRecord.getData("StateImg") + '" href="'
                            + oRecord.getData("PageURL") + '"  target=\"_blank\" title=\"Access goal details\">'
                            + '<img src="<%=ar.retPath%>assets/images/'
                            + oRecord.getData("StateImg") +'"/></a>';
    };
    var pageNameUrlFormater2 = function(elCell, oRecord, oColumn, sData)
    {
        elCell.innerHTML = '<a name="' + oRecord.getData("PageName") + '" href="<%=ar.retPath%>'
                            + oRecord.getData("PageURL") +
                            'public.htm" target=\"_blank\" title=\"Navigate to project\">'
                            + oRecord.getData("PageName") + '</a>';
    };



</script>
