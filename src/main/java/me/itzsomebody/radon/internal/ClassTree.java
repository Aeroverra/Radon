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

package me.itzsomebody.radon.internal;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Specifies subclasses and parents of a class.
 *
 * @author ItzSomebody
 */
public class ClassTree {
    /**
     * Class name.
     */
    public final String className;

    /**
     * Attached class node.
     */
    public ClassNode classNode;

    /**
     * Set of classes that inherit this class.
     */
    public final Set<String> subClasses = new HashSet<>();

    /**
     * Set of classes that this class inherits.
     */
    public final Set<String> parentClasses = new HashSet<>();

    /**
     * Indication of this class being a library (external) class.
     */
    public boolean libraryNode;

    /**
     * Constructor to make a {@link ClassTree} object.
     *
     * @param className name to assign to this {@link ClassTree}.
     */
    public ClassTree(String className, boolean libraryNode) {
        this.className = className;
        this.libraryNode = libraryNode;
    }
}
