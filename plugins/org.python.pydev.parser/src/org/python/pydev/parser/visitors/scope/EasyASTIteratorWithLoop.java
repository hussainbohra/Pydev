package org.python.pydev.parser.visitors.scope;

import java.util.Iterator;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.While;

public class EasyASTIteratorWithLoop extends EasyAstIteratorBase {
    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorBase#visitFor(org.python.pydev.parser.jython.ast.For)
     */
    public Object visitFor(For node) throws Exception {
        ASTEntry entry = before(node);
        parents.push(entry);
        traverse(node);
        parents.pop();
        after(entry);
        
        return null;
    }

    /** 
     * @see org.python.pydev.parser.jython.ast.VisitorWhile#visitFor(org.python.pydev.parser.jython.ast.While)
     */
    public Object visitWhile(While node) throws Exception {
        ASTEntry entry = before(node);
        parents.push(entry);
        traverse(node);
        parents.pop();
        after(entry);
        
        return null;

    }
 
    /**
     * @return an iterator for class and method definitions
     */
    public Iterator<ASTEntry> getForAndWhileIterator() {
        return getIterator(new Class[]{For.class, While.class});
    }

    /**
     * Creates the iterator and transverses the passed root so that the results can be gotten.
     */
    public static EasyASTIteratorWithLoop create(SimpleNode root){
        EasyASTIteratorWithLoop visitor = new EasyASTIteratorWithLoop();
        try {
            root.accept(visitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor;
    }

}
