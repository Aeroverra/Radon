/*
 * Copyright (C) 2018 ItzSomebody
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.itzsomebody.radon.transformers.renamer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import me.itzsomebody.radon.transformers.AbstractTransformer;
import me.itzsomebody.radon.utils.BytecodeUtils;
import me.itzsomebody.radon.utils.LoggerUtils;
import me.itzsomebody.radon.utils.NumberUtils;
import me.itzsomebody.radon.utils.StringUtils;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Transformer that renames classes and their members.
 * TODO: Figure out why this doesn't work with TestingProject
 *
 * @author ItzSomebody
 */
public class Renamer extends AbstractTransformer {
    /**
     * Used for class, method and field renamming. Format is oldName -> newName.
     */
    private Map<String, String> mappings = new HashMap<>();

    /**
     * Used to determine how the classes interact with each other.
     */
    private Map<String, ClassTree> hierarchy = new HashMap<>();

    /**
     * Indication to look for Bukkit/Bungee main methods.
     */
    private boolean spigotMode;

    /**
     * Constructor used to create a {@link Renamer} object.
     */
    public Renamer(boolean spigotMode) {
        this.spigotMode = spigotMode;
    }

    /**
     * Applies obfuscation.
     */
    public void obfuscate() {
        this.logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
        this.logStrings.add(LoggerUtils.stdOut("Starting renamer transformer"));
        this.logStrings.add(LoggerUtils.stdOut("Generating mappings."));
        this.createTrees();
        long current = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger();
        this.classNodes().forEach(classNode -> {
            classNode.methods.stream().filter(methodNode -> !Modifier.isNative(methodNode.access)
                    && !methodNode.name.equals("main")
                    && !methodNode.name.equals("premain")
                    && !methodNode.name.startsWith("<")
                    && !methodNode.name.contains("lambda")).forEach(methodNode -> {
                if (this.weCanRenameMethod(methodNode)) {
                    String newName = StringUtils.randomString(this.dictionary);
                    this.renameMethodTree(new ArrayList<>(), methodNode, classNode.name, newName);
                }
            });

            classNode.fields.forEach(fieldNode -> {
                if (this.weCanRenameField(fieldNode)) {
                    String newName = StringUtils.randomString(this.dictionary);
                    this.renameFieldTree(new ArrayList<>(), fieldNode, classNode.name, newName);
                }
            });

            if (!this.exempted(classNode.name, "Renamer")) {
                int packages = NumberUtils.getRandomInt(2) + 1;
                StringBuilder newName = new StringBuilder();
                for (int i = 0; i < packages; i++) {
                    newName.append(StringUtils.randomString(this.dictionary)).append('/');
                }

                this.mappings.put(classNode.name, newName.substring(0, newName.length() - 1));
                counter.incrementAndGet();
            }
        });
        this.logStrings.add(LoggerUtils.stdOut("Finished generated mappings. [" +
                String.valueOf(System.currentTimeMillis() - current) + "ms]"));
        this.logStrings.add(LoggerUtils.stdOut("Applying mappings."));
        current = System.currentTimeMillis();

        // Apply mapping
        Remapper simpleRemapper = new SimpleRemapper(this.mappings);
        for (ClassNode classNode : new ArrayList<>(this.classNodes())) {
            ClassNode copy = new ClassNode();
            classNode.accept(new ClassRemapper(copy, simpleRemapper));
            copy.access = BytecodeUtils.accessFixer(copy.access);
            for (MethodNode methodNode : copy.methods) {
                methodNode.access = BytecodeUtils.accessFixer(methodNode.access);
            }

            if (copy.fields != null) {
                for (FieldNode fieldNode : copy.fields) {
                    fieldNode.access = BytecodeUtils.accessFixer(fieldNode.access);
                }
            }

            this.getClassMap().remove(classNode.name);
            this.getClassPathMap().put(copy.name, copy);
            this.getClassMap().put(copy.name, copy);
        }

        this.logStrings.add(LoggerUtils.stdOut("Mapped " + counter + " members."));
        current = System.currentTimeMillis();

        // Fix screw ups in resources.
        this.logStrings.add(LoggerUtils.stdOut("Attempting to map class names in resources"));
        AtomicInteger fixed = new AtomicInteger();
        passThru.forEach((name, byteArray) -> {
            if (name.equals("META-INF/MANIFEST.MF")
                    || (name.equals("plugin.yml") && spigotMode)) {
                String stringVer = new String(byteArray);
                for (String mapping : mappings.keySet()) {
                    if (stringVer.contains(mapping.replace("/", "."))) {
                        stringVer = stringVer.replace(mapping.replace("/", "."), mappings.get(mapping).replace("/", "."));
                    }
                }

                try {
                    passThru.put(name, stringVer.getBytes("UTF-8"));
                    fixed.incrementAndGet();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e.getMessage());
                }
            }
        });
        this.logStrings.add(LoggerUtils.stdOut("Mapped " + fixed + " names in resources. [" + tookThisLong(current) + "ms]"));
        this.logStrings.add(LoggerUtils.stdOut("Finished applying mappings. [" +
                String.valueOf(System.currentTimeMillis() - current) + "ms]"));
    }

    /**
     * Creates {@link ClassTree}s needed for renaming.
     */
    private void createTrees() {
        long current = System.currentTimeMillis();
        this.logStrings.add(LoggerUtils.stdOut("Creating class hierarchy."));
        this.getClassPathMap().values().forEach(classNode -> {
            classNode.methods.forEach(methodNode -> {
                methodNode.owner = classNode.name;
            });
            classNode.fields.forEach(fieldNode -> {
                fieldNode.owner = classNode.name;
            });
            ClassTree classTree = new ClassTree(classNode.name, classNode.libraryNode);
            classTree.parentClasses.add(classNode.superName);
            classTree.parentClasses.addAll(classNode.interfaces);
            this.classNodes().stream().filter(node -> node.superName.equals(classNode.name)
                    || node.interfaces.contains(classNode.name)).forEach(node -> {
                classTree.subClasses.add(node.name);
            });

            classTree.methods.addAll(classNode.methods);
            classTree.fields.addAll(classNode.fields);
            this.hierarchy.put(classNode.name, classTree);
        });
        this.logStrings.add(LoggerUtils.stdOut("Finished creating class hierarchy. [" + tookThisLong(current) + "ms]"));
    }

    /**
     * Ultimately determines if we can rename a method without running into errors.
     *
     * @param methodNode {@link MethodNode} we want to check if we can rename without causing the JVM to spit lots of errors.
     * @return true if we can rename a method without running into errors.
     */
    private boolean weCanRenameMethod(MethodNode methodNode) {
        for (ClassTree ct : this.hierarchy.values()) {
            if (ct.subClasses.contains(methodNode.owner)
                    && this.isLibInheritedMN(new ArrayList<>(), methodNode, methodNode.owner)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to determine if the method we input is inherited from a library class or is exempted.
     *
     * @param visited    a list of {@link ClassTree}s which contain the {@link MethodNode}s we already checked.
     * @param methodNode {@link MethodNode} we want to check if we can rename without causing the JVM to spit lots of errors.
     * @param className  the name of the class we want to check
     * @return true if the method we input is inherited from a library class.
     */
    private boolean isLibInheritedMN(List<ClassTree> visited, MethodNode methodNode, String className) {
        ClassTree ct = this.hierarchy.get(className);
        if (ct == null)
            throw new RuntimeException(className + " doesn't exist in classpath.");
        if (!visited.contains(ct)) {
            visited.add(ct);
            if (!methodNode.owner.equals(className)) {
                for (MethodNode mn : ct.methods) {
                    if (mn.name.equals(methodNode.name) && mn.desc.equals(methodNode.desc)) {
                        if (ct.libraryNode) {
                            return true;
                        }
                    }
                }
            }
            if (exempted(className + '.' + methodNode.name + methodNode.desc, "Renamer")) {
                return true;
            }
            for (String parentClass : ct.parentClasses) {
                if (parentClass != null
                        && this.isLibInheritedMN(visited, methodNode, parentClass)) {
                    return true;
                }
            }

            for (String subClass : ct.subClasses) {
                if (subClass != null
                        && this.isLibInheritedMN(visited, methodNode, subClass)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Renames the methods in an inheritance tree to prevent inheritance errors.
     *
     * @param visited    a list of {@link ClassTree}s which contain the {@link MethodNode}s we already renamed.
     * @param methodNode the method information.
     * @param className  the class we are currently browsing through.
     * @param newName    the new name of the method.
     */
    private void renameMethodTree(List<ClassTree> visited, MethodNode methodNode, String className, String newName) {
        ClassTree ct = this.hierarchy.get(className);
        if (ct == null)
            throw new RuntimeException(className + " doesn't exist in classpath.");
        if (!ct.libraryNode && !visited.contains(ct)) {
            mappings.put(className + '.' + methodNode.name + methodNode.desc, newName);
            visited.add(ct);
            for (String parentClass : ct.parentClasses) {
                this.renameMethodTree(visited, methodNode, parentClass, newName);
            }
            for (String subClass : ct.subClasses) {
                this.renameMethodTree(visited, methodNode, subClass, newName);
            }
        }
    }

    /**
     * Ultimately determines if we can rename a field without running into errors.
     *
     * @param fieldNode {@link FieldNode} we want to check if we can rename without causing the JVM to spit lots of errors.
     * @return true if we can rename a field without running into errors.
     */
    private boolean weCanRenameField(FieldNode fieldNode) {
        for (ClassTree ct : this.hierarchy.values()) {
            if (ct.subClasses.contains(fieldNode.owner)
                    && this.isLibInheritedFN(new ArrayList<>(), fieldNode, fieldNode.owner)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Attempts to determine if the method we input is inherited from a library class or is exempted.
     *
     * @param fieldNode {@link FieldNode} we want to check if we can rename without causing the JVM to spit lots of errors.
     * @param className the name of the class we want to check
     * @return true if the method we input is inherited from a library class.
     */
    private boolean isLibInheritedFN(List<ClassTree> visited, FieldNode fieldNode, String className) {
        ClassTree ct = this.hierarchy.get(className);
        if (ct == null)
            throw new RuntimeException(className + " doesn't exist in classpath.");
        if (!visited.contains(ct)) {
            visited.add(ct);
            if (!fieldNode.owner.equals(className)) {
                for (MethodNode mn : ct.methods) {
                    if (mn.name.equals(fieldNode.name) && mn.desc.equals(fieldNode.desc)) {
                        if (ct.libraryNode) {
                            return true;
                        }
                    }
                }
            }
            if (exempted(className + '.' + fieldNode.name, "Renamer")) {
                return true;
            }
            for (String parentClass : ct.parentClasses) {
                if (parentClass != null
                        && this.isLibInheritedFN(visited, fieldNode, parentClass)) {
                    return true;
                }
            }

            for (String subClass : ct.subClasses) {
                if (subClass != null
                        && this.isLibInheritedFN(visited, fieldNode, subClass)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Renames the methods in an inheritance tree to prevent inheritance errors.
     *
     * @param visited   a list of {@link ClassTree}s which contain the
     *                  {@link MethodNode}s we already renamed.
     * @param fieldNode the method information.
     * @param className the class we are currently browsing through.
     * @param newName   the new name of the method.
     */
    private void renameFieldTree(List<ClassTree> visited, FieldNode fieldNode,
                                 String className, String newName) {
        ClassTree ct = this.hierarchy.get(className);
        if (ct == null)
            throw new RuntimeException(className + " doesn't exist in classpath.");
        if (!ct.libraryNode && !visited.contains(ct)) {
            this.mappings.put(className + '.' + fieldNode.name, newName);
            visited.add(ct);
            for (String parentClass : ct.parentClasses) {
                this.renameFieldTree(visited, fieldNode, parentClass, newName);
            }
            for (String subClass : ct.subClasses) {
                this.renameFieldTree(visited, fieldNode, subClass, newName);
            }
        }
    }
}