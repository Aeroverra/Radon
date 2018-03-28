package me.itzsomebody.radon.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import me.itzsomebody.radon.utils.CustomRegexUtils;

import java.util.*;

/**
 * Abstract class used to make transformers.
 *
 * @author ItzSomebody
 */
public abstract class AbstractTransformer implements Opcodes {
    /**
     * The classes in the input.
     */
    private Map<String, ClassNode> classes;

    /**
     * The almighty classpath.
     */
    private Map<String, ClassNode> classPath;

    /**
     * Exempt information.
     */
    private List<String> exempts;

    /**
     * Logged strings from transformer console output.
     */
    protected List<String> logStrings;

    /**
     * Init method.
     *
     * @param classes the classes.
     * @param exempts exempt information.
     */
    public void init(Map<String, ClassNode> classes,
                     List<String> exempts) {
        this.classes = classes;
        this.exempts = exempts;
        this.logStrings = new ArrayList<>();
    }

    /**
     * The other init method.
     *
     * @param classes   the classes.
     * @param classPath the almighty classpath. (Bow down to it)
     * @param exempts   the exempted classes.
     */
    public void init(Map<String, ClassNode> classes,
                     Map<String, ClassNode> classPath,
                     List<String> exempts) {
        this.classes = classes;
        this.classPath = classPath;
        this.exempts = exempts;
        this.logStrings = new ArrayList<>();
    }

    /**
     * Returns {@link AbstractTransformer#classes}.
     *
     * @return {@link AbstractTransformer#classes}.
     */
    protected Map<String, ClassNode> getClassMap() {
        return this.classes;
    }

    /**
     * Returns {@link AbstractTransformer#classPath}.
     *
     * @return {@link AbstractTransformer#classPath}.
     */
    protected Map<String, ClassNode> getClassPathMap() {
        return this.classPath;
    }

    /**
     * Returns the values of {@link AbstractTransformer#classes}.
     *
     * @return the values of {@link AbstractTransformer#classes}.
     */
    protected Collection<ClassNode> classNodes() {
        return this.classes.values();
    }

    /**
     * Returns the keyset of {@link AbstractTransformer#classes}.
     *
     * @return the keyset of {@link AbstractTransformer#classes}.
     */
    protected Collection<String> classNames() {
        return this.classes.keySet();
    }

    /**
     * Returns a {@link Long} which indicates how long a transformer
     * processed the classes.
     *
     * @param started time started.
     * @return a {@link Long} which indicates how long a transformer
     * processed the classes
     */
    protected long tookThisLong(long started) {
        return System.currentTimeMillis() - started;
    }

    /**
     * Returns true if member is exempted from obfuscation.
     *
     * @param checkThis string to check for exempt.
     * @param exemptId  per-transformer exempt identifier.
     * @return true if member is exempted from obfuscation.
     */
    protected boolean exempted(String checkThis, String exemptId) {
        String exemptKey = exemptId + ": ";
        for (String exempt : this.exempts) {
            if (exempt.startsWith(exemptKey)) {
                if (CustomRegexUtils.isMatched(exempt.replace(exemptKey, ""), checkThis)) {
                    return true;
                }
            } else if (exempt.startsWith("Class: ")) {
                if (CustomRegexUtils.isMatched(exempt.replace("Class: ", ""), checkThis)) {
                    return true;
                }
            } else if (exempt.startsWith("Method: ")) {
                if (CustomRegexUtils.isMatched(exempt.replace("Method: ", ""), checkThis)) {
                    return true;
                }
            } else if (exempt.startsWith("Field: ")) {
                if (CustomRegexUtils.isMatched(exempt.replace("Field: ", ""), checkThis)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get's the current size of the method.
     *
     * @param methodNode the input method to evaluate the size of.
     * @return the current size of the method.
     */
    protected int methodSize(MethodNode methodNode) {
        CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
        methodNode.accept(cse);
        return cse.getMaxSize();
    }

    /**
     * Returns a {@link List} of {@link String}s that were outputted into the
     * console by transformer.
     *
     * @return a {@link List} of {@link String}s that were outputted into the
     * console by transformer.
     */
    public List<String> getLogStrings() {
        return this.logStrings;
    }

    /**
     * Obfuscation time.
     */
    public abstract void obfuscate();
}
