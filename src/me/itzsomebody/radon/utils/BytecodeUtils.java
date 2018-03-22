package me.itzsomebody.radon.utils;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Utils used for operating on bytecode.
 *
 * @author ItzSomebody
 * @author Stringer dev team (who made {@link BytecodeUtils#genericType(Type)})
 * @author Caleb Whiting
 */
public class BytecodeUtils {
    /**
     * Returns the generic {@link Type}.
     *
     * @param type input {@link Type}.
     * @return the generic {@link Type}.
     */
    public static Type genericType(Type type) {
        Type newType;
        switch (type.getSort()) {
            case Type.OBJECT:
                newType = Type.getType(Object.class);
                break;
            default:
                newType = type;
                break;
        }
        return newType;
    }

    /**
     * Returns ICONST_0 or ICONST_1 based on
     * {@link NumberUtils#getRandomInt(int)}.
     *
     * @return ICONST_0 or ICONST_1 based on
     * {@link NumberUtils#getRandomInt(int)}.
     */
    public static InsnNode randTrueFalse() {
        return (NumberUtils.getRandomInt(2) == 1) ?
                new InsnNode(Opcodes.ICONST_1) : new InsnNode(Opcodes.ICONST_0);
    }

    /**
     * Returns true if input is a known ICONST else return false.
     *
     * @param ain the {@link AbstractInsnNode} to check.
     * @return true if input is a known ICONST else return false.
     */
    public static boolean isIConst(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();
        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
            return true;
        }

        return false;
    }

    /**
     * Check if the input {@link MethodNode} has the same name as another
     * method in the classpath.
     *
     * @param methodNode the {@link MethodNode} to check.
     * @param clazz      the {@link ClassNode} in which the input
     *                   {@link MethodNode} is contained.
     * @param classes    the class collection to check.
     * @return true if input {@link MethodNode} name and description match
     * another one in the classpath.
     */
    @Deprecated
    public static boolean hasSameMethod(MethodNode methodNode, ClassNode clazz,
                                        Collection<ClassNode> classes) {
        for (ClassNode classNode : classes) {
            if (classNode.name.equals(clazz.name)) continue;
            for (MethodNode method : classNode.methods) {
                if (methodNode.name.equals(method.name)) {
                    if (methodNode.desc.equals(method.desc)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if the input class is a main method.
     *
     * @param clazz      {@link ClassNode} to check for main methods.
     * @param spigotMode if obfuscator should consider the input
     *                   {@link ClassNode} as a Spigot/Bukkit/Bungee plugin.
     * @return true if the input {@link ClassNode} contains a main method,
     * false if not.
     */
    public static boolean isMain(ClassNode clazz, boolean spigotMode) {
        if (spigotMode) {
            if (clazz.superName.equals("org/bukkit/plugin/java/JavaPlugin")
                    || clazz.superName.equals("net/md_5/bungee/api/plugin/Plugin")) {
                return true;
            }
        }

        for (MethodNode methodNode : clazz.methods) {
            if (methodNode.name.equals("main")
                    || methodNode.name.equals("premain")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns access modifier without private or protected so that class
     * renaming works properly.
     *
     * @param access input access as {@link Integer}.
     * @return new {@link Integer} without restrictive flags.
     */
    public static int accessFixer(int access) {
        int a = Opcodes.ACC_PUBLIC;
        if ((access & Opcodes.ACC_NATIVE) != 0) a |= Opcodes.ACC_NATIVE;
        if ((access & Opcodes.ACC_ABSTRACT) != 0) a |= Opcodes.ACC_ABSTRACT;
        if ((access & Opcodes.ACC_ANNOTATION) != 0) a |= Opcodes.ACC_ANNOTATION;
        if ((access & Opcodes.ACC_BRIDGE) != 0) a |= Opcodes.ACC_BRIDGE;
        if ((access & Opcodes.ACC_DEPRECATED) != 0) a |= Opcodes.ACC_DEPRECATED;
        if ((access & Opcodes.ACC_ENUM) != 0) a |= Opcodes.ACC_ENUM;
        if ((access & Opcodes.ACC_FINAL) != 0) a |= Opcodes.ACC_FINAL;
        if ((access & Opcodes.ACC_INTERFACE) != 0) a |= Opcodes.ACC_INTERFACE;
        if ((access & Opcodes.ACC_MANDATED) != 0) a |= Opcodes.ACC_MANDATED;
        if ((access & Opcodes.ACC_MODULE) != 0) a |= Opcodes.ACC_MODULE;
        if ((access & Opcodes.ACC_OPEN) != 0) a |= Opcodes.ACC_OPEN;
        if ((access & Opcodes.ACC_STATIC) != 0) a |= Opcodes.ACC_STATIC;
        if ((access & Opcodes.ACC_STATIC_PHASE) != 0)
            a |= Opcodes.ACC_STATIC_PHASE;
        if ((access & Opcodes.ACC_STRICT) != 0) a |= Opcodes.ACC_STRICT;
        if ((access & Opcodes.ACC_SUPER) != 0) a |= Opcodes.ACC_SUPER;
        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0)
            a |= Opcodes.ACC_SYNCHRONIZED;
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) a |= Opcodes.ACC_SYNTHETIC;
        if ((access & Opcodes.ACC_TRANSIENT) != 0) a |= Opcodes.ACC_TRANSIENT;
        if ((access & Opcodes.ACC_VARARGS) != 0) a |= Opcodes.ACC_VARARGS;
        if ((access & Opcodes.ACC_VOLATILE) != 0) a |= Opcodes.ACC_VOLATILE;
        return a;
    }

    /**
     * Returns true if input is a return opcode, else false.
     *
     * @param ain {@link AbstractInsnNode} to check if return opcode.
     * @return true if input is a return opcode, else false.
     */
    public static boolean isReturn(AbstractInsnNode ain) {
        int opcode = ain.getOpcode();
        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
            return true;
        }

        return false;
    }

    /**
     * Returns an {@link InsnList} with bytecode instructions for expiration.
     *
     * @param expiryTime a {@link Long} representation of the expiration date.
     * @return an {@link InsnList} with bytecode instructions for expiration.
     */
    public static InsnList returnExpiry(long expiryTime, String expiredMsg) {
        InsnList expiryCode = new InsnList();
        LabelNode injectedLabel = new LabelNode(new Label());

        expiryCode.add(new TypeInsnNode(Opcodes.NEW, "java/util/Date"));
        expiryCode.add(new InsnNode(Opcodes.DUP));
        expiryCode.add(new LdcInsnNode(expiryTime));
        expiryCode.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "java/util/Date", "<init>", "(J)V", false));
        expiryCode.add(new TypeInsnNode(Opcodes.NEW, "java/util/Date"));
        expiryCode.add(new InsnNode(Opcodes.DUP));
        expiryCode.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "java/util/Date", "<init>", "()V", false));
        expiryCode.add(new InsnNode(Opcodes.SWAP));
        expiryCode.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "java/util/Date", "after", "(Ljava/util/Date;)Z", false));
        expiryCode.add(new JumpInsnNode(Opcodes.IFEQ, injectedLabel));
        expiryCode.add(new TypeInsnNode(Opcodes.NEW, "java/lang/Throwable"));
        expiryCode.add(new InsnNode(Opcodes.DUP));
        expiryCode.add(new LdcInsnNode(expiredMsg));
        expiryCode.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "java/lang/Throwable", "<init>",
                "(Ljava/lang/String;)V", false));
        expiryCode.add(new InsnNode(Opcodes.ATHROW));
        expiryCode.add(injectedLabel);

        return expiryCode;
    }

    /**
     * Returns a bytecode instruction representing a number.
     *
     * @param number the {@link Integer} for the obfuscator to contemplate.
     * @return a bytecode instruction representing a number.
     */
    public static AbstractInsnNode getNumberInsn(int number) {
        if (number >= -1 && number <= 5) {
            return new InsnNode(number + 3);
        } else if (number >= -128 && number <= 127) {
            return new IntInsnNode(Opcodes.BIPUSH, number);
        } else if (number >= -32768 && number <= 32767) {
            return new IntInsnNode(Opcodes.SIPUSH, number);
        } else {
            return new LdcInsnNode(number);
        }
    }

    /**
     * Returns true if access has native modifier.
     *
     * @param access method access to check.
     * @return true if access has native modifier.
     */
    public static boolean isNativeMethod(int access) {
        return (access & Opcodes.ACC_NATIVE) != 0;
    }

    /**
     * Returns true if access has abstract modifier.
     *
     * @param access method access to check.
     * @return true if access has abstract modifier.
     */
    public static boolean isAbstractMethod(int access) {
        return (access & Opcodes.ACC_ABSTRACT) != 0;
    }

    /**
     * Returns true if access has synthetic modifier.
     *
     * @param access method access to check.
     * @return true if access has abstract modifier.
     */
    public static boolean isSyntheticMethod(int access) {
        return (access & Opcodes.ACC_SYNTHETIC) != 0;
    }

    /**
     * Returns true if access has bride modifier.
     *
     * @param access method access to check.
     * @return true if access has abstract modifier.
     */
    public static boolean isBridgeMethod(int access) {
        return (access & Opcodes.ACC_BRIDGE) != 0;
    }

    /**
     * Returns true if input is an integer instruction.
     *
     * @param insn {@link AbstractInsnNode} to check.
     * @return true if input is an integer instruction.
     */
    public static boolean isNumberNode(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5
                || insn.getOpcode() == Opcodes.BIPUSH
                || insn.getOpcode() == Opcodes.SIPUSH
                || insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer);
    }

    /**
     * Returns {@link Integer} represented by bytecode instruction and/or
     * operand.
     *
     * @param insn {@link AbstractInsnNode} to check.
     * @return {@link Integer} represented by bytecode instruction and/or
     * operand.
     */
    public static int getNumber(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
            return opcode - 3;
        } else if (insn instanceof IntInsnNode
                && insn.getOpcode() != Opcodes.NEWARRAY) {
            return ((IntInsnNode) insn).operand;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer) {
            return (Integer) ((LdcInsnNode) insn).cst;
        }

        throw new IllegalStateException("Unexpected instruction");
    }

    /**
     * Determines if description is a primitive int.
     *
     * @param desc the description to check.
     * @return true if description is a primitive int.
     */
    public static boolean isPrimitiveType(String desc) {
        String rawDesc = desc.replace("[", "");
        return (rawDesc.equals("I")
                || rawDesc.equals("B")
                || rawDesc.equals("C")
                || rawDesc.equals("S")
                || rawDesc.equals("J")
                || rawDesc.equals("Z")
                || rawDesc.equals("F")
                || rawDesc.equals("D"));
    }

    /**
     * Checks if input methodNode contains goto opcode.
     *
     * @param methodNode input methodNode.
     * @return true if input methodNode contains goto opcode.
     */
    public static boolean containsGoto(MethodNode methodNode) {
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insn = methodNode.instructions.get(i);
            if (insn instanceof JumpInsnNode && insn.getOpcode() == Opcodes.GOTO) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if provided {@link ClassNode} contains a {@link MethodNode}
     * based on info given.
     *
     * @param name      name to check.
     * @param desc      description to check.
     * @param classNode classNode to look in.
     * @return true if provided {@link ClassNode} contains a
     * {@link MethodNode} based on info given.
     */
    public static boolean containsMethod(String name, String desc,
                                         ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals(name) && methodNode.desc.equals(desc)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if provided {@link ClassNode} contains a {@link FieldNode}
     * based on info given.
     *
     * @param name      name to check.
     * @param desc      description to check.
     * @param classNode classNode to look in.
     * @return true if provided {@link ClassNode} contains a
     * {@link FieldNode} based on info given.
     */
    public static boolean containsField(String name, String desc,
                                        ClassNode classNode) {
        if (classNode.fields != null) {
            for (FieldNode fieldNode : classNode.fields) {
                if (fieldNode.name.equals(name) && fieldNode.desc.equals(desc)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns true if last not-label and not-line number instruction is an iinc.
     *
     * @param insn {@link AbstractInsnNode} to use as the current opcode.
     * @return true if last not-label and not-line number instruction is an iinc.
     */
    public static boolean lastInsnIInc(AbstractInsnNode insn) {
        AbstractInsnNode before = insn.getPrevious();
        if (before instanceof LabelNode || before instanceof LineNumberNode) {
            do {
                if (before == null) return false;
                before = before.getPrevious();
            }
            while (!(before instanceof LabelNode)
                    && !(before instanceof LineNumberNode));
        }

        return (before instanceof IincInsnNode);
    }
}
