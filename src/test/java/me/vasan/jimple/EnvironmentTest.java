package me.vasan.jimple;

import me.vasan.jimple.Environment;
import org.junit.Assert;
import org.junit.Test;

public class EnvironmentTest {
    @Test
    public void testEnv() {
        Environment parentEnv = new Environment();
        Environment childEnv = new Environment(parentEnv);
        parentEnv.put("hello", 23);
        parentEnv.put("world", 32);
        childEnv.put("hello", 32);
        Assert.assertNotNull(parentEnv);
        Assert.assertNotNull(childEnv);
        Assert.assertTrue(parentEnv.exists("hello"));
        Assert.assertTrue(childEnv.exists("hello"));
        Assert.assertEquals(childEnv.get("world"), 32);
        Assert.assertEquals(childEnv.get("hello"), 32);
    }

    @Test
    public void testEnv2() {
        Environment parentEnv = new Environment();
        Environment childEnv = new Environment(parentEnv);
        parentEnv.put("hello", 23);
        parentEnv.put("world", 32);
        childEnv.put("hello", 32);
        Assert.assertNotNull(parentEnv);
        Assert.assertNotNull(childEnv);
        Assert.assertTrue(parentEnv.exists("hello"));
        Assert.assertTrue(childEnv.exists("hello"));
        Assert.assertEquals(childEnv.get("world"), 32);
        Assert.assertEquals(childEnv.get("hello"), 32);
    }
}
