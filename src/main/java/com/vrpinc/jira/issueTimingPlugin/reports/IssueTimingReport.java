package com.vrpinc.jira.issueTimingPlugin.reports;

import com.atlassian.query.Query;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.worklog.Worklog;
import com.atlassian.jira.issue.worklog.WorklogManager;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.opensymphony.util.TextUtils;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

 
public class IssueTimingReport extends AbstractReport
{
    private static final Logger log = Logger.getLogger(IssueTimingReport.class);
    private final JiraAuthenticationContext authenticationContext;
    private final SearchService searchService;
    private Map params;
    private final String issueTimespentNull = "0s";
    private final String issuePathUrl = "/browse/";
	private WorklogManager worlogManager;
    public IssueTimingReport(final JiraAuthenticationContext authenticationContext,
                                            final SearchService searchService, WorklogManager worklogManager)
    {
    	this.worlogManager = worklogManager;
        this.authenticationContext = authenticationContext;
        this.searchService = searchService;
    }

	public String generateReportHtml(ProjectActionSupport projectActionSupport, Map params) throws PermissionException{
		this.params = params;
		Long currentProjectId = getCurrentProjectId(params);
		List<Issue> issues = this.getAllIssuesInCurrentProject(currentProjectId);
		Long issueId = getSelectedIssueId(params);
		List<Worklog> worklogs = null;		
		if(issueId != -1){
			//worklogs = getWorklog(issues.get(0));
			worklogs = getWorklog(issues, issueId);
		}
		final Map startingParams;
		startingParams = EasyMap.build(
                "projectId", currentProjectId,
				"issues", issues,
                "issueId", issueId,
                "worklogs", worklogs,
                "th", this); 
		return descriptor.getHtml("view", startingParams);
    	
    }



	private Long getSelectedIssueId(Map params) {
		Long issueId;
		try{
			issueId = Long.valueOf(TextUtils.htmlEncode((String) params.get("issueId")));
		}catch(Exception e){
			issueId = (long) -1;
		}
		return issueId;
	}

	private Long getCurrentProjectId(Map params) {
		Long currentProjectId = Long.valueOf(TextUtils.htmlEncode((String) params.get("selectedProjectId")));
		return currentProjectId;
	}
	
    public String formatTime(final long sec) {
		String result ="";
    	final int secInMinute = 60;
		final int secInHour = 3600;
		final int secInDay = 86400;
		final int secInMonth = 2592000;
		final int secInYear = 31104000;
		int years = (int) (sec/secInYear);
		result = result.concat(isNotNull(result, years, "y "));
		int month = (int) ((sec - years*secInYear)/secInMonth);
		result = result.concat(isNotNull(result, month, "m"));
		int day = (int) ((sec - years*secInYear - month*secInMonth)/secInDay);
		result = result.concat(isNotNull(result, day, "d "));
		int hour = (int) ((sec - years*secInYear - month*secInMonth - day*secInDay)/secInHour);
		result = result.concat(isNotNull(result, hour, "h "));
		int minute = (int) ((sec - years*secInYear - month*secInMonth - day*secInDay - hour*secInHour)/secInMinute);
		result = result.concat(isNotNull(result, minute, "m "));
		int second = (int) ((sec - years*secInYear - month*secInMonth - day*secInDay - hour*secInHour - minute*secInMinute));
		result = result.concat(isNotNull(result, second, "s"));
    	return result;
		
	}

	private String isNotNull(String result, int years, String f) {
		
		if(Math.floor(years)!=0){
			return years+f;
		}
		return "";
	}

	public List<Issue> getAllIssuesInCurrentProject(Long currentProjectId)
    {
        final  JqlQueryBuilder builder = JqlQueryBuilder.newBuilder();
        builder.where().project(currentProjectId);
        Query query = (Query) builder.buildQuery();
        try
        {
            final SearchResults results = searchService.search(authenticationContext.getLoggedInUser(),
                     query, PagerFilter.getUnlimitedFilter());
            return results.getIssues();
        }
        catch (SearchException e)
        {
        	log.error("Error when get issues", e);
        }
        
        return Collections.emptyList();
    }
	
	private String issuesToTable(List<Issue> issues){
		String tableHtml = "<table border =\"2\"  align=\"center\" cellpadding=\"7\" cellspacing=\"7\">"
				+ "<tr>"
			+"<th align=\"left\">Worklog</th>"
			+"<th align=\"left\">Time spent</th>"
			+"<th align=\"left\">Issue Summary</th>"
		+"</tr>";
		Iterator<Issue> it = issues.iterator();
    	while(it.hasNext()){
    		Issue current = it.next();
    		String link = getIssuesPathUrl(current);
    		List<Worklog> worklog = getWorklog(current);
    		link = worklog.get(0).getUpdated().toString();
    		worklog.get(0).getUpdateAuthorFullName();
	    	if(current.getTimeSpent()!=null){
	    		long sec = current.getTimeSpent();
	    		String result = formatTime(sec);
	    		tableHtml = tableHtml.concat(addRawToTable(link,current.getSummary(), result));
    		}
	    	else{
	    		tableHtml = tableHtml.concat(addRawToTable(link, current.getSummary(), this.issueTimespentNull));
	    	}
    	}
    	tableHtml = tableHtml.concat("</table>");
		return tableHtml;
	}
	
	public Issue getIssueById(List<Issue> issues,Long issueId){
		Iterator<Issue> issueIterator = issues.iterator();
		while(issueIterator.hasNext()){
			Issue current = issueIterator.next();
			if(current.getId().equals(issueId)){
				return current;
			}
		}
		return null;
	}
	
	private List<Worklog> getWorklog(List<Issue> issues, Long issueId) {
		Issue issue = this.getIssueById(issues, issueId);
		return this.getWorklog(issue);
	}
	
	private List<Worklog> getWorklog(Issue issue) {
		List<Worklog> worklog = worlogManager.getByIssue(issue);
		return worklog;
	}
	
	public String getIssueWorklogReportURL(Long projectId, Long issueId){
		String url = ComponentManager.getInstance().getApplicationProperties().getString("jira.baseurl");
		url = url.concat("/secure/ConfigureReport.jspa?selectedProjectId="+projectId+"&reportKey=com.vrpinc.jira.issueTimingPlugin%3Aissue-timing-report&Next=Next");
		url = url.concat("&issueId="+issueId.toString());
		return url;
	}
	
	public String getIssuesPathUrl(Issue issue) {
		String link = ComponentManager.getInstance().getApplicationProperties().getString("jira.baseurl");
		link = link.concat(this.issuePathUrl);
		link = link.concat(issue.getKey());
		return link;
	}

	private String addRawToTable(String link, String summary, String result) {
		String rawHtml = "";
		rawHtml = rawHtml.concat("<tr>");
		rawHtml = rawHtml.concat("<td >"+link+"</td>");
		rawHtml = rawHtml.concat("<td >"+result+"</td>");
		rawHtml = rawHtml.concat("<td ><a href=\""+link+"\" target=\"_blank\">"+summary+"</a></td>");
		rawHtml = rawHtml.concat("</tr>");
		return rawHtml;
	}

}
