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

package me.itzsomebody.radon.transformers.localvariables;

import java.util.concurrent.atomic.AtomicInteger;
import me.itzsomebody.radon.transformers.AbstractTransformer;
import me.itzsomebody.radon.utils.LoggerUtils;
import me.itzsomebody.radon.utils.StringUtils;

/**
 * Transformer that applies a local variable obfuscation by changing the names.
 *
 * @author ItzSomebody
 */
public class ObfuscateLocalVariables extends AbstractTransformer {
    /**
     * Length of names to generate.
     */
    private static int len = 10;
    /**
     * Applies obfuscation.
     */
    public void obfuscate() {
        AtomicInteger counter = new AtomicInteger();
        long current = System.currentTimeMillis();
        this.logStrings.add(LoggerUtils.stdOut("------------------------------------------------"));
        this.logStrings.add(LoggerUtils.stdOut("Started local variable obfuscation transformer"));
        this.classNodes().stream().filter(classNode -> !this.exempted(classNode.name, "LocalVars")).forEach(classNode -> {
            classNode.methods.stream().filter(methodNode ->
                    !this.exempted(classNode.name + '.' + methodNode.name + methodNode.desc, "LocalVars")
                            && methodNode.localVariables != null).forEach(methodNode -> {
                methodNode.localVariables.forEach(localVariableNode -> {
                    localVariableNode.name = StringUtils.crazyString(len);
                    counter.incrementAndGet();
                });
            });
        });
        this.logStrings.add(LoggerUtils.stdOut("Obfuscated " + counter + " local variables."));
        this.logStrings.add(LoggerUtils.stdOut("Finished. [" + tookThisLong(current) + "ms]"));
    }
}
