package com.vrpinc.jira.issueTimingPlugin.reports;

import com.atlassian.query.Query;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.jira.web.bean.PagerFilter;
import com.opensymphony.util.TextUtils;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

 
public class IssueTimingReport extends AbstractReport
{
    private static final Logger log = Logger.getLogger(IssueTimingReport.class);
    private final JiraAuthenticationContext authenticationContext;
    private final SearchService searchService;
 
    public IssueTimingReport(final JiraAuthenticationContext authenticationContext,
                                            final SearchService searchService)
    {
        this.authenticationContext = authenticationContext;
        this.searchService = searchService;
    }
    
	public String generateReportHtml(ProjectActionSupport projectActionSupport, Map params) throws Exception {
		String message ="<h1>Report\n ";
		Long currentProjectId = getCurrentProjectId(params);
		message = message.concat("currentProjectId: "+currentProjectId+" \n");
		List<Issue> issues = this.getAllIssuesInCurrentProject(currentProjectId);
		message = message.concat("summary: "+ issues.size()+ " ");
		Iterator<Issue> it = issues.iterator();
    	while(it.hasNext()){
    		Issue current = it.next();
	    	if(current.getTimeSpent()!=null){
	    		long sec = current.getTimeSpent();
	    		String result = formatTime(sec);
	    		message = message.concat("Issue description: "+current.getSummary()+"\n");
	    		message = message.concat("Issue time spent: "+result+"\n");
    		}
    	}
    	message = message.concat("</h1>");
    	return message;
    }

	private Long getCurrentProjectId(Map params) {
		Long currentProjectId = Long.valueOf(TextUtils.htmlEncode((String) params.get("selectedProjectId")));
		return currentProjectId;
	}
	
    private String formatTime(final long sec) {
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
        	
        }
        
        return Collections.emptyList();
    }
}
