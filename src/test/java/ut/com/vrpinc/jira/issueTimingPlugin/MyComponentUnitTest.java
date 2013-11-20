package ut.com.vrpinc.jira.issueTimingPlugin;

import org.junit.Test;
import com.vrpinc.jira.issueTimingPlugin.MyPluginComponent;
import com.vrpinc.jira.issueTimingPlugin.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}