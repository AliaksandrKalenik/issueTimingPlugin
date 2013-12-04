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

 
public class IssueTimingReport extends AbstractReport{
    
	private static final Logger log = Logger.getLogger(IssueTimingReport.class);
    private final JiraAuthenticationContext authenticationContext;
    private final SearchService searchService;
    public static final String IF_ISSUE_ID_IS_NULL = "Worklog";
    public static final String ISSUE_PATH_URL = "/browse/";
    public static final long ISSUE_ID_IF_NOT_SELECT = -1;
    public static final String ISSUE_SUMMARY_TIME_IS_NULL = "0s";
	private WorklogManager worlogManager;
	
    public IssueTimingReport(final JiraAuthenticationContext authenticationContext,
                                            final SearchService searchService, 
                                            WorklogManager worklogManager)
    {
    	this.worlogManager = worklogManager;
        this.authenticationContext = authenticationContext;
        this.searchService = searchService;
    }

	public String generateReportHtml(ProjectActionSupport projectActionSupport, Map params) throws PermissionException{
		Long currentProjectId = getCurrentProjectId(params);
		List<Issue> issues = this.getAllIssuesInCurrentProject(currentProjectId);
		Long issueId = getSelectedIssueId(params);
		
		 //worklogs = null;		
		//if(issueId != null){
			//worklogs = getWorklog(issues.get(0));
		List<Worklog> worklogs = getWorklog(issues, issueId);
		//}
		final Map startingParams;
		startingParams = EasyMap.build(
                "projectId", currentProjectId,
				"issues", issues,
                "issueId", issueId,
                "worklogs", worklogs,
                "issueTimingReport", this); 
		return descriptor.getHtml("view", startingParams);
    	
    }



	private Long getSelectedIssueId(Map params) {
		Long issueId;
		try{
			issueId = Long.valueOf(TextUtils.htmlEncode((String) params.get("issueId")));
		}catch(Exception e){
			issueId = ISSUE_ID_IF_NOT_SELECT;
		}
		return issueId;
	}

	private Long getCurrentProjectId(Map params) {
		Long currentProjectId = Long.valueOf(TextUtils.htmlEncode((String) params.get("selectedProjectId")));
		return currentProjectId;
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
		if(issueId == ISSUE_ID_IF_NOT_SELECT){
			return null;
		}
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
		link = link.concat(this.ISSUE_PATH_URL);
		link = link.concat(issue.getKey());
		return link;
	}

	
	public String getSummaryTime(Issue issue){
		if(issue.getTimeSpent() != null){
			return TimeFormatter.formatTime(issue.getTimeSpent());
		}
		return ISSUE_SUMMARY_TIME_IS_NULL;
	}
	
	public String getWorklogDate(Worklog worklog){
		return worklog.getUpdated().toString();
	}
	
	public String getWorklogDuration(Worklog worklog){
		return TimeFormatter.formatTime(worklog.getTimeSpent());
	}
	
	public String getWorklogComment(Worklog worklog){
		return worklog.getComment();
	}
	
	public String getWorklogAuthor(Worklog worklog){
		return worklog.getUpdateAuthor();
	}
	
	public String getIssueSummary(List<Issue> issues, Long issueId){
		if(issueId == ISSUE_ID_IF_NOT_SELECT){
			return this.IF_ISSUE_ID_IS_NULL;
		}
		return this.getIssueById(issues, issueId).getSummary();
	}
	
	static class TimeFormatter{
		
	    public static String formatTime(final long sec) {
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
	
		private static String isNotNull(String result, int years, String f) {
			if(Math.floor(years)!=0){
				return years+f;
			}
			return "";
		}
	}

}
