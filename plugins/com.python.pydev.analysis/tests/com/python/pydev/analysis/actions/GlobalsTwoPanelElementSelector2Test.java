package com.python.pydev.analysis.actions;

import org.eclipse.ui.dialogs.SearchPattern;

import com.python.pydev.analysis.additionalinfo.ClassInfo;

import junit.framework.TestCase;

public class GlobalsTwoPanelElementSelector2Test extends TestCase{

    public void testPatternMatch() throws Exception{
        SearchPattern patternMatcher = new SearchPattern();
        patternMatcher.setPattern("aa");
        
        ClassInfo info = new ClassInfo();
        info.name = "aa";
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.name = "aaa";
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.name = "baaa";
        assertFalse(MatchHelper.matchItem(patternMatcher, info));
        
        info.name = "aaa";
        info.moduleDeclared = "coi.foo";
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.name = "aaa";
        info.moduleDeclared = "invalid.foo";
        patternMatcher.setPattern("xx.aa");
        assertFalse(MatchHelper.matchItem(patternMatcher, info));
        
        info.moduleDeclared = "xx.foo";
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.moduleDeclared = "xx.foo.bar";
        patternMatcher.setPattern("xx.foo.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.moduleDeclared = "xx.foo.bar";
        patternMatcher.setPattern("xx.foo.bar.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.moduleDeclared = "xx.foo.bar";
        patternMatcher.setPattern("xx.foo.bar.aa.aa");
        assertFalse(MatchHelper.matchItem(patternMatcher, info));
        
        info.moduleDeclared = "xx.foo.bar";
        patternMatcher.setPattern("xx.foo.ba.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.moduleDeclared = "xx.foo.bar";
        patternMatcher.setPattern("xx.fo*o.ba.aa");
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        info.moduleDeclared = "coilib50.basic.native";
        info.name = "Intersection";
        patternMatcher.setPattern("coi*.intersection");
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        patternMatcher.setPattern("coilib50.intersection");
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
        
        patternMatcher.setPattern("coilib50.");
        assertTrue(MatchHelper.matchItem(patternMatcher, info));
    }
    
    public void testPatternSubAndEquals() throws Exception{
        assertFalse(MatchHelper.equalsFilter("aa", "aa "));
        
        assertTrue(MatchHelper.equalsFilter("aa", "aa"));
        assertFalse(MatchHelper.equalsFilter("aa.", "aa"));
        assertFalse(MatchHelper.equalsFilter("aa", "aa."));
        assertTrue(MatchHelper.equalsFilter("aa.", "aa."));
        
        assertTrue(MatchHelper.isSubFilter("aa.", "aa."));
        assertTrue(MatchHelper.isSubFilter("aa", "aab"));
        assertFalse(MatchHelper.isSubFilter("", "a"));
        assertFalse(MatchHelper.isSubFilter("a.", "a"));
        assertFalse(MatchHelper.isSubFilter("a.", "a.a"));
        assertTrue(MatchHelper.isSubFilter("a.a", "a.ab"));
        assertTrue(MatchHelper.isSubFilter("aa.b", "aa.ba"));
        assertFalse(MatchHelper.isSubFilter("a.", "a.ab"));
        assertFalse(MatchHelper.isSubFilter("a", "a.ab"));
    }
    
}
